package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.visitor.IExpressionVisitor;
import org.plain.utils.converter.IConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * MyBatis-Plus Repository（Domain/PO 分离，通过 IConverter 转换）。
 *
 * @param <T> 领域实体类型
 * @param <P> 持久化对象类型
 * @param <TID> ID 类型
 * @author Jayden.Liang
 */
@Slf4j
public abstract class MybatisPlusBaseRepositoryOfP<T, P, TID extends Serializable>
        extends AbstractMybatisPlusRepository<T, TID> {

    protected final BaseMapper<P> baseMapper;
    protected final IConverter<T, P> converter;

    private volatile Class<?> entityClass;

    public MybatisPlusBaseRepositoryOfP(BaseMapper<P> baseMapper, IConverter<T, P> converter, Executor executor) {
        super(executor);
        this.baseMapper = baseMapper;
        this.converter = converter;
    }

    public MybatisPlusBaseRepositoryOfP(BaseMapper<P> baseMapper, IConverter<T, P> converter, Executor executor,
                                        PlatformTransactionManager transactionManager) {
        super(executor, transactionManager);
        this.baseMapper = baseMapper;
        this.converter = converter;
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
        Collection<P> poList = converter.convert(entities);
        baseMapper.insert(poList);
    }

    @Override
    protected void doUpdateBatch(Collection<T> entities) {
        Collection<P> poList = converter.convert(entities);
        baseMapper.updateById(poList);
    }

    @Override
    protected void doDeleteBatch(Collection<T> entities) {
        Collection<P> poList = converter.convert(entities);
        baseMapper.deleteByIds(poList);
    }

    // -------------------------------------------------------------------------
    // 同步 CRUD
    // -------------------------------------------------------------------------

    @Override
    public T save(T entity) {
        P po = converter.convert(entity);
        baseMapper.insert(po);
        T result = converter.reverse(po);
        if (result == null) {
            throw new IllegalStateException("Reverse converter returned null");
        }
        return result;
    }

    @Override
    public void update(T entity) {
        baseMapper.updateById(converter.convert(entity));
    }

    @Override
    public void delete(T entity) {
        baseMapper.deleteById(converter.convert(entity));
    }

    @Override
    public T findById(TID id) {
        P po = baseMapper.selectById(id);
        return po == null ? null : converter.reverse(po);
    }

    @Override
    public T findOne(ISpecification<T> specification) {
        P po = baseMapper.selectOne(compileToPo(specification));
        return po == null ? null : converter.reverse(po);
    }

    @Override
    public Collection<T> findRange(ISpecification<T> specification) {
        List<P> pList = baseMapper.selectList(compileToPo(specification));
        return converter.reverse(pList);
    }

    @Override
    public long count(ISpecification<T> specification) {
        return baseMapper.selectCount(compileToPo(specification));
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
    // 按条件删除（单条 SQL 优化）
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.runAsync(() -> {
            if (transactionTemplate != null) {
                transactionTemplate.executeWithoutResult(status -> baseMapper.delete(compileToPo(specification)));
            } else {
                baseMapper.delete(compileToPo(specification));
            }
        }, executor);
    }

    // -------------------------------------------------------------------------
    // 编译 + Visitor
    // -------------------------------------------------------------------------

    protected QueryWrapper<P> compileToPo(ISpecification<T> specification) {
        MybatisPlusEntityToPoVisitor<T, P> visitor = new MybatisPlusEntityToPoVisitor<>(entityClass());
        QueryWrapper<P> queryWrapper = Wrappers.query();
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
        return new MybatisPlusEntityToPoVisitor<>(entityClass());
    }

    /**
     * Specification 的实体类型，用于定位 {@link FieldMappingRegistry} 里的字段映射。
     * 解析不出来时返回 null，访问器退回按 lambda 声明类查找。
     */
    protected Class<?> entityClass() {
        Class<?> resolved = entityClass;
        if (resolved == null) {
            resolved = ReflectionKit.getSuperClassGenericType(getClass(), MybatisPlusBaseRepositoryOfP.class, 0);
            if (resolved != null) {
                entityClass = resolved;
            }
        }
        return resolved;
    }
}
