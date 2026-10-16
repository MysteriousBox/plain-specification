package org.plain.specification.redis.pushdown;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Executes a {@link NarrowPlan} inside Redis: one round trip per ZSET index window. The script
 * walks the index slice in score order, reads only the HASH fields the plan references, and returns
 * the ids that the plan cannot rule out.
 *
 * <p>The script is read-only and never touches the key layout: entity keys are derived from the
 * configured HASH prefix, which is the same rule {@code BaseRedisRepository#getKey(String)} uses.
 * Note that derived keys are only valid on a single-shard deployment; see the module notes.</p>
 */
@Slf4j
public class RedisNarrowScanner {

    private static final String OP_NULLABLE = "20";
    private static final int MAX_INTEGER_DIGITS = 64;

    /**
     * KEYS: [zsetKey]
     * ARGV: [hashKeyPrefix, windowStart, windowEnd, direction, fieldCount, fields..,
     * clauseCount, (condCount, (fieldIndex, opCode, operandCount, operands..)..)..]
     * Returns: the ids in the window that survive the narrowing, in index order.
     */
    private static final String LUA_NARROW_WINDOW =
            "local zkey = KEYS[1]\n"
                    + "local prefix = ARGV[1]\n"
                    + "local wstart = tonumber(ARGV[2])\n"
                    + "local wend = tonumber(ARGV[3])\n"
                    + "local reversed = ARGV[4] == 'desc'\n"
                    + "local cursor = 5\n"
                    + "local nfields = tonumber(ARGV[cursor]); cursor = cursor + 1\n"
                    + "local fields = {}\n"
                    + "for i = 1, nfields do fields[i] = ARGV[cursor]; cursor = cursor + 1 end\n"
                    + "local nclauses = tonumber(ARGV[cursor]); cursor = cursor + 1\n"
                    + "local clauses = {}\n"
                    + "for c = 1, nclauses do\n"
                    + "  local nconds = tonumber(ARGV[cursor]); cursor = cursor + 1\n"
                    + "  local conds = {}\n"
                    + "  for k = 1, nconds do\n"
                    + "    local fi = tonumber(ARGV[cursor]); cursor = cursor + 1\n"
                    + "    local op = tonumber(ARGV[cursor]); cursor = cursor + 1\n"
                    + "    local no = tonumber(ARGV[cursor]); cursor = cursor + 1\n"
                    + "    local ops = {}\n"
                    + "    for q = 1, no do ops[q] = ARGV[cursor]; cursor = cursor + 1 end\n"
                    + "    conds[k] = { f = fi + 1, op = op, o = ops }\n"
                    + "  end\n"
                    + "  clauses[c] = conds\n"
                    + "end\n"
                    // canonical signed integer string -> { isNegative, digits } or nil when not canonical
                    + "local function norm(s)\n"
                    + "  if type(s) ~= 'string' or #s == 0 or #s > " + MAX_INTEGER_DIGITS + " then return nil end\n"
                    + "  local neg = false\n"
                    + "  local body = s\n"
                    + "  local first = s:sub(1, 1)\n"
                    + "  if first == '-' then neg = true body = s:sub(2)\n"
                    + "  elseif first == '+' then return nil end\n"
                    + "  if #body == 0 then return nil end\n"
                    + "  for i = 1, #body do\n"
                    + "    local b = body:byte(i)\n"
                    + "    if b < 48 or b > 57 then return nil end\n"
                    + "  end\n"
                    + "  body = body:gsub('^0+', '')\n"
                    + "  if #body == 0 then neg = false body = '0' end\n"
                    + "  return { neg, body }\n"
                    + "end\n"
                    // exact integer comparison on canonical strings: no double precision loss
                    + "local function icmp(x, y)\n"
                    + "  if x[1] ~= y[1] then if x[1] then return -1 else return 1 end end\n"
                    + "  local a = x[2] local b = y[2] local r\n"
                    + "  if #a ~= #b then r = (#a > #b) and 1 or -1\n"
                    + "  elseif a == b then r = 0\n"
                    + "  else r = (a < b) and -1 or 1 end\n"
                    + "  if x[1] then return -r end\n"
                    + "  return r\n"
                    + "end\n"
                    + "local function inList(v, ops)\n"
                    + "  for i = 1, #ops do if v == ops[i] then return true end end\n"
                    + "  return false\n"
                    + "end\n"
                    + "local function intOp(op, v, ops)\n"
                    // absent or empty: the rebuilt value is null or a primitive default, undecided
                    + "  if v == false or v == '' then return true end\n"
                    + "  local x = norm(v)\n"
                    + "  if not x then return true end\n"
                    + "  if op == 3 or op == 4 or op == 1 or op == 2 then\n"
                    + "    local found = false\n"
                    + "    for i = 1, #ops do\n"
                    + "      local b = norm(ops[i])\n"
                    + "      if not b then return true end\n"
                    + "      if icmp(x, b) == 0 then found = true break end\n"
                    + "    end\n"
                    + "    if op == 1 then return found end\n"
                    + "    if op == 2 then return not found end\n"
                    + "    if op == 3 then return found end\n"
                    + "    return not found\n"
                    + "  end\n"
                    + "  local b = norm(ops[1])\n"
                    + "  if not b then return true end\n"
                    + "  local c = icmp(x, b)\n"
                    + "  if op == 5 then return c > 0 end\n"
                    + "  if op == 6 then return c >= 0 end\n"
                    + "  if op == 7 then return c < 0 end\n"
                    + "  return c <= 0\n"
                    + "end\n"
                    + "local function strOp(op, v, ops)\n"
                    // a derived getter has no backing field: absent can never be treated as a value
                    + "  if v == false then return true end\n"
                    + "  if op == 11 then return v == ops[1] end\n"
                    + "  if op == 12 then return v ~= ops[1] end\n"
                    + "  if op == 13 then return inList(v, ops) end\n"
                    + "  if op == 14 then return not inList(v, ops) end\n"
                    + "  if op == 15 then return string.find(v, ops[1], 1, true) ~= nil end\n"
                    + "  if op == 16 then return v:sub(1, #ops[1]) == ops[1] end\n"
                    + "  local n = #ops[1]\n"
                    + "  if n == 0 then return true end\n"
                    + "  return v:sub(#v - n + 1) == ops[1]\n"
                    + "end\n"
                    // only an absent key or an empty string can still rebuild as a null reference
                    + "local function undecidedNull(v)\n"
                    + "  return v == false or v == ''\n"
                    + "end\n"
                    + "local function decide(cond, v)\n"
                    + "  local op = cond.op\n"
                    + "  if op == " + OP_NULLABLE + " then return undecidedNull(v) end\n"
                    + "  if op <= 8 then return intOp(op, v, cond.o) end\n"
                    + "  return strOp(op, v, cond.o)\n"
                    + "end\n"
                    + "local ids\n"
                    + "if reversed then\n"
                    + "  ids = redis.call('ZREVRANGE', zkey, wstart, wend)\n"
                    + "else\n"
                    + "  ids = redis.call('ZRANGE', zkey, wstart, wend)\n"
                    + "end\n"
                    + "if #clauses == 0 then return ids end\n"
                    + "local out = {}\n"
                    + "for i = 1, #ids do\n"
                    + "  local key = prefix .. ids[i]\n"
                    + "  local vals = {}\n"
                    + "  for f = 1, nfields do vals[f] = redis.call('HGET', key, fields[f]) end\n"
                    + "  for c = 1, #clauses do\n"
                    + "    local keep = true\n"
                    + "    local conds = clauses[c]\n"
                    + "    for k = 1, #conds do\n"
                    + "      local cond = conds[k]\n"
                    + "      if not decide(cond, vals[cond.f]) then keep = false break end\n"
                    + "    end\n"
                    + "    if keep then out[#out + 1] = ids[i] break end\n"
                    + "  end\n"
                    + "end\n"
                    + "return out\n";

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> NARROW_WINDOW_SCRIPT =
            RedisScript.of(LUA_NARROW_WINDOW, List.class);

    private final StringRedisTemplate redisTemplate;
    private final String zSetKey;
    private final String hashKeyPrefix;

    /**
     * @param redisTemplate template used for the scan
     * @param zSetKey       the ZSET index key holding entity ids
     * @param hashKeyPrefix prefix that turns an id into its entity HASH key
     */
    public RedisNarrowScanner(StringRedisTemplate redisTemplate, String zSetKey, String hashKeyPrefix) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        if (zSetKey == null || zSetKey.isEmpty()) {
            throw new IllegalArgumentException("zSetKey must not be empty");
        }
        if (hashKeyPrefix == null) {
            throw new IllegalArgumentException("hashKeyPrefix must not be null");
        }
        this.zSetKey = zSetKey;
        this.hashKeyPrefix = hashKeyPrefix;
    }

    /**
     * @return the prefix that turns a ZSET member into its entity HASH key
     */
    public String hashKeyPrefix() {
        return hashKeyPrefix;
    }

    /**
     * @return the number of ids currently in the index, used to bound the window loop
     */
    public long indexSize() {
        Long size = redisTemplate.opsForZSet().size(zSetKey);
        return size == null ? 0L : size;
    }

    /**
     * Run the narrowing over one index window.
     *
     * @param plan       the narrowing plan
     * @param start      inclusive 0-based index offset
     * @param end        inclusive 0-based index offset
     * @param descending true to walk the index high-score first
     * @return surviving ids in index order, never {@code null}
     */
    public List<String> scanWindow(NarrowPlan plan, long start, long end, boolean descending) {
        final List<String> args = plan.buildArgs(hashKeyPrefix, start, end, descending);
        final List<Object> raw = redisTemplate.execute(NARROW_WINDOW_SCRIPT,
                Collections.singletonList(zSetKey), args.toArray());
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> ids = new ArrayList<String>(raw.size());
        for (Object element : raw) {
            final String id = decode(element);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Script results arrive either pre-decoded or as raw bytes depending on the configured
     * serializers, so both shapes are accepted.
     */
    private static String decode(Object element) {
        if (element instanceof String) {
            return (String) element;
        }
        if (element instanceof byte[]) {
            return new String((byte[]) element, StandardCharsets.UTF_8);
        }
        if (element == null) {
            return null;
        }
        log.debug("Unexpected script result element {}: {}", element.getClass(), element);
        return String.valueOf(element);
    }
}
