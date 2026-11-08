package dev.riserecode.seedcracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Retains downstream consumption progress; fixed-cost candidate work is reserved before side effects. */
public final class CandidateCursor<T> {
    private int cursor;
    private final List<T> accepted = new ArrayList<>();
    private List<T> result;
    public List<T> consume(List<T> candidates, int cost, Predicate<T> accept) {
        if (result != null) { return result; }
        while (cursor < candidates.size()) {
            FinderBudget.work(cost);
            T candidate = candidates.get(cursor);
            if (accept.test(candidate)) { accepted.add(candidate); }
            cursor++;
        }
        result = List.copyOf(accepted); return result;
    }
}
