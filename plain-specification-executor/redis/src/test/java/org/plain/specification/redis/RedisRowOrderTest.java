package org.plain.specification.redis;

import org.junit.jupiter.api.Test;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which ORDER BY shapes the ZSET index can serve, and what happens to the ones it cannot.
 */
class RedisRowOrderTest {

    static class Row {
        private int score;
        private String name;

        Row(int score, String name) {
            this.score = score;
            this.name = name;
        }

        public int getScore() { return score; }
        public String getName() { return name; }
    }

    private static ISpecification<Row> spec() {
        return new Specification<Row>();
    }

    @Test
    void noOrderingIsIndexOrder() {
        RedisRowOrder<Row> order = RedisRowOrder.of(spec(), "score");
        assertTrue(order.indexOrderIsRequested());
        assertFalse(order.descending());
    }

    @Test
    void orderingOnTheDeclaredScoreFieldIsIndexOrder() {
        ISpecification<Row> ascending = Specification.where(Row::getName).eq("a").orderBy(Row::getScore).build();
        RedisRowOrder<Row> order = RedisRowOrder.of(ascending, "score");
        assertTrue(order.indexOrderIsRequested(), "score order is the index order");
        assertFalse(order.descending());

        ISpecification<Row> descending = Specification.where(Row::getName).eq("a")
                .orderByDescending(Row::getScore).build();
        RedisRowOrder<Row> reversed = RedisRowOrder.of(descending, "score");
        assertTrue(reversed.indexOrderIsRequested());
        assertTrue(reversed.descending());
    }

    @Test
    void orderingOnAnotherFieldNeedsAnInMemorySort() {
        ISpecification<Row> byName = Specification.where(Row::getName).eq("a").orderBy(Row::getName).build();
        RedisRowOrder<Row> order = RedisRowOrder.of(byName, "score");
        assertFalse(order.indexOrderIsRequested());
        assertFalse(order.descending());
    }

    @Test
    void withoutADeclarationNothingIsIndexOrderExceptNoOrdering() {
        ISpecification<Row> byScore = Specification.where(Row::getName).eq("a").orderBy(Row::getScore).build();
        assertFalse(RedisRowOrder.of(byScore, null).indexOrderIsRequested(),
                "no scoreField declaration means the score carries an unknown value");
        assertTrue(RedisRowOrder.of(spec(), null).indexOrderIsRequested());
    }

    @Test
    void multipleOrderKeysAlwaysSortInMemory() {
        ISpecification<Row> composite = Specification.where(Row::getName).eq("a")
                .orderBy(Row::getScore).thenByDescending(Row::getName).build();
        assertFalse(RedisRowOrder.of(composite, "score").indexOrderIsRequested());
    }

    @Test
    void applyToSortsWhenTheIndexCannot() {
        List<Row> rows = new ArrayList<Row>(Arrays.asList(new Row(3, "c"), new Row(1, "a"), new Row(2, "b")));
        ISpecification<Row> byNameDesc = new Specification<Row>().query()
                .orderBy(Expressions.<Row>create().orderByDescending(Row::getName))
                .getSpecification();

        List<Row> sorted = RedisRowOrder.of(byNameDesc, "score").applyTo(rows);
        assertEquals(Arrays.asList("c", "b", "a"), names(sorted));
    }

    @Test
    void applyToLeavesIndexOrderedRowsAlone() {
        List<Row> rows = new ArrayList<Row>(Arrays.asList(new Row(3, "c"), new Row(1, "a")));
        ISpecification<Row> byScore = new Specification<Row>().query()
                .orderBy(Expressions.<Row>create().orderBy(Row::getScore))
                .getSpecification();

        List<Row> untouched = RedisRowOrder.of(byScore, "score").applyTo(rows);
        assertEquals(Arrays.asList(3, 1), scores(untouched), "the index order is the requested order");
    }

    @Test
    void nullScoreFieldNeverClaimsIndexOrder() {
        ISpecification<Row> byScore = Specification.where(Row::getName).eq("a").orderBy(Row::getScore).build();
        assertFalse(RedisRowOrder.of(byScore, "").indexOrderIsRequested());
    }

    @Test
    void declaringANonNumericScoreFieldClaimsNothing() {
        ISpecification<Row> byName = Specification.where(Row::getName).eq("a").orderBy(Row::getName).build();
        assertFalse(RedisRowOrder.of(byName, "name").indexOrderIsRequested(),
                "a ZSET score is a double and cannot carry the order of a text property");
    }

    private static List<String> names(List<Row> rows) {
        List<String> out = new ArrayList<String>(rows.size());
        for (Row row : rows) {
            out.add(row.getName());
        }
        return out;
    }

    private static List<Integer> scores(List<Row> rows) {
        List<Integer> out = new ArrayList<Integer>(rows.size());
        for (Row row : rows) {
            out.add(row.getScore());
        }
        return out;
    }
}
