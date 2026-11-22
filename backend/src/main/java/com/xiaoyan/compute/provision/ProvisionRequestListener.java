package com.xiaoyan.compute.provision;

import com.xiaoyan.compute.common.ProvisionRequestEvent;
import com.xiaoyan.compute.common.RocketTopics;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = RocketTopics.PROVISION_REQUEST, consumerGroup = "compute-provision-worker")
public class ProvisionRequestListener implements RocketMQListener<ProvisionRequestEvent> {
    private final ProvisionService provisionService;

    public ProvisionRequestListener(ProvisionService provisionService) {
        this.provisionService = provisionService;
    }

    @Override
    public void onMessage(ProvisionRequestEvent event) {
        provisionService.provision(event);
    }
}
