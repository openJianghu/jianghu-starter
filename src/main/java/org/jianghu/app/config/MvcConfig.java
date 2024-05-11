package org.jianghu.app.config;

import org.jianghu.app.middleware.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private JHConfig jhConfig;
    @Autowired
    private ContextInterceptor contextInterceptor;
    @Autowired
    private PagePackage pagePackage;
    @Autowired
    private PageAuthorization pageAuthorization;
    @Autowired
    private PageUserInfo pageUserInfo;
    @Autowired
    private PageHook pageHook;
    @Autowired
    private HttpPackage httpPackage;
    @Autowired
    private HttpAuthorization httpAuthorization;
    @Autowired
    private HttpUserInfo httpUserInfo;
    @Autowired
    private HttpResourceHook httpResourceHook;
    @Autowired
    private ImageResizeInterceptor imageResizeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 处理器会按照顺序执行，所以这里的顺序很重要，所以如果要加权限验证，要在这个处理器之前添加
        // 上下文 threadlocal 处理器
        registry.addInterceptor(contextInterceptor).addPathPatterns("/**");
        // 鉴权 resource
        String resourcePathMatch = "/" + jhConfig.eval("appId") + "/resource";
        registry.addInterceptor(httpPackage).addPathPatterns(resourcePathMatch);
        registry.addInterceptor(httpUserInfo).addPathPatterns(resourcePathMatch);
        registry.addInterceptor(httpAuthorization).addPathPatterns(resourcePathMatch);
        registry.addInterceptor(httpResourceHook).addPathPatterns(resourcePathMatch);
        // 鉴权 page
        String pagePathMatch = "/" + jhConfig.eval("appId") + "/page/**";
        registry.addInterceptor(pagePackage).addPathPatterns(pagePathMatch);
        registry.addInterceptor(pageUserInfo).addPathPatterns(pagePathMatch);
        registry.addInterceptor(pageAuthorization).addPathPatterns(pagePathMatch);
        registry.addInterceptor(pageHook).addPathPatterns(pagePathMatch);
        // 图片
        String imagePathMatch = "/" + jhConfig.eval("appId") + "/upload/**";
        registry.addInterceptor(imageResizeInterceptor).addPathPatterns(imagePathMatch);
    }

}
