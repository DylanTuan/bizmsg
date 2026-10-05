package com.duanjiajun.bizmsg.message.xml.model;

import java.math.BigDecimal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** 商品房转移报文模型，由办结事件生成。与 ReportMessage 共用同一个 JAXBContext。 */
@XmlRootElement(name = "transferReport")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransferReport {

    @XmlElement(name = "businessId", required = true)
    private String businessId;

    /** 固定 HOUSE-TRANSFER。 */
    @XmlElement(name = "messageType", required = true)
    private String messageType;

    /** yyyy-MM-dd HH:mm:ss */
    @XmlElement(name = "createdAt")
    private String createdAt;

    @XmlElement(name = "seller")
    private Party seller;

    @XmlElement(name = "buyer")
    private Party buyer;

    @XmlElement(name = "house")
    private House house;

    public TransferReport() {
    }

    public TransferReport(String businessId, String messageType, String createdAt, Party seller, Party buyer,
                          House house) {
        this.businessId = businessId;
        this.messageType = messageType;
        this.createdAt = createdAt;
        this.seller = seller;
        this.buyer = buyer;
        this.house = house;
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

    public Party getSeller() {
        return seller;
    }

    public void setSeller(Party seller) {
        this.seller = seller;
    }

    public Party getBuyer() {
        return buyer;
    }

    public void setBuyer(Party buyer) {
        this.buyer = buyer;
    }

    public House getHouse() {
        return house;
    }

    public void setHouse(House house) {
        this.house = house;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Party {

        @XmlElement(name = "name", required = true)
        private String name;

        @XmlElement(name = "idNo")
        private String idNo;

        public Party() {
        }

        public Party(String name, String idNo) {
            this.name = name;
            this.idNo = idNo;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdNo() {
            return idNo;
        }

        public void setIdNo(String idNo) {
            this.idNo = idNo;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class House {

        @XmlElement(name = "address", required = true)
        private String address;

        /** 平方米 */
        @XmlElement(name = "area")
        private BigDecimal area;

        /** 元 */
        @XmlElement(name = "price")
        private BigDecimal price;

        public House() {
        }

        public House(String address, BigDecimal area, BigDecimal price) {
            this.address = address;
            this.area = area;
            this.price = price;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public BigDecimal getArea() {
            return area;
        }

        public void setArea(BigDecimal area) {
            this.area = area;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
