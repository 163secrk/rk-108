package com.gangetong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 港融通系统启动类
 */
@SpringBootApplication
@MapperScan("com.gangetong.mapper")
public class GangetongApplication {

    public static void main(String[] args) {
        SpringApplication.run(GangetongApplication.class, args);
        System.out.println("========================================");
        System.out.println("  港融通钢材贸易管理系统启动成功！");
        System.out.println("  服务端口: 8108");
        System.out.println("  API文档: http://localhost:8108");
        System.out.println("========================================");
    }
}
