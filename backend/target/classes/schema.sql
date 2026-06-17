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
