package dio.budgeting.domain;

import java.util.UUID;

/** Identificador fortemente tipado: evita confundir um id de transação com qualquer outro UUID. */
public record TransactionId(UUID value) {

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }
}
