package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 刷新令牌拦截器
 *
 * @author CHEN
 * @date 2022/10/07
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从请求头中获取token
        String token = request.getHeader("authorization");
        if (StringUtils.isEmpty(token)) {
            //不存在token
            return true;
        }
        //从redis中获取用户
        Map<Object, Object> userMap =
                stringRedisTemplate.opsForHash()
                        .entries(RedisConstants.LOGIN_USER_KEY + token);
        //用户不存在
        if (userMap.isEmpty()) {
            return true;
        }
        //hash转UserDTO存入ThreadLocal，这一步主要是将redis存放的用户信息进行脱敏处理。
        UserHolder.saveUser(BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false));
        //token续命
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }


    /**
     * afterCompletion 是 HandlerInterceptor 接口中的一个方法，用于在整个请求处理完成后（包括视图渲染）执行的回调操作。
     * 它通常被用来进行一些资源清理或日志记录等工作。
     * 在这个拦截器中，afterCompletion 的主要作用是清理 ThreadLocal 中保存的用户信息，以避免内存泄漏或数据残留。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
