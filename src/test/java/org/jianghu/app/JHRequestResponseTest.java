package org.jianghu.app;

import com.alibaba.fastjson2.JSONWriter;
import org.jianghu.app.common.JHRequest;
import org.jianghu.app.common.JHResponse;
import org.jianghu.app.config.JHConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest(classes = JiangHuApplication.class)
public class JHRequestResponseTest {

    @Autowired
    private JHConfig jhConfig;

    @Test
    void response_test() throws Exception {
        JHResponse responseSuccess = JHResponse
                .success("studentManagement", "selectItemList")
                .setResultData(Map.of("rows", List.of("1001", "1002")));
        JHResponse responseFail = JHResponse
                .fail("studentManagement","selectItemList")
                .setResultData(List.of("1001", "1002"));
        log.info("responseSuccess: {}", responseSuccess.toJSONString(JSONWriter.Feature.PrettyFormat));
        log.info("responseFail: {}", responseFail.toJSONString(JSONWriter.Feature.PrettyFormat));
    }


    @Test
    void request_test() throws Exception {
        JHRequest requestBody = JHRequest
                .requestBody("studentManagement", "selectItemList")
                .set("$.appData.authToken", jhConfig.eval("authTokenForTest"))
                .set("$.appData.limit", 5)
                .set("$.appData.offset", 0)
                .set("$.appData.whereLike.studentId", "S1")
                .set("$.appData.whereLike.gender", "male")
                .set("$.appData.orderBy[0].column", "studentId")
                .set("$.appData.orderBy[0].order", "asc")
                .set("$.appData.actionData.__mock1__", "111")
                .set("$.appData.actionData.__mock2__", "222");
        log.info("requestBody: {}", requestBody.toJSONString(JSONWriter.Feature.PrettyFormat));
    }
}
