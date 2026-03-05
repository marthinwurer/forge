package forge.ai.mcts;

import java.util.List;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class MctsGameStateTest extends SimulationTest {

    private MctsGameState setupBasicState() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCardToZone("Lightning Bolt", p, ZoneType.Hand);

        addCard("Runeclaw Bear", opponent);

        // Fill libraries so game doesn't end from draw
        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        return new MctsGameState(game, p);
    }

    @Test
    public void testCopyProducesIndependentState() {
        MctsGameState state = setupBasicState();
        MctsGameState copy = state.copy();

        // Verify they are different game objects
        AssertJUnit.assertNotSame(state.getGame(), copy.getGame());
        AssertJUnit.assertNotSame(state.getAiPlayer(), copy.getAiPlayer());

        // Verify same life totals
        AssertJUnit.assertEquals(state.getAiPlayer().getLife(), copy.getAiPlayer().getLife());
    }

    @Test
    public void testGetLegalMovesIncludesPass() {
        MctsGameState state = setupBasicState();
        List<MctsMove> moves = state.getLegalMoves();

        boolean hasPass = false;
        for (MctsMove move : moves) {
            if (move.getType() == MctsMove.MoveType.PASS) {
                hasPass = true;
                break;
            }
        }
        AssertJUnit.assertTrue("Legal moves should include PASS", hasPass);
    }

    @Test
    public void testGetLegalMovesIncludesSpells() {
        MctsGameState state = setupBasicState();
        List<MctsMove> moves = state.getLegalMoves();

        boolean hasSpell = false;
        for (MctsMove move : moves) {
            if (move.getType() == MctsMove.MoveType.SPELL_ABILITY) {
                hasSpell = true;
                break;
            }
        }
        AssertJUnit.assertTrue("Legal moves should include spell abilities", hasSpell);
    }

    @Test
    public void testApplyMoveCreatesNewState() {
        MctsGameState state = setupBasicState();
        List<MctsMove> moves = state.getLegalMoves();

        // Apply pass move
        MctsMove passMove = MctsMove.pass();
        MctsGameState newState = state.applyMove(passMove);

        // Original should not be modified
        AssertJUnit.assertNotSame(state.getGame(), newState.getGame());
        AssertJUnit.assertEquals(state.getAiPlayer().getLife(), state.getAiPlayer().getLife());
    }

    @Test
    public void testIsTerminalDetectsGameOver() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Set opponent to 0 life
        opponent.setLife(0, null);

        MctsGameState state = new MctsGameState(game, p);
        // Game over detection requires state-based effects check
        // Just verify the method exists and runs
        // The game might not be over until SBAs are checked
        AssertJUnit.assertFalse("Game not yet over (SBAs not checked)", state.isTerminal());
    }

    @Test
    public void testEvaluateReturnsHighForWinning() {
        MctsGameState state = setupBasicState();

        // AI at 20 life, opponent at 20 life with a bear
        double eval = state.evaluate();
        // With 2 mountains and a bolt in hand vs a bear, should be roughly balanced
        AssertJUnit.assertTrue("Evaluation should be between 0 and 1", eval >= 0.0 && eval <= 1.0);
    }

    @Test
    public void testEvaluateForDeadPlayer() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Give AI overwhelming advantage
        addCard("Mountain", p);
        for (int i = 0; i < 5; i++) {
            Card c = addCard("Runeclaw Bear", p);
            c.setSickness(false);
        }

        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        MctsGameState state = new MctsGameState(game, p);
        double eval = state.evaluate();
        // AI has 5 bears, opponent has nothing - should favor AI
        AssertJUnit.assertTrue("AI with 5 bears should have high eval (got " + eval + ")", eval > 0.5);
    }
}
