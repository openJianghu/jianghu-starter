package org.jianghu.app.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

@Slf4j
@Component
public class SocketStatusSchedule {
    @Value("${jianghu.config.jianghuConfig.enableSyncSocketStatus}")
    private boolean isEnabled;

    @Scheduled(cron = "*/30 * * * * *")
    public void task() {
        if (!isEnabled) {
            return;
        }
        log.info("[SyncSocketStatus] task executed at " + LocalDateTime.now());
    }


    /**
     * 项目启动时立即执行任务
     */
    @PostConstruct
    public void immediateRunTask() {
        task();
    }

}
