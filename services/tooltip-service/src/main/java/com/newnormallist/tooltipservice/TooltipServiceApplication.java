package com.newnormallist.tooltipservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@SpringBootApplication
public class TooltipServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TooltipServiceApplication.class, args);
	}

}
