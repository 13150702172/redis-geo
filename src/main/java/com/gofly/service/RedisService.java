package com.gofly.service;

import com.gofly.entity.Geo;
import com.gofly.entity.Result;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private final static String KEY_NAME = "cities:locs";

    /**
     * 用户地址注册
     */
    public void register(Geo geo){
        Map<String,Point> pointMap = new HashMap<>();
        pointMap.put(geo.getName(),new Point(geo.getLongtitude(),geo.getLatitude()));
        Long aLong = redisTemplate.opsForGeo().add(KEY_NAME,pointMap);
        log.info("aLong:{}",aLong);
    }

    /**
     * 获取用户位置
     * @param name
     * @return
     */
    public List<Point> getUserLocation(String name){
        List<Point> position = redisTemplate.opsForGeo().position(KEY_NAME, name);
        log.info("获取用户地址：{}",position);
        return position;
    }

    /**
     * 查找附近的用户
     * @param value 查找的范围
     * @param name 用户名称
     * @return
     */
    public List<Result> findNearUser(String name, double value){

        List<Result> results = new ArrayList<>();

        try{
            /**
             * 需要查找的范围，比如：1公里之内
             * Distance:
             *     value 距离
             *     Metric 单位
             */
            Distance distance = new Distance(value, Metrics.KILOMETERS);
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().includeCoordinates().sortAscending();
            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = redisTemplate.opsForGeo().radius(KEY_NAME, name, distance, args);

            //转换相关数据到自定义的Result
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = geoResults.getContent();

            content.forEach(g ->{
                Result result = new Result();
                result.setName(g.getContent().getName());
                result.setX(g.getContent().getPoint().getX());
                result.setY(g.getContent().getPoint().getY());
                result.setValue(g.getDistance().getValue());

                results.add(result);
            });

            return results;

        }catch (Exception e){
            //如果关闭了查找附近的人功能，再次调用该方法会出现异常
            return null;
        }
    }

    /**
     * 关闭附近的人功能
     * @param name
     */
    public boolean delUserAddress(String name){
        Long r = redisTemplate.opsForGeo().remove(KEY_NAME,name);
        return r > 0 ? true : false;
    }

}
