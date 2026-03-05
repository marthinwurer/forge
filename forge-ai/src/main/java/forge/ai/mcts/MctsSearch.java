package forge.ai.mcts;

import java.util.List;

/**
 * MCTS search using UCT (Upper Confidence bounds applied to Trees).
 * Uses heuristic rollouts (existing AI) instead of random play.
 */
public class MctsSearch {

    private static final double DEFAULT_EXPLORATION = Math.sqrt(2);
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final long DEFAULT_TIME_LIMIT_MS = 5000;
    private static final int DEFAULT_ROLLOUT_TURNS = 3;

    private int maxIterations;
    private long timeLimitMs;
    private double explorationConstant;
    private int rolloutTurns;

    public MctsSearch() {
        this(DEFAULT_MAX_ITERATIONS, DEFAULT_TIME_LIMIT_MS, DEFAULT_EXPLORATION, DEFAULT_ROLLOUT_TURNS);
    }

    public MctsSearch(int maxIterations, long timeLimitMs, double explorationConstant, int rolloutTurns) {
        this.maxIterations = maxIterations;
        this.timeLimitMs = timeLimitMs;
        this.explorationConstant = explorationConstant;
        this.rolloutTurns = rolloutTurns;
    }

    /**
     * Find the best move from the given game state using MCTS.
     */
    public MctsMove findBestMove(MctsGameState rootState) {
        List<MctsMove> rootMoves = rootState.getLegalMoves();
        if (rootMoves.isEmpty()) {
            return MctsMove.pass();
        }
        if (rootMoves.size() == 1) {
            return rootMoves.get(0);
        }

        MctsNode root = new MctsNode(null, null, rootMoves);

        long startTime = System.currentTimeMillis();
        int iterations = 0;
        long totalCopyMs = 0;
        long totalSelectionMs = 0;
        long totalRolloutMs = 0;
        long totalEvalMs = 0;

        while (iterations < maxIterations
                && (System.currentTimeMillis() - startTime) < timeLimitMs) {
            // 1. Selection: walk tree via UCB1 until we find a node to expand
            long selStart = System.currentTimeMillis();
            MctsNode node = root;
            MctsGameState state = rootState.copy();

            while (node.isFullyExpanded() && !node.isLeaf()) {
                node = node.selectChild(explorationConstant);
                state = state.applyMove(node.getMove());
                if (state.isTerminal()) {
                    break;
                }
            }

            // 2. Expansion: add one untried move as child
            if (!state.isTerminal() && node.hasUntriedMoves()) {
                // Get the next untried move and apply it to get child state
                MctsMove untriedMove = node.getUntriedMoves().get(node.getUntriedMoves().size() - 1);
                MctsGameState childState = state.applyMove(untriedMove);
                List<MctsMove> childMoves = childState.isTerminal()
                        ? List.of()
                        : childState.getLegalMoves();
                node = node.expand(childMoves);
                state = childState;
            }
            long selEnd = System.currentTimeMillis();
            totalSelectionMs += (selEnd - selStart);

            // 3. Rollout: play forward using AI heuristics
            double value;
            if (state.isTerminal()) {
                long evalStart = System.currentTimeMillis();
                value = state.evaluate();
                totalEvalMs += (System.currentTimeMillis() - evalStart);
            } else {
                long copyStart = System.currentTimeMillis();
                MctsGameState rolloutState = state.copy();
                totalCopyMs += (System.currentTimeMillis() - copyStart);

                long rolloutStart = System.currentTimeMillis();
                rolloutState.rollout(rolloutTurns);
                totalRolloutMs += (System.currentTimeMillis() - rolloutStart);

                long evalStart = System.currentTimeMillis();
                value = rolloutState.evaluate();
                totalEvalMs += (System.currentTimeMillis() - evalStart);
            }

            // 4. Backpropagation: update statistics up to root
            node.backpropagate(value);

            iterations++;
        }

        long totalMs = System.currentTimeMillis() - startTime;
        MctsMove bestMove = root.getBestMove();
        System.out.printf("[MCTS] %d iterations in %dms (%.1f iter/s) | selection: %dms | rollout copy: %dms | rollout play: %dms | eval: %dms | best: %s (visits=%d, avg=%.3f) | %d legal moves%n",
                iterations, totalMs,
                iterations * 1000.0 / Math.max(totalMs, 1),
                totalSelectionMs, totalCopyMs, totalRolloutMs, totalEvalMs,
                bestMove,
                root.getBestChild() != null ? root.getBestChild().getVisitCount() : 0,
                root.getBestChild() != null ? root.getBestChild().getAverageValue() : 0.0,
                rootMoves.size());

        return bestMove;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public void setTimeLimitMs(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public void setExplorationConstant(double explorationConstant) {
        this.explorationConstant = explorationConstant;
    }

    public void setRolloutTurns(int rolloutTurns) {
        this.rolloutTurns = rolloutTurns;
    }
}
