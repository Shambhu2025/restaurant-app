package com.impactics.restaurant_app.messaging;

import com.impactics.restaurant_app.dto.OrderEvent;
import com.impactics.restaurant_app.entity.Order;
import com.impactics.restaurant_app.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDeadLetterConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderDeadLetterConsumer consumer;

    private ConsumerRecord<String, OrderEvent> recordFor(OrderEvent event) {
        return new ConsumerRecord<>("order-events.DLT", 0, 5L, "restaurant-key", event);
    }

    private OrderEvent eventFor(UUID orderId) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        return event;
    }

    @Test
    void handleDeadLetter_whenOrderPending_marksItFailedWithGenericReason() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus("PENDING");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        consumer.handleDeadLetter(recordFor(eventFor(orderId)));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).isEqualTo(OrderDeadLetterConsumer.FAILURE_REASON);
        verify(orderRepository).save(order);
    }

    @Test
    void handleDeadLetter_whenOrderAlreadyConfirmed_leavesItUntouched() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus("CONFIRMED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        consumer.handleDeadLetter(recordFor(eventFor(orderId)));

        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        assertThat(order.getFailureReason()).isNull();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handleDeadLetter_whenOrderMissing_doesNothing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        consumer.handleDeadLetter(recordFor(eventFor(orderId)));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handleDeadLetter_whenRecordHasNoOrderId_doesNothing() {
        consumer.handleDeadLetter(recordFor(new OrderEvent()));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleDeadLetter_whenRecordValueIsNull_doesNothing() {
        consumer.handleDeadLetter(recordFor(null));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleDeadLetter_whenRepositoryFails_swallowsExceptionToAvoidDltLoop() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> consumer.handleDeadLetter(recordFor(eventFor(orderId))))
                .doesNotThrowAnyException();
    }
}