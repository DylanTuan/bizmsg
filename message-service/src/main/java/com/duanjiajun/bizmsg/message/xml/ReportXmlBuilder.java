package com.duanjiajun.bizmsg.message.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.duanjiajun.bizmsg.message.xml.model.ReportMessage;
import com.duanjiajun.bizmsg.message.xml.model.TransferReport;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

/** 对象与 XML 的双向转换。JAXBContext 开销大但线程安全，所有报文模型一次性注册后复用。 */
@Component
public class ReportXmlBuilder {

    private static final JAXBContext CONTEXT = createContext();

    /** 受理报文转 XML，带缩进与 UTF-8 声明，便于人工核对。 */
    public String toXml(ReportMessage message) {
        return marshal(message, message.getBusinessId());
    }

    public String toXml(TransferReport report) {
        return marshal(report, report.getBusinessId());
    }

    /** 报文字段缺失或格式非法时抛 IllegalArgumentException。 */
    public ReportMessage parse(String xml) {
        try {
            return (ReportMessage) CONTEXT.createUnmarshaller().unmarshal(new StringReader(xml));
        } catch (JAXBException ex) {
            throw new IllegalArgumentException("解析 XML 报文失败", ex);
        }
    }

    /** 失败时带上业务号便于定位。 */
    private String marshal(Object model, String businessId) {
        try {
            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            StringWriter writer = new StringWriter();
            marshaller.marshal(model, writer);
            return writer.toString();
        } catch (JAXBException ex) {
            throw new IllegalStateException("生成 XML 报文失败，businessId=" + businessId, ex);
        }
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(ReportMessage.class, TransferReport.class);
        } catch (JAXBException ex) {
            throw new IllegalStateException("初始化 JAXBContext 失败", ex);
        }
    }
}
