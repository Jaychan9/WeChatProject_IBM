package com.wechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @author Jaychan
 * @date 2020/6/8
 * @description TODO
 */

@SpringBootApplication
@ComponentScan(basePackages= {"com.wechat", "org.n3r.idworker"})
@MapperScan(basePackages = "com.wechat.mapper")
public class Application {

    @Bean
    public SpringUtil getSpringUtil(){
        return new SpringUtil();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);

    }

}
