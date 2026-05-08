package com.example.bai_tap_lon.service;

import com.example.bai_tap_lon.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ProductStore {

    private static final ObservableList<Product> products =
            FXCollections.observableArrayList();

    public static ObservableList<Product> getProducts() {
        return products;
    }

    public static void addProduct(Product product) {
        products.add(product);
    }
}