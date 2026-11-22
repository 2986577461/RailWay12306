# Nebula Compute Backend

GPU 算力租赁与交付平台后端，Spring Boot 3.2 + MyBatis-Plus + MySQL + Redis + RocketMQ。

## 本地启动

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`。开发配置在 `src/main/resources/application-dev.yml`，包含远端 MySQL / Redis / RocketMQ 连接信息，该文件已被 gitignore。

`data.sql` 在每次启动时执行，采用 `DROP TABLE + CREATE TABLE + Seed` 的方式重建演示数据。`spring.sql.init.mode` 保持为 `always`。

## Demo 账号

```text
邮箱：demo@nebula.ai
密码：Nebula@2026
```

## 主要接口

### 邮箱认证

- `POST /api/auth/register`：`{ email, password, displayName }`
- `POST /api/auth/login`：`{ email, password }`
- `GET /api/auth/me`：Bearer Token

### GPU 目录

- `GET /api/gpus`
- `GET /api/gpus/{code}`
- `GET /api/gpus/{code}/availability?startAt=2026-11-22T19:00:00&hours=2`

### 售中订单

- `POST /api/orders`：创建订单，Header `Idempotency-Key` 可选
- `GET /api/orders`
- `GET /api/orders/{orderNo}`
- `POST /api/orders/{orderNo}/cancel`
- `POST /api/orders/{orderNo}/refund`
- `POST /api/orders/{orderNo}/payment`
- `POST /api/payments/mock-notify`

### 实例、控制台与账单

- `GET /api/instances`
- `GET /api/instances/{instanceNo}`
- `GET /api/dashboard`
- `GET /api/billing/summary`

## 售中状态机

```text
PENDING_PAYMENT -> PAID -> PROVISIONING -> RUNNING -> COMPLETED
        |             |           |
        v             v           v
    CANCELLED      REFUNDING   PROVISION_FAILED -> REFUNDING -> REFUNDED
```

核心链路：

1. 下单后通过 RocketMQ 异步锁定 Redis 小时级资源库存。
2. 支付回调将订单从待支付推进到已支付。
3. 支付事件驱动 mock provider 开通实例，订单进入运行中。
4. 开通失败重试耗尽后自动退款并释放未来租期。
5. 待支付超时自动取消；运行到期自动完成并终止实例。
