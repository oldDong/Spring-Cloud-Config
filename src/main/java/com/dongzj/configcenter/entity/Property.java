package com.dongzj.configcenter.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Date: 2018/7/12
 * Time: 14:02
 */
@Getter
@Setter
public class Property implements Serializable {

    private static final long serialVersionUID = 7466323502937873374L;

    /**
     * 主键
     */
    private String id;

    /**
     * 应用名
     */
    private String name;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 数据
     */
    private String data;

    /**
     * 创建时间
     */
    private String gmt_create;

    /**
     * 修改时间
     */
    private String gmt_modify;

    /**
     * 环境
     */
    private String env;
}
