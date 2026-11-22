package com.xiaoyan.compute.provision;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ProvisionRetryJob {
    private final ProvisionTaskMapper taskMapper;
    private final ProvisionService provisionService;

    public ProvisionRetryJob(ProvisionTaskMapper taskMapper, ProvisionService provisionService) {
        this.taskMapper = taskMapper;
        this.provisionService = provisionService;
    }

    @Scheduled(fixedDelayString = "${compute.provision.retry-scan-seconds:10000}")
    public void retryDueTasks() {
        List<ProvisionTask> tasks = taskMapper.selectList(Wrappers.<ProvisionTask>lambdaQuery()
                .eq(ProvisionTask::getStatus, ProvisionTaskStatus.RETRYING.getCode())
                .le(ProvisionTask::getNextRetryAt, LocalDateTime.now())
                .last("LIMIT 50"));
        tasks.forEach(provisionService::retry);
    }
}
