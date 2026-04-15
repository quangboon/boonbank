package com.boon.bank.service;

import com.boon.bank.dto.response.BalanceTierStats;
import com.boon.bank.dto.response.LocationStats;

import java.util.List;

public interface StatisticsService {

    List<BalanceTierStats> getBalanceTierStats();

    List<LocationStats> getCustomerByLocation();
}
