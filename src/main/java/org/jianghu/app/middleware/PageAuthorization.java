package org.jianghu.app.middleware;

import org.jianghu.app.common.BizEnum;
import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.context.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PageAuthorization implements HandlerInterceptor {
    @Autowired
    JHConfig jhConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        JSONPathObject packagePage = ContextHolder.eval("packagePage", JSONPathObject.class);
        // page check
        if (ContextHolder.eval(ContextHolder.PACKAGE_PAGE) == null) {
            Map<String, String> uriPathVariables = ContextHolder.eval(ContextHolder.URI_PATH_VARIABLES);
            String pageId = uriPathVariables.get("pageId");
            ContextHolder.set("packagePage.pageId", pageId);
            throw new BizException(BizEnum.page_not_found, request.getRequestURL().toString());
        }

        JSONPathObject user = ContextHolder.eval("userInfo.user", JSONPathObject.class);
        List<JSONPathObject> userAppList = ContextHolder.eval("userInfo.userAppList", List.class);
        List<JSONPathObject> allowPageList = ContextHolder.eval("userInfo.allowPageList", List.class);
        String pageId = packagePage.getString("pageId");

        // 对于 public page ====》不需要做 用户状态的校验
        // public: { user: "*", group: "public", role: "*" }
        boolean isPublic = allowPageList.stream().filter(item -> {
            JSONPathObject resource = item;
            return resource.getString("pageId").equals(pageId);
        }).anyMatch(item -> {
            JSONPathObject resource = item;
            return resource.getBoolean("isPublic");
        });
        if (isPublic) {
            return true;
        }

        // 1. 判断用户是否登录
        boolean isLoginUser = user != null && user.getString("userId") != null;
        if (!isLoginUser) {
            String loginPage = jhConfig.eval("loginPage");
            response.sendRedirect(loginPage);
            return false;
        }

        // 2. 判断用户状态
        String userStatus = user.getString("userStatus");
        if (userStatus.equals("banned")) {
            throw new BizException("error", "用户被禁用");
        }
        if (!userStatus.equals("active")) {
            throw new BizException("error", "用户状态异常");
        }

        // 3. 判断用户是否有当前app的权限
        String appType = ContextHolder.eval("appType", "");
        if (appType.equals("multiApp")) {
            if (userAppList.stream().noneMatch(item -> {
                JSONPathObject userApp = item;
                return userApp.getString("appId").equals(packagePage.getString("appId"));
            })) {
                throw new BizException("error", "用户没有当前app的权限");
            }
        }

        // 4. 判断用户是否有 当前 packagePage 的权限
        boolean isNotAllow = allowPageList.stream().noneMatch(item -> {
            JSONPathObject resource = item;
            return resource.getString("pageId").equals(pageId);
        });
        if (isNotAllow) {
            throw new BizException("error", "用户没有当前页面的权限");
        }

        // 5. 已登录 从登录页自动则重定向到首页
        if (pageId.equals("login") && ContextHolder.eval("paramMap.errorCode") != null) {
            String indexPage = jhConfig.eval("indexPage");
            response.sendRedirect(indexPage);
            return false;
        }

        return true;
    }

}
