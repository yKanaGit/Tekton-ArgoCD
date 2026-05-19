-- サンプル荷主データ
INSERT INTO shipper (shipper_code, name, contact_name, contact_email, contact_phone) VALUES
('SHP001', '東京物流株式会社', '山田太郎', 'yamada@tokyo-logistics.example.com', '03-1234-5678'),
('SHP002', '大阪運輸株式会社', '田中花子', 'tanaka@osaka-trans.example.com', '06-9876-5432'),
('SHP003', '名古屋流通センター', '佐藤次郎', 'sato@nagoya-dist.example.com', '052-1111-2222');

-- サンプル拠点データ
INSERT INTO site (site_code, name, address, shipper_id) VALUES
('SITE_TKY01', '東京本社倉庫', '東京都江東区豊洲1-1-1', (SELECT id FROM shipper WHERE shipper_code = 'SHP001')),
('SITE_TKY02', '東京北部センター', '東京都北区赤羽2-2-2', (SELECT id FROM shipper WHERE shipper_code = 'SHP001')),
('SITE_OSK01', '大阪中央倉庫', '大阪府大阪市西区西本町3-3-3', (SELECT id FROM shipper WHERE shipper_code = 'SHP002')),
('SITE_OSK02', '大阪南港DC', '大阪府大阪市住之江区南港北4-4-4', (SELECT id FROM shipper WHERE shipper_code = 'SHP002')),
('SITE_NGY01', '名古屋物流センター', '愛知県名古屋市港区入船5-5-5', (SELECT id FROM shipper WHERE shipper_code = 'SHP003'));

-- サンプル連携先パートナー
INSERT INTO integration_partner (partner_code, name, type) VALUES
('PARTNER_API01', 'クラウドEDIシステムA', 'API'),
('PARTNER_EDI01', 'JX手順EDIゲートウェイ', 'EDI'),
('PARTNER_FILE01', 'FTPファイル転送サービス', 'FILE');
