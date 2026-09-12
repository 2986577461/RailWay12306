# Railway Platform Backend

12306 高并发火车票系统后端。**Spring Boot 单体应用**（由原 Spring Cloud 微服务架构合并而来）。

- 单一 JVM / 单一 JAR / 单一端口（8080），本地和云服务器都能轻松跑
- 外部依赖只有 3 个：**MySQL**、**Redis**、**RocketMQ**
- 不再依赖 Nacos / Spring Cloud Gateway / LoadBalancer

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Java | 21 (LTS) |
| Spring Boot | 3.2.5 |
| RocketMQ | rocketmq-spring-boot-starter 2.3.2 |
| MySQL | 8.x |
| Redis | 7.x |

## 包结构（src/main/java/com/xiaoyan/railway/）

| 包 | 职责 |
| --- | --- |
| `common` | 统一响应 `ApiResponse`、业务异常、全局异常处理、RocketMQ 事件 |
| `config` / `security` | JWT 生成解析、`HandlerInterceptor` 鉴权、`UserContext`、BCrypt |
| `user` | 注册/登录/验证码（JWT）（`/api/users/**`） |
| `passenger` | 乘车人 CRUD（`/api/passengers/**`） |
| `basic` | 车站/车次/经停/席别/运行/票价 实体与维护（`/api/stations/**`、`/api/seat-types`） |
| `query` | 余票查询 + Redis 缓存（`/api/trains/**`） |
| `inventory` | 库存初始化、Redis Lua 分段锁库存、`railway-ticket-request` 消费 |
| `order` | 下单请求、订单生命周期（`/api/orders/**`） |
| `payment` | `railway-order-paid` 消费 |
| `admin` | 运营后台维护（`/api/admin/**`） |

入口类：`com.xiaoyan.railway.RailwayApplication`（端口 8080）。

## 本地运行

需要本机或 docker 提供 MySQL / Redis / RocketMQ：

```bash
# 1. 起基础设施（MySQL/Redis/RocketMQ；若你已自建可跳过对应服务）
docker compose up -d mysql redis rmqnamesrv rmqbroker

# 2. 编译打包
mvn -DskipTests package

# 3. 启动单体
mvn spring-boot:run
```

验证：

```bash
curl http://127.0.0.1:8080/api/stations
curl -G 'http://127.0.0.1:8080/api/trains/search' \
  --data-urlencode 'from=北京' --data-urlencode 'to=上海' --data-urlencode 'date=2026-09-12'
curl http://127.0.0.1:8080/api/seat-types
curl http://127.0.0.1:8080/actuator/health
```

## 已实现功能

- **用户**：注册/密码登录/验证码登录（JWT 无状态，拦截器鉴权，BCrypt 加密，验证码走 Redis TTL+限流）；乘车人 CRUD（上限 10 人、归属校验、身份证校验）
- **基础资料**：车站/车次/经停站/席别/票价/运行日期；`data.sql` 种子含 G101/G102/G7 三趟车 + 120 天运行
- **余票查询**：`GET /api/trains/search` 返回车次、区间、时长、各席别余票与票价；**Redis 查询缓存**（防穿透/击穿/雪崩）
- **库存闭环**：启动时把 `seat_inventory` 灌入 Redis `inventory:{runId}:{seatTypeId}:{seg}` 分段键；余票查询读 Redis（跨段取 min）、抢票用 Lua 原子扣减、同一套 key，**余票随抢票真实下降**
- **抢票下单**：RocketMQ 异步（`railway-ticket-request` → 锁库存 → `railway-inventory-locked`）+ 幂等键 + 分段锁库存
- **运营后台**（`/api/admin/**`，暂未鉴权）：车站/车次/票价/运行维护、库存重灌

启动后 Redis 里应能看到：`inventory:*`（分段库存）、`query:trains:*`（查询缓存）、`sms:*`（验证码）。

## 配置文件

`src/main/resources/application.yml` 中全部通过环境变量占位，默认值指向本机：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/railway?...` | MySQL 连接串 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | MySQL 账号密码 |
| `REDIS_HOST` / `REDIS_PORT` | `127.0.0.1` / `6379` | Redis 地址 |
| `REDIS_PASSWORD` | 空 | Redis 密码（无则留空） |
| `ROCKETMQ_NAME_SERVER` | `127.0.0.1:9876` | RocketMQ NameServer |

部署到云服务器时，二选一：改 `application.yml`，或 `export` 上述环境变量再启动。
表结构由 `data.sql` 在启动时自动初始化（`CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，幂等安全）。

## Docker 部署

```bash
docker compose up -d --build
```

Compose 会同时起 MySQL / Redis / RocketMQ 和单体 `app`，对外暴露 `8080`。若云上已自建 MySQL/Redis，可注释掉 compose 里对应 service，只保留 `rmqnamesrv` / `rmqbroker` / `app`。

## 出票异步流程（RocketMQ）

```
POST /api/orders/requests
      │  写入 orders 表，幂等键去重
      ▼
[railway-ticket-request] ──► inventory 消费
                                   │  Redis Lua 原子锁区间库存
                                   ▼
                        [railway-inventory-locked]
                                   │
                                   ▼
                        [railway-order-paid] ──► payment 消费（支付回调）
```

## 从微服务拆分说明

若后续要拆回 Spring Cloud 微服务：`order`/`inventory`/`payment` 等包的代码无需改动，只需恢复原 Spring Cloud 依赖（Nacos discovery + Gateway 路由）并按模块拆分即可。RocketMQ 生产者、消费者在单体内同 JVM，异步语义与原先一致。
