-- Railway demo schema and seed data. Production deployments should use Flyway/Liquibase migrations.
-- The `railway` database must already exist; Spring Boot connects to it via spring.datasource.url.
CREATE TABLE IF NOT EXISTS station
(
    id           BIGINT PRIMARY KEY,
    station_code VARCHAR(20) NOT NULL UNIQUE,
    station_name VARCHAR(64) NOT NULL,
    city_name    VARCHAR(64),
    pinyin       VARCHAR(128),
    status       TINYINT     NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS train
(
    id               BIGINT PRIMARY KEY,
    train_no         VARCHAR(32) NOT NULL UNIQUE,
    train_type       VARCHAR(32),
    start_station_id BIGINT      NOT NULL,
    end_station_id   BIGINT      NOT NULL,
    start_time       TIME,
    end_time         TIME,
    status           TINYINT     NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS train_stop
(
    id           BIGINT PRIMARY KEY,
    train_id     BIGINT NOT NULL,
    station_id   BIGINT NOT NULL,
    stop_seq     INT    NOT NULL,
    arrive_time  TIME,
    depart_time  TIME,
    stop_minutes INT DEFAULT 0,
    UNIQUE KEY uk_train_seq (train_id, stop_seq),
    UNIQUE KEY uk_train_station (train_id, station_id)
);
CREATE TABLE IF NOT EXISTS seat_type
(
    id   BIGINT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(32) NOT NULL
);
CREATE TABLE IF NOT EXISTS train_seat
(
    id           BIGINT PRIMARY KEY,
    train_id     BIGINT      NOT NULL,
    seat_type_id BIGINT      NOT NULL,
    carriage_no  INT         NOT NULL,
    seat_no      VARCHAR(16) NOT NULL,
    seat_row     VARCHAR(8),
    seat_col     VARCHAR(8),
    UNIQUE KEY uk_train_seat (train_id, carriage_no, seat_no)
);
CREATE TABLE IF NOT EXISTS train_run
(
    id            BIGINT PRIMARY KEY,
    train_id      BIGINT  NOT NULL,
    run_date      DATE    NOT NULL,
    sale_start_at DATETIME,
    status        TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_train_date (train_id, run_date)
);
CREATE TABLE IF NOT EXISTS seat_inventory
(
    id           BIGINT PRIMARY KEY,
    train_run_id BIGINT NOT NULL,
    seat_type_id BIGINT NOT NULL,
    total_count  INT    NOT NULL,
    locked_count INT    NOT NULL DEFAULT 0,
    sold_count   INT    NOT NULL DEFAULT 0,
    version      INT    NOT NULL DEFAULT 0,
    UNIQUE KEY uk_run_seat_type (train_run_id, seat_type_id)
);
CREATE TABLE IF NOT EXISTS seat_segment_inventory
(
    id           BIGINT PRIMARY KEY,
    train_run_id BIGINT NOT NULL,
    seat_type_id BIGINT NOT NULL,
    from_seq     INT    NOT NULL,
    to_seq       INT    NOT NULL,
    total_count  INT    NOT NULL,
    sold_count   INT    NOT NULL DEFAULT 0,
    locked_count INT    NOT NULL DEFAULT 0,
    UNIQUE KEY uk_segment (train_run_id, seat_type_id, from_seq, to_seq)
);
CREATE TABLE IF NOT EXISTS user_account
(
    id            BIGINT PRIMARY KEY,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    real_name     VARCHAR(64),
    id_card_no    VARCHAR(128),
    id_card_type  TINYINT               DEFAULT 1,
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL
);
CREATE TABLE IF NOT EXISTS passenger
(
    id             BIGINT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    name           VARCHAR(64)  NOT NULL,
    id_card_no     VARCHAR(128) NOT NULL,
    passenger_type TINYINT      NOT NULL DEFAULT 1,
    phone          VARCHAR(20),
    status         TINYINT      NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    UNIQUE KEY uk_user_card (user_id, id_card_no)
);
CREATE TABLE IF NOT EXISTS orders
(
    id              BIGINT PRIMARY KEY,
    order_no        VARCHAR(64)    NOT NULL UNIQUE,
    user_id         BIGINT         NOT NULL,
    train_run_id    BIGINT         NOT NULL,
    from_station_id BIGINT         NOT NULL,
    to_station_id   BIGINT         NOT NULL,
    order_status    TINYINT        NOT NULL,
    total_amount    DECIMAL(12, 2) NOT NULL,
    expire_at       DATETIME,
    idempotency_key VARCHAR(128)   NOT NULL,
    created_at      DATETIME       NOT NULL,
    updated_at      DATETIME       NOT NULL,
    UNIQUE KEY uk_user_idempotency (user_id, idempotency_key)
);
CREATE TABLE IF NOT EXISTS order_passenger
(
    id            BIGINT PRIMARY KEY,
    order_id      BIGINT         NOT NULL,
    passenger_id  BIGINT         NOT NULL,
    seat_type_id  BIGINT         NOT NULL,
    seat_id       BIGINT,
    ticket_price  DECIMAL(12, 2) NOT NULL,
    ticket_status TINYINT        NOT NULL DEFAULT 1,
    UNIQUE KEY uk_order_passenger (order_id, passenger_id)
);
CREATE TABLE IF NOT EXISTS inventory_lock
(
    id           BIGINT PRIMARY KEY,
    lock_no      VARCHAR(64) NOT NULL UNIQUE,
    order_id     BIGINT      NOT NULL,
    train_run_id BIGINT      NOT NULL,
    seat_id      BIGINT,
    from_seq     INT         NOT NULL,
    to_seq       INT         NOT NULL,
    expire_at    DATETIME    NOT NULL,
    status       TINYINT     NOT NULL DEFAULT 1,
    created_at   DATETIME    NOT NULL
);
CREATE TABLE IF NOT EXISTS ticket
(
    id                 BIGINT PRIMARY KEY,
    ticket_no          VARCHAR(64) NOT NULL UNIQUE,
    order_id           BIGINT      NOT NULL,
    order_passenger_id BIGINT      NOT NULL,
    seat_id            BIGINT      NOT NULL,
    ticket_status      TINYINT     NOT NULL,
    issued_at          DATETIME,
    refunded_at        DATETIME
);
CREATE TABLE IF NOT EXISTS payment
(
    id             BIGINT PRIMARY KEY,
    order_id       BIGINT         NOT NULL,
    payment_no     VARCHAR(64)    NOT NULL UNIQUE,
    channel        VARCHAR(32)    NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    payment_status TINYINT        NOT NULL,
    third_party_no VARCHAR(128),
    paid_at        DATETIME,
    UNIQUE KEY uk_payment_order (order_id)
);
CREATE TABLE IF NOT EXISTS outbox_event
(
    id             BIGINT PRIMARY KEY,
    event_id       VARCHAR(64)  NOT NULL UNIQUE,
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    payload        JSON         NOT NULL,
    status         TINYINT      NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL,
    published_at   DATETIME
);

INSERT IGNORE INTO station (id, station_code, station_name, city_name, pinyin)
VALUES (1, 'BJP', '北京', '北京', 'beijing'),
       (2, 'TJP', '天津', '天津', 'tianjin'),
       (3, 'SHH', '上海', '上海', 'shanghai');
INSERT IGNORE INTO seat_type (id, code, name)
VALUES (1, 'SECOND_CLASS', '二等座'),
       (2, 'FIRST_CLASS', '一等座'),
       (3, 'BUSINESS', '商务座');
INSERT IGNORE INTO train (id, train_no, train_type, start_station_id, end_station_id, start_time, end_time)
VALUES (1001, 'G101', '高速动车', 1, 3, '06:30:00', '11:36:00');
INSERT IGNORE INTO train_stop (id, train_id, station_id, stop_seq, arrive_time, depart_time)
VALUES (11001, 1001, 1, 1, NULL, '06:30:00'),
       (11002, 1001, 2, 2, '07:05:00', '07:07:00'),
       (11003, 1001, 3, 3, '11:36:00', NULL);
INSERT IGNORE INTO train_run (id, train_id, run_date, sale_start_at)
VALUES (12001, 1001, '2026-09-10', '2026-09-01 08:00:00');
INSERT IGNORE INTO seat_inventory (id, train_run_id, seat_type_id, total_count)
VALUES (13001, 12001, 1, 600),
       (13002, 12001, 2, 100),
       (13003, 12001, 3, 16);

INSERT IGNORE INTO train (id, train_no, train_type, start_station_id, end_station_id, start_time, end_time)
VALUES (1002, 'G102', '高速动车', 3, 1, '12:10:00', '17:18:00'),
       (1003, 'G7', '高速动车', 1, 3, '10:00:00', '14:28:00');
INSERT IGNORE INTO train_stop (id, train_id, station_id, stop_seq, arrive_time, depart_time)
VALUES (11011, 1002, 3, 1, NULL, '12:10:00'),
       (11012, 1002, 2, 2, '16:28:00', '16:30:00'),
       (11013, 1002, 1, 3, '17:18:00', NULL),
       (11021, 1003, 1, 1, NULL, '10:00:00'),
       (11022, 1003, 3, 2, '14:28:00', NULL);

INSERT IGNORE INTO train_run (id, train_id, run_date, sale_start_at, status)
WITH RECURSIVE dates AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM dates WHERE n < 119
)
SELECT 12000 + n + 1, 1001, DATE_ADD('2026-09-10', INTERVAL n DAY), '2026-09-01 08:00:00', 1 FROM dates
UNION ALL
SELECT 22000 + n + 1, 1002, DATE_ADD('2026-09-10', INTERVAL n DAY), '2026-09-01 08:00:00', 1 FROM dates
UNION ALL
SELECT 32000 + n + 1, 1003, DATE_ADD('2026-09-10', INTERVAL n DAY), '2026-09-01 08:00:00', 1 FROM dates;

INSERT IGNORE INTO seat_inventory (id, train_run_id, seat_type_id, total_count, locked_count, sold_count, version)
WITH RECURSIVE dates AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM dates WHERE n < 119
)
SELECT 13000 + n * 10 + st.id, 12000 + n + 1, st.id,
       CASE st.id WHEN 1 THEN 600 WHEN 2 THEN 100 ELSE 16 END, 0, 0, 0
FROM dates CROSS JOIN seat_type st
UNION ALL
SELECT 23000 + n * 10 + st.id, 22000 + n + 1, st.id,
       CASE st.id WHEN 1 THEN 560 WHEN 2 THEN 90 ELSE 12 END, 0, 0, 0
FROM dates CROSS JOIN seat_type st
UNION ALL
SELECT 33000 + n * 10 + st.id, 32000 + n + 1, st.id,
       CASE st.id WHEN 1 THEN 480 WHEN 2 THEN 80 ELSE 10 END, 0, 0, 0
FROM dates CROSS JOIN seat_type st;

CREATE TABLE IF NOT EXISTS fare (
  id BIGINT PRIMARY KEY,
  train_id BIGINT NOT NULL,
  seat_type_id BIGINT NOT NULL,
  from_station_id BIGINT NOT NULL,
  to_station_id BIGINT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  UNIQUE KEY uk_fare (train_id, seat_type_id, from_station_id, to_station_id)
);

-- G101 / G102 共用：北京-天津 / 天津-上海 / 北京-上海 各席别票价
INSERT IGNORE INTO fare (id, train_id, seat_type_id, from_station_id, to_station_id, price) VALUES
  (14001, 1001, 1, 1, 2, 54.50), (14002, 1001, 2, 1, 2, 87.00),  (14003, 1001, 3, 1, 2, 174.00),
  (14004, 1001, 1, 2, 3, 508.00), (14005, 1001, 2, 2, 3, 858.00), (14006, 1001, 3, 2, 3, 1700.00),
  (14007, 1001, 1, 1, 3, 553.00), (14008, 1001, 2, 1, 3, 933.00), (14009, 1001, 3, 1, 3, 1866.00),
  (14010, 1002, 1, 3, 2, 508.00), (14011, 1002, 2, 3, 2, 858.00), (14012, 1002, 3, 3, 2, 1700.00),
  (14013, 1002, 1, 3, 1, 553.00), (14014, 1002, 2, 3, 1, 933.00), (14015, 1002, 3, 3, 1, 1866.00),
  (14016, 1002, 1, 2, 1, 54.50), (14017, 1002, 2, 2, 1, 87.00),  (14018, 1002, 3, 2, 1, 174.00),
  (14019, 1003, 1, 1, 3, 553.00), (14020, 1003, 2, 1, 3, 933.00), (14021, 1003, 3, 1, 3, 1866.00);
