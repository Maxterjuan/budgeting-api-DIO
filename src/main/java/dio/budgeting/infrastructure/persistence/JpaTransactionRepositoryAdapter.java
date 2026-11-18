package dio.budgeting.infrastructure.persistence;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Adapter: traduz o contrato do domínio para o Spring Data JPA (e converte entidade <-> domínio). */
@Repository
class JpaTransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository jpa;

    JpaTransactionRepositoryAdapter(SpringDataTransactionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transaction save(Transaction t) {
        TransactionEntity saved = jpa.save(new TransactionEntity(
                t.id().value(), t.description(), t.amount(), t.type(), t.category(), t.date()));
        return toDomain(saved);
    }

    @Override
    public List<Transaction> findBetween(LocalDate from, LocalDate to) {
        return jpa.findByDateBetweenOrderByDateAscIdAsc(from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    private Transaction toDomain(TransactionEntity e) {
        return new Transaction(new TransactionId(e.getId()), e.getDescription(), e.getAmount(),
                e.getType(), e.getCategory(), e.getDate());
    }
}
