package com.duanjiajun.bizmsg.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 报文落盘配置，绑定 Nacos 中 message-service.yml 的 bizmsg.upload.* 节点。 */
@ConfigurationProperties(prefix = "bizmsg.upload")
public class UploadProperties {

    private String storageDir = "./data/upload";

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
