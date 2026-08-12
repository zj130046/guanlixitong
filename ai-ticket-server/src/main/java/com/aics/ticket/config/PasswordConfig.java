package com.aics.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 * <p>
 * 使用 BCrypt 进行密码哈希，仅引入 spring-security-crypto 加密库，
 * 不会激活 Spring Security 过滤器链，与 Sa-Token 共存无冲突。
 * </p>
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 密码编码器，强度因子 10。
     * 生成的密文以 {@code {bcrypt}} 前缀存储于数据库，与旧 {@code {noop}} 明文兼容。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
