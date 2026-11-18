package dio.budgeting.application;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListTransactionsUseCase {

    private final TransactionRepository repository;
    private final Clock clock;

    public ListTransactionsUseCase(TransactionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<Transaction> execute(LocalDate from, LocalDate to) {
        Period period = Period.of(from, to, clock);
        return repository.findBetween(period.from(), period.to());
    }
}
