package com.boylu;

import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableFileStorage
@EnableCaching
@MapperScan("com.boylu.mapper")
@Slf4j
public class NeatAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(NeatAdminApplication.class, args);
        log.info("系统启动成功！");
    }
}
