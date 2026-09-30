package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.expression.WhereExpressionInfo;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验「getter 声明在实体基类上」这一类条件的列名解析。
 * <p>
 * {@code Role::getId} 这类方法引用在 lambda 元数据里只能看到声明类（基类），
 * 而映射是按具体实体类注册的；同时 MyBatis-Plus 把主键放在 {@code TableInfo} 的
 * keyProperty/keyColumn 而非 {@code getFieldList()}。两点叠加会让所有以主键为条件的
 * Specification 直接抛 {@code IllegalStateException: 未注册映射}。
 * </p>
 *
 * @author Jayden.Liang
 */
class MybatisPlusEntityToPoVisitorTest {

    @Getter
    @Setter
    static abstract class EntityBase<TID> {
        private TID id;
    }

    static class Order extends EntityBase<Long> {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class OrderPO {
        @TableId("id")
        private Long id;

        @TableField("name")
        private String name;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    @BeforeAll
    static void registerMapping() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrderPO.class);
        FieldMappingRegistrar.registerMapping(Order.class, OrderPO.class);
    }

    private static QueryWrapper<OrderPO> compile(Expressions<Order> expressions) {
        WhereExpressionInfo<Order> info = new WhereExpressionInfo<>(expressions, null);
        return info.func(new MybatisPlusEntityToPoVisitor<Order, OrderPO>(Order.class));
    }

    @Test
    void primaryKeyIsRegisteredEvenThoughItIsNotInTableFieldList() {
        assertEquals("id", FieldMappingRegistry.getColumn(Order.class, "id"));
    }

    @Test
    void inheritedGetterResolvesThroughTheDomainClass() {
        QueryWrapper<OrderPO> wrapper = compile(Expressions.<Order>create().in(Order::getId, Arrays.asList(1L, 2L)));

        String sql = wrapper.getTargetSql();
        assertNotNull(sql);
        assertTrue(sql.contains("id IN"), "实际 SQL: " + sql);
    }

    @Test
    void inheritedGetterResolvesInsideNestedConditions() {
        Expressions<Order> expressions = Expressions.<Order>create()
                .equal(Order::getName, "book")
                .and(Expressions.<Order>create().in(Order::getId, Arrays.asList(3L)).build());

        String sql = compile(expressions).getTargetSql();
        assertTrue(sql.contains("id IN"), "实际 SQL: " + sql);
        assertTrue(sql.contains("name ="), "实际 SQL: " + sql);
    }

    @Test
    void ownGetterStillResolvesWithoutADomainClass() {
        WhereExpressionInfo<Order> info = new WhereExpressionInfo<>(
                Expressions.<Order>create().equal(Order::getName, "book"), null);
        QueryWrapper<OrderPO> wrapper = info.func(new MybatisPlusEntityToPoVisitor<>());

        assertTrue(wrapper.getTargetSql().contains("name ="));
    }
}
