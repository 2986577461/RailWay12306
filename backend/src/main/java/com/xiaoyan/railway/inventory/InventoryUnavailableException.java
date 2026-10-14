package com.xiaoyan.railway.inventory;

import com.xiaoyan.railway.common.BizException;

/**
 * Stock service cannot answer right now: the ledger rebuild holds the gate, or the Redis breaker is open.
 * Callers must retry later and must not treat this as insufficient stock.
 */
public class InventoryUnavailableException extends BizException {
    public InventoryUnavailableException() {
        super("库存服务暂时不可用，请稍后再试");
    }
}
