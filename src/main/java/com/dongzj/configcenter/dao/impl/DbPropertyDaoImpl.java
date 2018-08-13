package com.dongzj.configcenter.dao.impl;

import com.dongzj.configcenter.dao.PropertyDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: dongzj
 * Date: 2018/7/10
 * Time: 14:53
 */
@Repository
@Profile("db")
public class DbPropertyDaoImpl implements PropertyDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DbPropertyDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PropertySource> loadProperties(String application, String profile, String label) {
        String sql = "select data from t_property where name = '%s'";
        List<PropertySource> propertySources = new ArrayList<>();
        if (StringUtils.isEmpty(application) || StringUtils.isEmpty(profile)) {
            return propertySources;
        }
        String name = application + "-" + profile;
        sql = String.format(sql, name);
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> map : maps) {
            String data = map.get("data").toString();
            String[] datas = data.split("\n");
            for (String str : datas) {
                String[] kv = str.split(":");
                String key = kv[0];
                Object value = kv[1];
                value = resolverVal(value);
                Map<String, Object> source = new HashMap<>(1);
                source.put(key, value);
                PropertySource prop = new PropertySource(name, source);
                propertySources.add(prop);
            }
        }
        return propertySources;
    }

    /**
     * 对value值进行数据类型转化
     *
     * @param value
     * @return
     */
    private Object resolverVal(Object value) {
        String val = String.valueOf(value);
        Resolver resolver = new Resolver();
        Tag tag = resolver.resolve(NodeId.scalar, val, true);
        Object obj = val;
        if (tag.getValue().indexOf("int") > -1) {
            obj = Integer.valueOf(val);
        }
        if (tag.getValue().indexOf("float") > -1) {
            obj = Float.valueOf(val);
        }
        if (tag.getValue().indexOf("bool") > -1) {
            obj = Boolean.valueOf(val);
        }
        return obj;
    }

}
