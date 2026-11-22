package com.xiaoyan.compute.provision;

import com.xiaoyan.compute.catalog.GpuSku;
import com.xiaoyan.compute.order.RentalOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockCloudProviderAdapter implements CloudProviderAdapter {
    private final long delayMs;
    private final double failureRate;

    public MockCloudProviderAdapter(@Value("${compute.provision.delay-ms:1200}") long delayMs,
                                    @Value("${compute.provision.failure-rate:0}") double failureRate) {
        this.delayMs = delayMs;
        this.failureRate = failureRate;
    }

    @Override
    public ProvisionResult provision(RentalOrder order, GpuSku sku) throws Exception {
        if (delayMs > 0) Thread.sleep(delayMs);
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new IllegalStateException("provider queue timeout");
        }
        String suffix = Integer.toHexString(Math.abs((order.getOrderNo() + sku.getCode()).hashCode())) ;
        suffix = (suffix + "0000").substring(0, 4);
        String prefix = sku.getCode().replace("-", "").toLowerCase();
        String instanceNo = "inst-" + prefix + "-" + suffix;
        String ip = "10.24." + (10 + Math.abs(order.getId().intValue()) % 200) + "." + (10 + Math.abs(sku.getId().intValue()) % 200);
        String endpoint = instanceNo + ".nebula.internal";
        return new ProvisionResult(instanceNo, ip, endpoint, "nebula ssh " + instanceNo);
    }
}
