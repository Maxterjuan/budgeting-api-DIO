package dio.budgeting.application;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Repositório falso: os testes de caso de uso não precisam de banco nem de OpenAI. */
class InMemoryTransactionRepository implements TransactionRepository {

    private final List<Transaction> data = new ArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        data.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findBetween(LocalDate from, LocalDate to) {
        return data.stream()
                .filter(t -> !t.date().isBefore(from) && !t.date().isAfter(to))
                .toList();
    }
}
