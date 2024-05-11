package org.jianghu.app.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.SecureUtil;
import org.jianghu.app.common.BizEnum;
import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.common.annotation.JHValidate;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.config.JianghuKnex;
import org.jianghu.app.context.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("userServiceSystem")
public class UserService {

    @Autowired
    JHConfig jhConfig;

    @Autowired
    private JianghuKnex jianghuKnex;

    public JSONPathObject getUserInfo(String authToken) {
        return this.getUserInfo(authToken, null);
    }

    public JSONPathObject getUserInfo(String authToken, String groupId) {
        JSONPathObject user = this.getUserFromJwtAuthToken(authToken);
        String userId = user.getString("userId");
        String username = user.getString("username");

        JSONPathObject userInfoResult = JSONPathObject.of()
                .set("userId", userId)
                .set("username", username)
                .set("user", user);

        userInfoResult.putAll(this.captureUserRuleData(userId, groupId));

        return userInfoResult;

    }

    /**
     * 获取用户的 userGroupRoleList, allowResourceList, allowPageList, userAppList
     */
    private JSONPathObject captureUserRuleData(String userId, String groupId) {
        List<JSONPathObject> userGroupRoleList = new ArrayList<>();
        if (userId != null) {
            // Tip: resource指定groupId后 ===> 只能取当前的groupId ===> params: { groupId }
            if (groupId != null) {
                userGroupRoleList = jianghuKnex.select(
                        "knex('_user_group_role').where({ userId: ${userId}, groupId: ${groupId} }).select()",
                        JSONPathObject.of().set("userId", userId).set("groupId", groupId)
                );
            } else {
                userGroupRoleList = jianghuKnex.select(
                        "knex('_user_group_role').where({ userId: ${userId} }).select()",
                        JSONPathObject.of().set("userId", userId)
                );
            }
        }
        // 默认规则
        userGroupRoleList.add(JSONPathObject.of().set("userId", userId).set("groupId", "public").set("roleId", "--"));
        if (userId != null) {
            userGroupRoleList.add(JSONPathObject.of().set("userId", userId).set("groupId", "login").set("roleId", "--"));
        }

        List<JSONPathObject> allowResourceList = this.captureAllowResourceList(userGroupRoleList);
        List<JSONPathObject> allowPageList = this.captureAllowPageList(userGroupRoleList);

        String appType = jhConfig.getConfig().getString("appType");
        List<JSONPathObject> userAppList = Objects.equals(appType, "multiApp") ? this.captureUserAppList(userId) : new ArrayList<JSONPathObject>();

        // Tip: 需要把 groupName 和 roleName 带出来, 而且public和login不需要带出来
        List<JSONPathObject> userGroupRoleListForShow = new ArrayList<>();
        if (userId != null) {
            userGroupRoleListForShow = jianghuKnex.select("knex('_user_group_role')\n" +
                    "        .leftJoin('_role', '_user_group_role.roleId', '_role.roleId')\n" +
                    "        .leftJoin('_group', '_user_group_role.groupId', '_group.groupId')\n" +
                    "        .where('userId', ${userId})\n" +
                    "        .select(\n" +
                    "          '_user_group_role.userId AS userId',\n" +
                    "          '_user_group_role.groupId AS groupId',\n" +
                    "          '_group.groupName AS groupName',\n" +
                    "          '_user_group_role.roleId AS roleId',\n" +
                    "          '_role.roleName AS roleName'\n" +
                    "        )", JSONPathObject.of("userId", userId));
        }

        return JSONPathObject.of()
                .set("userGroupRoleList", userGroupRoleListForShow)
                .set("allowResourceList", allowResourceList)
                .set("allowPageList", allowPageList)
                .set("userAppList", userAppList);
    }

    private List<JSONPathObject> captureAllowResourceList(List<JSONPathObject> userGroupRoleList) {
        List<JSONPathObject> allResourceList = jianghuKnex.select("knex('_resource').select()");
        for (JSONPathObject resourceObj : allResourceList) {
            resourceObj.set("resourceId", resourceObj.getString("pageId") + "." + resourceObj.getString("actionId"));
        }
        List<JSONPathObject> allUserGroupRoleResourceList = jianghuKnex.select("knex('_user_group_role_resource').select()");
        List<JSONPathObject> allowResourceList = this.computeAllowList("resource", allResourceList, allUserGroupRoleResourceList, userGroupRoleList);
        return allowResourceList.stream().map(resource -> {
            JSONPathObject resourceObj = JsonUtil.toJSON(resource);
            return JSONPathObject.of()
                    .set("resourceId", resourceObj.get("resourceId"))
                    .set("pageId", resourceObj.get("pageId"))
                    .set("actionId", resourceObj.get("actionId"))
                    .set("resourceType", resourceObj.get("resourceType"))
                    .set("isPublic", resourceObj.get("isPublic"));
        }).collect(Collectors.toList());
    }

    private List<JSONPathObject> captureAllowPageList(List<JSONPathObject> userGroupRoleList) {
        List<JSONPathObject> allPageList = jianghuKnex.select("knex('_page').select()");
        List<JSONPathObject> allUserGroupRolePageList = jianghuKnex.select("knex('_user_group_role_page').select()");
        return this.computeAllowList("page", allPageList, allUserGroupRolePageList, userGroupRoleList);
    }

    private List<JSONPathObject> captureUserAppList(String userId) {
        return jianghuKnex.select(
                "knex('_user_app').where({ userId: ${userId} }).select()",
                JSONPathObject.of().set("userId", userId)
        );
    }

    private List<JSONPathObject> computeAllowList(String fieldKey, List<JSONPathObject> allItemList, List<JSONPathObject> allRuleList, List<JSONPathObject> userGroupRoleList) {
        String idFieldKey = fieldKey + "Id";
        List<JSONPathObject> allowItemList = new ArrayList<JSONPathObject>();
        Map<String, JSONPathObject> allItemMap = allItemList.stream().collect(Collectors.toMap(item -> item.eval(idFieldKey), item -> item));

        if (allItemList.isEmpty() || allRuleList.isEmpty()) {
            return allowItemList;
        }

        allItemList.forEach(item -> {
            JSONPathObject itemObj = JsonUtil.toJSON(item);
            String resultAllowOrDeny = "";
            boolean isPublic = false;
            for (Object rule : allRuleList) {
                JSONPathObject ruleObj = JsonUtil.toJSON(rule);
                // deny 的优先级高于全部，一旦有 deny 则不再需要判断
                if (resultAllowOrDeny.equals("deny")) {
                    continue;
                }
                if (!this.checkResource(itemObj.getString(idFieldKey), ruleObj.getString(fieldKey))) {
                    continue;
                }
                // 判断这条规则是否和当前用户匹配
                if (!this.checkRule(userGroupRoleList, ruleObj)) {
                    continue;
                }

                if (ruleObj.getString("group").equals("public")) {
                    isPublic = true;
                }
                resultAllowOrDeny = ruleObj.getString("allowOrDeny");
            }

            if (resultAllowOrDeny.equals("allow")) {
                JSONPathObject allItem = allItemMap.get(itemObj.getString(idFieldKey)).clone();
                allItem.set("isPublic", isPublic);
                allowItemList.add(allItem);
            }
        });
        return allowItemList;
    }

    /**
     * userGroupRoleList 是否和当前 rule匹配
     */
    private boolean checkRule(List<JSONPathObject> userGroupRoleList, JSONPathObject ruleObj) {
        List<JSONPathObject> userGroupRoleListRule = userGroupRoleList.stream()
                .filter(userGroupRole -> this.checkResource(userGroupRole.eval("userId"), ruleObj.eval("user")))
                .filter(userGroupRole -> this.checkResource(userGroupRole.eval("groupId"), ruleObj.eval("group")))
                .filter(userGroupRole -> this.checkResource(userGroupRole.eval("roleId"), ruleObj.eval("role")))
                .collect(Collectors.toList());
        return !userGroupRoleListRule.isEmpty();
    }

    /**
     * 判断资源是否符合规则，支持逗号及后缀通配符
     */
    private boolean checkResource(String checkResource, String ruleResource) {
        String[] ruleParts = ruleResource.split(",");
        return Arrays.stream(ruleParts).anyMatch(ruleValue -> {
            // 将后缀通配符转成正常正则
            String ruleValueWithoutSuffix = ruleValue
                    .replaceAll("\\.", "\\\\.")
                    .replaceAll("\\|", "\\\\|")
                    .replaceAll("\\*", ".*");
            String checkResourceValue = checkResource == null ? "" : checkResource;
            return checkResourceValue.matches("^" + ruleValueWithoutSuffix + "$");
        });
    }

    public JSONPathObject getUserFromJwtAuthToken(String authToken) {
        JSONPathObject user = JSONPathObject.of();
        if (StringUtils.isEmpty(authToken)) {
            return user;
        }

        JSONPathObject userSession = jianghuKnex.first(
                "knex('_user_session').where({ authToken: ${authToken} }).first()",
                JSONPathObject.of().set("authToken", authToken)
        );
        if (userSession != null && !userSession.isEmpty("userId")) {
            JSONPathObject userResult = jianghuKnex.first(
                    "knex('_view01_user').where({ userId: ${userId} }).first()",
                    userSession
            );
            if (userResult != null) {
                userResult.remove("clearTextPassword");
                userResult.remove("password");
                userResult.remove("md5Salt");
                user = userResult;
            }
        }
        return user;
    }

    public JSONPathObject passwordLogin(@JHValidate("{\n" +
            "    type: 'object',\n" +
            "    additionalProperties: true,\n" +
            "    required: [ 'userId', 'password', 'deviceId' ],\n" +
            "    properties: {\n" +
            "      userId: { type: 'string', minLength: 3 },\n" +
            "      password: { type: 'string' },\n" +
            "      deviceId: { type: 'string' },\n" +
            "      deviceType: { type: 'string' },\n" +
            "      needSetCookies: { anyOf: [{ type: 'boolean' }, { type: 'null' }] },\n" +
            "    },\n" +
            "  }")JSONPathObject actionData) throws Exception {

        String userId = actionData.eval("userId");
        String password = actionData.eval("password");
        String deviceType = actionData.eval("deviceType");
        String deviceId = actionData.eval("deviceId");
        Boolean needSetCookies = actionData.eval("needSetCookies");
        if (needSetCookies == null) {
            needSetCookies = true;
        }

        JSONPathObject userResult = jianghuKnex.first(
                "knex('_view01_user').where({ userId: ${userId} }).first()",
                JSONPathObject.of().set("userId", userId)
        );
        if (userResult == null || userResult.isEmpty("userId") || !Objects.equals(userResult.eval("userId"), userId)) {
            throw new BizException(BizEnum.login_user_not_exist);
        }

        String userStatus = userResult.eval("userStatus");
        if (!Objects.equals(userStatus, "active")) {
            if (Objects.equals(userStatus, "banned")) {
                throw new BizException(BizEnum.user_banned);
            }
            throw new BizException(BizEnum.user_status_error);
        }

        String passwordMd5 = SecureUtil.md5(password + "_" + userResult.eval("md5Salt"));
        if (!Objects.equals(passwordMd5, userResult.eval("password"))) {
            throw new BizException(BizEnum.user_password_error);
        }

        String authToken = UUID.randomUUID().toString();
        // 存session 的目的是为了
        //   1. 系统可以根据这个判断是否是自己生成的token
        //   2. 有时候系统升级需要 用户重新登陆/重新登陆，这时候可以通过清理旧session达到目的
        JSONPathObject userSession = jianghuKnex.first(
                "knex('_user_session').where({ userId: ${userId}, deviceId: ${deviceId} }).first()",
                JSONPathObject.of().set("userId", userId).set("deviceId", deviceId)
        );

        String userAgent = ContextHolder.eval("request.appData.userAgent", "");
        String userIp = ContextHolder.eval(ContextHolder.IP, "");

        if (userSession != null && !userSession.isEmpty("id")) {
            jianghuKnex.update(
                    "knex('_user_session').where({ id: ${sessionId} }).update({ authToken: ${authToken}, deviceType: ${deviceType}, userAgent: ${userAgent}, userIp: ${userIp} })",
                    JSONPathObject.of()
                            .set("sessionId", userSession.eval("id"))
                            .set("authToken", authToken)
                            .set("deviceType", deviceType)
                            .set("userAgent", userAgent)
                            .set("userIp", userIp)
            );
        } else {
            jianghuKnex.insert(
                    "knex('_user_session').insert({ userId: ${userId}, deviceId: ${deviceId}, userAgent: ${userAgent}, userIp: ${userIp}, deviceType: ${deviceType}, authToken: ${authToken} })",
                    JSONPathObject.of()
                            .set("userId", userId)
                            .set("deviceId", deviceId)
                            .set("userAgent", userAgent)
                            .set("userIp", userIp)
                            .set("deviceType", deviceType)
                            .set("authToken", authToken)
            );
        }

        if (needSetCookies) {
            HttpServletResponse response = ContextHolder.eval("response");
            String authTokenKey = ContextHolder.eval("config.authTokenKey", "");
            Cookie cookie = new Cookie(authTokenKey + "_authToken", authToken);
            cookie.setMaxAge(60 * 60 * 24 * 1080);
            cookie.setHttpOnly(false);
            response.addCookie(cookie);
        }

        return JSONPathObject.of()
                .set("userId", userId)
                .set("deviceId", deviceId)
                .set("authToken", authToken);
    }

    public void logout(JSONPathObject actionData) throws BizException {
        Boolean needSetCookies = actionData.eval("needSetCookies");
        if (needSetCookies == null) {
            needSetCookies = true;
        }

        if (needSetCookies) {
            HttpServletResponse response = ContextHolder.eval("response");
            String authTokenKey = ContextHolder.eval("config.authTokenKey", "");
            Cookie cookie = new Cookie(authTokenKey + "_authToken", null);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }

        JSONPathObject user = ContextHolder.eval("userInfo.user");
        if (user == null || user.isEmpty("userId")) {
            throw new BizException(BizEnum.user_not_exist);
        }

        JSONPathObject userSession = jianghuKnex.first(
                "knex('_user_session').where({ userId: ${userId}, deviceId: ${deviceId} }).first()",
                user
        );

        if (userSession != null && !userSession.isEmpty("id")) {
            // update user session authToken to ''
            jianghuKnex.update(
                    "knex('_user_session').where({ id: ${sessionId} }).update({ authToken: '' })",
                    JSONPathObject.of().set("sessionId", userSession.eval("id")));
        }
    }

    public void resetPassword(JSONPathObject actionData) throws BizException {
        String userId = ContextHolder.eval("userInfo.userId");
        JSONPathObject user = jianghuKnex.first(
                "knex('_user').where({ id: ${userId} }).first()",
                JSONPathObject.of().set("userId", userId)
        );

        String oldPassword = actionData.eval("oldPassword");
        String newPassword = actionData.eval("newPassword");

        // 旧密码检查
        String passwordMd5 = SecureUtil.md5(oldPassword + "_" + user.eval("md5Salt"));
        if (!Objects.equals(passwordMd5, user.eval("password"))) {
            throw new BizException(BizEnum.user_password_reset_old_error);
        }

        // 密码一致检查
        if (Objects.equals(oldPassword, newPassword)) {
            throw new BizException(BizEnum.user_password_reset_same_error);
        }

        // 修改数据库中密码
        String newMd5Salt = UUID.randomUUID().toString();
        String newPasswordMd5 = SecureUtil.md5(newPassword + "_" + newMd5Salt);
        jianghuKnex.update(
                "knex('_user').where({ userId: ${userId} }).update({ password: ${newPasswordMd5}, clearTextPassword: ${clearTextPassword}, md5Salt: ${newMd5Salt} })",
                JSONPathObject.of()
                        .set("userId", userId)
                        .set("newPasswordMd5", newPasswordMd5)
                        .set("clearTextPassword", newPassword)
                        .set("newMd5Salt", newMd5Salt)
        );
        // 更新 user session 的所有 authToken
        jianghuKnex.update(
                "knex('_user_session').where({ userId: ${userId} }).update({ authToken: '' })",
                JSONPathObject.of().set("userId", userId)
        );
    }

}
