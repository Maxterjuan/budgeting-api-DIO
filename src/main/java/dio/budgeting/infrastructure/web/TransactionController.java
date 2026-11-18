package dio.budgeting.infrastructure.web;

import dio.budgeting.application.CreateTransactionUseCase;
import dio.budgeting.application.GetSummaryUseCase;
import dio.budgeting.application.ListTransactionsUseCase;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransaction;
    private final ListTransactionsUseCase listTransactions;
    private final GetSummaryUseCase getSummary;

    public TransactionController(CreateTransactionUseCase createTransaction,
                                 ListTransactionsUseCase listTransactions,
                                 GetSummaryUseCase getSummary) {
        this.createTransaction = createTransaction;
        this.listTransactions = listTransactions;
        this.getSummary = getSummary;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        var command = new CreateTransactionUseCase.Command(
                request.description(), request.amount(), request.type(), request.category(), request.date());
        return TransactionResponse.from(createTransaction.execute(command));
    }

    @GetMapping
    public List<TransactionResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return listTransactions.execute(from, to).stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/summary")
    public GetSummaryUseCase.Summary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return getSummary.execute(from, to);
    }
}
