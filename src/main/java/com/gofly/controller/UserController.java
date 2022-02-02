package com.gofly.controller;

import com.gofly.entity.Geo;
import com.gofly.entity.Result;
import com.gofly.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private RedisService redisService;

    /**
     * 用户地址注册
     * @param geo
     * @return
     */
    @PostMapping("/register")
    public String register(Geo geo){
        redisService.register(geo);
        log.info("接收到geo:{}",geo);
        return "用户地址注册成功";
    }

    /**
     * 获取用户位置
     * @param name
     * @return
     */
    @GetMapping("/getUserLocation")
    public List<Point> getUserLocation(String name){
        List<Point> points = redisService.getUserLocation(name);
        return points;
    }

    /**
     * 查找附近的人
     * @param currentUserName
     * @param val 查找范围，单位km
     * @return
     */
    @GetMapping("/findNearUser")
    public List<Result> findNearUser(String currentUserName,double val){
        List<Result> nearUser = redisService.findNearUser(currentUserName, val);
        return nearUser;
    }

    /**
     * 关闭查找附近的人操作
     * @param currentUserName
     * @return
     */
    @GetMapping("/delUserAddress")
    public String delUserAddress(String currentUserName){
        boolean b = redisService.delUserAddress(currentUserName);
        if(b){
            return "关闭附近的人成功";
        }
        return "关闭附近的人功能失败";
    }


}
