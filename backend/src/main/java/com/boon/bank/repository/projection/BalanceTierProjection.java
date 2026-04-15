package com.boon.bank.repository.projection;

public interface BalanceTierProjection {
    String getTier();
    Long getAcctCount();
    Long getTxnCount();
}
