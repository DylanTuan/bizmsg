package com.duanjiajun.bizmsg.message.xml.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 受理报文模型：JAXB 通过字段注解映射为 XML 元素。
 * 用普通类而非 record，是因为 JAXB 对 record 的支持并不完整，字段需要无参构造 + setter。
 */
@XmlRootElement(name = "report")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReportMessage {

    /** 业务流水号，全局唯一，下游据此做幂等。 */
    @XmlElement(name = "businessId", required = true)
    private String businessId;

    /** 报文类型，例如 ACCEPT / RECEIPT。 */
    @XmlElement(name = "messageType", required = true)
    private String messageType;

    /** 报文生成时间，格式 yyyy-MM-dd HH:mm:ss。 */
    @XmlElement(name = "createdAt")
    private String createdAt;

    /** 业务内容，JAXB 会自动做 XML 转义。 */
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
