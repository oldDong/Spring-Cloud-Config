package com.dongzj.configcenter.config;

import com.dongzj.configcenter.service.DbEnvironmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @Profile(“db”)表示如果spring.profiles.active=db，将会以扩展的数据库配置模式启动
 * 参见EnvironmentRepositoryConfiguration
 *
 * User: dongzj
 * Date: 2018/7/10
 * Time: 10:03
 */
public class DbEnvironmentRepositoryConfiguration {

    @Configuration
    @Profile("db")
    protected static class NativeRepositoryConfiguration {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Bean
        public DbEnvironmentRepository nativeRepositoryConfiguration () {
            return new DbEnvironmentRepository(jdbcTemplate);
        }

    }
}
