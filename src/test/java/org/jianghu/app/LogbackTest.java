package org.jianghu.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = JiangHuApplication.class)
public class LogbackTest {

    @Test
    void test() throws Exception {
        log.debug("This is a debug message.  {}", System.currentTimeMillis());
        log.warn("This is a warning message. {}", "=^=");
        log.error("An error occurred during application startup.", new RuntimeException("Test exception"));
        // spring.profiles.active != local时,  logs/*.log才会记录日志,  日志只保留 最近15的
    }
}
