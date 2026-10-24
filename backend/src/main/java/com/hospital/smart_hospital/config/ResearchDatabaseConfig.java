package com.hospital.smart_hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class ResearchDatabaseConfig {

    @Bean(name = "researchJdbcTemplate")
    public JdbcTemplate researchJdbcTemplate(
            @Value("${research.datasource.url}") String url,
            @Value("${research.datasource.username}") String username,
            @Value("${research.datasource.password}") String password) {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return new JdbcTemplate(dataSource);
    }
}