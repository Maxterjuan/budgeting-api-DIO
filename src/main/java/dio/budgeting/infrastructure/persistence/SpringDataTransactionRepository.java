package dio.budgeting.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    List<TransactionEntity> findByDateBetweenOrderByDateAscIdAsc(LocalDate from, LocalDate to);
}
