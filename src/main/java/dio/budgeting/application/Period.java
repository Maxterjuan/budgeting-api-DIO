package dio.budgeting.application;

import java.time.Clock;
import java.time.LocalDate;

/** Período de consulta. Sem datas informadas, assume o mês corrente até hoje. */
public record Period(LocalDate from, LocalDate to) {

    public Period {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
    }

    public static Period of(LocalDate from, LocalDate to, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        LocalDate resolvedTo = to != null ? to : today;
        LocalDate resolvedFrom = from != null ? from : resolvedTo.withDayOfMonth(1);
        return new Period(resolvedFrom, resolvedTo);
    }
}
