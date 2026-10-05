package com.duanjiajun.bizmsg.business.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商品房转移受理入参。
 * 只保留报文生成真正需要的要素：买卖双方身份、房屋坐落、面积、成交价。
 */
public record TransferCreateRequest(

        @NotBlank(message = "卖方姓名不能为空")
        @Size(max = 32, message = "卖方姓名过长")
        String sellerName,

        @NotBlank(message = "卖方证件号不能为空")
        @Size(max = 32, message = "卖方证件号过长")
        String sellerIdNo,

        @NotBlank(message = "买方姓名不能为空")
        @Size(max = 32, message = "买方姓名过长")
        String buyerName,

        @NotBlank(message = "买方证件号不能为空")
        @Size(max = 32, message = "买方证件号过长")
        String buyerIdNo,

        @NotBlank(message = "房屋坐落不能为空")
        @Size(max = 128, message = "房屋坐落过长")
        String houseAddress,

        @NotNull(message = "建筑面积不能为空")
        @DecimalMin(value = "0.01", message = "建筑面积必须大于 0")
        BigDecimal houseArea,

        @NotNull(message = "成交价不能为空")
        @DecimalMin(value = "0.01", message = "成交价必须大于 0")
        BigDecimal housePrice) {
}
