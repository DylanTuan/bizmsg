package com.duanjiajun.bizmsg.report.controller;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.duanjiajun.bizmsg.report.client.UploadClient;
import com.duanjiajun.bizmsg.report.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.report.service.ReportService;
import com.duanjiajun.bizmsg.report.xml.ReportXmlBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 控制器单测：用桩件替换 Feign 客户端，不依赖 Nacos 与网络。
 * 注意 MockMvc 不走 Tomcat 连接器，因此这里验的是参数绑定与报文生成，不负责复现容器对 URI 的校验。
 */
class ReportControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(
                new ReportService(new ReportXmlBuilder(), successClient()))).build();
    }

    @Test
    @DisplayName("JSON body 传中文报文：生成 XML 并上传成功")
    void generateWithJsonBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"BIZ1001\",\"messageType\":\"ACCEPT\",\"payload\":\"开户受理\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("uploaded").asBoolean()).isTrue();
        assertThat(body.get("businessId").asText()).isEqualTo("BIZ1001");
        assertThat(body.get("receiptNo").asText()).isEqualTo("RCPT-TEST-0001");
        assertThat(body.get("xml").asText()).contains("<payload>开户受理</payload>");
    }

    @Test
    @DisplayName("查询参数方式兼容，messageType 缺省按 ACCEPT 处理且 businessId 自动生成")
    void generateWithQueryParams() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/report/generate")
                        .param("payload", "开户受理"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("uploaded").asBoolean()).isTrue();
        assertThat(body.get("messageType").asText()).isEqualTo("ACCEPT");
        assertThat(body.get("businessId").asText()).hasSize(32);
    }

    @Test
    @DisplayName("上传失败时降级：HTTP 仍 200，返回 degradeReason 且报文不丢")
    void degradeWhenUploadFails() throws Exception {
        MockMvc failingMvc = MockMvcBuilders.standaloneSetup(new ReportController(new ReportService(
                new ReportXmlBuilder(),
                request -> {
                    throw new IllegalStateException("连接 upload-service 失败");
                }))).build();

        MvcResult result = failingMvc.perform(post("/api/report/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"BIZ1002\",\"payload\":\"开户受理\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(body.get("uploaded").asBoolean()).isFalse();
        assertThat(body.get("degradeReason").asText()).contains("连接 upload-service 失败");
        assertThat(body.get("xml").asText()).contains("BIZ1002");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private UploadClient successClient() {
        return request -> new ReceiptResponse("RCPT-TEST-0001", "/tmp/RCPT-TEST-0001.xml",
                "2026-10-03 16:00:00", true, "回执已生成");
    }
}
