package com.duanjiajun.bizmsg.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 报文落盘相关配置，绑定 Nacos 中 message-service.yml 的 bizmsg.upload.* 节点。
 * 字段给出本地默认值，保证配置中心不可用时服务仍可独立启动。
 */
@ConfigurationProperties(prefix = "bizmsg.upload")
public class UploadProperties {

    /** 报文落盘目录。 */
    private String storageDir = "./data/upload";

    /** 回执编号前缀。 */
    private String receiptPrefix = "RCPT";

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public String getReceiptPrefix() {
        return receiptPrefix;
    }

    public void setReceiptPrefix(String receiptPrefix) {
        this.receiptPrefix = receiptPrefix;
    }
}
