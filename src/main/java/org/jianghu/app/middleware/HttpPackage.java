package org.jianghu.app.middleware;

import com.alibaba.fastjson2.JSONWriter;
import org.jianghu.app.common.*;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.config.JianghuKnex;
import org.jianghu.app.context.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
public class HttpPackage implements HandlerInterceptor {

    @Autowired
    private JianghuKnex jianghuKnex;
    @Autowired
    private JHConfig jhConfig;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ContextHolder.set("startTime", System.currentTimeMillis());
        ValidateUtil.validate(Constant.VALIDATE_SCHEMA_REQUEST_BODY, ContextHolder.eval(ContextHolder.REQUEST), "请求body不符合规范");
        String pageId = ContextHolder.eval("request.appData.pageId");
        String actionId = ContextHolder.eval("request.appData.actionId");
        String resourceSelectSql = String.format("knex('_resource').where({ pageId: '%s', actionId: '%s', }).first()", pageId, actionId);
        JSONPathObject resource = jianghuKnex.first(resourceSelectSql);
        if (resource == null) { throw new BizException(BizEnum.resource_not_found); }
        resource.set("resourceId", resource.eval("pageId") + "." + resource.eval("actionId"));

        JSONPathObject resourceData = JsonUtil.parseObject(resource.eval("resourceData", "{}", String.class));
        JSONPathObject appDataSchema = JsonUtil.parseObject(resource.eval("appDataSchema", "{}", String.class));
        resource.set("resourceData", resourceData);
        resource.set("appDataSchema", appDataSchema);
        ContextHolder.set(ContextHolder.PACKAGE_RESOURCE, resource);
        return true;
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        try {
            Map<String, String> ignoreListOfResourceLogRecord = jhConfig.eval("jianghuConfig.ignoreListOfResourceLogRecord", Map.class);
            Collection<String> ignoreListOfResourceLogRecordList = ignoreListOfResourceLogRecord.values();
            String resourceId = ContextHolder.eval("packageResource.resourceId");
            ResponseWrapper responseWrapper = (ResponseWrapper) response;
            String responseBody = new String(responseWrapper.getBody(), StandardCharsets.UTF_8);
            String requestBody = ContextHolder.eval(ContextHolder.REQUEST, String.class);
            JSONPathObject responsePackage = JsonUtil.parseObject(responseBody);
            JSONPathObject resourceLogInfo = JSONPathObject
                    .of("packageId", ContextHolder.eval("request.packageId"))
                    .set("resourceId", resourceId)
                    .set("deviceId", ContextHolder.eval("userInfo.user.deviceId"))
                    .set("userId", ContextHolder.eval("userInfo.userId"))
                    .set("userIp", ContextHolder.eval(ContextHolder.IP))
                    .set("userAgent", ContextHolder.eval("request.appData.userAgent", String.class))
                    .set("requestBody", requestBody.length() > 8144 ? "请求文本太大!" : requestBody)
                    .set("responseBody", responseBody.length() > 8144 ? "响应文本太大!" : responseBody)
                    .set("responseStatus", responsePackage.eval("status"));

            Long startTime = ContextHolder.eval("startTime", Long.class);
            Long useTime = System.currentTimeMillis() - startTime;
            if (!ignoreListOfResourceLogRecordList.contains(resourceId)) {
                log.info("[resource] [{} useTime: '{}/ms'] {}", resourceId, useTime, resourceLogInfo.toJSONString(JSONWriter.Feature.PrettyFormat));
            }
        } catch (Exception e) {
            log.error("[resource] 记录日志失败", e);
        }
    }

}
