package com.backend.cv_scanner.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CVRetryListener implements RetryListener {

    @Override
    public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
        log.info("Retrying CV processing (attempt {}) after: {}",
                retryState.getRetryCount() + 1,
                retryState.getLastException() != null ? retryState.getLastException().getMessage() : "unknown error");
    }

    @Override
    public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
        log.warn("Retries exhausted, the CV will be skipped: {}", exception.getMessage());
    }
}
