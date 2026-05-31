package bai_tap_lon.common.model.item;

import bai_tap_lon.common.model.user.Seller;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void isValidCategoryIsCaseInsensitiveAndTrimsInput() {
        assertTrue(ItemFactory.isValidCategory(" electronics "));
        assertTrue(ItemFactory.isValidCategory("ART"));
        assertTrue(ItemFactory.isValidCategory("vehicle"));
        assertFalse(ItemFactory.isValidCategory("book"));
        assertFalse(ItemFactory.isValidCategory(null));
    }

    @Test
    void createElectronicsMapsJsonAttributes() {
        Seller seller = new Seller("seller", "password", "seller@example.com");
        JsonObject attributes = new JsonObject();
        attributes.addProperty("brand", "Apple");
        attributes.addProperty("warrantyMonths", 24);

        Item item = ItemFactory.createItem(
                "electronics",
                "MacBook",
                "Laptop",
                20_000_000,
                seller,
                "/uploads/macbook.png",
                attributes
        );

        assertInstanceOf(Electronics.class, item);
        Electronics electronics = (Electronics) item;
        assertEquals("Electronics", electronics.getCategory());
        assertEquals("Apple", electronics.getBrand());
        assertEquals(24, electronics.getWarrantyMonths());
        assertEquals("Apple", electronics.getAttributes().get("brand").getAsString());
        assertTrue(electronics.validate());
    }

    @Test
    void createVehicleUsesDefaultsWhenAttributesAreMissing() {
        Seller seller = new Seller("seller", "password", "seller@example.com");

        Item item = ItemFactory.createItem(
                "Vehicle",
                "Bike",
                "City bike",
                1_000_000,
                seller,
                "/uploads/bike.png",
                null
        );

        assertInstanceOf(Vehicle.class, item);
        Vehicle vehicle = (Vehicle) item;
        assertEquals("", vehicle.getBrand());
        assertEquals(0, vehicle.getYear());
        assertTrue(vehicle.validate());
    }

    @Test
    void createItemRejectsUnknownCategory() {
        Seller seller = new Seller("seller", "password", "seller@example.com");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ItemFactory.createItem(
                        "Book",
                        "Clean Code",
                        "Book",
                        100_000,
                        seller,
                        "/uploads/book.png",
                        new JsonObject()
                )
        );

        assertTrue(ex.getMessage().contains("Unknown category"));
    }
}
