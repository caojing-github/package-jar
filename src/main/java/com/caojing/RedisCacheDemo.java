package com.caojing;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.serializer.SerializerFeature.PrettyFormat;

/**
 * RedisCache测试类
 *
 * @author CaoJing
 * @date 2019/10/22 20:49
 */
@Slf4j
public class RedisCacheDemo {

    /**
     * 获取token
     */
    @SuppressWarnings("unchecked")
    public static void test20191022204927(String env) throws Exception {

        // 初始化redis
        RedisCache.initialPool(env);

        List<Map> list = RedisCache.keys("pusher:user:token*list")
            .stream()
            .map(x -> {
                String[] split = x.split(":");
                String officeId = split[3];
                String userId = split[4];

                String sql = "select a.*, b.name AS roleName from pusher_user a, pusher_role b where a.id = '" + userId + "' and a.role_id = b.id";

                Map<String, Object> user = null;
                Map<String, Object> office = null;
                try {
                    List<Map<String, Object>> list1 = JdbcUtil.executeQuery(JdbcUtil.getByEnv(env).getConnection(), sql, null);
                    if (list1.isEmpty()) {
                        return null;
                    }

                    user = list1.get(0);
                    if (null == user || "99".equals(user.get("status"))) {
                        return null;
                    }

                    String sql2 = "select * from pusher_office a, pusher_user b where a.id = b.pusher_office_id and b.id = '" + userId + "'";

                    List<Map<String, Object>> list2 = JdbcUtil.executeQuery(JdbcUtil.getByEnv(env).getConnection(), sql2, null);
                    if (list2.isEmpty()) {
                        return null;
                    }

                    office = list2.get(0);
                } catch (Exception e) {
                    log.error("", e);
                }

                Map map = new HashMap();
                map.put("officeId", officeId);
                map.put("officeName", office.get("name"));
                map.put("userId", userId);
                map.put("userName", user.get("name"));
                map.put("roleName", user.get("roleName"));
                map.put("token", JSON.parseArray(RedisCache.getString(x), String.class));
                return map;

            }).filter(Objects::nonNull)
            .collect(Collectors.toList());

        FileOutputStream fos = new FileOutputStream(new File("token.txt"));

        // 以人类可读方式打印
        log.info(JSON.toJSONString(list, true));

        // 将结果输出到文件
        JSON.writeJSONString(fos, list, PrettyFormat);
    }

    public static void main(String[] args) throws Exception {
        if (ArrayUtils.isEmpty(args)) {
            test20191022204927("dev");
        } else {
            test20191022204927(args[0]);
        }
    }

}
