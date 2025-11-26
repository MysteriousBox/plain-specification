package org.plain.specification;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plain.specification.mybatisplus.MpFieldNameResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class MpSpecificationBuilderTest {

    @Data
    @AllArgsConstructor
    @TableName
    public static class Person{

        @TableId("id")
        private Long id;

        @TableField("name")
        private String name;

        @TableField("age")
        private Integer age;
    }

    @Data
    @AllArgsConstructor
    public static class Person2{
        private String name;
        private int age;
    }

    @Mock
    private BaseMapper<Person> mapper;

    @BeforeAll
    public static void init() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Person.class);
        Assertions.assertEquals("person", tableInfo.getTableName());
    }

    @Test
    public void test() {




    }

    private boolean matchesConditions(Person person, Map<String, Object> whereClause) {
        // 获取参数键值对（例如：{name=张三, age=18}）

        // 匹配所有条件
        boolean nameMatch = whereClause.containsKey("name") &&
                person.getName().equals(whereClause.get("name"));
        boolean ageMatch = whereClause.containsKey("age") &&
                person.getAge() == (int) whereClause.get("age");

        return nameMatch && ageMatch;
    }

    @Captor
    ArgumentCaptor<QueryWrapper<Person>> wrapperCaptor;
    // 测试用例
    @Test
    public void testFieldConversion() {

        // Arrange
        MpFieldNameResolver resolver = new MpFieldNameResolver();
        String nameField = resolver.resolve(Person::getName); // 应为 "name"

        when(mapper.selectList(any())).thenReturn(Collections.singletonList(new Person(1L, "张三", 18)));

        // Act
        LambdaQueryWrapper<Person> wrapper = Wrappers.<Person>lambdaQuery().eq(Person::getName, "张三");
        List<Person> people = mapper.selectList(wrapper);

        // Assert
        verify(mapper).selectList(wrapperCaptor.capture());
        QueryWrapper<Person> captured = wrapperCaptor.getValue();

        String sqlSegment = captured.getSqlSegment();
        System.out.println("Captured SQL Segment: " + sqlSegment);

        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("name"), "SQL should contain the column 'name'");

        // 读取参数值
        Map<String, Object> paramMap = (Map<String, Object>) ReflectionTestUtils.getField(captured, "paramNameValuePairs");

        System.out.println("Params: " + paramMap);

        assertNotNull(paramMap);
        assertTrue(paramMap.values().contains("张三"), "Should contain value '张三'");

    }



}