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
import java.util.Objects;

@Slf4j
@Component
public class HttpAuthorization implements HandlerInterceptor {
    @Autowired
    JHConfig jhConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        JSONPathObject packageResource = ContextHolder.eval("packageResource");
        JSONPathObject user = ContextHolder.eval("userInfo.user");
        List<JSONPathObject> userGroupRoleList = ContextHolder.eval("userInfo.userGroupRoleList");
        List<JSONPathObject> userAppList = ContextHolder.eval("userInfo.userAppList");
        List<JSONPathObject> allowResourceList = ContextHolder.eval("userInfo.allowResourceList");

        String resourceId = packageResource.getString("resourceId");
        boolean isPublic = allowResourceList.stream().anyMatch(item -> item.eval("resourceId").equals(resourceId) && item.eval("isPublic", Boolean.class));
        if (isPublic) {
            return true;
        }

        // 1. 判断用户是否登录
        boolean isLoginUser = user != null && user.getString("userId") != null;
        if (!isLoginUser) {
            throw new BizException(BizEnum.request_token_invalid);
        }

        // 2. 判断用户状态
        String userStatus = user.getString("userStatus");
        if (userStatus.equals("banned")) {
            throw new BizException(BizEnum.user_banned);
        }
        if (!userStatus.equals("active")) {
            throw new BizException(BizEnum.user_status_error);
        }

        // 3. 判断当前请求groupId 是否在用户 group列表中
        boolean isGroupIdRequired = packageResource.getBooleanValue("groupIdRequired");
        String groupId = ContextHolder.eval("request.appData.actionData.groupId");
        if (isGroupIdRequired) {
            if (groupId == null) {
                throw new BizException(BizEnum.request_data_invalid, "groupId 不能为空");
            }
            if (userGroupRoleList.stream().noneMatch(item -> {
                JSONPathObject userGroupRole = item;
                return userGroupRole.getString("groupId").equals(groupId);
            })) {
                throw new BizException(BizEnum.request_group_forbidden);
            }
        }

        // 4. 判断用户是否有当前app的权限
        String appType = jhConfig.getConfig().getString("appType");
        if (Objects.equals(appType, "multiApp")) {
            if (userAppList.stream().noneMatch(item -> {
                JSONPathObject userApp = item;
                return userApp.getString("appId").equals(packageResource.getString("appId"));
            })) {
                throw new BizException(BizEnum.request_app_forbidden);
            }
        }

        // 5. 判断用户是否有 当前 packageResource 的权限
        boolean isNotAllow = allowResourceList.stream().noneMatch(item -> {
            JSONPathObject resource = item;
            return resource.getString("resourceId").equals(resourceId);
        });
        if (isNotAllow) {
            throw new BizException(BizEnum.resource_forbidden);
        }

        return true;
    }
}
