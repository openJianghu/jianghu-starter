package org.jianghu.app.middleware;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.servlet.ServletUtil;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.common.RequestWrapper;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.context.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 在请求处理之前，初始化 context
 * 在请求处理之后，清空 context
 */
@Slf4j
@Component
public class ContextInterceptor implements HandlerInterceptor {

    @Autowired
    private JHConfig jhConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ContextHolder.set("app.config", jhConfig.getConfig());
        ContextHolder.set(ContextHolder.CONFIG, jhConfig.getConfig());

        RequestWrapper requestWrapper = new RequestWrapper(request);
        if (requestWrapper.getContentType() != null && requestWrapper.getContentType().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            ContextHolder.set(ContextHolder.REQUEST, JsonUtil.parseObject(ServletUtil.getBody(requestWrapper)).get("body"));
        } else {
            ContextHolder.set(ContextHolder.REQUEST, JsonUtil.parseObject(ServletUtil.getBody(requestWrapper)));
        }
        Map<String, String> headerMap = ServletUtil.getHeaderMap(requestWrapper);
        ContextHolder.set(ContextHolder.HEADERS, headerMap);
        String clientIP = headerMap.get("x-real-ip");
        if (clientIP == null) {
            clientIP = ServletUtil.getClientIP(requestWrapper);
        }
        ContextHolder.set(ContextHolder.IP, clientIP);
        ContextHolder.set(ContextHolder.PARAM_MAP, ServletUtil.getParamMap(requestWrapper));

        // cookies 处理
        ContextHolder.set(ContextHolder.COOKIES, getCookieMap(requestWrapper));

        // response
        ContextHolder.set(ContextHolder.RESPONSE, response);

        // 路由的变量
        Map<String, String> uriPathVariables = (Map<String, String>) requestWrapper.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        ContextHolder.set(ContextHolder.URI_PATH_VARIABLES, uriPathVariables);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        ContextHolder.clear();
    }

    private JSONPathObject getCookieMap(HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (ArrayUtil.isEmpty(cookies)) {
            return new JSONPathObject();
        }

        JSONPathObject cookiesMap = new JSONPathObject();
        for (Cookie cookie : cookies) {
            cookiesMap.set(cookie.getName(), cookie.getValue());
        }
        return cookiesMap;
    }

}
