package dio.budgeting.domain;

import java.time.LocalDate;
import java.util.List;

/** Contrato do repositório: o domínio não sabe se por trás existe JPA, Mongo ou memória. */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findBetween(LocalDate from, LocalDate to);
}
