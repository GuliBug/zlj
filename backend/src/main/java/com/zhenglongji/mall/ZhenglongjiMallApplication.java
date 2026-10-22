package com.zhenglongji.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 正隆记槟榔商城后端
 */
@EnableScheduling
@SpringBootApplication
public class ZhenglongjiMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhenglongjiMallApplication.class, args);
    }
}
