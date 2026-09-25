package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.visitor.IExpressionVisitor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * MyBatis-Plus Repository（直接映射，无 PO 转换）。
 *
 * @param <T> 实体类型
 * @param <TID> ID 类型
 * @author Jayden.Liang
 */
public abstract class MybatisPlusBaseRepository<T, TID extends Serializable>
        extends AbstractMybatisPlusRepository<T, TID> {

    protected final BaseMapper<T> baseMapper;

    public MybatisPlusBaseRepository(BaseMapper<T> baseMapper, Executor executor) {
        super(executor);
        this.baseMapper = baseMapper;
    }

    public MybatisPlusBaseRepository(BaseMapper<T> baseMapper, Executor executor,
                                     PlatformTransactionManager transactionManager) {
        super(executor, transactionManager);
        this.baseMapper = baseMapper;
    }

    // -------------------------------------------------------------------------
    // 抽象 hooks 实现
    // -------------------------------------------------------------------------

    @Override
    protected void doDeleteById(TID id) {
        baseMapper.deleteById(id);
    }

    @Override
    protected void doDeleteByIds(Collection<TID> ids) {
        baseMapper.deleteByIds(ids);
    }

    @Override
    protected void doSaveBatch(Collection<T> entities) {
        baseMapper.insert(entities);
    }

    @Override
    protected void doUpdateBatch(Collection<T> entities) {
        baseMapper.updateById(entities);
    }

    @Override
    protected void doDeleteBatch(Collection<T> entities) {
        baseMapper.deleteByIds(entities);
    }

    // -------------------------------------------------------------------------
    // 同步 CRUD
    // -------------------------------------------------------------------------

    @Override
    public T save(T entity) {
        baseMapper.insert(entity);
        return entity;
    }

    @Override
    public void update(T entity) {
        baseMapper.updateById(entity);
    }

    @Override
    public void delete(T entity) {
        baseMapper.deleteById(entity);
    }

    @Override
    public T findById(TID id) {
        return baseMapper.selectById(id);
    }

    @Override
    public T findOne(ISpecification<T> specification) {
        return baseMapper.selectOne(compile(specification));
    }

    @Override
    public Collection<T> findRange(ISpecification<T> specification) {
        return baseMapper.selectList(compile(specification));
    }

    @Override
    public long count(ISpecification<T> specification) {
        return baseMapper.selectCount(compile(specification));
    }

    @Override
    public IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery) {
        QueryWrapper<T> queryWrapper = compile(specification);
        Page<T> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        IPage<T> pageResult = baseMapper.selectPage(page, queryWrapper);
        return new PageResultAdapter<>(pageResult);
    }

    // -------------------------------------------------------------------------
    // 按条件删除（单条 SQL 优化）
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.runAsync(() -> {
            if (transactionTemplate != null) {
                transactionTemplate.executeWithoutResult(status -> baseMapper.delete(compile(specification)));
            } else {
                baseMapper.delete(compile(specification));
            }
        }, executor);
    }

    // -------------------------------------------------------------------------
    // 编译 + Visitor
    // -------------------------------------------------------------------------

    protected QueryWrapper<T> compile(ISpecification<T> specification) {
        MybatisPlusExpressionVisitor<T> visitor = new MybatisPlusExpressionVisitor<>();
        QueryWrapper<T> queryWrapper = Wrappers.query();
        for (IExpressionDescriptor<T> whereExpression : specification.getWhereExpressions()) {
            queryWrapper = whereExpression.func(visitor);
        }
        for (OrderExpressionInfo<T> orderExpression : specification.getOrderExpressions()) {
            queryWrapper = orderExpression.func(visitor);
        }
        return queryWrapper;
    }

    @Override
    public IExpressionVisitor<T, ?> getVisitor() {
        return new MybatisPlusExpressionVisitor<>();
    }
}
