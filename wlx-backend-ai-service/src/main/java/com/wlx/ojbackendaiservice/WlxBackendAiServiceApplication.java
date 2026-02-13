package com.wlx.ojbackendaiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"com.wlx.ojbackendserviceclient.service"})
public class WlxBackendAiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WlxBackendAiServiceApplication.class, args);
    }

}
