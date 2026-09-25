package org.plain.specification.example.repository;

import org.plain.specification.example.entity.User;
import org.plain.specification.example.mapper.UserMapper;
import org.plain.specification.mybatisplus.MybatisPlusBaseRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.Executor;

@Repository
public class UserRepository extends MybatisPlusBaseRepository<User, Long> {

    public UserRepository(UserMapper baseMapper, Executor executor) {
        super(baseMapper, executor);
    }
}
