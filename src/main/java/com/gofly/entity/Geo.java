package com.gofly.entity;

import lombok.Data;

/**
 * 实体类
 */
@Data
public class Geo {
    /**
     * 用户名称
     */
    private String name;

    /**
     * 经度
     */
    private double longtitude;

    /**
     * 纬度
     */
    private double latitude;
}
