package com.bcsport.admin.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component("demoTask")
public class DemoTask {

    public void noParams() {
        log.info("DemoTask 无参数执行 {}", LocalDateTime.now());
    }

    public void withParams(String msg) {
        log.info("DemoTask 带参数执行 {}, time: {}", msg, LocalDateTime.now());
    }
}
