package ai.gameragkit.client;

/**
 * Options that control how the GameRAGKit service generates answers.
 */
public record AskOptions(
        Integer topK,
        Boolean inCharacter,
        Double importance,
        Boolean forceLocal,
        Boolean forceCloud
) {
    /**
     * Returns the default set of options recommended by the GameRAGKit service.
     */
    public static AskOptions defaults() {
        return new AskOptions(4, true, 0.2, false, false);
    }
}
