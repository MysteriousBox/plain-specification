package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.plain.specification.core.IBaseRepository;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.*;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.spi.ISpecificationExecutor;
import org.plain.specification.core.visitor.IExpressionVisitor;
import org.plain.utils.converter.IConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Mybatis plus 基础 repository
 * 实现了常用的 数据库操作方法 保存、更新、删除等。
 * @param <T>
 * @author Jayden.Liang
 */
@SuppressWarnings({"AlibabaAbstractClassShouldStartWithAbstractNamingRule", "AbstractClassShouldStartWithAbstractNamingRule"})
@Slf4j
public abstract class MybatisPlusBaseRepositoryOfP<T,P,TID extends Serializable> implements IBaseRepository<T,TID>, ISpecificationExecutor<T> {

    protected final BaseMapper<P> baseMapper;
    protected final IConverter<T,P> converter;
    private final Executor executor;
    private final TransactionTemplate transactionTemplate;

    public MybatisPlusBaseRepositoryOfP(BaseMapper<P> baseMapper,
                                        IConverter<T, P> converter,
                                        Executor executor) {
        this.baseMapper = baseMapper;
        this.converter = converter;
        this.executor = executor;
        this.transactionTemplate = null;
    }

    public MybatisPlusBaseRepositoryOfP(BaseMapper<P> baseMapper,
                                        IConverter<T, P> converter,
                                        Executor executor,
                                        PlatformTransactionManager transactionManager) {
        this.baseMapper = baseMapper;
        this.converter = converter;
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
        P po = converter.convert(entity);
        baseMapper.insert(po);
        T result = converter.reverse(po);
        if (result == null){
            throw new IllegalStateException("Reverse converter return null");
        }
        return result;
    }

    @Override
    public CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            try{
                P po = converter.convert(entity);
                baseMapper.insert(po);
                T result = converter.reverse(po);
                if (result == null){
                    throw new IllegalStateException("Reverse converter return null");
                }
                return result;
            }catch (Exception e){
                log.error("Failed to save entity ",e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            if (transactionTemplate != null) {
                return transactionTemplate.execute(status -> {
                    Collection<P> poList = converter.convert(entities);
                    baseMapper.insert(poList);
                    return converter.reverse(poList);
                });
            }
            Collection<P> poList = converter.convert(entities);
            baseMapper.insert(poList);
            return converter.reverse(poList);
        }, executor);
    }

    @Override
    public void update(T entity) {
        P po = converter.convert(entity);
        baseMapper.updateById(po);
    }

    @Override
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(()->{
            try{
                P po = converter.convert(entity);
                baseMapper.updateById(po);
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
                        Collection<P> poList = converter.convert(entities);
                        baseMapper.updateById(poList);
                    });
                } else {
                    Collection<P> poList = converter.convert(entities);
                    baseMapper.updateById(poList);
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
        P po = converter.convert(entity);
        baseMapper.deleteById(po);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(()->{
            try{
                P po = converter.convert(entity);
                baseMapper.deleteById(po);
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
                        Collection<P> poList = converter.convert(entities);
                        baseMapper.deleteByIds(poList);
                    });
                } else {
                    Collection<P> poList = converter.convert(entities);
                    baseMapper.deleteByIds(poList);
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
                        QueryWrapper<P> queryWrapper = compileToPo(specification);
                        baseMapper.delete(queryWrapper);
                    });
                } else {
                    QueryWrapper<P> queryWrapper = compileToPo(specification);
                    baseMapper.delete(queryWrapper);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public T findById(TID id) {
        P po = baseMapper.selectById(id);
        if (po == null){
            return null;
        }
        return converter.reverse(po);
    }

    @Override
    public CompletableFuture<T> findByIdAsync(TID id) {
        return CompletableFuture.supplyAsync(()->{
            try {
                P po = baseMapper.selectById(id);
                if (po == null){
                    return null;
                }
                return converter.reverse(po);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private QueryWrapper<P> compileToPo(ISpecification<T> specification) {
        MybatisplusEntityToPoVisitor<T,P> tMybatisplusExpressionVisitor = new MybatisplusEntityToPoVisitor<>();
        QueryWrapper<P> queryWrapper = Wrappers.query();
        for (IExpressionDescriptor<T> whereExpression : specification.getWhereExpressions()) {
            queryWrapper = whereExpression.func(tMybatisplusExpressionVisitor);
        }
        for (OrderExpressionInfo<T> orderExpression : specification.getOrderExpressions()) {
            queryWrapper = orderExpression.func(tMybatisplusExpressionVisitor);
        }
        return queryWrapper;
    }

    @Override
    public T findOne(ISpecification<T> specification) {
        P po = baseMapper.selectOne(compileToPo(specification));
        if (po == null) {
            return null;
        }
        return converter.reverse(po);
    }

    @Override
    public CompletableFuture<T> findOneAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> findOne(specification), executor);
    }

    @Override
    public Collection<T> findRange(ISpecification<T> specification) {
        QueryWrapper<P> pQueryWrapper = compileToPo(specification);
        List<P> pList = baseMapper.selectList(pQueryWrapper);
        return converter.reverse(pList);
    }

    @Override
    public CompletableFuture<Collection<T>> findRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(()->{
            QueryWrapper<P> pQueryWrapper = compileToPo(specification);
            List<P> pList = baseMapper.selectList(pQueryWrapper);
            return converter.reverse(pList);
        }, executor);
    }

    @Override
    public long count(ISpecification<T> specification) {
        QueryWrapper<P> pQueryWrapper = compileToPo(specification);
        return baseMapper.selectCount(pQueryWrapper);
    }

    @Override
    public CompletableFuture<Long> countAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(()->{
            QueryWrapper<P> pQueryWrapper = compileToPo(specification);
            return baseMapper.selectCount(pQueryWrapper);
        }, executor);
    }

    @Override
    public IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery) {
        QueryWrapper<P> queryWrapper = compileToPo(specification);
        Page<P> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        IPage<P> pageResult = baseMapper.selectPage(page, queryWrapper);
        Collection<T> records = converter.reverse(pageResult.getRecords());
        Page<T> tPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        tPage.setRecords(new ArrayList<>(records));
        return new PageResultAdapter<>(tPage);
    }

    // -------------------------------------------------------------------------
    // ISpecificationExecutor SPI
    // -------------------------------------------------------------------------

    @Override
    public IExpressionVisitor<T, ?> getVisitor() {
        return new MybatisplusEntityToPoVisitor<>();
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
