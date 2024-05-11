package org.jianghu.app.common;

import com.alibaba.fastjson2.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于阿里巴巴fastjson的JSONPath扩展，提供便捷的JSONPath表达式操作。
 */
@Slf4j
// implements 序列化
public class JSONPathObject extends JSONObject {

    public JSONPathObject() { super(); }

    public JSONPathObject(int initialCapacity) { super(initialCapacity); }

    public JSONPathObject(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }

    public JSONPathObject(int initialCapacity, float loadFactor, boolean accessOrder) { super(initialCapacity, loadFactor, accessOrder); }

    public JSONPathObject(Map map) { super(map); }

    /**
     * 通过JSONPath表达式获取值。
     *
     * @param path JSONPath表达式。
     * @return 返回表达式匹配的值。
     */
    public <T> T eval(String path) {
        return (T) JSONPath.eval(this, path);
    }

    public <T> T eval(String path, T defaultVal) {
        Object value = JSONPath.eval(this, path);
        if (value == null || value.equals("")) { return defaultVal; }
        return (T) value;
    }

    public <T> T eval(String path, T defaultVal, Class<T> clazz) {
        Object value = this.eval(path, clazz);
        if (value == null || value.equals("")) { return defaultVal; }
        return (T) value;
    }

    /**
     * 通过JSONPath表达式获取值，并转换为指定类型。
     *
     * @param path  JSONPath表达式。
     * @param clazz 需要转换的目标类型。
     * @return 返回表达式匹配的值，转换为目标类型后返回。
     *  evalObject
     *  evalList
     *
     *  evalObject("actionData.info")
     *  eval("actionData.info", JSONPathObject.class)
     */
    public <T> T eval(String path, Class<T> clazz) {
        Object pathValue = JSONPath.eval(this, path);
        if (pathValue == null) { return null;}

        if (clazz == List.class || clazz == ArrayList.class) {
            return (T) this.evalList(path);
        }

        if (pathValue.equals("")) {
            return (T) null;
        }

        if (pathValue.getClass().equals(clazz)) {
            return (T) pathValue;
        }
        return (T) JSON.to(clazz, pathValue);
    }

    public List<Object> evalList(String path) {
        Object pathValue = JSONPath.eval(this, path);
        if (pathValue == null) {
            return null;
        }
        JSONArray jsonArray = JSON.to(JSONArray.class, pathValue);
        if (jsonArray == null) return null;
        List<Object> list = jsonArray.stream().map(item -> {
            if (item instanceof JSONObject) {
                return new JSONPathObject((JSONObject) item);
            } else {
                return item;
            }
        }).collect(Collectors.toList());
        return list;
    }

    /**
     * 通过JSONPath表达式设置值。
     *
     * @param path  JSONPath表达式。
     * @param value 要设置的值。
     * @return 返回当前对象，支持链式调用。
     */
    public JSONPathObject set(String path, Object value) {
        path = path.replaceAll("-", "\\\\-");
        if (!path.startsWith("$.")) {
            path = "$." + path;
        }
        JSONPath.set(this, path, value);
        return this;
    }

    public static JSONPathObject from(Object obj) { return JsonUtil.toJSON(obj); }

    public static JSONPathObject from(Object obj, JSONWriter.Feature... writeFeatures) { return JsonUtil.toJSON(obj, writeFeatures); }

    /**
     * 通过指定的路径从JSON对象中移除元素。
     *
     * @param path 移除元素的路径，使用JSONPath语法来指定。
     * @return 返回修改后的JSONPathObject实例，支持链式调用。
     */
    public JSONPathObject remove(String path) {
        // 使用JSONPath的remove方法，根据提供的路径从当前对象中移除元素
        JSONPath.remove(this, path);
        return this;
    }

    /**
     * 克隆当前JSONPathObject实例。
     *
     * @return 返回一个新的JSONPathObject实例，包含当前对象的所有属性。
     */
    public JSONPathObject clone() {
        return new JSONPathObject(this);
    }

    public static JSONPathObject of() {
        return new JSONPathObject();
    }

    public static JSONPathObject of(String key, Object value) {
        JSONPathObject object = new JSONPathObject(1, 1F);
        object.put(key, value);
        return object;
    }

    /**
     * 判断通过JSONPath表达式获取的值是否为空。
     *
     * @param path JSONPath表达式。
     * @return 如果表达式结果为null或空字符串，则返回true，否则返回false。
     */
    public boolean isEmpty(String path) {
        Object value = JSONPath.eval(this, path);
        boolean isEmpty = value == null || value.toString().isEmpty();
        return isEmpty;
    }
}
