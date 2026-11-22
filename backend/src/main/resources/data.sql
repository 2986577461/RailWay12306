-- Nebula Compute demo schema and seed data.
-- Executed on every boot because spring.sql.init.mode=always.
-- This is intentionally a demo reset: drop and recreate the core tables.

SET NAMES utf8mb4;
SET time_zone = '+08:00';
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS billing_invoice;
DROP TABLE IF EXISTS gpu_instance;
DROP TABLE IF EXISTS provision_task;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS rental_order;
DROP TABLE IF EXISTS gpu_inventory_slot;
DROP TABLE IF EXISTS gpu_sku;
DROP TABLE IF EXISTS outbox_event;
DROP TABLE IF EXISTS app_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE app_user (
    id            BIGINT PRIMARY KEY,
    email         VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(64)  NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 1,
    balance       DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gpu_sku (
    id             BIGINT PRIMARY KEY,
    code           VARCHAR(64)  NOT NULL UNIQUE,
    name           VARCHAR(128) NOT NULL,
    architecture   VARCHAR(64)  NOT NULL,
    memory_gb      INT          NOT NULL,
    memory_type    VARCHAR(32)  NOT NULL,
    bandwidth      VARCHAR(32)  NOT NULL,
    tflops         VARCHAR(32)  NOT NULL,
    price_per_hour DECIMAL(10,2) NOT NULL,
    region         VARCHAR(64)  NOT NULL,
    badge          VARCHAR(32),
    description    VARCHAR(255),
    use_cases      VARCHAR(255),
    accent         VARCHAR(16),
    capacity       INT          NOT NULL DEFAULT 1,
    status         TINYINT      NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gpu_inventory_slot (
    id           BIGINT PRIMARY KEY,
    sku_id       BIGINT   NOT NULL,
    slot_start   DATETIME NOT NULL,
    total_count  INT      NOT NULL,
    locked_count INT      NOT NULL DEFAULT 0,
    sold_count   INT      NOT NULL DEFAULT 0,
    version      INT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sku_slot (sku_id, slot_start),
    KEY idx_slot_start (slot_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rental_order (
    id                    BIGINT PRIMARY KEY,
    order_no              VARCHAR(64) NOT NULL UNIQUE,
    user_id               BIGINT      NOT NULL,
    sku_id                BIGINT      NOT NULL,
    region                VARCHAR(64) NOT NULL,
    quantity              INT         NOT NULL,
    start_at              DATETIME    NOT NULL,
    end_at                DATETIME    NOT NULL,
    hours                 INT         NOT NULL,
    unit_price            DECIMAL(10,2) NOT NULL,
    total_amount          DECIMAL(12,2) NOT NULL,
    status                TINYINT     NOT NULL,
    lock_status           TINYINT     NOT NULL DEFAULT 0,
    lock_fail_reason      VARCHAR(255),
    provision_status      TINYINT     NOT NULL DEFAULT 0,
    provision_retry_count INT         NOT NULL DEFAULT 0,
    provision_error       VARCHAR(255),
    instance_id           BIGINT,
    payment_no            VARCHAR(64),
    expire_at             DATETIME,
    paid_at               DATETIME,
    provisioned_at        DATETIME,
    completed_at          DATETIME,
    cancelled_at          DATETIME,
    idempotency_key       VARCHAR(128) NOT NULL,
    created_at            DATETIME    NOT NULL,
    updated_at            DATETIME    NOT NULL,
    UNIQUE KEY uk_user_idempotency (user_id, idempotency_key),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status_expire (status, expire_at),
    KEY idx_status_end (status, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment (
    id             BIGINT PRIMARY KEY,
    payment_no     VARCHAR(64) NOT NULL UNIQUE,
    order_id       BIGINT      NOT NULL UNIQUE,
    channel        VARCHAR(32) NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    status         TINYINT     NOT NULL,
    third_party_no VARCHAR(128),
    paid_at        DATETIME,
    refunded_at    DATETIME,
    created_at     DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE provision_task (
    id                   BIGINT PRIMARY KEY,
    order_id             BIGINT NOT NULL UNIQUE,
    status               TINYINT NOT NULL,
    attempt              INT NOT NULL DEFAULT 0,
    next_retry_at        DATETIME,
    last_error           VARCHAR(255),
    provider_instance_id VARCHAR(128),
    created_at           DATETIME NOT NULL,
    updated_at           DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gpu_instance (
    id            BIGINT PRIMARY KEY,
    instance_no   VARCHAR(64) NOT NULL UNIQUE,
    order_id      BIGINT      NOT NULL UNIQUE,
    sku_id        BIGINT      NOT NULL,
    region        VARCHAR(64) NOT NULL,
    status        TINYINT     NOT NULL,
    private_ip    VARCHAR(64),
    endpoint      VARCHAR(128),
    ssh_command   VARCHAR(255),
    utilization   INT         NOT NULL DEFAULT 0,
    started_at    DATETIME,
    expires_at    DATETIME,
    terminated_at DATETIME,
    created_at    DATETIME    NOT NULL,
    updated_at    DATETIME    NOT NULL,
    KEY idx_instance_user_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_invoice (
    id           BIGINT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    invoice_no   VARCHAR(64) NOT NULL UNIQUE,
    period_label VARCHAR(32) NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    status       VARCHAR(32) NOT NULL,
    issued_at    DATE        NOT NULL,
    KEY idx_invoice_user (user_id, issued_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbox_event (
    id             BIGINT PRIMARY KEY,
    event_id       VARCHAR(64) NOT NULL UNIQUE,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(64) NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    payload        JSON NOT NULL,
    status         TINYINT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL,
    published_at   DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- demo account: demo@nebula.ai / Nebula@2026
INSERT INTO app_user (id, email, password_hash, display_name, status, balance, created_at, updated_at)
VALUES (10001, 'demo@nebula.ai',
        '$2a$10$fJPBTloYiUADcwkpADCkc.MtpDapbYZwMYkZwkT3Zf6XpNWUnxm9i',
        'Alex Chen', 1, 12680.00, NOW(), NOW());

INSERT INTO gpu_sku (id, code, name, architecture, memory_gb, memory_type, bandwidth, tflops,
                     price_per_hour, region, badge, description, use_cases, accent, capacity, status)
VALUES
(1, 'h100-sxm', 'NVIDIA H100 SXM', 'Hopper', 80, 'HBM3', '3.35 TB/s', '1,979 TFLOPS',
 42.00, '上海 · CN-East', '旗舰', '为大模型训练、长上下文推理和科学计算准备的旗舰算力。', 'LLM 训练,推理集群,科学计算', '#b8f36a', 16, 1),
(2, 'a100-80g', 'NVIDIA A100', 'Ampere', 80, 'HBM2e', '2.0 TB/s', '624 TFLOPS',
 26.00, '北京 · CN-North', '高性价比', '成熟稳定的通用 GPU 工作负载，适合微调、向量检索和推理。', '模型微调,向量检索,批量推理', '#65d9f7', 24, 1),
(3, 'l40s', 'NVIDIA L40S', 'Ada Lovelace', 48, 'GDDR6', '864 GB/s', '733 TFLOPS',
 18.00, '深圳 · CN-South', '推理优选', '面向图形渲染、生成式 AI 和视频推理的高密度推理节点。', '生成式 AI,视频推理,图形渲染', '#c7a8ff', 20, 1),
(4, 'rtx-4090', 'NVIDIA RTX 4090', 'Ada Lovelace', 24, 'GDDR6X', '1.0 TB/s', '330 TFLOPS',
 9.00, '上海 · CN-East', '开发者友好', '适合个人开发者、原型验证、小型模型微调和实时推理。', '原型验证,小型微调,实时推理', '#ffb86b', 32, 1);

-- 72 hourly slots from the current hour for each SKU.
INSERT INTO gpu_inventory_slot (id, sku_id, slot_start, total_count, locked_count, sold_count, version)
WITH RECURSIVE hours AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM hours WHERE n < 71
)
SELECT sku.id * 1000 + h.n,
       sku.id,
       DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL h.n HOUR),
       sku.capacity,
       0, 0, 0
FROM gpu_sku sku CROSS JOIN hours h;

-- Demo orders: running, provisioning, pending payment, and a degraded running instance.
INSERT INTO rental_order
(id, order_no, user_id, sku_id, region, quantity, start_at, end_at, hours, unit_price, total_amount,
 status, lock_status, provision_status, instance_id, payment_no, expire_at, paid_at, provisioned_at,
 completed_at, cancelled_at, idempotency_key, created_at, updated_at)
VALUES
(90001, 'ORD-90001', 10001, 1, '上海 · CN-East', 2,
 DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 2 HOUR),
 DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 6 HOUR), 8, 42.00, 672.00,
 4, 1, 3, 91001, 'PAY-90001', DATE_ADD(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 2 HOUR),
 DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NULL, 'seed-90001', DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW()),
(90002, 'ORD-90002', 10001, 2, '北京 · CN-North', 1,
 DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 4 HOUR),
 DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 16 HOUR), 12, 26.00, 312.00,
 3, 1, 1, NULL, 'PAY-90002', DATE_ADD(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 1 HOUR),
 NULL, NULL, NULL, 'seed-90002', DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW()),
(90003, 'ORD-90003', 10001, 3, '深圳 · CN-South', 4,
 DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 19 HOUR),
 DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 28 HOUR), 9, 18.00, 648.00,
 1, 1, 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL 15 MINUTE), NULL, NULL, NULL, NULL,
 'seed-90003', DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW()),
(90004, 'ORD-90004', 10001, 3, '深圳 · CN-South', 1,
 DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 1 HOUR),
 DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), INTERVAL 8 HOUR), 9, 18.00, 162.00,
 4, 1, 3, 91002, 'PAY-90004', DATE_ADD(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 1 HOUR),
 DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NULL, 'seed-90004', DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW());

INSERT INTO payment (id, payment_no, order_id, channel, amount, status, third_party_no, paid_at, created_at)
VALUES
(92001, 'PAY-90001', 90001, 'MOCK', 672.00, 1, 'MOCK-SEED-1', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(92002, 'PAY-90002', 90002, 'MOCK', 312.00, 1, 'MOCK-SEED-2', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(92003, 'PAY-90004', 90004, 'MOCK', 162.00, 1, 'MOCK-SEED-4', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR));

INSERT INTO provision_task (id, order_id, status, attempt, last_error, provider_instance_id, created_at, updated_at)
VALUES
(93001, 90002, 2, 0, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW());

INSERT INTO gpu_instance
(id, instance_no, order_id, sku_id, region, status, private_ip, endpoint, ssh_command,
 utilization, started_at, expires_at, terminated_at, created_at, updated_at)
VALUES
(91001, 'inst-h100-7f2a', 90001, 1, '上海 · CN-East', 1, '10.24.8.117',
 'inst-h100-7f2a.nebula.internal', 'nebula ssh inst-h100-7f2a', 78,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW()),
(91002, 'inst-l40s-8c90', 90004, 3, '深圳 · CN-South', 3, '10.31.2.44',
 'inst-l40s-8c90.nebula.internal', 'nebula ssh inst-l40s-8c90', 41,
 DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 8 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW());

INSERT INTO billing_invoice (id, user_id, invoice_no, period_label, amount, status, issued_at)
VALUES
(94001, 10001, 'INV-2026-11-001', '2026 年 11 月', 8420.00, '已结算', '2026-11-21'),
(94002, 10001, 'INV-2026-10-001', '2026 年 10 月', 7492.00, '已结算', '2026-10-31'),
(94003, 10001, 'INV-2026-09-001', '2026 年 9 月', 6128.00, '已结算', '2026-09-30');
