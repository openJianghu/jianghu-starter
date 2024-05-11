package org.jianghu.app.service;

import com.alibaba.fastjson2.JSONWriter;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.common.annotation.JHValidate;
import org.jianghu.app.config.JianghuKnex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service("errorServiceSystem")
public class ErrorService {

    @Autowired
    private JianghuKnex jianghuKnex;

    public void htmlErrorLogRecord(@JHValidate("{\n" +
            "    type: 'object',\n" +
            "    additionalProperties: true,\n" +
            "    required: [ 'errorLogList' ],\n" +
            "    properties: {\n" +
            "      errorLogList: {\n" +
            "        type: 'array',\n" +
            "        items: {\n" +
            "          type: 'object',\n" +
            "          additionalProperties: true,\n" +
            "          required: [ 'errorTime', 'errorMessage' ],\n" +
            "          properties: {\n" +
            "            userId: { anyOf: [{ type: 'null' }, { type: 'string' }, { type: 'number' }] },\n" +
            "            deviceId: { anyOf: [{ type: 'null' }, { type: 'string' }, { type: 'number' }] },\n" +
            "            errorTime: { anyOf: [{ type: 'string' }] },\n" +
            "            errorMessage: { anyOf: [{ type: 'string' }, { type: 'number' }] },\n" +
            "          },\n" +
            "        },\n" +
            "      },\n" +
            "    },\n" +
            "  }")JSONPathObject actionData) throws Exception {
        List<Map<String, String>> errorLogList = actionData.eval("errorLogList");
        errorLogList.forEach(errorLog -> {
            log.error("[html] errorLog: {}", JsonUtil.toJSONString(errorLog, JSONWriter.Feature.PrettyFormat));
        });
    }
}
