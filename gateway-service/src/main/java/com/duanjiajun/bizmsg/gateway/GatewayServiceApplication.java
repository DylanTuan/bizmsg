package com.duanjiajun.bizmsg.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** 网关服务启动类，默认 8080。路由、跨域配置都在 Nacos 的 gateway-service.yml，本地只留 Nacos 地址。 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
