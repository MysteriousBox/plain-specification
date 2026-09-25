package org.plain.specification.mybatisplus;

import org.plain.specification.core.IBaseRepository;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.spi.ISpecificationExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * MyBatis-Plus Repository 抽象基类，提取了两个实现的公共逻辑。
 * <p>
 * 子类只需实现同步 CRUD 方法、编译方法和抽象 hooks。
 * 异步批量操作的事务处理由本类统一管理。
 * </p>
 *
 * @param <T> 实体类型
 * @param <TID> ID 类型
 * @author Jayden.Liang
 */
public abstract class AbstractMybatisPlusRepository<T, TID extends Serializable>
        implements IBaseRepository<T, TID>, ISpecificationExecutor<T> {

    protected final Executor executor;
    protected final TransactionTemplate transactionTemplate;

    protected AbstractMybatisPlusRepository(Executor executor) {
        this.executor = executor;
        this.transactionTemplate = null;
    }

    protected AbstractMybatisPlusRepository(Executor executor, PlatformTransactionManager transactionManager) {
        this.executor = executor;
        if (transactionManager != null) {
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        } else {
            this.transactionTemplate = null;
        }
    }

    @Override
    public Executor getAsyncExecutor() {
        return executor;
    }

    // -------------------------------------------------------------------------
    // 抽象 hooks — 子类通过 mapper 实现
    // -------------------------------------------------------------------------

    protected abstract void doDeleteById(TID id);

    protected abstract void doDeleteByIds(Collection<TID> ids);

    protected abstract void doSaveBatch(Collection<T> entities);

    protected abstract void doUpdateBatch(Collection<T> entities);

    protected abstract void doDeleteBatch(Collection<T> entities);

    // -------------------------------------------------------------------------
    // IBaseRepository — deleteById / deleteByIds 委托给子类
    // -------------------------------------------------------------------------

    @Override
    public void deleteById(TID id) {
        doDeleteById(id);
    }

    @Override
    public void deleteByIds(Collection<TID> ids) {
        doDeleteByIds(ids);
    }

    // -------------------------------------------------------------------------
    // IBaseRepository — 事务性批量 async（覆盖接口 default）
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            if (transactionTemplate != null) {
                return transactionTemplate.execute(status -> {
                    doSaveBatch(entities);
                    return entities;
                });
            }
            doSaveBatch(entities);
            return entities;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(() -> {
            if (transactionTemplate != null) {
                transactionTemplate.executeWithoutResult(status -> doUpdateBatch(entities));
            } else {
                doUpdateBatch(entities);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(() -> {
            if (transactionTemplate != null) {
                transactionTemplate.executeWithoutResult(status -> doDeleteBatch(entities));
            } else {
                doDeleteBatch(entities);
            }
        }, executor);
    }

    // -------------------------------------------------------------------------
    // ISpecificationExecutor SPI
    // -------------------------------------------------------------------------

    @Override
    public List<T> execute(ISpecification<T> specification) {
        return new ArrayList<>(findRange(specification));
    }

    @Override
    public IPageResult<T> execute(ISpecification<T> specification, PageQuery pageQuery) {
        return page(specification, pageQuery);
    }

    @SuppressWarnings("unchecked")
    protected void addExpressionType(Set<Class<? extends IExpression<T>>> types, Class<?> clazz) {
        types.add((Class<? extends IExpression<T>>) clazz);
    }

    @Override
    public Set<Class<? extends IExpression<T>>> supportedExpressions() {
        Set<Class<? extends IExpression<T>>> types = new LinkedHashSet<>();
        addExpressionType(types, EqualExpression.class);
        addExpressionType(types, NotEqualExpression.class);
        addExpressionType(types, GreaterThanExpression.class);
        addExpressionType(types, LessThanExpression.class);
        addExpressionType(types, GreaterThanOrEqualExpression.class);
        addExpressionType(types, LessThanOrEqualExpression.class);
        addExpressionType(types, InExpression.class);
        addExpressionType(types, NotInExpression.class);
        addExpressionType(types, BetweenExpression.class);
        addExpressionType(types, LikeExpression.class);
        addExpressionType(types, IsNullExpression.class);
        addExpressionType(types, IsNotNullExpression.class);
        addExpressionType(types, AndExpression.class);
        addExpressionType(types, OrExpression.class);
        addExpressionType(types, NotExpression.class);
        addExpressionType(types, OrderExpression.class);
        return Collections.unmodifiableSet(types);
    }
}
