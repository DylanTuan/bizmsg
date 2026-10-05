package com.duanjiajun.bizmsg.message.xml;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.duanjiajun.bizmsg.message.xml.model.ReportMessage;
import com.duanjiajun.bizmsg.message.xml.model.TransferReport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品房转移报文生成单测：验证结构完整、特殊字符转义，以及两个报文模型共用同一个 JAXBContext。
 */
class TransferReportXmlTest {

    private final ReportXmlBuilder builder = new ReportXmlBuilder();

    @Test
    @DisplayName("转移报文含根节点 transferReport 与买卖双方、房屋全部要素")
    void buildTransferReport() {
        String xml = builder.toXml(transferReport("TRF-20261005001", "北京市朝阳区某小区 1 号楼 101"));

        assertThat(xml).contains("<transferReport>")
                .contains("<businessId>TRF-20261005001</businessId>")
                .contains("<messageType>HOUSE-TRANSFER</messageType>")
                .contains("<seller>")
                .contains("<name>张三</name>")
                .contains("<idNo>110101199001011234</idNo>")
                .contains("<buyer>")
                .contains("<name>李四</name>")
                .contains("<house>")
                .contains("<address>北京市朝阳区某小区 1 号楼 101</address>")
                .contains("<area>88.50</area>")
                .contains("<price>3200000.00</price>");
    }

    @Test
    @DisplayName("生成的转移报文是格式良好的 XML")
    void transferReportIsWellFormed() throws Exception {
        String xml = builder.toXml(transferReport("TRF-002", "上海市浦东新区某路 2 号"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        assertThat(factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement().getTagName()).isEqualTo("transferReport");
    }

    @Test
    @DisplayName("房屋坐落含特殊字符时被正确转义")
    void escapeSpecialCharacters() {
        String xml = builder.toXml(transferReport("TRF-003", "A&B 大厦 <3 层>"));

        assertThat(xml).contains("A&amp;B 大厦 &lt;3 层&gt;")
                .doesNotContain("<address>A&B 大厦 <3 层></address>");
    }

    @Test
    @DisplayName("受理报文与转移报文共用一个 JAXBContext，可连续序列化互不干扰")
    void bothModelsShareJaxbContext() {
        String acceptXml = builder.toXml(new ReportMessage("BIZ-1", "ACCEPT", "2026-10-05 10:00:00", "开户受理"));
        String transferXml = builder.toXml(transferReport("TRF-004", "广州市天河区某街 3 号"));

        assertThat(acceptXml).contains("<report>").contains("<businessId>BIZ-1</businessId>");
        assertThat(transferXml).contains("<transferReport>").contains("<businessId>TRF-004</businessId>");
        // 反解仍然只认受理报文模型，两者不会互相污染
        assertThat(builder.parse(acceptXml).getBusinessId()).isEqualTo("BIZ-1");
    }

    private TransferReport transferReport(String businessId, String address) {
        return new TransferReport(businessId, "HOUSE-TRANSFER", "2026-10-05 10:00:00",
                new TransferReport.Party("张三", "110101199001011234"),
                new TransferReport.Party("李四", "110202199003034567"),
                new TransferReport.House(address, new BigDecimal("88.50"), new BigDecimal("3200000.00")));
    }
}
