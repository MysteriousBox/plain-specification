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
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.mybatisplus.config.AsyncConstant;
import org.plain.utils.converter.IConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Mybatis plus 基础 repository
 * 实现了常用的 数据库操作方法 保存、更新、删除等。
 * @param <T>
 */
@Slf4j
public abstract class MybatisPlusBaseRepositoryOfP<T,P,TID extends Serializable> implements IBaseRepository<T,TID> {

    protected final BaseMapper<P> baseMapper;
    protected final IConverter<T,P> converter;

    public MybatisPlusBaseRepositoryOfP(BaseMapper<P> baseMapper,
                                        IConverter<T, P> converter) {
        this.baseMapper = baseMapper;
        this.converter = converter;
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
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
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
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW)
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        return CompletableFuture.supplyAsync(()->{
            Collection<P> poList = converter.convert(entities);
            baseMapper.insert(poList);
            return converter.reverse(poList);
        });
    }

    @Override
    public void update(T entity) {
        P po = converter.convert(entity);
        baseMapper.updateById(po);
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(()->{
            try{
                P po = converter.convert(entity);
                baseMapper.updateById(po);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW)
    public CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(()->{
            try{
                Collection<P> poList = converter.convert(entities);
                baseMapper.updateById(poList);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
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
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    public CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(()->{
            try{
                P po = converter.convert(entity);
                baseMapper.deleteById(po);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW)
    public CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(()->{
            try{
                Collection<P> poList = converter.convert(entities);
                baseMapper.deleteByIds(poList);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW)
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.runAsync(()->{
            QueryWrapper<P> queryWrapper = compileToPo(specification);
            baseMapper.delete(queryWrapper);
        });
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
    public  CompletableFuture<T> findByIdAsync(TID id) {
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
        });
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
        return converter.reverse(po);
    }

    @Override
    public CompletableFuture<T> findOneAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> findOne(specification));
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
        });
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
        });
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
}
