package com.bn.berrynovel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class BerrynovelApplication {

	public static void main(String[] args) {
		ApplicationContext beans = SpringApplication.run(BerrynovelApplication.class, args);
		for (String s : beans.getBeanDefinitionNames()) {
			System.out.println(s);
		}
	}

}
