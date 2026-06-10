package com.wh.jobsbackend.application.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.jobsbackend.application.entity.UserEntity;
import com.wh.jobsbackend.application.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username).last("LIMIT 1");
        UserEntity user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return new AppUserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole(), user.getStatus());
    }
}
