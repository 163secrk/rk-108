package com.gangetong.config;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseMigrationConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始检查数据库迁移...");
        try {
            migrateInTransitStockTable();
            createInventoryTransferTables();
            logger.info("数据库迁移检查完成");
        } catch (Exception e) {
            logger.error("数据库迁移失败", e);
        }
    }

    private void migrateInTransitStockTable() {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(in_transit_stock)");
            List<String> columnNames = columns.stream()
                    .map(col -> String.valueOf(col.get("name")))
                    .toList();

            addColumnIfNotExists("in_transit_stock", "type", "VARCHAR(20) DEFAULT 'PURCHASE'", columnNames);
            addColumnIfNotExists("in_transit_stock", "from_warehouse_id", "INTEGER", columnNames);
            addColumnIfNotExists("in_transit_stock", "to_warehouse_id", "INTEGER", columnNames);
            addColumnIfNotExists("in_transit_stock", "transfer_id", "INTEGER", columnNames);
            addColumnIfNotExists("in_transit_stock", "transfer_item_id", "INTEGER", columnNames);
            addColumnIfNotExists("in_transit_stock", "furnace_no", "VARCHAR(50)", columnNames);
            addColumnIfNotExists("in_transit_stock", "cost_unit_price", "DECIMAL(12,2) DEFAULT 0", columnNames);
            addColumnIfNotExists("in_transit_stock", "cost_amount", "DECIMAL(14,2) DEFAULT 0", columnNames);

        } catch (Exception e) {
            logger.error("迁移 in_transit_stock 表失败", e);
        }
    }

    private void createInventoryTransferTables() {
        try {
            jdbcTemplate.execute("""
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
                )
            """);
            logger.info("inventory_transfer 表检查完成");
        } catch (Exception e) {
            logger.error("创建 inventory_transfer 表失败", e);
        }

        try {
            jdbcTemplate.execute("""
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
                )
            """);
            logger.info("inventory_transfer_item 表检查完成");
        } catch (Exception e) {
            logger.error("创建 inventory_transfer_item 表失败", e);
        }

        try {
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_transfer_from_warehouse ON "inventory_transfer" (from_warehouse_id)
            """);
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_transfer_to_warehouse ON "inventory_transfer" (to_warehouse_id)
            """);
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_transfer_status ON "inventory_transfer" (status)
            """);
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_transfer_item_transfer ON "inventory_transfer_item" (transfer_id)
            """);
            logger.info("库存调拨相关索引检查完成");
        } catch (Exception e) {
            logger.error("创建索引失败", e);
        }
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition, List<String> existingColumns) {
        if (!existingColumns.contains(columnName)) {
            String sql = String.format("ALTER TABLE \"%s\" ADD COLUMN %s %s", tableName, columnName, columnDefinition);
            try {
                jdbcTemplate.execute(sql);
                logger.info("添加列成功: {}.{}", tableName, columnName);
            } catch (Exception e) {
                logger.error("添加列失败: {}.{}", tableName, columnName, e);
            }
        }
    }
}
