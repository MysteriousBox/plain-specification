package org.plain.specification.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.plain.specification.example.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
