package com.boon.bank.dto.response;

public record BalanceTierStats(String tier, long accountCount, long transactionCount) {}
