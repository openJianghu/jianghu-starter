package org.jianghu.app.common;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JsonUtil类提供了一系列的方法用于处理JSON数据。
 */
@Slf4j
public class JsonUtil {

    /**
     * 将文本解析为JSONPathObject对象。
     *
     * @param text 要解析的文本
     * @return 返回解析后的JSONPathObject对象
     */
    public static JSONPathObject parseObject(String text) {
        JSONPathObject jsonPathObject = JSON.parseObject(text, JSONPathObject.class);
        return jsonPathObject;
    }

    public static JSONPathObject parseObject(String text, JSONReader.Feature... features) {
        JSONPathObject jsonPathObject = JSON.parseObject(text, JSONPathObject.class, features);
        return jsonPathObject;
    }


    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        return JSON.parseArray(text, clazz);
    }

    public static List<JSONPathObject> toJSONList(List list) {
        List<JSONPathObject> listNew = new ArrayList<JSONPathObject>();
        list.forEach(item -> listNew.add(JsonUtil.toJSON(item)));
        return listNew;
    }

    /**
     * 将对象转换为JSONPathObject对象。
     *
     * @param object 要转换的对象
     * @return 返回转换后的JSONPathObject对象
     */
    public static JSONPathObject toJSON(Object object) {
        if (object instanceof Map) {
            return new JSONPathObject((Map) object);
        }
        JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
        JSONPathObject jsonPathObject = new JSONPathObject();
        jsonPathObject.putAll(jsonObject);
        return jsonPathObject;
    }

    /**
     * 将对象转换为JSONPathObject对象，可以指定特性。
     *
     * @param object   要转换的对象
     * @param features 要应用的特性
     * @return 返回转换后的JSONPathObject对象
     */
    public static JSONPathObject toJSON(Object object, JSONWriter.Feature... features) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(object, features);
        JSONPathObject jsonPathObject = new JSONPathObject();
        jsonPathObject.putAll(jsonObject);
        return jsonPathObject;
    }

    /**
     * 将对象转换为JSON字符串。
     *
     * @param obj 要转换的对象
     * @return 返回转换后的JSON字符串
     */
    public static String toJSONString(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static String toJSONString(Object obj, JSONWriter.Feature... features) {
        return JSON.toJSONString(obj, features);
    }

    /**
     * 使用给定的模板字符串和对象替换字符串。
     *
     * @param templateString 模板字符串     例如：knex('student').where({ level: ${actionData.level} }).select()
     * @param objs           用于替换的对象
     * @return 返回替换后的字符串
     */
    public static String replaceString(String templateString, Object... objs) {
        JSONPathObject jsonPathObject = new JSONPathObject();
        for (Object obj : objs) {
            if (obj instanceof JSONPathObject) {
                jsonPathObject.putAll((JSONPathObject)obj);
                continue;
            }
            JSONObject jsonObject = JsonUtil.toJSON(obj);
            jsonPathObject.putAll(jsonObject);
        }
        JSONPathObjectStringLookup lookup = new JSONPathObjectStringLookup(jsonPathObject);
        StringSubstitutor stringSubstitutor = new StringSubstitutor(lookup);
        String replaceString = stringSubstitutor.replace(templateString);
        return replaceString;
    }

    /**
     * JSONPathObjectStringLookup是一个内部类，用于查找JSONPathObject中的值。
     */
    private static class JSONPathObjectStringLookup implements StringLookup {
        private final JSONPathObject jsonPathObject;

        /**
         * 构造函数，接受一个JSONPathObject对象。
         *
         * @param jsonPathObject JSONPathObject对象
         */
        public JSONPathObjectStringLookup(JSONPathObject jsonPathObject) {
            this.jsonPathObject = jsonPathObject;
        }

        /**
         * 查找给定键的值。
         *
         * @param key 要查找的键
         * @return 返回找到的值，如果没有找到则返回null
         */
        @Override
        public String lookup(String key) {
            String keyForJsonPath = "$." + key;
            Object value = jsonPathObject.eval(keyForJsonPath);
            if (value == null) {
                return "null";
            }
            if (NumberUtil.isNumber(value.toString())) {
                return value.toString();
            }
            if (value instanceof String) {
                return "'" + value + "'";
            }
            return JsonUtil.toJSONString(value);
        }
    }

}
