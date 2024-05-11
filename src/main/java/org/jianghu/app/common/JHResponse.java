package org.jianghu.app.common;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONPath;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * JHResult类继承自JSONPathObject，用于处理JSON数据。
 */
public class JHResponse extends JSONPathObject {
    private String packageId;
    private String packageType;
    private String timestamp;
    private String status;            // "success" "fail"
    private Map<String, Object> appData = new HashMap<>();
    // @Doc: response场景
    //      - status
    //      - appData
    //          - appId                String
    //          - pageId               String
    //          - actionId             String
    //          - resultData           Map<String, Object>           { rows: [{}, {}, ...] }

    public JHResponse() { super(); }

    /**
     * 将指定的值设置到当前对象的指定路径上。
     *
     * @param path  表示属性路径的字符串，用于指定要设置值的位置。
     * @param value 要设置的值，其类型可以是任意对象。
     * @return 返回当前的JHResult对象，支持链式调用。
     */
    @Override
    public JHResponse set(String path, Object value) {
        path = path.replaceAll("-", "\\\\-");
        if (!path.startsWith("$.")) {
            path = "$." + path;
        }
        JSONPath.set(this, path, value);
        return this;
    }

    /**
     * 构造函数，根据包类型初始化JHResult对象。
     *
     * @param packageType 包类型
     */
    public JHResponse(String packageType) {
        String appId = System.getProperty("appId", "__default__");
        String packageId = System.currentTimeMillis() + "_" + new Random().nextInt(9999999);
        this.set("$.packageId", packageId);
        this.set("$.timestamp", DateUtil.date().toString(Constant.ISO8601));
        this.set("$.packageType", "httpResponse");
        this.set("$.appData.appId", appId);
        this.set("$.appData.resultData", null);
    }

    /**
     * 创建一个成功的响应结果。
     *
     * @param pageId     页面ID
     * @param actionId   操作ID
     * @param resultData 结果数据
     * @return 返回创建的JHResult对象
     */
    public static JHResponse success(String pageId, String actionId, Object resultData) {
        JHResponse jhResult = new JHResponse("httpResponse");
        jhResult.set("$.status", "success");
        jhResult.set("$.appData.pageId", pageId);
        jhResult.set("$.appData.actionId", actionId);
        jhResult.set("$.appData.resultData", resultData);
        return jhResult;
    }

    /**
     * 创建一个失败的响应结果。
     *
     * @param pageId     页面ID
     * @param actionId   操作ID
     * @param resultData 结果数据
     * @return 返回创建的JHResult对象
     */
    public static JHResponse fail(String pageId, String actionId, Object resultData) {
        JHResponse jhResult = new JHResponse("httpResponse");
        jhResult.set("$.status", "fail");
        jhResult.set("$.appData.pageId", pageId);
        jhResult.set("$.appData.actionId", actionId);
        jhResult.set("$.appData.resultData", resultData);
        return jhResult;
    }


    /**
     * 创建一个成功的响应结果，结果数据为空。
     *
     * @param pageId   页面ID
     * @param actionId 操作ID
     * @return 返回创建的JHResult对象
     */
    public static JHResponse success(String pageId, String actionId) {
        return success(pageId, actionId, new HashMap<String, Object>());
    }

    /**
     * 创建一个失败的响应结果，结果数据为空。
     *
     * @param pageId   页面ID
     * @param actionId 操作ID
     * @return 返回创建的JHResult对象
     */
    public static JHResponse fail(String pageId, String actionId) {
        return fail(pageId, actionId, null);
    }

    /**
     * 该方法将给定的数据设置到JSON对象的"appData.resultData"路径下。
     *
     * @param resultData 需要设置的结果数据，可以是任意类型。
     * @return 返回当前的JHResult实例，支持链式调用。
     */
    public JHResponse setResultData(Object resultData) {
        this.set("$.appData.resultData", resultData);
        return this;
    }


}
