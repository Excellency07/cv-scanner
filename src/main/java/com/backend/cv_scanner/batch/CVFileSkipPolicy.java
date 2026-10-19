package com.backend.cv_scanner.batch;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * A single bad CV must never stop the whole batch, so any item-level error is skipped
 * (after the retries configured on the step). Infrastructure failures are different:
 * if the database is down every following item would fail too, so the job is failed
 * instead and can be re-run later.
 */
@Component
public class CVFileSkipPolicy implements SkipPolicy {

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {
        return !(throwable instanceof DataAccessResourceFailureException
                || throwable instanceof CannotCreateTransactionException
                || throwable instanceof Error);
    }
}
