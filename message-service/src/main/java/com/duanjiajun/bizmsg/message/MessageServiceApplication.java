package com.duanjiajun.bizmsg.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.duanjiajun.bizmsg.message.config.MessageMqProperties;
import com.duanjiajun.bizmsg.message.config.UploadProperties;

/**
 * 报文服务启动类，默认 8081。由原 report-service 和 upload-service 合并而来，生成与落盘在同一进程内，
 * 不再需要 {@code @EnableFeignClients}。同时也是 MQ 消费端，监听办结事件异步生成报文。
 */
@EnableDiscoveryClient
@EnableConfigurationProperties({UploadProperties.class, MessageMqProperties.class})
@SpringBootApplication
public class MessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
