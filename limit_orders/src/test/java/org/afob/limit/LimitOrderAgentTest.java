package org.afob.limit;

import org.afob.execution.ExecutionClient;
import org.afob.execution.ExecutionClient.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LimitOrderAgentTest {

    private ExecutionClient executionClient;
    private LimitOrderAgent limitOrderAgent;

    @BeforeEach
    void setUp() {
        executionClient = mock(ExecutionClient.class);
        limitOrderAgent = new LimitOrderAgent(executionClient);
    }

    @Test
    void testAddOrder() {
        limitOrderAgent.addOrder(true, "product1", 10, new BigDecimal("100.00"));
        limitOrderAgent.addOrder(false, "product2", 5, new BigDecimal("200.00"));

        assertEquals(3, limitOrderAgent.orders.size());

        LimitOrderAgent.Order ibmOrder = limitOrderAgent.orders.get(0);
        assertTrue(ibmOrder.isBuy);
        assertEquals("IBM", ibmOrder.productId);
        assertEquals(1000, ibmOrder.amount);
        assertEquals(new BigDecimal("100.00"), ibmOrder.limitPrice);

        LimitOrderAgent.Order firstOrder = limitOrderAgent.orders.get(1);
        assertTrue(firstOrder.isBuy);
        assertEquals("product1", firstOrder.productId);
        assertEquals(10, firstOrder.amount);
        assertEquals(new BigDecimal("100.00"), firstOrder.limitPrice);

        LimitOrderAgent.Order secondOrder = limitOrderAgent.orders.get(2);
        assertFalse(secondOrder.isBuy);
        assertEquals("product2", secondOrder.productId);
        assertEquals(5, secondOrder.amount);
        assertEquals(new BigDecimal("200.00"), secondOrder.limitPrice);
    }

    @Test
    void testPriceTickBuyOrderExecuted() throws ExecutionException {
        limitOrderAgent.addOrder(true, "product1", 10, new BigDecimal("100.00"));
        limitOrderAgent.priceTick("product1", new BigDecimal("90.00"));
        verify(executionClient).buy("product1", 10);
    }

    @Test
    void testPriceTickSellOrderExecuted() throws ExecutionException {
        limitOrderAgent.addOrder(false, "product1", 10, new BigDecimal("100.00"));
        limitOrderAgent.priceTick("product1", new BigDecimal("110.00"));
        verify(executionClient).sell("product1", 10);
    }

    @Test
    void testPriceTickNoOrderExecuted() throws ExecutionException {
        limitOrderAgent.addOrder(true, "product1", 10, new BigDecimal("100.00"));
        limitOrderAgent.priceTick("product1", new BigDecimal("110.00"));
        verify(executionClient, never()).buy(anyString(), anyInt());
        verify(executionClient, never()).sell(anyString(), anyInt());
    }

    @Test
    void testPriceTickExecutionException() throws ExecutionException {
        limitOrderAgent.addOrder(true, "product1", 10, new BigDecimal("100.00"));
        doThrow(new ExecutionException("Execution failed")).when(executionClient).buy(anyString(), anyInt());

        int initialOrderSize = limitOrderAgent.orders.size();

        limitOrderAgent.priceTick("product1", new BigDecimal("90.00"));

        verify(executionClient).buy("product1", 10);

        assertEquals(initialOrderSize, limitOrderAgent.orders.size());
    }

    @Test
    void testPriceTickIBMOrderExecuted() throws ExecutionException {
        limitOrderAgent.priceTick("IBM", new BigDecimal("99.00"));
        verify(executionClient).buy("IBM", 1000);
    }
}
