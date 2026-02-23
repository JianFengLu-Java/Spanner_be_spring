package com.lujianfeng.spanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SpannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpannerApplication.class, args);
    }

}
