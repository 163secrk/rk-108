-- 用户表
CREATE TABLE IF NOT EXISTS "user" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    avatar VARCHAR(500)
);

-- 插入初始用户数据（密码均为 123456 的 BCrypt 加密值）
INSERT OR IGNORE INTO "user" (username, password, nickname, role, avatar) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'),
('purchaser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '采购员张三', 'PURCHASER', 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png'),
('saler', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '销售员李四', 'SALER', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'),
('finance', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '财务王五', 'FINANCE', 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png');

-- ============================================
-- 基础资料模块：材质表
-- ============================================
CREATE TABLE IF NOT EXISTS "material" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    standard VARCHAR(100),
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

INSERT OR IGNORE INTO "material" (id, name, description, standard, create_time) VALUES
(1, 'Q235B', '普通碳素结构钢', 'GB/T 700', '2024-01-15'),
(2, 'Q355D', '低合金高强度结构钢', 'GB/T 1591', '2024-01-16'),
(3, 'Q355B', '低合金高强度结构钢', 'GB/T 1591', '2024-02-01'),
(4, '20#', '优质碳素结构钢', 'GB/T 8163', '2024-02-10'),
(5, '45#', '优质碳素结构钢', 'GB/T 699', '2024-03-05');

-- ============================================
-- 基础资料：钢材规格表
-- ============================================
CREATE TABLE IF NOT EXISTS "steel_spec" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    material_id INTEGER NOT NULL,
    diameter VARCHAR(20) NOT NULL,
    wall_thickness VARCHAR(20) NOT NULL,
    length VARCHAR(20),
    weight_per_meter DECIMAL(10,3) NOT NULL DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

INSERT OR IGNORE INTO "steel_spec" (id, material_id, diameter, wall_thickness, length, weight_per_meter, create_time) VALUES
(1, 1, '57', '3.5', '6000', 4.620, '2024-01-15'),
(2, 1, '76', '4.0', '6000', 7.100, '2024-01-16'),
(3, 1, '89', '4.5', '6000', 9.380, '2024-01-20'),
(4, 2, '108', '5.0', '9000', 12.700, '2024-02-01'),
(5, 2, '133', '6.0', '9000', 18.790, '2024-02-10'),
(6, 3, '159', '6.0', '6000', 22.640, '2024-02-15'),
(7, 3, '219', '8.0', '12000', 41.630, '2024-03-01'),
(8, 4, '32', '3.0', '6000', 2.150, '2024-03-05'),
(9, 4, '48', '3.5', '6000', 3.840, '2024-03-10'),
(10, 5, '102', '5.0', '6000', 11.960, '2024-03-15');

-- ============================================
-- 基础资料：商品档案表
-- ============================================
CREATE TABLE IF NOT EXISTS "product" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_code VARCHAR(50) NOT NULL UNIQUE,
    product_name VARCHAR(200) NOT NULL,
    material_id INTEGER NOT NULL,
    spec_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(10) DEFAULT '支',
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

INSERT OR IGNORE INTO "product" (id, product_code, product_name, material_id, spec_id, quantity, unit, unit_price, create_time) VALUES
(1, 'P202403001', '无缝钢管 Q235B φ57×3.5', 1, 1, 100, '支', 4500.00, '2024-03-01'),
(2, 'P202403002', '无缝钢管 Q235B φ76×4.0', 1, 2, 80, '支', 4600.00, '2024-03-02'),
(3, 'P202403003', '无缝钢管 Q355D φ108×5.0', 2, 4, 50, '支', 5200.00, '2024-03-05'),
(4, 'P202403004', '无缝钢管 Q355B φ219×8.0', 3, 7, 30, '支', 4800.00, '2024-03-10'),
(5, 'P202403005', '无缝钢管 20# φ48×3.5', 4, 9, 200, '支', 5500.00, '2024-03-12');

-- ============================================
-- 基础资料：仓库及库位表（支持多级：仓库→分区→货位）
-- ============================================
CREATE TABLE IF NOT EXISTS "warehouse" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    parent_id INTEGER DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    address VARCHAR(300),
    remark VARCHAR(500),
    status INTEGER NOT NULL DEFAULT 1,
    sort_no INTEGER DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

INSERT OR IGNORE INTO "warehouse" (id, name, code, parent_id, level, address, remark, status, sort_no, create_time) VALUES
(1, 'A仓', 'WH-A', 0, 1, '厂区东侧', '主力仓库', 1, 1, '2024-01-15'),
(2, 'B仓', 'WH-B', 0, 1, '厂区西侧', '备用仓库', 1, 2, '2024-01-16'),
(3, 'A区', 'WH-A-ZA', 1, 2, NULL, 'A仓A区', 1, 1, '2024-01-15'),
(4, 'B区', 'WH-A-ZB', 1, 2, NULL, 'A仓B区', 1, 2, '2024-01-15'),
(5, 'A区', 'WH-B-ZA', 2, 2, NULL, 'B仓A区', 1, 1, '2024-01-16'),
(6, 'A-01号位', 'WH-A-ZA-01', 3, 3, NULL, NULL, 1, 1, '2024-01-15'),
(7, 'A-02号位', 'WH-A-ZA-02', 3, 3, NULL, NULL, 1, 2, '2024-01-15'),
(8, 'B-01号位', 'WH-A-ZB-01', 4, 3, NULL, NULL, 1, 1, '2024-01-15'),
(9, 'B-02号位', 'WH-A-ZB-02', 4, 3, NULL, NULL, 1, 2, '2024-01-15'),
(10, 'A-01号位', 'WH-B-ZA-01', 5, 3, NULL, NULL, 1, 1, '2024-01-16');

-- ============================================
-- 基础资料：往来单位表（供应商/客户统一管理）
-- ============================================
CREATE TABLE IF NOT EXISTS "partner" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    contact_person VARCHAR(50),
    phone VARCHAR(30),
    credit_limit DECIMAL(14,2) NOT NULL DEFAULT 0,
    payment_days INTEGER NOT NULL DEFAULT 0,
    address VARCHAR(300),
    remark VARCHAR(500),
    status INTEGER NOT NULL DEFAULT 1,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

INSERT OR IGNORE INTO "partner" (id, name, code, type, contact_person, phone, credit_limit, payment_days, address, remark, status, create_time) VALUES
(1, '河北钢铁集团', 'SUP-001', 'SUPPLIER', '张经理', '13800001111', 500000.00, 30, '河北省石家庄市', '核心供应商', 1, '2024-01-10'),
(2, '首钢集团', 'SUP-002', 'SUPPLIER', '李总', '13800002222', 800000.00, 45, '北京市石景山区', '长期合作供应商', 1, '2024-01-12'),
(3, '中建三局', 'CUS-001', 'CUSTOMER', '王工', '13900001111', 300000.00, 30, '湖北省武汉市', '大客户', 1, '2024-02-01'),
(4, '中铁建设', 'CUS-002', 'CUSTOMER', '赵经理', '13900002222', 500000.00, 60, '北京市海淀区', '基建项目客户', 1, '2024-02-05'),
(5, '宝钢股份', 'SUP-003', 'SUPPLIER', '刘主任', '13800003333', 1000000.00, 30, '上海市宝山区', '高端钢材供应商', 1, '2024-02-10');

-- ============================================
-- 采购管理：采购合同主表
-- ============================================
CREATE TABLE IF NOT EXISTS "purchase_contract" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contract_no VARCHAR(50) NOT NULL UNIQUE,
    supplier_id INTEGER NOT NULL,
    sign_date VARCHAR(20) NOT NULL,
    delivery_date VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_by INTEGER,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    update_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- 采购管理：采购合同明细表
-- ============================================
CREATE TABLE IF NOT EXISTS "purchase_contract_item" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contract_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    material_id INTEGER NOT NULL,
    spec_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    in_stock_quantity INTEGER NOT NULL DEFAULT 0,
    sort_no INTEGER DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- 库存管理：在途库存表
-- ============================================
CREATE TABLE IF NOT EXISTS "in_transit_stock" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    material_id INTEGER NOT NULL,
    spec_id INTEGER NOT NULL,
    contract_id INTEGER NOT NULL,
    contract_item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- 采购管理：入库单主表
-- ============================================
CREATE TABLE IF NOT EXISTS "stock_in_order" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    contract_id INTEGER NOT NULL,
    supplier_id INTEGER,
    warehouse_id INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_quantity INTEGER NOT NULL DEFAULT 0,
    total_theoretical_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    total_actual_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_by INTEGER,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    update_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    audit_by INTEGER,
    audit_time VARCHAR(20)
);

-- ============================================
-- 采购管理：入库单明细表（强制录入炉批号用于质量追溯）
-- ============================================
CREATE TABLE IF NOT EXISTS "stock_in_item" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    contract_item_id INTEGER,
    product_id INTEGER NOT NULL,
    material_id INTEGER,
    spec_id INTEGER,
    furnace_no VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    theoretical_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    actual_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    deviation_rate DECIMAL(8,4) NOT NULL DEFAULT 0,
    amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- 库存管理：库存表（按炉批号分批次记录，记录入库成本单价）
-- ============================================
CREATE TABLE IF NOT EXISTS "stock" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    material_id INTEGER,
    spec_id INTEGER,
    warehouse_id INTEGER NOT NULL,
    furnace_no VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    cost_unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    cost_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    update_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_stock_product_warehouse_furnace ON "stock" (product_id, warehouse_id, furnace_no);

-- ============================================
-- 销售管理：销售订单主表
-- ============================================
CREATE TABLE IF NOT EXISTS "sales_order" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    customer_id INTEGER NOT NULL,
    order_date VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    is_over_credit INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_by INTEGER,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    update_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- 销售管理：销售订单明细表（每个明细可关联多个库存批次）
-- ============================================
CREATE TABLE IF NOT EXISTS "sales_order_item" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    material_id INTEGER NOT NULL,
    spec_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    out_stock_quantity INTEGER NOT NULL DEFAULT 0,
    sort_no INTEGER DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- 销售管理：库存锁定表（销售订单确认后锁定库存，不扣实际库存）
-- ============================================
CREATE TABLE IF NOT EXISTS "inventory_lock" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    order_item_id INTEGER NOT NULL,
    stock_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    furnace_no VARCHAR(50) NOT NULL,
    lock_quantity INTEGER NOT NULL DEFAULT 0,
    lock_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    release_time VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_inventory_lock_order ON "inventory_lock" (order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_lock_stock ON "inventory_lock" (stock_id, status);
CREATE INDEX IF NOT EXISTS idx_inventory_lock_product ON "inventory_lock" (product_id, warehouse_id, furnace_no, status);

-- 销售出库单
CREATE TABLE IF NOT EXISTS sales_outbound (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    outbound_no VARCHAR(50) NOT NULL,
    order_id INTEGER NOT NULL,
    customer_id INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_quantity INTEGER,
    total_weight DECIMAL(12,3),
    plate_no VARCHAR(50),
    driver_name VARCHAR(100),
    driver_phone VARCHAR(20),
    remark VARCHAR(500),
    create_by INTEGER,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    update_time VARCHAR(20),
    audit_by INTEGER,
    audit_time VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_outbound_order ON sales_outbound (order_id);
CREATE INDEX IF NOT EXISTS idx_outbound_status ON sales_outbound (status);

-- 销售出库明细
CREATE TABLE IF NOT EXISTS sales_outbound_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    outbound_id INTEGER NOT NULL,
    order_item_id INTEGER,
    lock_id INTEGER,
    product_id INTEGER,
    material_id INTEGER,
    spec_id INTEGER,
    stock_id INTEGER,
    warehouse_id INTEGER,
    furnace_no VARCHAR(100),
    plan_quantity INTEGER,
    actual_quantity INTEGER,
    plan_weight DECIMAL(12,3),
    actual_weight DECIMAL(12,3),
    unit_price DECIMAL(12,3),
    amount DECIMAL(12,3),
    sort_no INTEGER,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_outbound_item ON sales_outbound_item (outbound_id);

-- ============================================
-- 库存管理：库存调拨单主表
-- ============================================
CREATE TABLE IF NOT EXISTS "inventory_transfer" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transfer_no VARCHAR(50) NOT NULL UNIQUE,
    from_warehouse_id INTEGER NOT NULL,
    to_warehouse_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_quantity INTEGER NOT NULL DEFAULT 0,
    total_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    received_quantity INTEGER NOT NULL DEFAULT 0,
    received_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_by INTEGER,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    update_time VARCHAR(20) DEFAULT (datetime('now', 'localtime')),
    audit_by INTEGER,
    audit_time VARCHAR(20),
    receive_by INTEGER,
    receive_time VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_transfer_from_warehouse ON "inventory_transfer" (from_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_transfer_to_warehouse ON "inventory_transfer" (to_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_transfer_status ON "inventory_transfer" (status);

-- ============================================
-- 库存管理：库存调拨单明细表
-- ============================================
CREATE TABLE IF NOT EXISTS "inventory_transfer_item" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transfer_id INTEGER NOT NULL,
    stock_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    material_id INTEGER,
    spec_id INTEGER,
    furnace_no VARCHAR(50) NOT NULL,
    plan_quantity INTEGER NOT NULL DEFAULT 0,
    plan_weight DECIMAL(12,3) NOT NULL DEFAULT 0,
    actual_quantity INTEGER DEFAULT 0,
    actual_weight DECIMAL(12,3) DEFAULT 0,
    diff_quantity INTEGER DEFAULT 0,
    diff_weight DECIMAL(12,3) DEFAULT 0,
    cost_unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    cost_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    sort_no INTEGER DEFAULT 0,
    create_time VARCHAR(20) DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_transfer_item_transfer ON "inventory_transfer_item" (transfer_id);

-- ============================================
-- 在途库存表升级说明
-- ============================================
-- 注意：在途库存表的新增字段由 DatabaseMigrationConfig 在应用启动时自动添加
-- 包括：type, from_warehouse_id, to_warehouse_id, transfer_id, transfer_item_id,
--       furnace_no, cost_unit_price, cost_amount
