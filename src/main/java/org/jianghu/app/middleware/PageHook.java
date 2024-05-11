package org.jianghu.app.middleware;

import cn.hutool.core.util.ReflectUtil;
import org.jianghu.app.common.BizEnum;
import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.context.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.jianghu.app.controller.controllerUtil.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
@Component
public class PageHook implements HandlerInterceptor {

    @Autowired
    private ResourceUtil resourceUtil;

    private void invokeHook(JSONPathObject actionData, List<JSONPathObject> hooks) throws BizException, InvocationTargetException, IllegalAccessException {
        for (JSONPathObject hook : hooks) {
            String serviceFunction = hook.eval("serviceFunc");
            String field = hook.eval("field");
            Object service = resourceUtil.getServiceBean(hook.eval("service"));
            Method method = ReflectUtil.getMethod(service.getClass(), serviceFunction, JSONPathObject.class);
            if (method == null) {
                throw new BizException(BizEnum.resource_service_not_found);
            }
            try {
                Object invokeResult = method.invoke(service, actionData);
                if (invokeResult != null) {
                    ContextHolder.set(ContextHolder.HOOK_RESULT + "." + field, invokeResult);
                }
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getTargetException();
                if (targetException instanceof BizException) {
                    throw (BizException) targetException;
                } else {
                    throw new RuntimeException(targetException);
                }
            }
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        JSONPathObject actionData = ContextHolder.eval("request.appData.actionData");
        JSONPathObject pageHook = JsonUtil.parseObject(ContextHolder.eval("packagePage.pageHook", "{}"));
        List<JSONPathObject> beforeHooks = pageHook.eval("beforeHook", List.class);

        if (beforeHooks != null && !beforeHooks.isEmpty()) {
            invokeHook(actionData, beforeHooks);
        }

        return true;
    }

}
