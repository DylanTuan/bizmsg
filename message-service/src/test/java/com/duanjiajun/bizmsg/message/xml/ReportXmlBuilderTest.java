package com.duanjiajun.bizmsg.message.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.duanjiajun.bizmsg.message.xml.model.ReportMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ReportXmlBuilderTest {

    private final ReportXmlBuilder builder = new ReportXmlBuilder();

    @Test
    @DisplayName("生成的 XML 含根节点 report 与全部业务字段")
    void buildXmlContainsAllFields() {
        ReportMessage message = new ReportMessage("BIZ20261003001", "ACCEPT", "2026-10-03 16:00:00", "开户受理");

        String xml = builder.toXml(message);

        assertThat(xml).contains("<report>")
                .contains("<businessId>BIZ20261003001</businessId>")
                .contains("<messageType>ACCEPT</messageType>")
                .contains("<createdAt>2026-10-03 16:00:00</createdAt>")
                .contains("<payload>开户受理</payload>");
    }

    @Test
    @DisplayName("报文内容含特殊字符时被正确转义，反解可还原原值")
    void escapeSpecialCharactersAndRoundTrip() {
        ReportMessage message = new ReportMessage("BIZ&002", "ACCEPT", "2026-10-03 16:00:01", "金额<1000> & \"备注\"");

        String xml = builder.toXml(message);
        ReportMessage parsed = builder.parse(xml);

        assertThat(xml).contains("BIZ&amp;002").doesNotContain("<payload>金额<1000>");
        assertThat(parsed.getBusinessId()).isEqualTo("BIZ&002");
        assertThat(parsed.getPayload()).isEqualTo("金额<1000> & \"备注\"");
    }

    @Test
    @DisplayName("生成的报文是格式良好的 XML")
    void wellFormedXml() throws Exception {
        String xml = builder.toXml(new ReportMessage("BIZ003", "ACCEPT", "2026-10-03 16:00:02", "内容"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        assertThat(factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement().getTagName()).isEqualTo("report");
    }
}
