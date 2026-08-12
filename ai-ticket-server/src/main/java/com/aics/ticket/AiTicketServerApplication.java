package com.aics.ticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.aics.ticket.**.mapper")
@SpringBootApplication
@EnableScheduling
public class AiTicketServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTicketServerApplication.class, args);
    }
}
