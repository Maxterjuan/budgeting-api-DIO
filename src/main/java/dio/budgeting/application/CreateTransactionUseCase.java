package dio.budgeting.application;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class CreateTransactionUseCase {

    public record Command(
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            LocalDate date) {}

    private final TransactionRepository repository;
    private final Clock clock;

    public CreateTransactionUseCase(TransactionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Transaction execute(Command command) {
        LocalDate today = LocalDate.now(clock);
        LocalDate date = command.date() != null ? command.date() : today;
        // Melhoria: o modelo pode errar a data ("amanhã", ano errado). Não aceitamos lançamentos futuros.
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("A data da transação não pode estar no futuro (hoje é " + today + ").");
        }
        Transaction transaction = new Transaction(
                TransactionId.generate(),
                command.description(),
                command.amount(),
                command.type(),
                command.category(),
                date);
        return repository.save(transaction);
    }
}
