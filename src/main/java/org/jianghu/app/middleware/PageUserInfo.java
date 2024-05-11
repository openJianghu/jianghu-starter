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
public class PageUserInfo implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 默认放空
        ContextHolder.set("userInfo", new JSONPathObject());

        // 获取 userInfo
        String authTokenKey = ContextHolder.eval("config.authTokenKey", ContextHolder.eval("config.appId", String.class));
        String authToken = ContextHolder.eval("cookies." + authTokenKey + "_authToken");
        JSONPathObject userInfo = userService.getUserInfo(authToken);
        if (userInfo == null) {
            return true;
        }
        ContextHolder.set(ContextHolder.USER_INFO, userInfo);
        return true;
    }
}
