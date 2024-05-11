package org.jianghu.app.context;

import org.jianghu.app.common.JSONPathObject;

/**
 * 使用 threadlocal 来实现 ctx
 * 里面的结构：
 * request
 * response
 * packageResource
 * packagePage
 * host
 * path
 * query
 * userInfo
 * - userId
 * - userName
 * - user obj
 * - userAppList: []
 * - userGroupRoleList: []
 * - allowPageList: [page]
 * - allowResourceList: [resource]
 */
public class ContextHolder {

    public static final String CONFIG = "config";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String HEADERS = "headers";
    public static final String IP = "ip";
    public static final String PARAM_MAP = "paramMap";
    public static final String COOKIES = "cookies";
    public static final String URI_PATH_VARIABLES = "uriPathVariables";
    public static final String PACKAGE_RESOURCE = "packageResource";
    public static final String PACKAGE_PAGE = "packagePage";
    public static final String HOOK_RESULT = "hookResult";
    public static final String ERROR = "error";
    public static final String USER_INFO = "userInfo";
    // 上下文 ctx
    public static ThreadLocal<JSONPathObject> ctx = ThreadLocal.withInitial(JSONPathObject::new);

    public ContextHolder() {
    }

    public static JSONPathObject getCtx() {
        return ctx.get();
    }

    public static void set(String path, Object value) {
        ctx.get().set(path, value);
    }

    public static <T> T eval(String path) {
        return ctx.get().eval(path);
    }

    public static <T> T eval(String path, T defaultVal) {
        return ctx.get().eval(path, defaultVal);
    }

    public static <T> T eval(String path, Class<T> clazz) {
        return ctx.get().eval(path, clazz);
    }

    public static <T> T eval(String path, T defaultVal, Class<T> clazz) {
        return ctx.get().eval(path, defaultVal, clazz);
    }

    public static void clear() {
        ctx.remove();
    }

}
