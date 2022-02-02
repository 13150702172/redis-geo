

> 搜索附近的人应用
> 

我们在微信当中有一个“附近的人”，那么我们思考一个该功能是怎么实现的呢？

- 将所有开启“附近的人”的用户经纬度信息存储
- 当前用户开启“附近的人”之后，获取当前用户的经纬度
- 通过当前用户的经纬度和其他用户比对，查找符合的用户信息

基本流程如下：

![WechatIMG2.jpeg](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/a4356410-5a31-4fe7-b5ac-388321f191a9/WechatIMG2.jpeg)

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/3c1d3c3e-cc89-455e-8827-861c3a5b3205/Untitled.png)

> 实现方式
> 

<aside>
🌕 mysql

</aside>

使用mysql的话，虽然从存储来说是可行的，可以进行持久化，但是持久化经纬度之后，我们还需要自行计算`GeoHash` (**空间索引编码**)，自行计算的话门槛较高，而且稍微复杂，需要学会更多的知识点

这个内容后面再展开叙述

<aside>
🌕 Redis 中的 GEO

</aside>

使用redis的话，可以很简单的就实现两个点之间距离等计算，相比较mysql，可以很简单的就实现，具体的语法如下：

[Untitled](https://www.notion.so/3b8aa6ad60274c0b9e882d095f77b93d)

**注意：**Redis 会假设地球为完美的球形, 所以可能有一些位置计算偏差，据说`<=0.5%`，对于有严格地理位置要求的需求来说要经过一些场景测试来检验是否能够满足需求。

- **GEOADD写入地理位置信息**

语法:

```java
geoadd key longitude latitude member [longitude latitude member ...
```

案例:

```bash
#金色明郡坐标
geoadd cities 100.968562 22.792277 puer     

#普洱市人民医院坐标
geoadd cities geoadd cities 100.972897 22.781852 puerrmyy

#对于添加坐标，我们还有其他的写法,可以同时添加多个坐标
geoadd cities 100.968562 22.792277 puer 100.972897 22.781852 puerrmyy
```

- **GEODIST计算两点的距离**

语法：

```bash
#单位默认为米
geodist key member1 member2[unit]
```

案例：

```bash
#我们使用GEOADD添加的坐标用于测试，单位为米，我们这里可以指定单位
geodist cities puer purmyy km

#结果
#如果不指定单位
127.0.0.1:6379> geodist cities puer puerrmyy
"1241.8"

#指定单位
127.0.0.1:6379> geodist cities puer puerrmyy km
"1.2418"
```

- **GEOPOS：从 key 里返回所有给定位置元素的位置（经度和纬度）**

语法：

```bash
geopos key member[member……]
```

案例：

```bash
#一个条件
127.0.0.1:6379> geopos cities puer
1) 1) "100.96856385469436646"
   2) "22.79227730016541642"

#多个条件
127.0.0.1:6379> geopos cities puer puerrmyy
1) 1) "100.96856385469436646"
   2) "22.79227730016541642"
2) 1) "100.97289830446243286"
   2) "22.7818519920370548"
```

- **GEOHASH：返回一个或多个位置元素的 Geohash 表示**

语法：

```bash
geohash key member [member……]
```

案例：

```bash
#多个条件
127.0.0.1:6379> geohash cities puer puerrmyy
1) "whpc9t5ndh0"
2) "whpc9eq1zj0"

#一个条件
127.0.0.1:6379> geohash cities puer
1) "whpc9t5ndh0"
```

- **GEORADIUS：以给定的经纬度为中心， 找出某一半径内的元素**

语法：

```bash
georadius key longtitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC]

1、radius 半径长度，必选项。后面的m、km、ft、mi、是长度单位选项，四选一。
2、WITHCOORD 将位置元素的经度和维度也一并返回，非必选。
3、WITHDIST 在返回位置元素的同时， 将位置元素与中心点的距离也一并返回。距离的单位和查询单位一致，非必选。
4、WITHHASH 返回位置的 52 位精度的Geohash值，非必选。这个我反正很少用，可能其它一些偏向底层的LBS应用服务需要这个。
5、COUNT 返回符合条件的位置元素的数量，非必选。比如返回前 10 个，以避免出现符合的结果太多而出现性能问题。
6、ASC|DESC 排序方式，非必选。默认情况下返回未排序，但是大多数我们需要进行排序。参照中心位置，从近到远使用ASC ，从远到近使用DESC。
```

案例：

```bash
#我们查找普洱卫校（100.974334,22.786263）周边1公里的区域
georadius cities 100.974334 22.786263 1 km

127.0.0.1:6379> georadius cities 100.974334 22.786263 2 km
1) "puerrmyy"
2) "puer"

#我们查找普洱飞机场（100.963241,22.79475）周边1公里的区域
georadius cities 100.963241 22.79475 1 km

127.0.0.1:6379> georadius cities 100.963241 22.79475 1 km
1) "puer"
```

- **GEORADIUSBYMEMBER：找出位于指定范围内的元素，中心点是由给定的位置元素决定**

这个命令和 [GEORADIUS](https://www.redis.net.cn/order/3689.html) 命令一样， 都可以找出位于指定范围内的元素， 但是 `GEORADIUSBYMEMBER` 的中心点是由给定的位置元素决定的， 而不是像 [GEORADIUS](https://www.redis.net.cn/order/3689.html) 那样， 使用输入的经度和纬度来决定中心点

**GEORADIUS：中心点可以自定义，随意改变**

```bash
georadius cities 100.974334 22.786263 2 km
```

**GEORADIUSBYMEMBER：中心点需要从存储的数据中指定，而不是自行指定**

```bash
georadiusbymember cities puer 1 km
```

语法：

```bash
GEORADIUSBYMEMBER key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC] [STORE key] [STOREDIST key]
```

案例：

```bash
georadiusbymember cities puerrmyy 2 km

#指定返回符合条件的条数：2条
georadiusbymember cities puerrmyy 2 km count 2

#排序
georadiusbymember cities puerrmyy 2 km count 2 desc
```

> Java实现附近的人
> 

<aside>
🌕 注册用户地址

</aside>

该功能相当于开启***附近的人***

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/556c25b2-373c-46b5-89bf-efc57f4ff2bd/Untitled.png)

- 对相关参数封装Geo实体

```java
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
```

- 服务层

```java
   /**
     * 用户地址注册
     */
    public void register(Geo geo){
        Map<String,Point> pointMap = new HashMap<>();
        pointMap.put(geo.getName(),new Point(geo.getLongtitude(),geo.getLatitude()));
        Long aLong = redisTemplate.opsForGeo().add(KEY_NAME,pointMap);
        log.info("aLong:{}",aLong);
    }
```

- 控制层

```java
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
```

<aside>
🌕 关闭附近的人功能

</aside>

- 服务层

```java
/**
     * 关闭附近的人功能
     * @param name
     */
    public boolean delUserAddress(String name){
        Long r = redisTemplate.opsForGeo().remove(KEY_NAME,name);
        return r > 0 ? true : false;
    }
```

- 控制层

```java
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
```

<aside>
🌕 获取用户位置

</aside>

- 服务层

```java
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
```

- 控制层

```java
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
```

<aside>
🌕 搜索附近的人信息

</aside>

- 封装返回值对象 Result

```java
/**
 * 用于存储返回值
 */
@Data
public class Result {
    private String name;
    private double x;
    private double y;
    private double value;
}
```

- 服务层

```java
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
            
            //参数
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
```

- 控制层

```java
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
```
