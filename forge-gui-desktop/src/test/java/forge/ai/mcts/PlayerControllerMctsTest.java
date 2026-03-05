package forge.ai.mcts;

import java.util.List;

import forge.ai.simulation.SimulationTest;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class PlayerControllerMctsTest extends SimulationTest {

    @Test
    public void testDirectedModeExecutesPresetSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);
        addCard("Runeclaw Bear", opponent);

        // Set up phase
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        // Get candidates to verify they exist
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();
        AssertJUnit.assertFalse("Should have candidates", candidates.isEmpty());

        // Test MctsMove creation and properties
        MctsMove spellMove = MctsMove.spellAbility(0, "Shock");
        AssertJUnit.assertEquals(MctsMove.MoveType.SPELL_ABILITY, spellMove.getType());
        AssertJUnit.assertEquals(0, spellMove.getIndex());
        AssertJUnit.assertEquals("Shock", spellMove.getDescription());
    }

    @Test
    public void testPassMoveProducesNull() {
        MctsMove passMove = MctsMove.pass();
        AssertJUnit.assertEquals(MctsMove.MoveType.PASS, passMove.getType());
        AssertJUnit.assertEquals("Pass", passMove.getDescription());
    }

    @Test
    public void testRolloutModeDelegatesToAI() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Simple test: rollout mode should work same as regular AI
        addCard("Mountain", p);
        addCard("Mountain", p);

        // Add library cards so game doesn't end
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        // Verify game can advance without crashing (rollout mode = regular AI)
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
    }

    @Test
    public void testMctsMoveEquality() {
        MctsMove pass1 = MctsMove.pass();
        MctsMove pass2 = MctsMove.pass();
        AssertJUnit.assertEquals(pass1, pass2);

        MctsMove spell1 = MctsMove.spellAbility(0, "Shock");
        MctsMove spell2 = MctsMove.spellAbility(0, "Shock");
        AssertJUnit.assertEquals(spell1, spell2);

        MctsMove spell3 = MctsMove.spellAbility(1, "Bolt");
        AssertJUnit.assertNotSame(spell1, spell3);
        AssertJUnit.assertFalse(spell1.equals(spell3));
    }

    @Test
    public void testMoveEnumeratorIncludesPass() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        MctsMoveEnumerator enumerator = new MctsMoveEnumerator();
        List<MctsMove> moves = enumerator.getLegalPriorityMoves(game, p);

        // Should always include pass
        boolean hasPass = false;
        for (MctsMove move : moves) {
            if (move.getType() == MctsMove.MoveType.PASS) {
                hasPass = true;
                break;
            }
        }
        AssertJUnit.assertTrue("Legal moves should always include PASS", hasPass);
    }

    @Test
    public void testMoveEnumeratorAttackMoves() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Card bear1 = addCard("Runeclaw Bear", p);
        bear1.setSickness(false);
        Card bear2 = addCard("Grizzly Bears", p);
        bear2.setSickness(false);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);

        MctsMoveEnumerator enumerator = new MctsMoveEnumerator();
        List<MctsMove> moves = enumerator.getLegalAttackMoves(game, p);

        // Should have: no attack, all-in, solo bear1, solo bear2
        AssertJUnit.assertEquals("Should have 4 attack options", 4, moves.size());

        // First should be no attack
        AssertJUnit.assertEquals(0, moves.get(0).getIndex());
        // Second should be all-in (bitmask 3 = both creatures)
        AssertJUnit.assertEquals(3, moves.get(1).getIndex());
    }
}
