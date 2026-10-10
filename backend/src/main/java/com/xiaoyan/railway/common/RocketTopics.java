package com.xiaoyan.railway.common;

public final class RocketTopics {
    public static final String TICKET_REQUEST = "railway-ticket-request";
    public static final String INVENTORY_LOCKED = "railway-inventory-locked";
    public static final String INVENTORY_RELEASE = "railway-inventory-release";
    public static final String ORDER_PAID = "railway-order-paid";
    public static final String CACHE_EVICT = "railway-cache-evict";
    private RocketTopics() { }
}
