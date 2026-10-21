package com.mindhaven.ai;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OutputSafetyService {
    private final AiAdapter adapter;
    private final OutputSafetyRules rules = new OutputSafetyRules();
    public OutputSafetyService(AiAdapter adapter) { this.adapter = adapter; }

    public AiAdapter.OutputReview review(String userMessage, String content) {
        var local = rules.review(content);
        if (!local.allowed()) return local;
        try {
            var review = adapter.reviewOutput(userMessage, content);
            if (review == null || review.codes() == null || (review.allowed() && !review.codes().isEmpty())
                    || (!review.allowed() && review.codes().isEmpty())) return unavailable();
            return review;
        } catch (RuntimeException ignored) {
            // Fail closed, without logging rejected content or provider response bodies.
            return unavailable();
        }
    }

    private AiAdapter.OutputReview unavailable() {
        return new AiAdapter.OutputReview(false, List.of("OUTPUT_REVIEW_UNAVAILABLE"));
    }
}
