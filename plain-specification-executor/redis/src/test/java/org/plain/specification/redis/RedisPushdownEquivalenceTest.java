package org.plain.specification.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.expression.NotExpression;
import org.plain.specification.core.expression.EqualExpression;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Differential test against a real Redis: every specification is executed twice over the same data,
 * once through the pushdown scan and once through the original batch scan, and the two answers must
 * agree exactly — same ids, same order, same total, and the same failure when the in-memory
 * predicate itself fails.
 *
 * <p>Run with {@code -Dplain.redis.pushdown.host=127.0.0.1 -Dplain.redis.pushdown.port=16379}.
 * Without those properties the whole class is skipped: it is the only place the Lua script's
 * behaviour is actually observed, so a green default build does not claim to have tested it.</p>
 */
@EnabledIfSystemProperty(named = "plain.redis.pushdown.host", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisPushdownEquivalenceTest {

    private static final int SEED_COUNT = 400;
    /** Above 2^53, where a double has a spacing of 1024 and adjacent longs are indistinguishable. */
    private static final long BIG_BASE = 9_000_000_000_000_000_000L;
    private static final Pattern CALLS = Pattern.compile("calls=(\\d+)");

    private static StringRedisTemplate template;
    private static LettuceConnectionFactory connectionFactory;
    private static String zSetKey;
    private static String hashPrefix;
    private static List<String> seededIds;

    /** Entity exercising the value domains the narrowing cares about. */
    public static class Doc {
        private String id;
        private String status;
        private String name;
        private Integer score;
        private long bigId;
        private String blob;
        private Float ratio;
        private float weird;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public long getBigId() { return bigId; }
        public void setBigId(long bigId) { this.bigId = bigId; }
        public String getBlob() { return blob; }
        public void setBlob(String blob) { this.blob = blob; }
        public Float getRatio() { return ratio; }
        public void setRatio(Float ratio) { this.ratio = ratio; }
        public float getWeird() { return weird; }
        public void setWeird(float weird) { this.weird = weird; }
    }

    /** Entity with a getter that has no backing field, stored by the reflection-free extractor. */
    public static class Lite {
        private String id;
        private String first;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFirst() { return first; }
        public void setFirst(String first) { this.first = first; }
        public String getFullName() { return first + "/" + id; }
    }

    @BeforeAll
    static void setUp() {
        String host = System.getProperty("plain.redis.pushdown.host", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("plain.redis.pushdown.port", "16379"));
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        connectionFactory = new LettuceConnectionFactory(standalone);
        connectionFactory.afterPropertiesSet();
        template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();

        String run = UUID.randomUUID().toString().substring(0, 8);
        zSetKey = "push:" + run + ":doc:index";
        hashPrefix = "push:" + run + ":doc:";
        seed();
    }

    @AfterAll
    static void tearDown() {
        if (template != null && seededIds != null) {
            List<String> keys = new ArrayList<String>(seededIds.size() + 1);
            keys.add(zSetKey);
            for (String id : seededIds) {
                keys.add(hashPrefix + id);
            }
            template.delete(keys);
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    private static void seed() {
        GenericRedisRepository<Doc, String> writer = repository(false);
        seededIds = new ArrayList<String>(SEED_COUNT);
        String[] statuses = {"ACTIVE", "BLOCKED", "PENDING"};
        Random random = new Random(20261016L);
        for (int i = 0; i < SEED_COUNT; i++) {
            Doc doc = new Doc();
            doc.setId("d" + i);
            seededIds.add(doc.getId());
            if (i % 7 == 0) {
                doc.setStatus(null);
            } else if (i % 11 == 0) {
                doc.setStatus("");
            } else {
                doc.setStatus(statuses[i % statuses.length]);
            }
            doc.setName("name-" + i + (i % 13 == 0 ? "-x" : ""));
            doc.setScore(i % 5 == 0 ? null : i % 500);
            // every bigId sits far above 2^53, where doubles have a spacing of 1024: all 400 values
            // collapse onto the same double, so a double-based ordering would prune real matches
            doc.setBigId(BIG_BASE + i);
            char[] filler = new char[220];
            Arrays.fill(filler, (char) ('a' + random.nextInt(26)));
            doc.setBlob(new String(filler));
            doc.setRatio(i * 0.1f);
            // a float whose stored form (via the Jackson round trip as a double) is not the string
            // the query value renders as: narrowing floats on strings would drop a real match
            doc.setWeird(i == 7 ? 1.5234153E-31f : 0.0f);
            writer.save(doc);
        }
    }

    /**
     * @param pushdown whether the repository may use the server-side narrowing
     * @return a repository over the shared test data
     */
    private static GenericRedisRepository<Doc, String> repository(boolean pushdown) {
        RedisRepositoryConfig<Doc, String> config = new RedisRepositoryConfig.Builder<Doc, String>()
                .resolvedZsetKey(zSetKey)
                .resolvedHashKeyPrefix(hashPrefix)
                .batchSize(50)
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Doc.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Doc.class, null))
                .scoreProvider(DefaultStrategies.defaultScoreProvider())
                .ttlProvider(DefaultStrategies.defaultTtlProvider())
                .fieldExtractor(DefaultStrategies.reflectionFieldExtractor())
                .entityBuilder(DefaultStrategies.reflectionEntityBuilder(Doc.class))
                .expressionPushdown(pushdown)
                .build();
        return new GenericRedisRepository<Doc, String>(template, config);
    }

    // ---- the specification battery ----

    private static Map<String, ISpecification<Doc>> battery() {
        Map<String, ISpecification<Doc>> specs = new LinkedHashMap<String, ISpecification<Doc>>();
        specs.put("eq status ACTIVE (string)", Specification.where(Doc::getStatus).eq("ACTIVE").build());
        specs.put("eq status '' (empty string)", Specification.where(Doc::getStatus).eq("").build());
        specs.put("eq status null", Specification.where(Doc::getStatus).eq((String) null).build());
        specs.put("isNull status", Specification.where(Doc::getStatus).isNull().build());
        specs.put("isNotNull status", Specification.where(Doc::getStatus).isNotNull().build());
        specs.put("neq status ACTIVE", Specification.where(Doc::getStatus).neq("ACTIVE").build());
        specs.put("in status", Specification.where(Doc::getStatus).in("ACTIVE", "BLOCKED").build());
        specs.put("in empty set", Specification.where(Doc::getStatus).in(new String[0]).build());
        specs.put("eq score 100", Specification.where(Doc::getScore).eq(100).build());
        specs.put("eq score 0 (no match)", Specification.where(Doc::getScore).eq(-1).build());
        specs.put("gte score 300", Specification.where(Doc::getScore).gte(300).build());
        specs.put("lte score 5", Specification.where(Doc::getScore).lte(5).build());
        specs.put("between score", Specification.where(Doc::getScore).between(100, 120).build());
        specs.put("gt score (null values -> throws)", Specification.where(Doc::getScore).gt(250).build());
        specs.put("eq bigId beyond 2^53", Specification.where(Doc::getBigId).eq(BIG_BASE + 17).build());
        specs.put("gt bigId beyond double spacing", Specification.where(Doc::getBigId).gt(BIG_BASE + 200).build());
        specs.put("gte bigId beyond double spacing", Specification.where(Doc::getBigId)
                .gte(BIG_BASE + 200).build());
        specs.put("lt bigId beyond double spacing", Specification.where(Doc::getBigId)
                .lt(BIG_BASE + 99).build());
        specs.put("between bigId", Specification.where(Doc::getBigId).between(
                BIG_BASE + 10, BIG_BASE + 12).build());
        specs.put("like contains", Specification.where(Doc::getName).like("%-12%").build());
        specs.put("like prefix", Specification.where(Doc::getName).like("name-1%").build());
        specs.put("like suffix", Specification.where(Doc::getName).like("%23").build());
        specs.put("like exact", Specification.where(Doc::getName).like("name-12").build());
        specs.put("like single-char wildcard", Specification.where(Doc::getName).like("name-1_-").build());
        specs.put("eq ratio (float, not narrowed)", Specification.where(Doc::getRatio).eq(0.0f).build());
        specs.put("eq weird (float whose stored text differs from the query text)",
                Specification.where(Doc::getWeird).eq(1.5234153E-31f).build());
        specs.put("and two narrowed leaves", Specification.where(Doc::getStatus).eq("ACTIVE")
                .and(Doc::getScore).eq(4).build());
        specs.put("or of two branches", Specification.where(Doc::getStatus).eq("PENDING")
                .or(Doc::getScore).eq(12).build());
        specs.put("and over or", Specification.where(Doc::getStatus).eq("ACTIVE")
                .or(Doc::getStatus).eq("BLOCKED")
                .and(Doc::getBigId).gte(BIG_BASE + 300).build());
        specs.put("not(eq) collapses", new Specification<Doc>().query().where(
                Expressions.<Doc>create().equal(Doc::getStatus, "ACTIVE")
                        .not(new NotExpression<>(new EqualExpression<>(Doc::getName, "name-1"))))
                .getSpecification());
        specs.put("empty specification", new Specification<Doc>());
        return specs;
    }

    @Test
    void findRangeMatchesTheBatchScan() {
        GenericRedisRepository<Doc, String> pushed = repository(true);
        GenericRedisRepository<Doc, String> baseline = repository(false);
        assertTrue(pushed.supportedExpressions().size() > 0, "pushdown must advertise the narrowed types");
        assertTrue(baseline.supportedExpressions().isEmpty(), "the default configuration must stay silent");

        for (Map.Entry<String, ISpecification<Doc>> entry : battery().entrySet()) {
            String pushedOutcome = outcome(() -> idsOf(pushed.findRange(entry.getValue())).toString());
            String baselineOutcome = outcome(() -> idsOf(baseline.findRange(entry.getValue())).toString());
            assertEquals(baselineOutcome, pushedOutcome, () -> entry.getKey() + " differs (order included)");
            if (pushedOutcome.startsWith("OK")) {
                List<String> pushedIds = Arrays.asList(pushedOutcome.substring(3).split(","));
                List<String> baselineIds = Arrays.asList(baselineOutcome.substring(3).split(","));
                assertNoSymmetricDifference(entry.getKey(), pushedIds, baselineIds);
            }
        }
    }

    @Test
    void countAndFindOneAndPageMatchTheBatchScan() {
        GenericRedisRepository<Doc, String> pushed = repository(true);
        GenericRedisRepository<Doc, String> baseline = repository(false);
        for (Map.Entry<String, ISpecification<Doc>> entry : battery().entrySet()) {
            String label = entry.getKey();
            ISpecification<Doc> spec = entry.getValue();

            assertEquals(outcome(() -> String.valueOf(baseline.count(spec))),
                    outcome(() -> String.valueOf(pushed.count(spec))), label + " -> count");

            assertEquals(outcome(() -> idOrNone(baseline.findOne(spec))),
                    outcome(() -> idOrNone(pushed.findOne(spec))), label + " -> findOne");

            for (PageQuery pageQuery : Arrays.asList(new PageQuery(1, 7), new PageQuery(3, 11), new PageQuery(99, 5))) {
                assertEquals(outcome(() -> render(baseline.page(spec, pageQuery))),
                        outcome(() -> render(pushed.page(spec, pageQuery))),
                        label + " -> page " + pageQuery.getPage());
            }
        }
    }

    private static String idOrNone(Doc doc) {
        return doc == null ? "<none>" : doc.getId();
    }

    @Test
    void executeMatchesFindRange() {
        GenericRedisRepository<Doc, String> pushed = repository(true);
        GenericRedisRepository<Doc, String> baseline = repository(false);
        for (Map.Entry<String, ISpecification<Doc>> entry : battery().entrySet()) {
            String pushedResult = outcome(() -> idsOf(pushed.execute(entry.getValue())).toString());
            String baselineResult = outcome(() -> idsOf(baseline.execute(entry.getValue())).toString());
            assertEquals(baselineResult, pushedResult, entry.getKey() + " -> execute");
        }
    }

    @Test
    void emptyIndexAndUnmappedFieldsBehaveTheSame() {
        // a ZSET that exists but holds nothing, and one that does not exist at all
        for (String suffix : new String[]{"empty", "missing"}) {
            RedisRepositoryConfig<Doc, String> pushedCfg = configFor(true,
                    DefaultStrategies.reflectionEntityBuilder(Doc.class));
            RedisRepositoryConfig<Doc, String> baselineCfg = configFor(false,
                    DefaultStrategies.reflectionEntityBuilder(Doc.class));
            GenericRedisRepository<Doc, String> pushed = repositoryWithSuffix(pushedCfg, suffix);
            GenericRedisRepository<Doc, String> baseline = repositoryWithSuffix(baselineCfg, suffix);
            for (Map.Entry<String, ISpecification<Doc>> entry : battery().entrySet()) {
                String pushedResult = outcome(() -> idsOf(pushed.findRange(entry.getValue())).toString());
                String baselineResult = outcome(() -> idsOf(baseline.findRange(entry.getValue())).toString());
                assertEquals(baselineResult, pushedResult, suffix + " / " + entry.getKey());
            }
            assertEquals("OK:[]", outcome(() -> idsOf(pushed.findRange(
                    Specification.where(Doc::getStatus).eq("ACTIVE").build())).toString()));
        }
    }

    /** Point a copy of the config at an unused key namespace. */
    private static GenericRedisRepository<Doc, String> repositoryWithSuffix(RedisRepositoryConfig<Doc, String> cfg,
                                                                           String suffix) {
        RedisRepositoryConfig<Doc, String> scoped = new RedisRepositoryConfig.Builder<Doc, String>()
                .resolvedZsetKey(zSetKey + ":" + suffix)
                .resolvedHashKeyPrefix(hashPrefix + ":" + suffix + ":")
                .batchSize(cfg.getBatchSize())
                .serializer(cfg.getSerializer())
                .deserializer(cfg.getDeserializer())
                .idExtractor(cfg.getIdExtractor())
                .scoreProvider(cfg.getScoreProvider())
                .ttlProvider(cfg.getTtlProvider())
                .fieldExtractor(cfg.getFieldExtractor())
                .entityBuilder(cfg.getEntityBuilder())
                .expressionPushdown(cfg.isExpressionPushdownEnabled())
                .build();
        return new GenericRedisRepository<Doc, String>(template, scoped);
    }

    /**
     * A getter with no backing field is invisible to the reflection-free extractor: the HASH has no
     * such key, so the narrowing must keep the candidate and let memory decide. Pruning on "field
     * absent" would silently drop these matches.
     */
    @Test
    void derivedGettersWithoutBackingFieldAreNeverDropped() {
        GenericRedisRepository<Lite, String> writer = liteRepository(false);
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i < 20; i++) {
            Lite lite = new Lite();
            lite.setId("l" + i);
            lite.setFirst(i % 2 == 0 ? "A" : "B");
            writer.save(lite);
            ids.add(lite.getId());
        }
        try {
            GenericRedisRepository<Lite, String> pushed = liteRepository(true);
            GenericRedisRepository<Lite, String> baseline = liteRepository(false);
            ISpecification<Lite> derivedEq = Specification.where(Lite::getFullName).eq("A/l0").build();
            ISpecification<Lite> derivedLike = Specification.where(Lite::getFullName).like("%/3").build();
            ISpecification<Lite> derivedIsNull = Specification.where(Lite::getFullName).isNull().build();
            ISpecification<Lite> backedField = Specification.where(Lite::getFirst).eq("A").build();

            assertEquals(1, baseline.findRange(derivedEq).size(), "the derived value does match in memory");
            for (ISpecification<Lite> spec : Arrays.asList(derivedEq, derivedLike, derivedIsNull, backedField)) {
                assertEquals(outcome(() -> liteIds(baseline.findRange(spec)).toString()),
                        outcome(() -> liteIds(pushed.findRange(spec)).toString()),
                        "derived getter: " + spec);
            }
        } finally {
            List<String> keys = new ArrayList<String>(ids.size() + 1);
            keys.add(zSetKey + ":lite");
            for (String id : ids) {
                keys.add(hashPrefix + ":lite:" + id);
            }
            template.delete(keys);
        }
    }

    private static GenericRedisRepository<Lite, String> liteRepository(boolean pushdown) {
        RedisRepositoryConfig<Lite, String> config = new RedisRepositoryConfig.Builder<Lite, String>()
                .resolvedZsetKey(zSetKey + ":lite")
                .resolvedHashKeyPrefix(hashPrefix + ":lite:")
                .batchSize(50)
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Lite.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Lite.class, null))
                .scoreProvider(DefaultStrategies.defaultScoreProvider())
                .ttlProvider(DefaultStrategies.defaultTtlProvider())
                .fieldExtractor(DefaultStrategies.fastReflectionFieldExtractor())
                .entityBuilder(DefaultStrategies.reflectionEntityBuilder(Lite.class))
                .expressionPushdown(pushdown)
                .build();
        return new GenericRedisRepository<Lite, String>(template, config);
    }

    private static List<String> liteIds(java.util.Collection<Lite> docs) {
        return docs.stream().map(Lite::getId).collect(Collectors.toList());
    }

    /**
     * Boundary demanded by the brief: HASH values whose types do not match the entity's declared
     * field type. These cannot come from the repository's own write path, so they are planted
     * directly — the point is that a narrowing which cannot decide them must keep them, and the
     * in-memory verifier then produces exactly what the batch scan produces (including a skip when
     * the value cannot be coerced at all).
     */
    @Test
    void typeInconsistentStoredValuesBehaveIdentically() {
        String zKey = zSetKey + ":weird";
        String hPrefix = hashPrefix + ":weird:";
        List<String> planted = new ArrayList<String>();
        // each row isolates one oddity; a null argument means the HASH key is not written at all
        plant(zKey, hPrefix, "w0", "007", "ACTIVE", "007", planted);
        plant(zKey, hPrefix, "w1", "+7", "", "-0", planted);
        plant(zKey, hPrefix, "w2", "", null, "9000000000000000001", planted);
        plant(zKey, hPrefix, "w3", null, "10", "9000000000000000000", planted);
        plant(zKey, hPrefix, "w4", "1.5", "true", "1e5", planted);
        plant(zKey, hPrefix, "w5", "abc", "0", "true", planted);
        plant(zKey, hPrefix, "w6", "7", "007", "", planted);
        plant(zKey, hPrefix, "w7", "-8", "-1", "9007199254740993", planted);
        List<String> keys = new ArrayList<String>();
        keys.add(zKey);
        keys.addAll(planted);
        try {
            GenericRedisRepository<Doc, String> pushed = repositoryAt(true, zKey, hPrefix);
            GenericRedisRepository<Doc, String> baseline = repositoryAt(false, zKey, hPrefix);
            List<ISpecification<Doc>> specs = Arrays.asList(
                    Specification.where(Doc::getScore).eq(7).build(),
                    Specification.where(Doc::getScore).eq(-8).build(),
                    Specification.where(Doc::getScore).eq(1).build(),
                    Specification.where(Doc::getScore).gt(5).build(),
                    Specification.where(Doc::getScore).lt(0).build(),
                    Specification.where(Doc::getScore).between(0, 100).build(),
                    Specification.where(Doc::getScore).isNull().build(),
                    Specification.where(Doc::getScore).in(7, 1).build(),
                    Specification.where(Doc::getScore).neq(7).build(),
                    Specification.where(Doc::getStatus).eq("ACTIVE").build(),
                    Specification.where(Doc::getStatus).eq("").build(),
                    Specification.where(Doc::getStatus).eq("007").build(),
                    Specification.where(Doc::getStatus).isNull().build(),
                    Specification.where(Doc::getStatus).like("%1%").build(),
                    Specification.where(Doc::getBigId).eq(7L).build(),
                    Specification.where(Doc::getBigId).eq(0L).build(),
                    Specification.where(Doc::getBigId).gt(9000000000000000000L).build(),
                    Specification.where(Doc::getBigId).lt(8L).build(),
                    Specification.where(Doc::getName).like("name-%").build());
            int matchedSomewhere = 0;
            for (int i = 0; i < specs.size(); i++) {
                ISpecification<Doc> spec = specs.get(i);
                String baselineResult = outcome(() -> idsOf(baseline.findRange(spec)).toString());
                String pushedResult = outcome(() -> idsOf(pushed.findRange(spec)).toString());
                assertEquals(baselineResult, pushedResult, "type-inconsistent values, spec #" + i);
                if (baselineResult.startsWith("OK:[") && !"OK:[]".equals(baselineResult)) {
                    matchedSomewhere++;
                }
            }
            assertTrue(matchedSomewhere >= 12, "the planted rows must actually produce matches, got " + matchedSomewhere);
            // an empty string is a String *value*, not a null reference: only the row without the
            // key at all reads back as null
            assertEquals("OK:[w2]", outcome(() ->
                    idsOf(baseline.findRange(Specification.where(Doc::getStatus).isNull().build())).toString()));
            assertEquals("OK:[w2, w3]", outcome(() ->
                    idsOf(baseline.findRange(Specification.where(Doc::getScore).isNull().build())).toString()));
        } finally {
            template.delete(keys);
        }
    }

    /** Write one HASH by hand, bypassing the repository write path. */
    private static void plant(String zKey, String prefix, String id, String score, String status,
                              String bigId, List<String> collectedKeys) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("id", id);
        fields.put("name", "name-" + id);
        fields.put("blob", "x");
        if (score != null) {
            fields.put("score", score);
        }
        if (status != null) {
            fields.put("status", status);
        }
        if (bigId != null) {
            fields.put("bigId", bigId);
        }
        String key = prefix + id;
        template.opsForHash().putAll(key, fields);
        template.opsForZSet().add(zKey, id, 0d);
        collectedKeys.add(key);
    }

    private static GenericRedisRepository<Doc, String> repositoryAt(boolean pushdown, String zKey, String prefix) {
        RedisRepositoryConfig<Doc, String> scoped = new RedisRepositoryConfig.Builder<Doc, String>()
                .resolvedZsetKey(zKey)
                .resolvedHashKeyPrefix(prefix)
                .batchSize(3)
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Doc.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Doc.class, null))
                .scoreProvider(DefaultStrategies.defaultScoreProvider())
                .ttlProvider(DefaultStrategies.defaultTtlProvider())
                .fieldExtractor(DefaultStrategies.reflectionFieldExtractor())
                .entityBuilder(DefaultStrategies.reflectionEntityBuilder(Doc.class))
                .expressionPushdown(pushdown)
                .build();
        return new GenericRedisRepository<Doc, String>(template, scoped);
    }

    @Test
    void reportsTheCostOfEachPath() {
        ISpecification<Doc> selective = Specification.where(Doc::getScore).eq(273).build();
        ISpecification<Doc> stringFilter = Specification.where(Doc::getStatus).eq("ACTIVE").build();
        StringBuilder report = new StringBuilder("\n");
        report.append(String.format("%-22s %10s %10s %12s %10s %8s%n",
                "path", "commands", "roundtrip", "bytes out", "builds", "ms"));
        for (String label : new String[]{"batch scan", "pushdown"}) {
            boolean pushdown = "pushdown".equals(label);
            for (Map.Entry<String, ISpecification<Doc>> entry :
                    Arrays.asList(new java.util.AbstractMap.SimpleEntry<>("score=273", selective),
                            new java.util.AbstractMap.SimpleEntry<>("status=ACTIVE", stringFilter))) {
                CountingTemplate counting = new CountingTemplate(template);
                MutableEntityBuilder<Doc> builder = new MutableEntityBuilder<Doc>(
                        DefaultStrategies.reflectionEntityBuilder(Doc.class));
                RedisRepositoryConfig<Doc, String> cfg = configFor(pushdown, builder);
                GenericRedisRepository<Doc, String> repo = new GenericRedisRepository<Doc, String>(counting, cfg);
                Properties before = info();
                long startNanos = System.nanoTime();
                List<Doc> result = new ArrayList<Doc>(repo.findRange(entry.getValue()));
                long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
                Properties after = info();
                // the trailing INFO itself is counted by the server, so take it back out
                long commands = deltaLong(after, before, "total_commands_processed") - 1L;
                long bytes = deltaLong(after, before, "total_net_output_bytes");
                // ZRANGE is a client command on the batch path but runs inside the script on the
                // pushdown path, where only the ZCARD that bounds the loop costs a round trip.
                long roundTrips = counting.roundTrips()
                        + (counting.usesScripts()
                        ? commandDelta(after, before, "zcard")
                        : commandDelta(after, before, "zrange"));
                report.append(String.format("%-22s %10d %10d %12d %10d %8d   %-14s matches=%d%n",
                        label, commands, roundTrips, bytes, builder.count, elapsedMillis,
                        entry.getKey(), result.size()));
            }
        }
        System.out.println(report);
    }

    private static RedisRepositoryConfig<Doc, String> configFor(boolean pushdown,
                                                                EntityBuilder<Doc> entityBuilder) {
        return new RedisRepositoryConfig.Builder<Doc, String>()
                .resolvedZsetKey(zSetKey)
                .resolvedHashKeyPrefix(hashPrefix)
                .batchSize(50)
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Doc.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Doc.class, null))
                .scoreProvider(DefaultStrategies.defaultScoreProvider())
                .ttlProvider(DefaultStrategies.defaultTtlProvider())
                .fieldExtractor(DefaultStrategies.reflectionFieldExtractor())
                .entityBuilder(entityBuilder)
                .expressionPushdown(pushdown)
                .build();
    }

    // ---- helpers ----

    private static List<String> idsOf(java.util.Collection<Doc> docs) {
        return docs.stream().map(Doc::getId).collect(Collectors.toList());
    }

    private static void assertNoSymmetricDifference(String label, List<String> pushed, List<String> baseline) {
        Set<String> a = new HashSet<String>(pushed);
        Set<String> b = new HashSet<String>(baseline);
        a.removeAll(b);
        b.clear();
        b.addAll(baseline);
        b.removeAll(pushed);
        assertTrue(a.isEmpty() && b.isEmpty(), label + " -> symmetric difference " + a + " / " + b);
    }

    private static String render(IPageResult<Doc> page) {
        return page.getTotal() + "|" + idsOf(page.getRecords());
    }

    /**
     * @return "OK:&lt;value&gt;" or "THROW:&lt;exception class&gt;", so a specification that fails in
     *         memory has to fail the same way through the pushdown path
     */
    private static String outcome(java.util.function.Supplier<String> supplier) {
        try {
            return "OK:" + supplier.get();
        } catch (RuntimeException ex) {
            return "THROW:" + ex.getClass().getName();
        }
    }

    private static Properties info() {
        return template.execute((org.springframework.data.redis.core.RedisCallback<Properties>)
                connection -> connection.info("ALL"));
    }

    private static long deltaLong(Properties after, Properties before, String key) {
        return Long.parseLong(after.getProperty(key, "0")) - Long.parseLong(before.getProperty(key, "0"));
    }

    private static long commandDelta(Properties after, Properties before, String command) {
        return calls(after, command) - calls(before, command);
    }

    private static long calls(Properties info, String command) {
        String value = info.getProperty("cmdstat_" + command);
        if (value == null) {
            return 0L;
        }
        Matcher matcher = CALLS.matcher(value);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
    }

    /** Counts client round trips: every non-pipelined command plus one per pipeline. */
    private static final class CountingTemplate extends StringRedisTemplate {
        private int pipelines;
        private int scripts;

        CountingTemplate(StringRedisTemplate source) {
            super(source.getConnectionFactory());
            afterPropertiesSet();
        }

        int roundTrips() {
            return pipelines + scripts;
        }

        boolean usesScripts() {
            return scripts > 0;
        }

        @Override
        public List<Object> executePipelined(SessionCallback<?> sessionCallback) {
            pipelines++;
            return super.executePipelined(sessionCallback);
        }

        @Override
        public <T> T execute(org.springframework.data.redis.core.script.RedisScript<T> script,
                             List<String> keys, Object... args) {
            scripts++;
            return super.execute(script, keys, args);
        }
    }

    /** Counts how many entities the read path materialises. */
    private static final class MutableEntityBuilder<T> implements EntityBuilder<T> {
        private final EntityBuilder<T> delegate;
        private int count;

        MutableEntityBuilder(EntityBuilder<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T buildEntity(Map<Object, Object> fields) {
            count++;
            return delegate.buildEntity(fields);
        }
    }
}
