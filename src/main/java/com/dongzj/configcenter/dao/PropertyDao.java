package com.dongzj.configcenter.dao;

import org.springframework.cloud.config.environment.PropertySource;

import java.util.List;

/**
 * User: dongzj
 * Date: 2018/7/10
 * Time: 10:15
 */
public interface PropertyDao {

    /**
     * 从数据库中查询配置文件
     *
     * @param application
     * @param profile
     * @param label
     * @return List<PropertySource>
     */
    List<PropertySource> loadProperties(String application, String profile, String label);
}
