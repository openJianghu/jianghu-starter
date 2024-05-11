package org.jianghu.app.config;

import org.jianghu.app.common.JSONPathObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Component
@ConfigurationProperties(prefix = "jianghu")
@Data
public class JHConfig {
    private JSONPathObject config = new JSONPathObject();
    private String resourceUrl = null;

    public <T> T eval(String path) {
        return config.eval(path);
    }

    public <T> T eval(String path, Class<T> clazz) {
        return config.eval(path, clazz);
    }

    public <T> T eval(String path, T defaultVal) {
        return config.eval(path, defaultVal);
    }

    public <T> T eval(String path, T defaultVal, Class<T> clazz) {
        return config.eval(path, defaultVal, clazz);
    }

    public JHConfig set(String path, Object value) {
        config.set(path, value);
        return this;
    }

    public void setConfig(Map<String, Object> config) {
        config.putAll(config);
    }

    public JSONPathObject getConfig() {
        return config;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    @PostConstruct
    public void init() {
        // 提供给JHResponse使用, JHResponse要足够简单 不宜引入其它类
        String appId = config.eval("appId", String.class);
        System.setProperty("appId", appId);
        resourceUrl = String.format("/%s/resource", appId);
    }
}
