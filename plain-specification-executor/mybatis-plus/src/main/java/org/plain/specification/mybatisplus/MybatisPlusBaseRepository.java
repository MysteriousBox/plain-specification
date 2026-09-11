package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.plain.specification.core.IBaseRepository;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.spi.ISpecificationExecutor;
import org.plain.specification.core.visitor.IExpressionVisitor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Class MybatisPlusBaseRepository.
 *
 * @author Jayden.Liang
 */
@SuppressWarnings({"AlibabaAbstractClassShouldStartWithAbstractNamingRule", "AbstractClassShouldStartWithAbstractNamingRule"})
public abstract class MybatisPlusBaseRepository<T, TID extends Serializable>
        implements IBaseRepository<T, TID>, ISpecificationExecutor<T> {

    protected final BaseMapper<T> baseMapper;
    private final Executor executor;
    private final TransactionTemplate transactionTemplate;

    public MybatisPlusBaseRepository(BaseMapper<T> baseMapper, Executor executor) {
        this.baseMapper = baseMapper;
        this.executor = executor;
        this.transactionTemplate = null;
    }

    public MybatisPlusBaseRepository(BaseMapper<T> baseMapper, Executor executor,
                                     PlatformTransactionManager transactionManager) {
        this.baseMapper = baseMapper;
        this.executor = executor;
        if (transactionManager != null) {
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        } else {
            this.transactionTemplate = null;
        }
    }

    @Override
    public T save(T entity) {
        baseMapper.insert(entity);
        return entity;
    }

    @Override
    public CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
                 baseMapper.insert(entity);
                 return entity;
        }, executor);
    }

    @Override
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            if (transactionTemplate != null) {
                return transactionTemplate.execute(status -> {
                    baseMapper.insert(entities);
                    return entities;
                });
            }
            baseMapper.insert(entities);
            return entities;
        }, executor);
    }

    @Override
    public void update(T entity) {
        baseMapper.updateById(entity);
    }

    @Override
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try{
                baseMapper.updateById(entity);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (transactionTemplate != null) {
                    transactionTemplate.executeWithoutResult(status -> {
                        baseMapper.updateById(entities);
                    });
                } else {
                    baseMapper.updateById(entities);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public void deleteById(TID id) {
        baseMapper.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<TID> ids) {
        baseMapper.deleteByIds(ids);
    }

    @Override
    public void delete(T entity) {
        baseMapper.deleteById(entity);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            try{
                baseMapper.deleteById(entity);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (transactionTemplate != null) {
                    transactionTemplate.executeWithoutResult(status -> {
                        baseMapper.deleteByIds(entities);
                    });
                } else {
                    baseMapper.deleteByIds(entities);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (transactionTemplate != null) {
                    transactionTemplate.executeWithoutResult(status -> {
                        QueryWrapper<T> wrapper = compile(specification);
                        baseMapper.delete(wrapper);
                    });
                } else {
                    QueryWrapper<T> wrapper = compile(specification);
                    baseMapper.delete(wrapper);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public T findById(TID id) {
        return baseMapper.selectById(id);
    }

    @Override
    public CompletableFuture<T> findByIdAsync(TID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return baseMapper.selectById(id);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public T findOne(ISpecification<T> specification) {
        QueryWrapper<T> queryWrapper = compile(specification);
        return baseMapper.selectOne(queryWrapper);
    }

    private QueryWrapper<T> compile(ISpecification<T> specification) {
        MybatisplusExpressionVisitor<T> tMybatisplusExpressionVisitor = new MybatisplusExpressionVisitor<>();
        QueryWrapper<T> queryWrapper = Wrappers.query();
        for (IExpressionDescriptor<T> whereExpression : specification.getWhereExpressions()) {
            queryWrapper = whereExpression.func(tMybatisplusExpressionVisitor);
        }
        for (OrderExpressionInfo<T> orderExpression : specification.getOrderExpressions()) {
            queryWrapper = orderExpression.func(tMybatisplusExpressionVisitor);
        }
        return queryWrapper;
    }

    @Override
    public CompletableFuture<T> findOneAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<T> queryWrapper = compile(specification);
            return baseMapper.selectOne(queryWrapper);
        }, executor);
    }

    @Override
    public Collection<T> findRange(ISpecification<T> specification) {
        QueryWrapper<T> queryWrapper = compile(specification);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public CompletableFuture<Collection<T>> findRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<T> queryWrapper = compile(specification);
            return baseMapper.selectList(queryWrapper);
        }, executor);
    }

    @Override
    public long count(ISpecification<T> specification) {
        QueryWrapper<T> queryWrapper = compile(specification);
        return baseMapper.selectCount(queryWrapper);
    }

    @Override
    public CompletableFuture<Long> countAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<T> queryWrapper = compile(specification);
            return baseMapper.selectCount(queryWrapper);
        }, executor);
    }

    @Override
    public IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery) {
        QueryWrapper<T> queryWrapper = compile(specification);
        Page<T> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        IPage<T> pageResult = baseMapper.selectPage(page, queryWrapper);
        return new PageResultAdapter<>(pageResult);
    }

    // -------------------------------------------------------------------------
    // ISpecificationExecutor SPI — MyBatis-Plus 支持全部表达式下推
    // -------------------------------------------------------------------------

    @Override
    public IExpressionVisitor<T, ?> getVisitor() {
        return new MybatisplusExpressionVisitor<>();
    }

    @Override
    public List<T> execute(ISpecification<T> specification) {
        return new ArrayList<>(findRange(specification));
    }

    @Override
    public IPageResult<T> execute(ISpecification<T> specification, PageQuery pageQuery) {
        return page(specification, pageQuery);
    }

    @SuppressWarnings("unchecked")
    private void addExpressionType(Set<Class<? extends IExpression<T>>> types, Class<?> clazz) {
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