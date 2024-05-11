package org.jianghu.app.middleware;

import cn.hutool.core.collection.ListUtil;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.config.JianghuKnex;
import org.jianghu.app.context.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class PagePackage implements HandlerInterceptor {

    @Autowired
    private JianghuKnex jianghuKnex;
    @Autowired
    private JHConfig jhConfig;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Map<String, String> uriPathVariables = ContextHolder.eval(ContextHolder.URI_PATH_VARIABLES);
        String pageId = uriPathVariables.get("pageId");

        JSONPathObject page = null;

        // 除了特殊的多层路由，其它多层路由都变成 /page/pageName/param0/param1...
        // 并将 param[] 放到 ctx.pathParams 中
        if (pageId.contains("/")) {
            String[] parts = pageId.split("/");
            String pageSelectSql = String.format("knex('_page').where({ pageId: '%s' }).first()", parts[0]);
            page = jianghuKnex.first(pageSelectSql);
            if (page != null && Objects.equals(page.eval("pageType", String.class), "seo")) {
                ContextHolder.set("pathParams", ListUtil.sub(Arrays.asList(parts), 1, parts.length));
            }
        }

        if (page == null) {
            String pageSelectSql = String.format("knex('_page').where({ pageId: '%s' }).first()", pageId);
            page = jianghuKnex.first(pageSelectSql);
        }
        ContextHolder.set(ContextHolder.PACKAGE_PAGE, page);

        return true;
    }
}
