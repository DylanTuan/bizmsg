package com.duanjiajun.bizmsg.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.duanjiajun.bizmsg.report.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.report.dto.UploadReceiptRequest;

/**
 * 上传服务 Feign 客户端：服务名与路径都取自 Nacos 配置（bizmsg.upload.*），
 * 冒号后为本地默认值，保证配置中心不可用时仍能启动。
 * 连接/读取超时统一由 bizmsg-common.yml 的 spring.cloud.openfeign.client.config.default 控制。
 */
@FeignClient(name = "${bizmsg.upload.service-name:upload-service}",
        path = "${bizmsg.upload.path:/api/upload/receipt}")
public interface UploadClient {

    @PostMapping
    ReceiptResponse upload(@RequestBody UploadReceiptRequest request);
}
