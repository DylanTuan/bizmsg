package com.duanjiajun.bizmsg.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类：注册到 Nacos 并对外提供统一入口（默认 8080）。
 * 路由、跨域等配置统一放在 Nacos 的 gateway-service.yml，本地只保留 Nacos 地址。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
