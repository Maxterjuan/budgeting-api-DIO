package dio.budgeting.application;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GetSummaryUseCase {

    public record Summary(
            LocalDate from,
            LocalDate to,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal balance,
            Map<String, BigDecimal> expensesByCategory) {}

    private final TransactionRepository repository;
    private final Clock clock;

    public GetSummaryUseCase(TransactionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Summary execute(LocalDate from, LocalDate to) {
        Period period = Period.of(from, to, clock);
        List<Transaction> transactions = repository.findBetween(period.from(), period.to());

        BigDecimal income = total(transactions, TransactionType.INCOME);
        BigDecimal expense = total(transactions, TransactionType.EXPENSE);

        Map<String, BigDecimal> byCategory = transactions.stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));

        return new Summary(period.from(), period.to(), income, expense, income.subtract(expense), byCategory);
    }

    private BigDecimal total(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.type() == type)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
