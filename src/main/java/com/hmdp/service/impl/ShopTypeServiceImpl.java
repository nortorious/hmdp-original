package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getTypeList() {
        String typeKey= RedisConstants.CACHE_TYPE_KEY;
        //从redis中查询有多少条数据
        Long typeListSize = stringRedisTemplate.opsForList().size(typeKey);

        //redis存在数据
        if (typeListSize!=null&&typeListSize!=0){
            //从redis中获取所有数据
            List<String> typeJsonList = stringRedisTemplate.opsForList().range(typeKey, 0, typeListSize-1);

            //将JSON格式的字符串列表转换成ShopType对象列表
            List<ShopType> typeList=new ArrayList<>();
            for (String typeJson : typeJsonList) {
                typeList.add(JSONUtil.toBean(typeJson,ShopType.class));
            }
            //返回成功的结果
            return Result.ok(typeList);
        }


        //redis不存在数据 查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList==null){
            //数据库不存在数据
            return Result.fail("发生错误");
        }
        //将数据库查询出来的ShopType类型转成JSON字符串，并且加入到redis
        List<String> typeJsonList=new ArrayList<>();
        for (ShopType shopType : typeList) {
            typeJsonList.add(JSONUtil.toJsonStr(shopType));
        }
        //将数据库数据 转后成JSON字符串后写入redis
        stringRedisTemplate.opsForList().rightPushAll(typeKey,typeJsonList);
        //返回数据
        return Result.ok(typeList);
    }
}
