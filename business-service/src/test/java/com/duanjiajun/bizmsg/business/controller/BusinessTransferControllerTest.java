package com.duanjiajun.bizmsg.business.controller;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.duanjiajun.bizmsg.business.dto.TransferCompletedEvent;
import com.duanjiajun.bizmsg.business.exception.MqPublishException;
import com.duanjiajun.bizmsg.business.mq.TransferEventPublisher;
import com.duanjiajun.bizmsg.business.repository.TransferRepository;
import com.duanjiajun.bizmsg.business.service.TransferService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 业务控制器单测：用桩件替换 MQ 发布者，不依赖 Nacos 与 RabbitMQ。
 * 重点验证 HTTP 语义——受理 201、参数问题 400、业务不存在 404、MQ 不可用 503（可重试）。
 */
class BusinessTransferControllerTest {

    private static final String VALID_BODY = """
            {"sellerName":"张三","sellerIdNo":"110101199001011234","buyerName":"李四","buyerIdNo":"110202199003034567",
             "houseAddress":"北京市朝阳区某小区 1 号楼 101","houseArea":88.50,"housePrice":3200000.00}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TransferEventPublisher publisher;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        publisher = mock(TransferEventPublisher.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BusinessTransferController(
                        new TransferService(new TransferRepository(), publisher)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("受理成功：201 + 状态 ACCEPTED + 业务号")
    void acceptReturns201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/business/transfer")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("businessId").asText()).startsWith("TRF-");
        assertThat(body.get("status").asText()).isEqualTo("ACCEPTED");
        assertThat(body.get("statusLabel").asText()).isEqualTo("已受理");
        assertThat(body.get("messageId").isNull()).isTrue();
    }

    @Test
    @DisplayName("受理参数非法：400，且返回具体字段提示")
    void acceptRejectsInvalidBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/business/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellerName\":\"\",\"sellerIdNo\":\"\",\"buyerName\":\"\",\"buyerIdNo\":\"\","
                                + "\"houseAddress\":\"\",\"houseArea\":0,\"housePrice\":0}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(readBody(result).get("message").asText()).contains("不能为空").contains("必须大于 0");
    }

    @Test
    @DisplayName("办结成功：200 + 状态 COMPLETED + 消息 ID")
    void completeReturnsCompleted() throws Exception {
        String businessId = acceptOne();

        MvcResult result = mockMvc.perform(post("/api/business/transfer/{id}/complete", businessId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(body.get("completedAt").asText()).isNotBlank();
        assertThat(body.get("messageId").asText()).isNotBlank();
    }

    @Test
    @DisplayName("办结不存在的业务：404")
    void completeUnknownReturns404() throws Exception {
        mockMvc.perform(post("/api/business/transfer/{id}/complete", "TRF-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MQ 不可用：503，且提示状态未变更可重试")
    void completeReturns503WhenMqUnavailable() throws Exception {
        String businessId = acceptOne();
        doThrow(new MqPublishException("broker 拒绝消息")).when(publisher).publish(any(TransferCompletedEvent.class));

        MvcResult result = mockMvc.perform(post("/api/business/transfer/{id}/complete", businessId))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertThat(readBody(result).get("message").asText()).contains("状态未变更");

        // 状态确实还停在「已受理」，调用方重试办结即可
        MvcResult query = mockMvc.perform(get("/api/business/transfer/{id}", businessId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readBody(query).get("status").asText()).isEqualTo("ACCEPTED");
    }

    private String acceptOne() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/business/transfer")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        return readBody(result).get("businessId").asText();
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
