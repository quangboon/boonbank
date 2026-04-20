package com.boon.bank.mapper;

import com.boon.bank.common.money.Money;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.Currency;

@Mapper(componentModel = "spring")
public interface CommonMapper {

    default BigDecimal moneyToBigDecimal(Money money) {
        return money == null ? null : money.amount();
    }

    default String currencyCode(Currency currency) {
        return currency == null ? null : currency.getCurrencyCode();
    }

    default Money toMoney(BigDecimal amount, String currencyCode) {
        if (amount == null || currencyCode == null) return null;
        return Money.of(amount, currencyCode);
    }
}
