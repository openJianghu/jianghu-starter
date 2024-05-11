package org.jianghu.app.service;

import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.config.JianghuKnex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Priority;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("pageLogServiceSystem")
public class PageLogService {

    @Autowired
    private JianghuKnex jianghuKnex;
    @Autowired
    private JHConfig jhConfig;

    public JSONPathObject selectLogFileList(JSONPathObject actionData) throws Exception {
        Path targetLogDirPath = Paths.get(System.getProperty("user.dir"), "logs");
        String appId = jhConfig.eval("appId", String.class);
        String targetLogNamePrefix = appId + ".page.json";

        if (!Files.exists(targetLogDirPath)) {
            throw new BizException("log_dir_not_exist", "日志目录不存在");
        }
        List<Map<String, String>> rows = Files.list(targetLogDirPath)
                .filter(item -> item.getFileName().toString().startsWith(targetLogNamePrefix))
                .map(item -> Map.of("filename", item.getFileName().toString()))
                .collect(Collectors.toList());

        return JSONPathObject.of("rows", rows);
    }


    public JSONPathObject selectItemListFromLogFile(JSONPathObject actionData) throws Exception {
        String logFile = actionData.eval("logFile", String.class);
        String targetLogFilePath = Paths.get(System.getProperty("user.dir"), "logs", logFile).toString();
        Path logFilePath = Paths.get(targetLogFilePath);

        if (!Files.exists(logFilePath)) {
            throw new BizException("log_file_not_exist", "日志不存在");
        }

        String strData = Files.readString(logFilePath);
        strData = strData.replace("'}\n", "'},").replace("'}\r\n", "'},");
        strData = "[" + strData + "]";
        List<JSONPathObject> rows = JsonUtil.parseArray(strData, JSONPathObject.class);
        rows.stream().forEach(row -> {
            row.set("date", row.eval("timestamp", String.class));
            row.set("pageName", row.eval("message", String.class));
        });
        return JSONPathObject.of("rows", rows);
    }
}
