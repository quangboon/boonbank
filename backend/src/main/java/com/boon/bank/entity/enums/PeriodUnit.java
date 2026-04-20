package com.boon.bank.entity.enums;

public enum PeriodUnit {
    WEEK("week"),
    QUARTER("quarter"),
    YEAR("year");

    private final String sqlLiteral;

    PeriodUnit(String sqlLiteral) {
        this.sqlLiteral = sqlLiteral;
    }

    public String sqlLiteral() {
        return sqlLiteral;
    }
}
