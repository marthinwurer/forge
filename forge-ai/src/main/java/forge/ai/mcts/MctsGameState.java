package forge.ai.mcts;

import java.util.List;

import forge.ai.LobbyPlayerAi;
import forge.ai.simulation.GameCopier;
import forge.ai.simulation.GameStateEvaluator;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * Pull-based wrapper over the push-based Forge game loop.
 * Provides copy/getLegalMoves/applyMove/isTerminal/evaluate for MCTS.
 */
public class MctsGameState {

    private final Game game;
    private final Player aiPlayer;
    private final MctsMoveEnumerator enumerator;

    public MctsGameState(Game game, Player aiPlayer) {
        this.game = game;
        this.aiPlayer = aiPlayer;
        this.enumerator = new MctsMoveEnumerator();
    }

    /**
     * Deep-copy the game state via GameCopier.
     */
    public MctsGameState copy() {
        GameCopier copier = new GameCopier(game);
        Game gameCopy = copier.makeCopy();
        // Ensure priority is enabled on the copy (GameCopier doesn't set this)
        gameCopy.getPhaseHandler().onStackResolved();
        Player aiCopy = (Player) copier.find(aiPlayer);
        return new MctsGameState(gameCopy, aiCopy);
    }

    /**
     * Get legal moves at the current decision point.
     */
    public List<MctsMove> getLegalMoves() {
        PhaseType phase = game.getPhaseHandler().getPhase();
        if (phase == PhaseType.COMBAT_DECLARE_ATTACKERS
                && game.getPhaseHandler().getPlayerTurn().equals(aiPlayer)) {
            return enumerator.getLegalAttackMoves(game, aiPlayer);
        }
        if (phase == PhaseType.COMBAT_DECLARE_BLOCKERS
                && !game.getPhaseHandler().getPlayerTurn().equals(aiPlayer)) {
            return enumerator.getLegalBlockMoves(game, aiPlayer);
        }
        return enumerator.getLegalPriorityMoves(game, aiPlayer);
    }

    /**
     * Apply a move: copies the game, installs a DIRECTED controller,
     * plays the move, then advances to the next AI decision point.
     * The original state is not modified.
     */
    public MctsGameState applyMove(MctsMove move) {
        MctsGameState newState = copy();
        newState.executeMove(move);
        return newState;
    }

    /**
     * Execute a move on THIS game state (mutating).
     * Installs directed controller, plays the move, advances to next decision point.
     *
     * The controller stays in DIRECTED mode throughout advancement. After the queued
     * move is played, DIRECTED mode returns null (pass) for all priority checks,
     * allowing the stack to resolve and SBAs to be checked naturally.
     */
    private void executeMove(MctsMove move) {
        // Install MCTS controller in DIRECTED mode
        PlayerControllerMcts controller = installMctsController(aiPlayer);
        controller.setMode(PlayerControllerMcts.Mode.DIRECTED);
        controller.queueMove(move);

        // Ensure priority is enabled (devModeSet and GameCopier don't set this)
        game.getPhaseHandler().onStackResolved();

        // Run mainLoopStep until we reach the next AI decision point.
        // In DIRECTED mode, the controller plays the queued move once, then
        // passes for all subsequent priority checks. This ensures the stack
        // resolves and SBAs get checked before we stop.
        int maxSteps = 500;
        for (int i = 0; i < maxSteps && !game.isGameOver(); i++) {
            game.getPhaseHandler().mainLoopStep();

            if (game.isGameOver()) {
                break;
            }

            // Only stop at a clean decision point: stack empty, AI has priority
            if (game.getStack().isEmpty()) {
                Player priorityPlayer = game.getPhaseHandler().getPriorityPlayer();
                if (priorityPlayer != null && priorityPlayer.equals(aiPlayer)) {
                    break;
                }
            }
        }

        // Switch controller to ROLLOUT mode for future use (rollouts, etc.)
        controller.setMode(PlayerControllerMcts.Mode.ROLLOUT);
    }

    /**
     * Install a PlayerControllerMcts on the given player, replacing the existing controller.
     */
    private PlayerControllerMcts installMctsController(Player player) {
        if (player.getController() instanceof PlayerControllerMcts) {
            return (PlayerControllerMcts) player.getController();
        }
        PlayerControllerMcts controller = new PlayerControllerMcts(
                game, player, (LobbyPlayerAi) player.getLobbyPlayer());
        player.dangerouslySetController(controller);
        return controller;
    }

    /**
     * Run a rollout from this state for the specified number of turns.
     * Uses existing AI heuristics for all decisions.
     * Mutates this game state.
     */
    public void rollout(int maxTurns) {
        // Ensure MCTS controller is in ROLLOUT mode
        installMctsController(aiPlayer).setMode(PlayerControllerMcts.Mode.ROLLOUT);

        int startTurn = game.getPhaseHandler().getTurn();
        int maxSteps = 2000;
        int steps = 0;
        while (!game.isGameOver() && steps < maxSteps
                && (game.getPhaseHandler().getTurn() - startTurn) < maxTurns) {
            game.getPhaseHandler().mainLoopStep();
            steps++;
        }
    }

    public boolean isTerminal() {
        return game.isGameOver();
    }

    /**
     * Evaluate the game state from the AI player's perspective.
     * Returns a value in [0, 1] where 1.0 = AI winning, 0.0 = AI losing.
     */
    public double evaluate() {
        if (game.isGameOver()) {
            if (aiPlayer.hasLost()) {
                return 0.0;
            }
            if (aiPlayer.hasWon()) {
                return 1.0;
            }
            return 0.5; // draw
        }

        GameStateEvaluator evaluator = new GameStateEvaluator();
        GameStateEvaluator.Score score = evaluator.getScoreForGameState(game, aiPlayer);

        // Normalize to [0, 1] using sigmoid
        return sigmoid(score.value / 100.0);
    }

    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public Game getGame() {
        return game;
    }

    public Player getAiPlayer() {
        return aiPlayer;
    }
}
