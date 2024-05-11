package org.jianghu.app.controller;

import org.jianghu.app.common.JHResponse;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.config.JianghuKnex;
import org.jianghu.app.context.ContextHolder;
import org.jianghu.app.controller.controllerUtil.ResourceUtil;
import org.jianghu.app.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
public class ResourceController {

    @Autowired
    private ResourceUtil resourceUtil;
    @Autowired
    private FileService fileService;

    @PostMapping(value ="/${jianghu.config.appId}/resource", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JHResponse resourceController(@RequestBody JSONPathObject requestBody) throws Throwable {

        String pageId = requestBody.eval("$.appData.pageId");
        String actionId = requestBody.eval("$.appData.actionId");

        JSONPathObject resource = ContextHolder.eval(ContextHolder.PACKAGE_RESOURCE, JSONPathObject.class);
        String resourceType = resource.eval("resourceType");

        JHResponse responseBody = JHResponse.success(pageId, actionId);
        if (resourceType.equals("sql")) {
            Map<String, Object> resultData = resourceUtil.sqlResource(requestBody, resource);
            responseBody.setResultData(resultData);
        }
        if (resourceType.equals("service")) {
            Object resultData = resourceUtil.serviceResource(requestBody, resource);
            responseBody.setResultData(resultData);
        }
        return responseBody;
    }

    @PostMapping(value = "/${jianghu.config.appId}/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public JHResponse resourceControllerOfFile(@RequestPart(value = "files", required = false) MultipartFile[] files,
                                               @RequestPart(value = "body", required = false) String body) throws Exception {
        long startTime = System.currentTimeMillis();
        JSONPathObject requestBody = JsonUtil.parseObject(body);
        String pageId = requestBody.eval("$.appData.pageId");
        String actionId = requestBody.eval("$.appData.actionId");
        String resourceId = pageId + "." + actionId;
        JSONPathObject actionData = requestBody.eval("appData.actionData", JSONPathObject.class);
        switch (resourceId) {
            case "allPage.httpUploadByStream":
                fileService.uploadFileChunkByStream(actionData, files);
                break;
            default:
                break;
        }
        log.info("[resource] {}.{} useTime: '{}/ms'", pageId, actionId, System.currentTimeMillis() - startTime);
        return JHResponse.success(pageId, actionId);
    }

}
