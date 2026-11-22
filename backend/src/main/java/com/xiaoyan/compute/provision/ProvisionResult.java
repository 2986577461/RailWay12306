package com.xiaoyan.compute.provision;

public record ProvisionResult(String instanceNo, String privateIp, String endpoint, String sshCommand) { }
