package org.jianghu.app.common;

public enum BizEnum {
    request_body_invalid("request_body_invalid", "请求body不符合规范"),
    request_data_invalid("request_data_invalid", "请求data不符合规范"),
    request_repeated("request_repeated", "重复的请求"),
    request_token_expired("request_token_expired", "token失效"),
    request_token_invalid("request_token_invalid", "无效token"),
    request_app_forbidden("request_app_forbidden", "你没有这个应用的访问权限"),
    request_group_forbidden("request_group_forbidden", "你不在当前群内"),
    request_rate_limit_exceeded("request_rate_limit_exceeded", "请求过于频繁，请稍后再试"),

    server_error("server_error", "服务器开小差了"),

    resource_sql_operation_invalid("resource_sql_operation_invalid", "无效的operation !"),
    resource_forbidden("resource_forbidden", "无执行权限"),
    resource_not_found("resource_not_found", "协议不存在"),
    resource_not_support("resource_not_support", "协议不支持"),
    resource_sql_missing_params("resource_sql_missing_params", "缺少必填参数"),
    resource_sql_where_options_invalid("resource_sql_where_options_invalid", "无效的 whereOptions 参数"),
    resource_sql_need_condition("resource_sql_need_condition", "更新或删除数据需要提供数据条件"),
    resource_sql_unique_check_fail("resource_sql_unique_check_fail", "数据已存在! 请勿重复操作。"),
    resource_sql_exception_of_update_and_delete("resource_sql_exception_of_update_and_delete", "Sql操作异常! 请追加where条件。"),
    resource_service_not_found("resource_service_not_found", "接口不存在"),
    resource_service_method_not_found("resource_service_method_not_found", "接口(方法)不存在"),

    page_forbidden("page_forbidden", "无访问权限"),
    page_not_found("page_not_found", "页面不存在"),

    data_not_found("data_not_found", "数据不存在"),

    user_not_exist("request_user_not_exist", "用户不存在"),
    login_user_not_exist("login_user_not_exist", "用户不存在"),
    user_password_error("user_password_error", "用户名 或 密码错误, 请重新输入!"),
    user_banned("user_banned", "账号被封禁! 请联系管理员。"),
    user_status_error("user_status_error", "用户状态异常! "),
    user_password_reset_old_error("user_password_reset_old_error", "旧密码错误, 请重新输入!"),
    user_password_reset_same_error("user_password_reset_same_error", "新旧密码不能一样, 请重新输入!"),
    wx_login_config_error("wx_login_config_error", "微信登录配置异常"),
    wx_login_error("wx_login_error", "微信登录异常"),

    file_directory("file_directory", "文件目录异常"),
    file_please_upload_file("file_please_upload_file", "请选择文件"),
    file_damaged("file_damaged", "上传失败! 文件损坏!"),
    file_is_incomplete("file_is_incomplete", "文件不完整, 请重新上传!"),
    file_buffer_is_null("file_buffer_is_null", "文件是空的!"),
    file_base64_is_null("file_base64_is_null", "文件是空的!"),
    file_not_found("file_not_found", "文件不存在!"),

    resource_sql_where_options_missing("resource_sql_where_options_missing", "缺少where条件"),
    user_id_exist("user_id_exist","用户id已存在"),
    page_render_error("page_render_error", "页面渲染异常" ),
    resource_error("resource_error", "Resource异常"),
    resource_hook_error("resource_hook_error", "ResourceHook异常"),
    page_hook_error("resource_hook_error", "PageHook异常"),
    valid_schema_invalid("@JHValid schema is invalid", "@JHValid schema不是JSON字符串"),
    knex_execution_exception("knex_execution_exception",  "knex执行异常");

    private final String errorCode;
    private final String errorReason;

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorReason() {
        return errorReason;
    }

    BizEnum(String errorCode, String errorReason) {
        this.errorCode = errorCode;
        this.errorReason = errorReason;
    }
}

