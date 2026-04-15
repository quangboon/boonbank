package com.boon.bank.service.fee;

import com.boon.bank.entity.enums.TransactionType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FeeService {
    private final Map<TransactionType, FeeCalculator> calculators;

    public FeeService(List<FeeCalculator> list) {
        this.calculators = list.stream()
                .collect(Collectors.toMap(FeeCalculator::getType, Function.identity()));
    }

    public FeeCalculator resolve(TransactionType type) {
        var calc = calculators.get(type);
        if (calc == null) throw new IllegalStateException("No fee calculator for " + type);
        return calc;
    }
}
