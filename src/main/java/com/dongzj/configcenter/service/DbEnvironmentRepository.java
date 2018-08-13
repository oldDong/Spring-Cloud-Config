package com.dongzj.configcenter.service;

import com.dongzj.configcenter.dao.PropertyDao;
import com.dongzj.configcenter.dao.impl.DbPropertyDaoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.PassthruEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * User: dongzj
 * Date: 2018/7/10
 * Time: 10:12
 */
@ConfigurationProperties("spring.cloud.config.server.db")
public class DbEnvironmentRepository implements EnvironmentRepository {

    private final static String DEFAULT_VERSION = "1";

    @Autowired
    private PropertyDao propertiesDao;

    public DbEnvironmentRepository(JdbcTemplate jdbcTemplate) {
        this.propertiesDao = new DbPropertyDaoImpl(jdbcTemplate);
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        return fillEnviroment(new PassthruEnvironmentRepository(environment).findOne(application, profile, label));
    }

    private Environment fillEnviroment(Environment value) {
        Environment environment = new Environment(value.getName(), value.getProfiles(), value.getLabel(),
                DEFAULT_VERSION, value.getState());
        environment.addAll(value.getPropertySources());
        List<PropertySource> sources = propertiesDao.loadProperties(value.getName(), value.getProfiles()[0], value.getLabel());
        List<PropertySource> baseSources = propertiesDao.loadProperties("base","base",null);
        environment.addAll(sources);
        environment.addAll(baseSources);
        return environment;
    }

}
