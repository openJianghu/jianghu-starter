package org.jianghu.app.controller;

import com.alibaba.fastjson2.JSONWriter;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.config.jinjava.ViewRender;
import org.jianghu.app.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.sql.SQLException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    @Autowired
    private JHConfig jhConfig;

    @GetMapping(value ="/${jianghu.config.appId}/pageDoc", produces= "text/html;charset=UTF-8")
    @ResponseBody
    public String pageDoc() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>手册</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>《操作手册》开发中, 敬请期待!</h1>\n" +
                "</body>\n" +
                "</html>";
    }


    @GetMapping({"/", "/${jianghu.config.appId}", "/${jianghu.config.appId}/page"})
    public String indexPage() {
        String indexPage = jhConfig.eval("indexPage");
        return String.format("redirect:%s", indexPage);
    }

    @GetMapping( value = "/${jianghu.config.appId}/page/{pageId}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String getPage() throws IOException, SQLException {
        long startTime = System.currentTimeMillis();
        String pageId = ContextHolder.eval("packagePage.pageFile", ContextHolder.eval("packagePage.pageId", String.class));
        String pagePath = String.format("page/%s.html", pageId);
        String pageContent = ViewRender.renderPage(pagePath);
        try {
            JSONPathObject pageLogInfo = JSONPathObject
                    .of("pageId", pageId)
                    .set("pageName", ContextHolder.eval("packagePage.pageName"))
                    .set("deviceId", ContextHolder.eval("userInfo.user.deviceId"))
                    .set("deviceType", ContextHolder.eval("userInfo.user.deviceType"))
                    .set("userId", ContextHolder.eval("userInfo.userId"))
                    .set("username", ContextHolder.eval("userInfo.username"));

            log.info("[page] [/page/{} useTime: '{}/ms'] {}", pageId, System.currentTimeMillis() - startTime, pageLogInfo.toJSONString(JSONWriter.Feature.PrettyFormat));
        } catch (Exception e) {
            log.error("[page] [/page/{}] 日志打印报错", pageId, e);
        }
        return pageContent;
    }
}
