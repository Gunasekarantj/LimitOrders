package org.afob.limit;

import org.afob.execution.ExecutionClient;
import org.afob.execution.ExecutionClient.ExecutionException;
import org.afob.prices.PriceListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LimitOrderAgent implements PriceListener {

    private final ExecutionClient executionClient;
    final List<Order> orders;

    public LimitOrderAgent(final ExecutionClient ec) {
        this.executionClient = ec;
        this.orders = new ArrayList<>();
        addOrder(true, "IBM", 1000, new BigDecimal("100.00"));
    }

    @Override
    public void priceTick(String productId, BigDecimal price) {
        List<Order> executedOrders = new ArrayList<>();
        for (Order order : orders) {
            if (order.productId.equals(productId) &&
                    ((order.isBuy && price.compareTo(order.limitPrice) < 0) ||
                            (!order.isBuy && price.compareTo(order.limitPrice) > 0))) {
                try {
                    if (order.isBuy) {
                        executionClient.buy(productId, order.amount);
                    } else {
                        executionClient.sell(productId, order.amount);
                    }
                    executedOrders.add(order);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        orders.removeAll(executedOrders);
    }

    public void addOrder(boolean isBuy, String productId, int amount, BigDecimal limitPrice) {
        orders.add(new Order(isBuy, productId, amount, limitPrice));
    }

    static class Order {
        boolean isBuy;
        String productId;
        int amount;
        BigDecimal limitPrice;

        Order(boolean isBuy, String productId, int amount, BigDecimal limitPrice) {
            this.isBuy = isBuy;
            this.productId = productId;
            this.amount = amount;
            this.limitPrice = limitPrice;
        }
    }
}
