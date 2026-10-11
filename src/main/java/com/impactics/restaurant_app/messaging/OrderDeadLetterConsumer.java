package com.impactics.restaurant_app.messaging;

import com.impactics.restaurant_app.config.KafkaConfig;
import com.impactics.restaurant_app.dto.OrderEvent;
import com.impactics.restaurant_app.entity.Order;
import com.impactics.restaurant_app.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class OrderDeadLetterConsumer {

    static final String FAILURE_REASON =
            "Order could not be processed due to a system error after multiple attempts";

    private final OrderRepository orderRepository;

    public OrderDeadLetterConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Deliberately NOT @Transactional and deliberately never throws: the shared error handler
    // routes failed records to this same DLT, so a failure here would loop back into it.
    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_DLT, groupId = "order-dlt-group", concurrency = "1")
    public void handleDeadLetter(ConsumerRecord<String, OrderEvent> record) {
        try {
            OrderEvent event = record.value();
            if (event == null || event.getOrderId() == null) {
                log.error("Dead-letter record has no usable order id (partition={}, offset={}) - no order updated",
                        record.partition(), record.offset());
                return;
            }

            Optional<Order> orderOpt = orderRepository.findById(event.getOrderId());
            if (orderOpt.isEmpty()) {
                log.warn("Dead-lettered order {} does not exist in the database - nothing to update", event.getOrderId());
                return;
            }

            Order order = orderOpt.get();
            if (!"PENDING".equals(order.getStatus())) {
                log.info("Dead-lettered order {} is already {} - leaving it unchanged", order.getId(), order.getStatus());
                return;
            }

            order.setStatus("FAILED");
            order.setFailureReason(FAILURE_REASON);
            orderRepository.save(order);

            log.error("Order {} marked FAILED after exhausting retries (DLT partition={}, offset={})",
                    order.getId(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("Could not process dead-letter record (partition={}, offset={}); order status left unchanged",
                    record.partition(), record.offset(), e);
        }
    }
}