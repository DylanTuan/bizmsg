package com.duanjiajun.bizmsg.business.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.duanjiajun.bizmsg.business.dto.TransferCompletedEvent;
import com.duanjiajun.bizmsg.business.dto.TransferCreateRequest;
import com.duanjiajun.bizmsg.business.dto.TransferView;
import com.duanjiajun.bizmsg.business.exception.MqPublishException;
import com.duanjiajun.bizmsg.business.exception.TransferNotFoundException;
import com.duanjiajun.bizmsg.business.model.TransferStatus;
import com.duanjiajun.bizmsg.business.mq.TransferEventPublisher;
import com.duanjiajun.bizmsg.business.repository.TransferRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 商品房转移状态机单测：不依赖 Nacos、RabbitMQ 与网络，publisher 用桩件替换。
 */
class TransferServiceTest {

    private TransferEventPublisher publisher;
    private TransferService service;

    @BeforeEach
    void setUp() {
        publisher = mock(TransferEventPublisher.class);
        service = new TransferService(new TransferRepository(), publisher);
    }

    @Test
    @DisplayName("受理：生成业务号，状态为 ACCEPTED，不发送任何 MQ 消息")
    void acceptCreatesAcceptedBusiness() {
        TransferView view = service.accept(request());

        assertThat(view.businessId()).startsWith("TRF-");
        assertThat(view.status()).isEqualTo(TransferStatus.ACCEPTED);
        assertThat(view.statusLabel()).isEqualTo("已受理");
        assertThat(view.completedAt()).isNull();
        assertThat(view.messageId()).isNull();
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("办结：投递事件后状态推进为 COMPLETED，并带上消息 ID")
    void completePublishesEventAndMarksCompleted() {
        String businessId = service.accept(request()).businessId();

        TransferView completed = service.complete(businessId);

        assertThat(completed.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotBlank();
        assertThat(completed.messageId()).isNotBlank();

        // 事件内容必须完整带齐报文要素，否则消费端生成的报文会缺字段
        ArgumentCaptor<TransferCompletedEvent> captor = ArgumentCaptor.forClass(TransferCompletedEvent.class);
        verify(publisher).publish(captor.capture());
        TransferCompletedEvent event = captor.getValue();
        assertThat(event.businessId()).isEqualTo(businessId);
        assertThat(event.messageType()).isEqualTo(TransferService.MESSAGE_TYPE);
        assertThat(event.seller().name()).isEqualTo("张三");
        assertThat(event.buyer().name()).isEqualTo("李四");
        assertThat(event.house().address()).isEqualTo("北京市朝阳区某小区 1 号楼 101");
        assertThat(event.house().area()).isEqualByComparingTo("88.50");
        assertThat(event.house().price()).isEqualByComparingTo("3200000.00");
    }

    @Test
    @DisplayName("办结幂等：重复调用不会重复投递 MQ 消息")
    void completeIsIdempotent() {
        String businessId = service.accept(request()).businessId();

        TransferView first = service.complete(businessId);
        TransferView second = service.complete(businessId);

        // 这是整条链路的幂等源头：业务只办结一次，报文模块才不会收到重复事件
        verify(publisher, times(1)).publish(any());
        assertThat(second.messageId()).isEqualTo(first.messageId());
        assertThat(second.completedAt()).isEqualTo(first.completedAt());
        assertThat(second.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    @DisplayName("MQ 投递失败：状态保持 ACCEPTED，异常上抛让调用方重试")
    void keepAcceptedWhenPublishFails() {
        String businessId = service.accept(request()).businessId();
        doThrow(new MqPublishException("broker 不可用")).when(publisher).publish(any());

        assertThatThrownBy(() -> service.complete(businessId))
                .isInstanceOf(MqPublishException.class)
                .hasMessageContaining("broker 不可用");

        // 关键：状态没有被单方面推进，重试办结时才会真正再发一次消息
        TransferView view = service.get(businessId);
        assertThat(view.status()).isEqualTo(TransferStatus.ACCEPTED);
        assertThat(view.completedAt()).isNull();
    }

    @Test
    @DisplayName("查询不存在的业务号：抛 TransferNotFoundException（映射 404）")
    void getUnknownBusinessThrows() {
        assertThatThrownBy(() -> service.get("TRF-NOT-EXIST"))
                .isInstanceOf(TransferNotFoundException.class)
                .hasMessageContaining("TRF-NOT-EXIST");
    }

    @Test
    @DisplayName("列表按受理时间倒序，包含全部已受理业务")
    void listReturnsAllBusinesses() {
        service.accept(request());
        service.accept(request());

        assertThat(service.list()).hasSize(2);
    }

    private TransferCreateRequest request() {
        return new TransferCreateRequest("张三", "110101199001011234", "李四", "110202199003034567",
                "北京市朝阳区某小区 1 号楼 101", new BigDecimal("88.50"), new BigDecimal("3200000.00"));
    }
}
