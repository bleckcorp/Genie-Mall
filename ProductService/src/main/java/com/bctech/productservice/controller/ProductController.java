package com.bctech.productservice.controller;


import com.bctech.productservice.dto.request.ProductRequest;
import com.bctech.productservice.dto.response.ProductResponse;
import com.bctech.productservice.service.ProductService;
import com.bctech.productservice.service.implementation.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

//TODO: Change the return type of the methods to ResponseEntity, and also create corresponding integration tests
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createProduct(@RequestBody ProductRequest productRequest) {
        productService.createProduct(productRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

}
