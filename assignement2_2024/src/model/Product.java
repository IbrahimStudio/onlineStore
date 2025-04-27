package model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Represents a product with an identifier, name, price, and available stock quantity.
 */
public class Product {
    private static final Logger logger = Logger.getLogger(Product.class.getName());

    private final String id;
    private final String name;
    private final double price;
    private final AtomicInteger quantity;

    /**
     * Constructs a new Product.
     *
     * @param id              unique product identifier
     * @param name            product name
     * @param price           product price (must be non-negative)
     * @param initialQuantity initial stock quantity (must be non-negative)
     * @throws IllegalArgumentException if price or initialQuantity is negative
     */
    public Product(String id, String name, double price, int initialQuantity) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (initialQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = new AtomicInteger(initialQuantity);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity.get();
    }

    /**
     * Attempts to purchase one unit of this product. Decrements stock if available.
     *
     * @return true if the purchase succeeded; false if out of stock
     */
    public boolean purchase() {
        while (true) {
            int current = quantity.get();
            if (current <= 0) {
                logger.warning("Attempt to purchase out-of-stock product: " + id);
                return false;
            }
            if (quantity.compareAndSet(current, current - 1)) {
                logger.info("Product purchased: " + id + ", remaining stock: " + (current - 1));
                return true;
            }
        }
    }

    /**
     * Restocks one unit of this product.
     */
    public void restock() {
        int newQty = quantity.incrementAndGet();
        logger.info("Product restocked: " + id + ", new stock: " + newQty);
    }

    @Override
    public String toString() {
        // id;name;price;quantity
        return String.format("%s;%s;%.2f;%d", id, name, price, getQuantity());
    }
}
