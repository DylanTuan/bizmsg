package com.duanjiajun.bizmsg.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.duanjiajun.bizmsg.upload.config.UploadProperties;

/**
 * 上传服务启动类：注册到 Nacos（默认 8082），接收 report-service 的 Feign 上传请求。
 */
@EnableDiscoveryClient
@EnableConfigurationProperties(UploadProperties.class)
@SpringBootApplication
public class UploadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UploadServiceApplication.class, args);
    }
}
