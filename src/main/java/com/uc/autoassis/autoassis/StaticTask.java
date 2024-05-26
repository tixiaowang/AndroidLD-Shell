package com.uc.autoassis.autoassis;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnableAsync
public class StaticTask {

    @Scheduled(cron = "0/5 * * * * ?")
    public void jobTest() {
        Main.main();
    }
}
