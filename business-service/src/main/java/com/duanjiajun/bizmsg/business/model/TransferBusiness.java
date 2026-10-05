package com.duanjiajun.bizmsg.business.model;

import java.math.BigDecimal;

/**
 * 商品房转移业务实体（内存态）。
 * 生产环境应换成带状态字段与乐观锁的表，并用本地消息表保证「办结」与「发消息」的最终一致。
 */
public class TransferBusiness {

    /** 同时作为 MQ 业务键和报文模块的幂等键。 */
    private final String businessId;

    private TransferStatus status;

    private final String sellerName;
    private final String sellerIdNo;
    private final String buyerName;
    private final String buyerIdNo;
    private final String houseAddress;
    private final BigDecimal houseArea;
    private final BigDecimal housePrice;

    private final String acceptedAt;
    private String completedAt;

    /** 办结时投递的消息 ID，可按业务号反查消息轨迹。 */
    private String messageId;

    public TransferBusiness(String businessId, String sellerName, String sellerIdNo, String buyerName, String buyerIdNo,
                            String houseAddress, BigDecimal houseArea, BigDecimal housePrice, String acceptedAt) {
        this.businessId = businessId;
        this.status = TransferStatus.ACCEPTED;
        this.sellerName = sellerName;
        this.sellerIdNo = sellerIdNo;
        this.buyerName = buyerName;
        this.buyerIdNo = buyerIdNo;
        this.houseAddress = houseAddress;
        this.houseArea = houseArea;
        this.housePrice = housePrice;
        this.acceptedAt = acceptedAt;
    }

    /** 记录办结时间与消息 ID，状态推进到 COMPLETED。 */
    public void markCompleted(String completedAt, String messageId) {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = completedAt;
        this.messageId = messageId;
    }

    public boolean isCompleted() {
        return status == TransferStatus.COMPLETED;
    }

    public String getBusinessId() {
        return businessId;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getSellerIdNo() {
        return sellerIdNo;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerIdNo() {
        return buyerIdNo;
    }

    public String getHouseAddress() {
        return houseAddress;
    }

    public BigDecimal getHouseArea() {
        return houseArea;
    }

    public BigDecimal getHousePrice() {
        return housePrice;
    }

    public String getAcceptedAt() {
        return acceptedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getMessageId() {
        return messageId;
    }
}
