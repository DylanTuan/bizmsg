package com.duanjiajun.bizmsg.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.duanjiajun.bizmsg.business.config.BusinessMqProperties;

/**
 * 业务服务启动类，默认 8082。办结时把事件投到 RabbitMQ，由 message-service 异步生成报文，
 * 报文模块短暂不可用不会阻塞办结。
 */
@EnableDiscoveryClient
@EnableConfigurationProperties(BusinessMqProperties.class)
@SpringBootApplication
public class BusinessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
}
