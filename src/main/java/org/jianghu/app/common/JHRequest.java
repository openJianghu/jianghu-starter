package org.jianghu.app.common;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONPath;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * JHResult类继承自JSONPathObject，用于处理JSON数据。
 */
public class JHRequest extends JSONPathObject {
    private String packageId;
    private String packageType;
    private String timestamp;
    private Map<String, Object> appData = new HashMap<>();
    // Tip: request场景
    //      - appData
    //          - appId                String
    //          - pageId               String
    //          - actionId             String
    //          - authToken            String
    //          - actionData           Map<String, Object>            { name: '张三丰', level: '03' }
    //          - where                Map<String, String>            { name: '张三丰', classId: '2021-01级-02班' }
    //          - whereIn              Map<String, List>              {name: ['张三丰', '张无忌']}
    //          - whereLike            Map<String, String>            { name: '张%' }
    //          - whereOptions         List<List<String>>             [['name', '=', 'zhangshan'],['level', '>', 3],['name', 'like', '%zhang%']]
    //          - whereOrOptions       List<List<String>>             [['name', '=', 'zhangshan'],['level', '>', 3],['a', 100]]
    //          - orderBy              List<Map<String,String>>       [{ column: 'age', order: 'desc' }]
    //          - offset               Integer                        10
    //          - limit                Integer

    public JHRequest() { super(); }
    /**
     * 将指定的值设置到当前对象的指定路径上。
     *
     * @param path  表示属性路径的字符串，用于指定要设置值的位置。
     * @param value 要设置的值，其类型可以是任意对象。
     * @return 返回当前的JHResult对象，支持链式调用。
     */
    @Override
    public JHRequest set(String path, Object value) {
        path = path.replaceAll("-", "\\\\-");
        if (!path.startsWith("$.")) {
            path = "$." + path;
        }
        JSONPath.set(this, path, value);
        return this;
    }

    /**
     * 创建一个请求体。
     *
     * @param pageId     页面ID
     * @param actionId   操作ID
     * @param actionData 操作数据
     * @param where      查询条件
     * @return 返回创建的JHResult对象
     */
    public static JHRequest requestBody(String pageId, String actionId, Object actionData, Map<String, String> where) {
        JHRequest jhRequest = new JHRequest();
        String appId = System.getProperty("appId", "__default__");
        String packageId = System.currentTimeMillis() + "_" + new Random().nextInt(9999999);
        jhRequest.set("$.packageId", packageId);
        jhRequest.set("$.timestamp", DateUtil.date().toString(Constant.ISO8601));
        jhRequest.set("$.packageType", "httpRequest");
        jhRequest.set("$.appData.appId", appId);
        jhRequest.set("$.appData.pageId", pageId);
        jhRequest.set("$.appData.actionId", actionId);
        jhRequest.set("$.appData.actionData", actionData);
        if (where != null) {
            jhRequest.set("$.appData.where", where);
        }
        return jhRequest;
    }

    /**
     * 该方法将给定的数据设置到JSON对象的"appData.actionData"路径下。
     *
     * @param actionData 动作数据，可以是任意类型的对象。
     * @return 返回当前实例，支持链式调用。
     */
    public JHRequest setActionData(Object actionData) {
        this.set("$.appData.actionData", actionData);
        return this;
    }

    /**
     * 创建一个请求体，操作数据为空，查询条件为空。
     *
     * @param pageId   页面ID
     * @param actionId 操作ID
     * @return 返回创建的JHResult对象
     */
    public static JHRequest requestBody(String pageId, String actionId) {
        return requestBody(pageId, actionId, new HashMap<String, Object>(), null);
    }

    /**
     * 创建一个请求体，查询条件为空。
     *
     * @param pageId     页面ID
     * @param actionId   操作ID
     * @param actionData 操作数据
     * @return 返回创建的JHResult对象
     */
    public static JHRequest requestBody(String pageId, String actionId, Object actionData) {
        return requestBody(pageId, actionId, actionData, null);
    }

}
