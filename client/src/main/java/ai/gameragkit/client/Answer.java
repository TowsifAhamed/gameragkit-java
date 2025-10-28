package ai.gameragkit.client;

import java.util.List;

/**
 * Response returned by the GameRAGKit service when asking an NPC a question.
 *
 * @param text       The generated answer text.
 * @param sources    Optional identifiers for the knowledge sources that were used.
 * @param scores     Confidence scores corresponding to {@code sources}.
 * @param fromCloud  {@code true} when the answer was produced by the cloud fallback model.
 */
public record Answer(
        String text,
        List<String> sources,
        List<Double> scores,
        boolean fromCloud
) {
}
