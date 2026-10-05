package com.duanjiajun.bizmsg.business.dto;

import java.math.BigDecimal;

import com.duanjiajun.bizmsg.business.model.TransferBusiness;
import com.duanjiajun.bizmsg.business.model.TransferStatus;

/**
 * 商品房转移业务视图：受理/办结/查询统一返回该结构。
 * status 同时给出英文枚举与中文描述，前端不必硬编码翻译。
 */
public record TransferView(String businessId,
                           TransferStatus status,
                           String statusLabel,
                           String sellerName,
                           String sellerIdNo,
                           String buyerName,
                           String buyerIdNo,
                           String houseAddress,
                           BigDecimal houseArea,
                           BigDecimal housePrice,
                           String acceptedAt,
                           String completedAt,
                           String messageId) {

    public static TransferView of(TransferBusiness business) {
        return new TransferView(business.getBusinessId(), business.getStatus(), business.getStatus().getLabel(),
                business.getSellerName(), business.getSellerIdNo(), business.getBuyerName(), business.getBuyerIdNo(),
                business.getHouseAddress(), business.getHouseArea(), business.getHousePrice(),
                business.getAcceptedAt(), business.getCompletedAt(), business.getMessageId());
    }
}
