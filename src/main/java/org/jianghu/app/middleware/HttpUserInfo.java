package org.jianghu.app.middleware;

import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.context.ContextHolder;
import org.jianghu.app.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class HttpUserInfo implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 默认放空
        ContextHolder.set("userInfo", new JSONPathObject());
        // 获取 userInfo
        String authToken = ContextHolder.eval("request.appData.authToken");
        ContextHolder.getCtx().remove("request.appData.authToken");
        JSONPathObject userInfo = userService.getUserInfo(authToken);
        ContextHolder.set(ContextHolder.USER_INFO, userInfo);
        return true;
    }
}
