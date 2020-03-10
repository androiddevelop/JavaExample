package me.codeboy.example.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * key为int时的序列化和反序列化
 * Created by yuedong.li on 2020/3/9
 */
public class JsonTest {
    public static void main(String[] args) {
        Map<Object, String> map = new HashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(true, "c");
        String json = JSON.toJSONString(map);
        System.out.println(json);

        // 使用fastjson
        JSONObject jsonObject1 = JSONObject.parseObject(json);
        System.out.println(jsonObject1.keySet());
        System.out.println(jsonObject1.get(1)); // "a"
        System.out.println(jsonObject1.get("1"));  // null

        // 官方json
        org.json.JSONObject jsonObject2 = new org.json.JSONObject(json);
        System.out.println(jsonObject2.keySet());
        System.out.println(jsonObject2.get("1"));  // "a"

    }
}
