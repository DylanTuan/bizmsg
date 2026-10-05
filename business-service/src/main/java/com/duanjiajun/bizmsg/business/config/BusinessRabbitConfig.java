package com.duanjiajun.bizmsg.business.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 生产者侧拓扑：只声明交换机和 JSON 转换器，队列与绑定由消费方声明。
 * 声明是幂等的，两边同时声明同一交换机不冲突，但属性必须一致。
 */
@Configuration
public class BusinessRabbitConfig {

    @Bean
    public TopicExchange businessExchange(BusinessMqProperties properties) {
        return ExchangeBuilder.topicExchange(properties.getExchange()).durable(true).build();
    }

    /**
     * Spring Boot 不会自动装配 Jackson 转换器，默认的 SimpleMessageConverter 只支持
     * String / byte[] / Serializable，直接发 record 会抛异常，所以这里显式声明。
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        // 消费端按监听方法参数类型反序列化，不依赖 __TypeId__，两侧 DTO 可独立演进
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
