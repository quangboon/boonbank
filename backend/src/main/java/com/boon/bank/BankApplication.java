package com.boon.bank;

import com.boon.bank.config.properties.BalanceTierProperties;
import com.boon.bank.config.properties.GeoAnomalyProperties;
import com.boon.bank.config.properties.ReportProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BalanceTierProperties.class, ReportProperties.class, GeoAnomalyProperties.class})
public class BankApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}

}
