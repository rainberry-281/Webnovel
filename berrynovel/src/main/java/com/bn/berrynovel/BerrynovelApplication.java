package com.bn.berrynovel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
// @SpringBootApplication(exclude =
// org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
public class BerrynovelApplication {

	public static void main(String[] args) {
		ApplicationContext beans = SpringApplication.run(BerrynovelApplication.class, args);
		for (String s : beans.getBeanDefinitionNames()) {
			System.out.println(s);
		}
	}

}
