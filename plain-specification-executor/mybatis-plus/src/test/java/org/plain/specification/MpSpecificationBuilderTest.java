package org.plain.specification;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class MpSpecificationBuilderTest {

    @Data
    @AllArgsConstructor
    @TableName
    static class Person{

        @TableId("id")
        private Long id;

        @TableField("name")
        private String name;

        @TableField("age")
        private Integer age;
    }

    @Data
    @AllArgsConstructor
    static class Person2{
        private String name;
        private int age;
    }

    @Mock
    private BaseMapper<Person> mapper;

    @BeforeAll
    static void init() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Person.class);
        Assertions.assertEquals("person", tableInfo.getTableName());
    }


    @Captor
    ArgumentCaptor<Wrapper<Person>> wrapperCaptor;
    // 测试用例
    @Test
    @SuppressWarnings("unchecked")
    void testFieldConversion() {

        // Arrange
         // 应为 "name"

        when(mapper.selectList(Mockito.<Wrapper<Person>>any())).thenReturn(Collections.singletonList(new Person(1L, "张三", 18)));

        // Act
        LambdaQueryWrapper<Person> wrapper = Wrappers.<Person>lambdaQuery().eq(Person::getName, "张三");
        List<Person> people = mapper.selectList(wrapper);

        // Assert
        verify(mapper).selectList(wrapperCaptor.capture());
        assertEquals(1, people.size());
        assertEquals("张三", people.get(0).getName());
        Wrapper<Person> captured = wrapperCaptor.getValue();
        assertNotNull(captured);

        String sqlSegment;
        try {
            sqlSegment = (String) ReflectionTestUtils.invokeMethod(captured, "getSqlSegment");
        } catch (Exception e) {
            sqlSegment = captured.toString();
        }
        System.out.println("Captured SQL Segment: " + sqlSegment);

        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("name"), "SQL should contain the column 'name'");

        // 读取参数值
        Map<String, Object> paramMap = null;
        try {
            Object rawParamMap = ReflectionTestUtils.getField(captured, "paramNameValuePairs");
            if (rawParamMap instanceof Map) {
                //noinspection unchecked
                paramMap = (Map<String, Object>) rawParamMap;
            }
        } catch (Exception ignore) { 
             // 反射失败，可能是因为 MyBatis-Plus 版本不同，尝试从 toString 中解析参数
            String capturedStr = captured.toString();
            int start = capturedStr.indexOf("{");
            int end = capturedStr.lastIndexOf("}");
            if (start != -1 && end != -1 && end > start) {
                String paramsStr = capturedStr.substring(start + 1, end);
                paramMap = new java.util.HashMap<>();
                for (String param : paramsStr.split(", ")) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        paramMap.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }   
        }
        System.out.println("Params: " + paramMap);
        assertNotNull(paramMap);
        assertTrue(paramMap.containsValue("张三"), "Should contain value '张三'");
    }



}