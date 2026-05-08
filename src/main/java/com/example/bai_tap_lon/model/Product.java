package com.example.bai_tap_lon.model;

import javafx.beans.property.*;

public class Product {

    private final StringProperty name;
    private final DoubleProperty price;
    private final StringProperty imagePath;
    private final LongProperty endTime;

    public Product(String name,
                   double price,
                   String imagePath,
                   long endTime) {

        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
        this.imagePath = new SimpleStringProperty(imagePath);
        this.endTime = new SimpleLongProperty(endTime);
    }

    public String getName() {
        return name.get();
    }

    public double getPrice() {
        return price.get();
    }

    public String getImagePath() {
        return imagePath.get();
    }

    public long getEndTime() {
        return endTime.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public DoubleProperty priceProperty() {
        return price;
    }
}