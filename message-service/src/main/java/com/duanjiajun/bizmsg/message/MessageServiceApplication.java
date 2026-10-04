package com.duanjiajun.bizmsg.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.duanjiajun.bizmsg.message.config.UploadProperties;

/**
 * 报文服务启动类：注册到 Nacos（默认 8081）。
 * 由原 report-service 与 upload-service 合并而来——报文生成与落盘回执在同一进程内完成，
 * 调用链从「生成 → Feign → 落盘」两次网络跳转收敛为一次方法调用，因此不再需要
 * {@code @EnableFeignClients} 与 spring-cloud-starter-loadbalancer。
 */
@EnableDiscoveryClient
@EnableConfigurationProperties(UploadProperties.class)
@SpringBootApplication
public class MessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
