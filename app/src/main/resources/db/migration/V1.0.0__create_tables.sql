-- Sequenceの作成（Hibernate/Panacheと互換性のある命名）
CREATE SEQUENCE shipper_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE site_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE integration_partner_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE integration_event_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE shipment_order_SEQ START WITH 1 INCREMENT BY 50;

-- 荷主テーブル
CREATE TABLE shipper (
    id BIGINT NOT NULL DEFAULT nextval('shipper_SEQ') PRIMARY KEY,
    shipper_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50)
);

-- 拠点テーブル
CREATE TABLE site (
    id BIGINT NOT NULL DEFAULT nextval('site_SEQ') PRIMARY KEY,
    site_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    shipper_id BIGINT NOT NULL REFERENCES shipper(id) ON DELETE CASCADE
);

-- 連携先パートナーテーブル
CREATE TABLE integration_partner (
    id BIGINT NOT NULL DEFAULT nextval('integration_partner_SEQ') PRIMARY KEY,
    partner_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL
);

-- 連携イベントテーブル
CREATE TABLE integration_event (
    id BIGINT NOT NULL DEFAULT nextval('integration_event_SEQ') PRIMARY KEY,
    correlation_id VARCHAR(100) NOT NULL UNIQUE,
    shipper_code VARCHAR(50) NOT NULL,
    site_code VARCHAR(50),
    channel VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    message TEXT,
    payload TEXT
);

-- 出荷指示テーブル
CREATE TABLE shipment_order (
    id BIGINT NOT NULL DEFAULT nextval('shipment_order_SEQ') PRIMARY KEY,
    order_number VARCHAR(100) NOT NULL UNIQUE,
    shipper_id BIGINT NOT NULL REFERENCES shipper(id) ON DELETE CASCADE,
    origin_site_id BIGINT NOT NULL REFERENCES site(id) ON DELETE RESTRICT,
    destination_site_id BIGINT NOT NULL REFERENCES site(id) ON DELETE RESTRICT,
    item_description TEXT,
    quantity INTEGER,
    status VARCHAR(20),
    correlation_id VARCHAR(100),
    created_at TIMESTAMP
);

-- インデックス作成
CREATE INDEX idx_site_shipper_id ON site(shipper_id);
CREATE INDEX idx_integration_event_shipper_code ON integration_event(shipper_code);
CREATE INDEX idx_integration_event_status ON integration_event(status);
CREATE INDEX idx_integration_event_correlation_id ON integration_event(correlation_id);
CREATE INDEX idx_shipment_order_shipper_id ON shipment_order(shipper_id);
CREATE INDEX idx_shipment_order_correlation_id ON shipment_order(correlation_id);

-- コメント
COMMENT ON TABLE shipper IS '荷主マスタ';
COMMENT ON TABLE site IS '拠点マスタ';
COMMENT ON TABLE integration_partner IS '連携先パートナーマスタ';
COMMENT ON TABLE integration_event IS '連携イベントログ';
COMMENT ON TABLE shipment_order IS '出荷指示データ';
