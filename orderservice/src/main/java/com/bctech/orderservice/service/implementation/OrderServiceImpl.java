package com.bctech.orderservice.service.implementation;

import com.bctech.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import brave.Span;
import brave.Tracer;
import com.bctech.orderservice.dto.response.InventoryResponse;
import com.bctech.orderservice.dto.response.OrderLineItemsDto;
import com.bctech.orderservice.dto.request.OrderRequest;
import com.bctech.orderservice.event.OrderPlacedEvent;
import com.bctech.orderservice.model.Order;
import com.bctech.orderservice.model.OrderLineItems;
import com.bctech.orderservice.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final WebClient.Builder webClientBuilder;
        private final Tracer tracer;
        private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;


        @Override
        public String placeOrder(OrderRequest orderRequest) {
            Order order = new Order();

            //ordernumber is different from orderid
            order.setOrderNumber(UUID.randomUUID().toString());

            List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                    .stream()
                    .map(this::mapToDto)
                    .toList();

            order.setOrderLineItemsList(orderLineItems);

            List<String> skuCodes = order.getOrderLineItemsList().stream()
                    .map(OrderLineItems::getSkuCode)
                    .toList();

            Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

            try (Tracer.SpanInScope isLookup = tracer.withSpanInScope(inventoryServiceLookup.start())) {

                inventoryServiceLookup.tag("call", "inventory-service");
                // Call Inventory Service, and place order if product is in
                // stock
                InventoryResponse[] inventoryResponsArray = webClientBuilder.build().get()
                        .uri("http://inventory-service/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();

                boolean allProductsInStock = Arrays.stream(inventoryResponsArray)
                        .allMatch(InventoryResponse::isInStock);

                if (allProductsInStock) {
                    orderRepository.save(order);
                    kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                    return "Order Placed Successfully";
                } else {
                    throw new IllegalArgumentException("Product is not in stock, please try again later");
                }
            } finally {
                inventoryServiceLookup.flush();
            }
        }

        private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
            OrderLineItems orderLineItems = new OrderLineItems();
            orderLineItems.setPrice(orderLineItemsDto.getPrice());
            orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
            orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
            return orderLineItems;
        }
    }

