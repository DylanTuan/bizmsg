package com.duanjiajun.bizmsg.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 报文生成服务启动类：注册到 Nacos（默认 8081），并通过 Feign 调用 upload-service。
 */
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
