package org.jianghu.app.controller;

import cn.hutool.core.text.AntPathMatcher;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.context.ContextHolder;
import org.jianghu.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Controller
public class StaticController {

    @Autowired
    private UserService userService;

    @GetMapping("/${jianghu.config.appId}/public/**")
    public ResponseEntity<FileSystemResource> getPublicResource(HttpServletRequest request) throws IOException {
        String filepath = "/public/" + new AntPathMatcher().extractPathWithinPattern("/appId/public/**", request.getRequestURI());
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().getHeaderValue());
        return ResponseEntity.ok()
                .headers(headers)
                .body(new FileSystemResource(new ClassPathResource(filepath).getFile().getPath()));
    }

    @GetMapping("/${jianghu.config.appId}/upload/**")
    public ResponseEntity<FileSystemResource> getUploadResource(HttpServletRequest request) {
        String dir = System.getProperty("user.dir");
        String uploadPath = dir + "/upload/" + new AntPathMatcher().extractPathWithinPattern("/appId/upload/**", request.getRequestURI());

        Boolean enableUploadStaticFileCache = ContextHolder.eval("config.jianghuConfig.enableUploadStaticFileCache", true);
        Boolean enableUploadStaticFileAuthorization = ContextHolder.eval("config.jianghuConfig.enableUploadStaticFileAuthorization", false);
        Long uploadFileMaxAge = ContextHolder.eval("config.jianghuConfig.uploadFileMaxAge", 2592000000L); // 30d in milliseconds

        HttpHeaders headers = new HttpHeaders();

        if (enableUploadStaticFileCache) {
            if (enableUploadStaticFileAuthorization) {
                // 用户登录判断
                String authTokenKey = ContextHolder.eval("config.authTokenKey", "");
                String authToken = ContextHolder.eval("cookies." + authTokenKey + "_authToken");
                JSONPathObject user = userService.getUserFromJwtAuthToken(authToken);
                if (user.isEmpty("userId")) {
                    return ResponseEntity.status(403).body(null);
                }
            }
            headers.setCacheControl(CacheControl.maxAge(uploadFileMaxAge, TimeUnit.MILLISECONDS).getHeaderValue());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new FileSystemResource(uploadPath));
    }
}
