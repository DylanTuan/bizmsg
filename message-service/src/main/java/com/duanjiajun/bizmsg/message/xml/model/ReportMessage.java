package com.duanjiajun.bizmsg.message.xml.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** 受理报文模型。JAXB 对 record 支持不完整，所以用普通类加无参构造和 setter。 */
@XmlRootElement(name = "report")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReportMessage {

    @XmlElement(name = "businessId", required = true)
    private String businessId;

    @XmlElement(name = "messageType", required = true)
    private String messageType;

    /** yyyy-MM-dd HH:mm:ss */
    @XmlElement(name = "createdAt")
    private String createdAt;

    /** 业务内容，JAXB 负责 XML 转义。 */
    @XmlElement(name = "payload")
    private String payload;

    public ReportMessage() {
    }

    public ReportMessage(String businessId, String messageType, String createdAt, String payload) {
        this.businessId = businessId;
        this.messageType = messageType;
        this.createdAt = createdAt;
        this.payload = payload;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
