package org.jianghu.app.config;

import org.jianghu.app.common.BizEnum;
import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JHResponse;
import org.jianghu.app.config.jinjava.ViewRender;
import org.jianghu.app.context.ContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private JHConfig jhConfig;

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Object> handleBizException(BizException e, HttpServletRequest request, HttpServletResponse response) throws Exception {
        e.printStackTrace();
        String errorCode = e.getErrorCode();
        String errorReason = e.getErrorReason();
        String errorReasonSupplement = e.getErrorReasonSupplement();
        ContextHolder.set("error.errorCode", errorCode);
        ContextHolder.set("error.errorReason", errorReason);
        ContextHolder.set("error.errorReasonSupplement", errorReasonSupplement == null ? "" : errorReasonSupplement);

        if (errorCode.equals("request_token_invalid") ||
                errorCode.equals("request_user_not_exist") ||
                errorCode.equals("request_token_expired") ||
                errorCode.equals("request_app_forbidden") ||
                errorCode.equals("user_banned")) {
            clearCookie(request, response);
        }
        String pageId = ContextHolder.eval("request.appData.pageId");
        String actionId = ContextHolder.eval("request.appData.actionId");
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/page/")) {
            try {
                String pageContent = ViewRender.renderPage("/page/helpV4.html");
                return ResponseEntity.ok(pageContent);
            } catch (Exception renderException) {
                renderException.printStackTrace();
                return ResponseEntity.ok("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>500</title></head>\n" +
                        "<body>\n<h1>Error</h1>\n<p>" + e.getMessage() + "</p>\n</body>\n</html>");
            }
        }
        JHResponse responseBody = JHResponse
                .fail(pageId, actionId)
                .set("appData.errorCode", errorCode)
                .set("appData.errorReason", errorReason)
                .set("appData.errorReasonSupplement", errorReasonSupplement);
        return ResponseEntity.ok(responseBody);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws Exception{
        e.printStackTrace();
        String errorCode = BizEnum.server_error.getErrorCode();
        String errorReason = BizEnum.server_error.getErrorReason();
        String errorReasonSupplement = e.getMessage() != null && e.getMessage().length() > 1000 ? e.getMessage().substring(0, 1000) + "..." : e.getMessage();
        ContextHolder.set("error.errorCode", errorCode);
        ContextHolder.set("error.errorReason", errorReason);
        ContextHolder.set("error.errorReasonSupplement", errorReasonSupplement == null ? "" : errorReasonSupplement);

        String pageId = ContextHolder.eval("request.appData.pageId");
        String actionId = ContextHolder.eval("request.appData.actionId");
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/page/")) {
            try {
                String pageContent = ViewRender.renderPage("/page/helpV4.html");
                return ResponseEntity.ok(pageContent);
            } catch (Exception renderException) {
                renderException.printStackTrace();
                return ResponseEntity.ok("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>500</title></head>\n" +
                        "<body>\n<h1>Error</h1>\n<p>" + e.getMessage() + "</p>\n</body>\n</html>");
            }
        }
        if (requestURI.contains("/public/") || requestURI.contains("/upload/")) {
            if (e instanceof FileNotFoundException) {
                return ResponseEntity.notFound().build();
            }
        }
        JHResponse responseBody = JHResponse
                .fail(pageId, actionId)
                .set("appData.errorCode", errorCode)
                .set("appData.errorReason", errorReason)
                .set("appData.errorReasonSupplement", errorReasonSupplement);
        return ResponseEntity.ok(responseBody);
    }

    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        String cookieName = System.getProperty("appId") + "_authToken";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    cookie.setValue(null);
                    cookie.setMaxAge(0); // 设置cookie立即过期
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }


}
