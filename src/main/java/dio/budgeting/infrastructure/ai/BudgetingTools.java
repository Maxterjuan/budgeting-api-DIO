package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.CreateTransactionUseCase;
import dio.budgeting.application.GetSummaryUseCase;
import dio.budgeting.application.ListTransactionsUseCase;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Ferramentas que o modelo pode chamar. Cada método apenas delega a um caso de uso:
 * a IA nunca acessa repositório ou banco diretamente.
 */
@Component
public class BudgetingTools {

    public record TransactionView(String id, String description, BigDecimal amount,
                                  TransactionType type, String category, LocalDate date) {
        static TransactionView from(Transaction t) {
            return new TransactionView(t.id().value().toString(), t.description(), t.amount(),
                    t.type(), t.category(), t.date());
        }
    }

    private final CreateTransactionUseCase createTransaction;
    private final ListTransactionsUseCase listTransactions;
    private final GetSummaryUseCase getSummary;

    public BudgetingTools(CreateTransactionUseCase createTransaction,
                          ListTransactionsUseCase listTransactions,
                          GetSummaryUseCase getSummary) {
        this.createTransaction = createTransaction;
        this.listTransactions = listTransactions;
        this.getSummary = getSummary;
    }

    @Tool(description = "Registra uma nova transação financeira: uma receita (INCOME) ou uma despesa (EXPENSE).")
    public TransactionView createTransaction(
            @ToolParam(description = "Descrição curta, por exemplo: almoço, salário, uber") String description,
            @ToolParam(description = "Valor positivo, sem sinal") BigDecimal amount,
            @ToolParam(description = "INCOME para receita ou EXPENSE para despesa") TransactionType type,
            @ToolParam(description = "Categoria, por exemplo: alimentação, transporte, salário", required = false)
            String category,
            @ToolParam(description = "Data no formato yyyy-MM-dd. Omita para usar a data de hoje", required = false)
            String date) {
        var command = new CreateTransactionUseCase.Command(
                description, amount, type, category, parseDate(date));
        return TransactionView.from(createTransaction.execute(command));
    }

    @Tool(description = "Lista as transações de um período. Sem datas, lista o mês atual até hoje.")
    public List<TransactionView> listTransactions(
            @ToolParam(description = "Data inicial yyyy-MM-dd", required = false) String from,
            @ToolParam(description = "Data final yyyy-MM-dd", required = false) String to) {
        return listTransactions.execute(parseDate(from), parseDate(to)).stream()
                .map(TransactionView::from)
                .toList();
    }

    @Tool(description = "Resumo de um período: total de receitas, total de despesas, saldo e despesas por categoria. "
            + "Sem datas, resume o mês atual até hoje.")
    public GetSummaryUseCase.Summary getSummary(
            @ToolParam(description = "Data inicial yyyy-MM-dd", required = false) String from,
            @ToolParam(description = "Data final yyyy-MM-dd", required = false) String to) {
        return getSummary.execute(parseDate(from), parseDate(to));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException e) {
            // Mensagem clara volta para o modelo, que pode corrigir e tentar de novo.
            throw new IllegalArgumentException("Data inválida: '" + value + "'. Use o formato yyyy-MM-dd.");
        }
    }
}
