package com.bctech.orderservice.service.implementation;

import com.bctech.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bctech.orderservice.dto.response.InventoryResponse;
import com.bctech.orderservice.dto.response.CartItemsDto;
import com.bctech.orderservice.dto.request.OrderRequest;
import com.bctech.orderservice.event.OrderPlacedEvent;
import com.bctech.orderservice.model.Order;
import com.bctech.orderservice.model.CartItem;
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
//        private final Tracer tracer;
        private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;


        @Override
        public String placeOrder(OrderRequest orderRequest) {
            Order order = new Order();

            //ordernumber is different from orderid
            order.setOrderNumber(UUID.randomUUID().toString());

            //convert order items to cartlist

            List<CartItem> cartList = orderRequest.getCartItemsDto()
                    .stream()
                    .map(this::mapToDto)
                    .toList();

            order.setCartList(cartList);

            // get skucodes from cartlist to check for stock
            List<String> skuCodes = order.getCartList().stream()
                    .map(CartItem::getSkuCode)
                    .toList();
//
//            Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");
//
//            try (Tracer.SpanInScope isLookup = tracer.withSpanInScope(inventoryServiceLookup.start())) {
//
//                inventoryServiceLookup.tag("call", "inventory-service");

                // Call Inventory Service, and place order if product is in
                // stock

            //
            boolean allProductsInStock = checkStockViaInventoryService(skuCodes);

            if (allProductsInStock) {
                    orderRepository.save(order);
                    kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                    return "Order Placed Successfully";
                } else {
                    throw new IllegalArgumentException("Product is not in stock, please try again later");
                }

//            finally {
//                inventoryServiceLookup.flush();
//            }
        }

    private boolean checkStockViaInventoryService(List<String> skuCodes) {

            //using a synchronous communication -->  web client

        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block(); // for synchronized call

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(InventoryResponse::isInStock);
        return allProductsInStock;
    }

    private CartItem mapToDto(CartItemsDto cartItemsDto) {
            CartItem cartList = new CartItem();
            cartList.setPrice(cartItemsDto.getPrice());
            cartList.setQuantity(cartItemsDto.getQuantity());
            cartList.setSkuCode(cartItemsDto.getSkuCode());
            return cartList;
        }
    }

