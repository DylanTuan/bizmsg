package com.duanjiajun.bizmsg.message.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消费端 RabbitMQ 拓扑：谁消费谁声明队列，生产者只声明业务交换机。
 * 失败重试走 TTL + 死信回投做延迟，不用 requeue，避免热循环。
 * 完整拓扑图见 message-service/README.md。
 */
@Configuration
public class RabbitMqConfig {

    /** 业务事件交换机，必须与 business-service 声明的同名同类型同持久化属性。 */
    @Bean
    public TopicExchange businessExchange(MessageMqProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    /** 重试耗尽的消息最终落这里，等人工处理。 */
    @Bean
    public DirectExchange deadLetterExchange(MessageMqProperties properties) {
        return new DirectExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public DirectExchange retryExchange(MessageMqProperties properties) {
        return new DirectExchange(properties.getRetryExchange(), true, false);
    }

    /** 报文生成主队列，消费失败且重试耗尽时由 broker 按下面的死信配置转投 DLX。 */
    @Bean
    public Queue transferQueue(MessageMqProperties properties) {
        return QueueBuilder.durable(properties.getTransferQueue())
                .deadLetterExchange(properties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Binding transferBinding(@Qualifier("transferQueue") Queue transferQueue,
                                   TopicExchange businessExchange,
                                   MessageMqProperties properties) {
        return BindingBuilder.bind(transferQueue).to(businessExchange).with(properties.getRoutingKey());
    }

    /** 消息只停留 retryDelayMillis，到期后死信回业务交换机，重新进主队列。 */
    @Bean
    public Queue transferRetryQueue(MessageMqProperties properties) {
        return QueueBuilder.durable(properties.getRetryQueue())
                .ttl((int) properties.getRetryDelayMillis())
                .deadLetterExchange(properties.getExchange())
                .deadLetterRoutingKey(properties.getRoutingKey())
                .build();
    }

    @Bean
    public Binding transferRetryBinding(@Qualifier("transferRetryQueue") Queue transferRetryQueue,
                                        DirectExchange retryExchange,
                                        MessageMqProperties properties) {
        return BindingBuilder.bind(transferRetryQueue).to(retryExchange).with(properties.getRetryRoutingKey());
    }

    @Bean
    public Queue transferDeadLetterQueue(MessageMqProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding transferDeadLetterBinding(@Qualifier("transferDeadLetterQueue") Queue transferDeadLetterQueue,
                                             DirectExchange deadLetterExchange,
                                             MessageMqProperties properties) {
        return BindingBuilder.bind(transferDeadLetterQueue).to(deadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    /**
     * {@code TypePrecedence.INFERRED}：两侧 DTO 是各自独立的类，消息头里的 {@code __TypeId__}
     * 指向生产端 FQCN，消费端加载不到，只能按监听方法的参数类型反序列化。
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
