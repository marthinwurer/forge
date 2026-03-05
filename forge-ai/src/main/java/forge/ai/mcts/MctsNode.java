package forge.ai.mcts;

import java.util.ArrayList;
import java.util.List;

/**
 * MCTS tree node using UCB1 (Upper Confidence Bound) for selection.
 */
public class MctsNode {

    private final MctsMove move; // The move that led to this node (null for root)
    private final MctsNode parent;
    private final List<MctsNode> children = new ArrayList<>();
    private final List<MctsMove> untriedMoves;

    private int visitCount = 0;
    private double totalValue = 0.0;

    public MctsNode(MctsMove move, MctsNode parent, List<MctsMove> legalMoves) {
        this.move = move;
        this.parent = parent;
        this.untriedMoves = new ArrayList<>(legalMoves);
    }

    /**
     * Select child with highest UCB1 value.
     */
    public MctsNode selectChild(double explorationConstant) {
        MctsNode best = null;
        double bestUcb = Double.NEGATIVE_INFINITY;

        for (MctsNode child : children) {
            double ucb = child.getUcb1Value(explorationConstant);
            if (ucb > bestUcb) {
                bestUcb = ucb;
                best = child;
            }
        }
        return best;
    }

    /**
     * Expand by picking the next untried move and creating a child node.
     */
    public MctsNode expand(List<MctsMove> childLegalMoves) {
        if (untriedMoves.isEmpty()) {
            return null;
        }
        MctsMove move = untriedMoves.remove(untriedMoves.size() - 1);
        MctsNode child = new MctsNode(move, this, childLegalMoves);
        children.add(child);
        return child;
    }

    /**
     * Backpropagate a simulation result up to the root.
     */
    public void backpropagate(double value) {
        MctsNode node = this;
        while (node != null) {
            node.visitCount++;
            node.totalValue += value;
            node = node.parent;
        }
    }

    /**
     * Get the best child node (highest visit count - most robust selection).
     */
    public MctsNode getBestChild() {
        MctsNode bestChild = null;
        int bestVisits = -1;
        for (MctsNode child : children) {
            if (child.visitCount > bestVisits) {
                bestVisits = child.visitCount;
                bestChild = child;
            }
        }
        return bestChild;
    }

    /**
     * Get the best move (child with highest visit count - most robust selection).
     */
    public MctsMove getBestMove() {
        MctsNode bestChild = getBestChild();
        return bestChild != null ? bestChild.move : null;
    }

    private double getUcb1Value(double explorationConstant) {
        if (visitCount == 0) {
            return Double.MAX_VALUE; // Unvisited nodes get max priority
        }
        double exploitation = totalValue / visitCount;
        double exploration = explorationConstant * Math.sqrt(Math.log(parent.visitCount) / visitCount);
        return exploitation + exploration;
    }

    public boolean isFullyExpanded() {
        return untriedMoves.isEmpty();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean hasUntriedMoves() {
        return !untriedMoves.isEmpty();
    }

    public MctsMove getMove() {
        return move;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public double getAverageValue() {
        return visitCount > 0 ? totalValue / visitCount : 0.0;
    }

    public List<MctsNode> getChildren() {
        return children;
    }

    public MctsNode getParent() {
        return parent;
    }

    public List<MctsMove> getUntriedMoves() {
        return untriedMoves;
    }
}
