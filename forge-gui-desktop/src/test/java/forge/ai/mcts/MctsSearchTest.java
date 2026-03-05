package forge.ai.mcts;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class MctsSearchTest extends SimulationTest {

    @Test
    public void testFindsLethalDamage() {
        // AI at 1 life, opponent at 2 life with an attacker.
        // If AI passes, opponent attacks next turn and AI dies.
        // If AI casts Shock at opponent, opponent dies.
        // MCTS should find that casting Shock is the winning move.
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        p.setLife(1, null);

        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(2, null);

        // Opponent has attacker that will kill us if we pass
        Card bear = addCard("Runeclaw Bear", opponent);
        bear.setSickness(false);

        // Fill libraries
        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        MctsGameState state = new MctsGameState(game, p);
        // Use 0 rollout turns so we evaluate the position directly.
        // With 0 rollout turns: Shock path → opponent dead → value 1.0,
        // Pass path → opponent alive → value < 1.0
        MctsSearch search = new MctsSearch(50, 10000, Math.sqrt(2), 0);
        MctsMove bestMove = search.findBestMove(state);

        // Best move should be a spell (Shock), not pass
        AssertJUnit.assertNotNull("Should find a move", bestMove);
        AssertJUnit.assertEquals("Should play a spell (Shock for lethal)",
                MctsMove.MoveType.SPELL_ABILITY, bestMove.getType());
    }

    @Test
    public void testReturnsPassWhenNoGoodMoves() {
        // Empty hand, only tapped lands - should pass
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Card mountain = addCard("Mountain", p);
        mountain.tap(true, null, p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        MctsGameState state = new MctsGameState(game, p);
        MctsSearch search = new MctsSearch(10, 5000, Math.sqrt(2), 2);
        MctsMove bestMove = search.findBestMove(state);

        // Only legal move is pass
        AssertJUnit.assertNotNull("Should return a move", bestMove);
        AssertJUnit.assertEquals("Should pass when no spells available",
                MctsMove.MoveType.PASS, bestMove.getType());
    }

    @Test
    public void testCompletesWithinTimeLimit() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCard("Runeclaw Bear", opponent);

        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        MctsGameState state = new MctsGameState(game, p);
        // Set tight time limit
        MctsSearch search = new MctsSearch(1000, 3000, Math.sqrt(2), 2);

        long start = System.currentTimeMillis();
        MctsMove bestMove = search.findBestMove(state);
        long elapsed = System.currentTimeMillis() - start;

        AssertJUnit.assertNotNull("Should find a move", bestMove);
        // Allow some margin for the last iteration
        AssertJUnit.assertTrue("Should complete within time limit (took " + elapsed + "ms)",
                elapsed < 10000);
    }

    @Test
    public void testMctsNodeBasics() {
        // Test basic node operations
        java.util.List<MctsMove> moves = new java.util.ArrayList<>();
        moves.add(MctsMove.spellAbility(0, "Shock"));
        moves.add(MctsMove.pass());

        MctsNode root = new MctsNode(null, null, moves);
        AssertJUnit.assertTrue(root.hasUntriedMoves());
        AssertJUnit.assertTrue(root.isLeaf());
        AssertJUnit.assertFalse(root.isFullyExpanded());
        AssertJUnit.assertEquals(0, root.getVisitCount());

        // Expand
        java.util.List<MctsMove> childMoves = java.util.List.of(MctsMove.pass());
        MctsNode child = root.expand(childMoves);
        AssertJUnit.assertNotNull(child);
        AssertJUnit.assertFalse(root.isLeaf());

        // Backpropagate
        child.backpropagate(0.8);
        AssertJUnit.assertEquals(1, child.getVisitCount());
        AssertJUnit.assertEquals(1, root.getVisitCount());
        AssertJUnit.assertEquals(0.8, child.getAverageValue(), 0.001);
    }
}
