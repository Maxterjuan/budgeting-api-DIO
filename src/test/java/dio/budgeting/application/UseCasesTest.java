package dio.budgeting.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dio.budgeting.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UseCasesTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("UTC"));

    private CreateTransactionUseCase create;
    private ListTransactionsUseCase list;
    private GetSummaryUseCase summary;

    @BeforeEach
    void setUp() {
        var repository = new InMemoryTransactionRepository();
        create = new CreateTransactionUseCase(repository, CLOCK);
        list = new ListTransactionsUseCase(repository, CLOCK);
        summary = new GetSummaryUseCase(repository, CLOCK);
    }

    private void add(String description, String amount, TransactionType type, String category, LocalDate date) {
        create.execute(new CreateTransactionUseCase.Command(description, new BigDecimal(amount), type, category, date));
    }

    @Test
    void usesTodayWhenDateIsOmitted() {
        add("almoço", "30.00", TransactionType.EXPENSE, "alimentação", null);

        var result = list.execute(null, null);

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 11, 17), result.get(0).date());
    }

    @Test
    void summaryCalculatesTotalsAndGroupsExpensesByCategory() {
        add("salário", "5000.00", TransactionType.INCOME, "salário", LocalDate.of(2026, 11, 5));
        add("mercado", "400.00", TransactionType.EXPENSE, "alimentação", LocalDate.of(2026, 11, 6));
        add("restaurante", "100.00", TransactionType.EXPENSE, "Alimentação", LocalDate.of(2026, 11, 10));
        add("ônibus", "50.00", TransactionType.EXPENSE, "transporte", LocalDate.of(2026, 11, 11));

        var result = summary.execute(null, null);

        assertEquals(new BigDecimal("5000.00"), result.totalIncome());
        assertEquals(new BigDecimal("550.00"), result.totalExpense());
        assertEquals(new BigDecimal("4450.00"), result.balance());
        assertEquals(new BigDecimal("500.00"), result.expensesByCategory().get("alimentação"));
    }

    @Test
    void summaryIgnoresTransactionsOutsideThePeriod() {
        add("antigo", "999.00", TransactionType.EXPENSE, "outros", LocalDate.of(2026, 10, 1));

        var result = summary.execute(null, null);

        assertEquals(BigDecimal.ZERO, result.totalExpense());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> add("erro", "0", TransactionType.EXPENSE, "outros", null));
    }

    // ---- Melhoria: validações antes de salvar ----

    @Test
    void rejectsFutureDate() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> add("viagem", "100.00", TransactionType.EXPENSE, "lazer", LocalDate.of(2026, 11, 18)));
        assertTrue(ex.getMessage().contains("futuro"));
    }

    @Test
    void acceptsTodayAndPastDates() {
        add("hoje", "10.00", TransactionType.EXPENSE, "outros", LocalDate.of(2026, 11, 17));
        add("ontem", "10.00", TransactionType.EXPENSE, "outros", LocalDate.of(2026, 11, 16));
        assertEquals(2, list.execute(null, null).size());
    }

    @Test
    void rejectsAmountAboveLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> add("fortuna", "10000000.01", TransactionType.INCOME, "outros", null));
    }

    @Test
    void acceptsAmountExactlyAtLimit() {
        add("limite", "10000000", TransactionType.INCOME, "outros", null);
        assertEquals(1, list.execute(null, null).size());
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThrows(IllegalArgumentException.class,
                () -> add("quebrado", "10.999", TransactionType.EXPENSE, "outros", null));
    }

    @Test
    void acceptsTrailingZerosBeyondTwoDecimals() {
        add("ok", "10.500", TransactionType.EXPENSE, "outros", null);
        assertEquals(1, list.execute(null, null).size());
    }

    @Test
    void rejectsTooLongDescription() {
        assertThrows(IllegalArgumentException.class,
                () -> add("x".repeat(101), "10.00", TransactionType.EXPENSE, "outros", null));
    }

    @Test
    void rejectsTooLongCategory() {
        assertThrows(IllegalArgumentException.class,
                () -> add("ok", "10.00", TransactionType.EXPENSE, "c".repeat(51), null));
    }

    @Test
    void trimsDescriptionAndNormalizesCategory() {
        add("  almoço  ", "10.00", TransactionType.EXPENSE, "  Alimentação ", null);
        var saved = list.execute(null, null).get(0);
        assertEquals("almoço", saved.description());
        assertEquals("alimentação", saved.category());
    }

    @Test
    void blankCategoryBecomesOutros() {
        add("ok", "10.00", TransactionType.EXPENSE, "  ", null);
        assertEquals("outros", list.execute(null, null).get(0).category());
    }

    @Test
    void rejectsInvertedPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> list.execute(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 1)));
    }
}
