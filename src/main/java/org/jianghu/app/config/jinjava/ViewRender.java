package org.jianghu.app.config.jinjava;

import org.jianghu.app.common.BizEnum;
import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.context.ContextHolder;
import com.google.common.base.Charsets;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.LegacyOverrides;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.RenderResult;
import com.hubspot.jinjava.interpret.TemplateError;
import com.hubspot.jinjava.lib.filter.Filter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

public class ViewRender {


    public static String renderPage(String pagePath) throws IOException {
        // ctxForRender
        JSONPathObject ctxForRender = new JSONPathObject().set("ctx", ContextHolder.getCtx());
        JSONPathObject hookResult = ContextHolder.eval(ContextHolder.HOOK_RESULT, JSONPathObject.class);
        if (hookResult != null) {
            ctxForRender.putAll(hookResult);
        }
        ctxForRender.set("page.passcode", ContextHolder.eval("packagePage.passcode", String.class));


        JinjavaConfig jConfig = JinjavaConfig.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .withFailOnUnknownTokens(true) // 没有的值，异常抛出
                .withLegacyOverrides(LegacyOverrides.newBuilder().withUsePyishObjectMapper(true).build()) // 重写JSON解析器
                .withTokenScannerSymbols(new CustomTokenScannerSymbols()) // 自定义标签
                .build();
        Jinjava jinjava = new Jinjava(jConfig);
        jinjava.getGlobalContext().setExpressionStrategy(new CustomExpressionStrategy());
        jinjava.getGlobalContext().registerFilter(new Filter() {
            @Override
            public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
                if (var == null) {
                    return null;
                }
                String json = JsonUtil.toJSONString(var);
                return json;
            }
            @Override
            public String getName() {
                return "dump";
            }
        });

        jinjava.setResourceLocator((fullName, encoding, interpreter) -> {
            // TODO: view 覆盖特性 && check 文件是否存在
            ClassPathResource classPathResource = new ClassPathResource("/view/" + fullName);
            String fileContent = StreamUtils.copyToString(classPathResource.getInputStream(), encoding);
            fileContent = fileContent
                    .replaceAll("<\\$", "{\\$")
                    .replaceAll("\\$>", "\\$}");
            return fileContent;
        });

        String template = jinjava.getResourceLocator().getString(pagePath, Charsets.UTF_8, null);
        RenderResult result = jinjava.renderForResult(template, ctxForRender);

        List<TemplateError> errors = result.getErrors();
        if (!CollectionUtils.isEmpty(errors)) {
            TemplateError error = errors.get(0);
            String errorReasonSupplement = String.format("%s: line %d, %s", formatErrorReason(error.getReason().name()), error.getLineno(), error.getMessage());
            throw new BizException(BizEnum.page_render_error, errorReasonSupplement);
        }
        return result.getOutput();
    }

    private static String formatErrorReason(final String s) {
        String formatted = Pattern.compile("_([a-z])")
                .matcher(s.toLowerCase())
                .replaceAll(m -> m.group(1).toUpperCase());
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }


}
