package com.dongzj.configcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableConfigServer
@RestController
@ComponentScan(basePackages = "com.dongzj.configcenter")
@SpringBootApplication
public class ConfigCenterApplication {

    @RequestMapping(value = "checkstatus")
    public String checkstatus() {
        return "success";
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigCenterApplication.class, args);
    }
}
