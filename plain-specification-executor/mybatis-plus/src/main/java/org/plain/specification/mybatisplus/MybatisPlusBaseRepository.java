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
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.mybatisplus.config.AsyncConstant;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Class MybatisPlusBaseRepository.
 *
 * @author Jayden.Liang
 */
@SuppressWarnings({"AlibabaAbstractClassShouldStartWithAbstractNamingRule", "AbstractClassShouldStartWithAbstractNamingRule"})
public abstract class MybatisPlusBaseRepository<T, TID extends Serializable>  implements IBaseRepository<T,TID> {

    protected final BaseMapper<T> baseMapper;

    public MybatisPlusBaseRepository(BaseMapper<T> baseMapper) {
        this.baseMapper = baseMapper;
    }

    @Override
    public T save(T entity) {
        baseMapper.insert(entity);
        return entity;
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    public CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
                 baseMapper.insert(entity);
                 return entity;
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        return CompletableFuture.supplyAsync(()->{
            baseMapper.insert(entities);
            return entities;
        });
    }

    @Override
    public void update(T entity) {
        baseMapper.updateById(entity);
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(()->{
            try{
                baseMapper.updateById(entity);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)
    public CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(()->{
            try{
                baseMapper.updateById(entities);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public  void deleteById(TID id) {
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
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    public CompletableFuture<Void> deleteAsync(T entity) {
        return CompletableFuture.runAsync(()->{
            try{
                baseMapper.deleteById(entity);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)
    public CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(()->{
            try{
                baseMapper.deleteByIds(entities);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    @Async(AsyncConstant.THREAD_POOL_TASK_EXECUTOR_NAME)
    @Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.runAsync(()->{
            try{
                QueryWrapper<T> wrapper = compile(specification);
                baseMapper.delete(wrapper);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public T findById(TID id) {
        return baseMapper.selectById(id);
    }

    @Override
    public  CompletableFuture<T> findByIdAsync(TID id) {
        return CompletableFuture.supplyAsync(()->{
            try {
                return baseMapper.selectById(id);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public T findOne(ISpecification<T> specification) {
        QueryWrapper<T> queryWrapper = compile(specification);
        return  baseMapper.selectOne(queryWrapper);
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

        return CompletableFuture.supplyAsync(()->{
            QueryWrapper<T> queryWrapper = compile(specification);
            return baseMapper.selectOne(queryWrapper);
        });
    }

    @Override
    public Collection<T> findRange(ISpecification<T> specification) {

        QueryWrapper<T> queryWrapper = compile(specification);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public CompletableFuture<Collection<T>> findRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(()->{
            QueryWrapper<T> queryWrapper = compile(specification);
            return baseMapper.selectList(queryWrapper);
        });
    }

    @Override
    public long count(ISpecification<T> specification) {
        QueryWrapper<T> queryWrapper = compile(specification);
        return baseMapper.selectCount(queryWrapper);
    }

    @Override
    public CompletableFuture<Long> countAsync(ISpecification<T> specification) {

        return CompletableFuture.supplyAsync(()->{
            QueryWrapper<T> queryWrapper = compile(specification);
            return baseMapper.selectCount(queryWrapper);
        });
    }

    @Override
    public IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery) {
        QueryWrapper<T> queryWrapper = compile(specification);
        Page<T> page = new Page<>(pageQuery.getPage(), pageQuery.getPageSize());
        IPage<T> pageResult = baseMapper.selectPage(page, queryWrapper);
        return new PageResultAdapter<>(pageResult);
    }


}