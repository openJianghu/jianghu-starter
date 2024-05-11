package org.jianghu.app;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONWriter;
import org.jianghu.app.common.Constant;
import org.jianghu.app.common.JHRequest;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.config.JHConfig;
import org.jianghu.app.controller.controllerUtil.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.Map;

@Slf4j
//@SpringBootTest(classes = JiangHuApplication.class)
@SpringBootTest(classes = JiangHuApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResourceCallTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ResourceUtil resourceUtil;
    @Autowired
    private JHConfig jhConfig;

    @Test
    void ResourceUtil_buildWhereCondition() throws Exception {
        JHRequest requestBody = JHRequest.requestBody("studentManagement", "selectItemList")
                .set("appData.limit", 5)
                .set("appData.offset", 0)
//                .set("appData.where.studentId", "S1001")
//                .set("appData.whereLike.studentId", "S1")
//                .set("appData.whereLike.gender", "male")
                .set("appData.orderBy[0].column", "studentId")
                .set("appData.orderBy[0].order", "asc")
                .set("appData.orderBy[1].column", "name")
                .set("appData.orderBy[1].order", "desc")
                .set("appData.actionData.__mock1__", "111")
                .set("appData.actionData.__mock2__", "222");

        Map<String, Object> resultData = resourceUtil.sqlResource(requestBody, null);
        System.out.println(String.format("resultData: %s", resultData));
    }

    @Test
    void sqlResource_byJson() throws Exception {
        String requestBodyString = "{\"appData\":{\"pageId\":\"studentManagement\",\"actionId\":\"selectItemListTest\",\"actionData\":{\"level\":\"01\",\"gender\":\"male\"},\"where\":{},\"whereLike\":{},\"orderBy\":[{\"column\":\"operationAt\",\"order\":\"desc\"}],\"appId\":\"jianghujs-1table-crud\",\"userAgent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36\",\"authToken\":null},\"packageId\":\"1714133656913_5277982\",\"packageType\":\"httpRequest\"}";
        JSONPathObject requestBody = JsonUtil.toJSON(requestBodyString);
        Map<String, Object> resultData = resourceUtil.sqlResource(requestBody, null);
        System.out.println(String.format("resultData: %s", resultData));
    }

    /**
     * [sql Resource参数](https://cn.jianghujs.org/jianghu-doc-v2-seo/page/article/12531)
     * @throws Exception
     */
    @Test
    void sqlResource_byObject() throws Exception {
        JHRequest requestBody = JHRequest
                .requestBody("studentManagement", "selectItemList")
                .set("appData.authToken", jhConfig.eval("authTokenForTest"))
                .set("appData.limit", 5)
                .set("appData.offset", 0)
                .set("appData.whereLike.studentId", "S1")
                .set("appData.whereLike.gender", "male")
                .set("appData.orderBy[0].column", "studentId")
                .set("appData.orderBy[0].order", "asc")
                .set("appData.orderBy[1].column", "name")
                .set("appData.orderBy[1].order", "desc")
                .set("appData.actionData.__mock1__", "111")
                .set("appData.actionData.__mock2__", "222");
        Map<String, Object> resultData = resourceUtil.sqlResource(requestBody, null);
        log.info("requestBody: {}", requestBody.toJSONString(JSONWriter.Feature.PrettyFormat));
        log.info("responseBody.resultData: {}", JsonUtil.toJSONString(resultData, JSONWriter.Feature.PrettyFormat));
    }


    @Test
    void sqlResource_byController() throws Exception {
        JHRequest requestBody = JHRequest.requestBody("studentManagement", "selectItemList")
                .set("appData.authToken", jhConfig.eval("authTokenForTest"))
                .set("appData.limit", 5)
                .set("appData.offset", 0)
                .set("appData.whereLike.studentId", "S1")
                .set("appData.whereLike.gender", "male");
        JHRequest responseBody = restTemplate.postForEntity(jhConfig.getResourceUrl(), requestBody, JHRequest.class).getBody();
        log.info("responseBody: {}", responseBody.toJSONString(JSONWriter.Feature.PrettyFormat));
    }

    @Test
    void serviceResource_of_htmlErrorLogRecord() throws Exception {
        JHRequest requestBody = JHRequest.requestBody("allPage", "htmlErrorLogRecord")
                .set("appData.authToken", jhConfig.eval("authTokenForTest"))
                .set("appData.actionData.errorLogList[0]", Map.ofEntries(
                        Map.entry("userId", "test01"),
                        Map.entry("deviceId", "test01__deviceId__"),
                        Map.entry("errorTime", DateUtil.date().toString(Constant.ISO8601)),
                        Map.entry("errorMessage", "vue runtime error: Cannot read property 'length' of undefined")
                ))
                .set("appData.actionData.errorLogList[1]", Map.ofEntries(
                        Map.entry("userId", "test01"),
                        Map.entry("deviceId", "test01__deviceId__"),
                        Map.entry("errorTime", DateUtil.date().toString(Constant.ISO8601)),
                        Map.entry("errorMessage", "v-for requires an array value but got undefined")
                ));
        JHRequest responseBody = restTemplate.postForEntity(jhConfig.getResourceUrl(), requestBody, JHRequest.class).getBody();
        log.info("responseBody: {}", responseBody.toJSONString(JSONWriter.Feature.PrettyFormat));
    }

    @Test
    void serviceResource_of_httpUploadByStream() throws Exception {
        JHRequest requestBody = JHRequest.requestBody("allPage", "httpUploadByStream")
                .set("appData.authToken", jhConfig.eval("authTokenForTest"))
                .set("appData.actionData.chunkSize", 3145728)
                .set("appData.actionData.indexString", "0000")
                .set("appData.actionData.hash", "ecba184421c1c757d6e124f4e14e29b2")
                .set("appData.actionData.total", 1)
                .set("appData.actionData.filename", "test.txt");
        File file = FileUtil.writeUtf8String("这是一个测试文件>>>>>>>>>>>", new File("test.txt"));
        FileSystemResource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);
        body.add("body", requestBody.toJSONString());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        JHRequest responseBody = restTemplate.postForEntity(jhConfig.getResourceUrl(), requestEntity, JHRequest.class).getBody();
        log.info("responseBody: {}", responseBody.toJSONString(JSONWriter.Feature.PrettyFormat));
    }

    @Test
    void serviceResource_of_addStudent() throws Exception {
        JHRequest requestBody = JHRequest.requestBody(null, "addStudent-test")
                .set("appData.authToken", jhConfig.eval("authTokenForTest"))
                .set("appData.actionData.studentId1", "test0001")
                .set("appData.actionData.name", "test0001");
        JHRequest responseBody = restTemplate.postForEntity(jhConfig.getResourceUrl(), requestBody, JHRequest.class).getBody();
        log.info("responseBody: {}", responseBody.toJSONString(JSONWriter.Feature.PrettyFormat));
    }

}
