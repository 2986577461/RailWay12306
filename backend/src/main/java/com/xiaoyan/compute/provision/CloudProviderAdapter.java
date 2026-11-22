package com.xiaoyan.compute.provision;

import com.xiaoyan.compute.catalog.GpuSku;
import com.xiaoyan.compute.order.RentalOrder;

public interface CloudProviderAdapter {
    ProvisionResult provision(RentalOrder order, GpuSku sku) throws Exception;
}
