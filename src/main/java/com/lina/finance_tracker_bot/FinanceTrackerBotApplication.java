package com.lina.finance_tracker_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FinanceTrackerBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceTrackerBotApplication.class, args);
	}

}
