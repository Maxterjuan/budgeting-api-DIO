package dio.budgeting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Transaction(
        TransactionId id,
        String description,
        BigDecimal amount,
        TransactionType type,
        String category,
        LocalDate date) {

    /** Limites de validação (melhoria): evitam lixo vindo de erros de transcrição ou de alucinação do modelo. */
    public static final int MAX_DESCRIPTION_LENGTH = 100;
    public static final int MAX_CATEGORY_LENGTH = 50;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    public Transaction {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(type, "O tipo (INCOME/EXPENSE) é obrigatório.");
        Objects.requireNonNull(date, "A data é obrigatória.");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "A descrição deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("O valor máximo por transação é " + MAX_AMOUNT.toPlainString() + ".");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O valor deve ter no máximo 2 casas decimais.");
        }
        if (category != null && category.trim().length() > MAX_CATEGORY_LENGTH) {
            throw new IllegalArgumentException(
                    "A categoria deve ter no máximo " + MAX_CATEGORY_LENGTH + " caracteres.");
        }
        description = description.trim();
        category = (category == null || category.isBlank()) ? "outros" : category.trim().toLowerCase();
    }
}
