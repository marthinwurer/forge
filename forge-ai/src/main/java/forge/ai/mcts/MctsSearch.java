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

        while (iterations < maxIterations
                && (System.currentTimeMillis() - startTime) < timeLimitMs) {
            // 1. Selection: walk tree via UCB1 until we find a node to expand
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

            // 3. Rollout: play forward using AI heuristics
            double value;
            if (state.isTerminal()) {
                value = state.evaluate();
            } else {
                MctsGameState rolloutState = state.copy();
                rolloutState.rollout(rolloutTurns);
                value = rolloutState.evaluate();
            }

            // 4. Backpropagation: update statistics up to root
            node.backpropagate(value);

            iterations++;
        }

        return root.getBestMove();
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
