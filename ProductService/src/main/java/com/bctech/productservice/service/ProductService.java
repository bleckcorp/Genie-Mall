package com.bctech.productservice.service;

import com.bctech.productservice.dto.request.ProductRequest;
import com.bctech.productservice.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    void createProduct(ProductRequest productRequest);

    List<ProductResponse> getAllProducts();
}
