package org.jianghu.app;

import com.alibaba.fastjson2.JSONWriter;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.config.JHConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest(classes = JiangHuApplication.class)
public class JHConfigTest {
    @Autowired
    private JHConfig jhConfig;

    @Test
    void get() throws Exception {
        List<String> ignoreListOfResourceLogRecord = jhConfig.eval("jianghuConfig.ignoreListOfResourceLogRecord", ArrayList.class);
        System.out.println("jianghuConfig.ignoreListOfResourceLogRecord: " + JsonUtil.toJSONString(ignoreListOfResourceLogRecord));
        System.out.println("config: ==================================================");
        System.out.println(jhConfig.getConfig().toJSONString(JSONWriter.Feature.PrettyFormat));
    }
}
