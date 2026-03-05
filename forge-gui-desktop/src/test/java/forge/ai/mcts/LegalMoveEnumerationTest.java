package forge.ai.mcts;

import java.util.List;

import forge.ai.simulation.GameCopier;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import forge.ai.simulation.SimulationTest;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class LegalMoveEnumerationTest extends SimulationTest {

    @Test
    public void testEnumerateBasicSpells() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();

        boolean foundShock = false;
        for (SpellAbility sa : candidates) {
            if (sa.getHostCard().getName().equals("Shock")) {
                foundShock = true;
                break;
            }
        }
        AssertJUnit.assertTrue("Shock should be a candidate spell", foundShock);
    }

    @Test
    public void testEnumerateLandPlays() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCardToZone("Mountain", p, ZoneType.Hand);

        // Set phase to MAIN1 on player's turn so land plays are legal
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        // In main phase with no land drop used, land play should be available
        AssertJUnit.assertEquals(1, p.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals("Mountain", p.getCardsIn(ZoneType.Hand).get(0).getName());
        AssertJUnit.assertTrue("Player should have land drops remaining",
                p.canPlayLand(p.getCardsIn(ZoneType.Hand).get(0), false, null));
    }

    @Test
    public void testEnumerateNoMoves() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // Tapped land, empty hand - no spells to cast
        Card mountain = addCard("Mountain", p);
        mountain.tap(true, null, p);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();

        AssertJUnit.assertTrue("No candidates expected with tapped land and empty hand",
                candidates.isEmpty());
    }

    @Test
    public void testEnumerateMultipleSpells() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();

        boolean foundBolt = false;
        boolean foundShock = false;
        for (SpellAbility sa : candidates) {
            String name = sa.getHostCard().getName();
            if (name.equals("Lightning Bolt")) foundBolt = true;
            if (name.equals("Shock")) foundShock = true;
        }
        AssertJUnit.assertTrue("Lightning Bolt should be a candidate", foundBolt);
        AssertJUnit.assertTrue("Shock should be a candidate", foundShock);
    }

    @Test
    public void testEnumerateCombatAttackers() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Card bear1 = addCard("Runeclaw Bear", p);
        bear1.setSickness(false);
        Card bear2 = addCard("Grizzly Bears", p);
        bear2.setSickness(false);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Move to combat phase so creatures can attack
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);

        CardCollection possibleAttackers = CombatUtil.getPossibleAttackers(p);
        AssertJUnit.assertEquals("Both creatures should be possible attackers", 2, possibleAttackers.size());
    }

    @Test
    public void testSummoningSicknessFilter() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        // Old creature (clear sickness)
        Card bear = addCard("Runeclaw Bear", p);
        bear.setSickness(false);

        // Fresh creature (has sickness)
        Card freshBear = addCard("Grizzly Bears", p);
        freshBear.setSickness(true);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);

        CardCollection possibleAttackers = CombatUtil.getPossibleAttackers(p);
        AssertJUnit.assertEquals("Only the non-sick creature should be able to attack",
                1, possibleAttackers.size());
        AssertJUnit.assertEquals("Runeclaw Bear", possibleAttackers.get(0).getName());
    }

    @Test
    public void testGameCopyPreservesLegalMoves() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCardToZone("Lightning Bolt", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);

        // Get candidates in original game
        SpellAbilityPicker originalPicker = new SpellAbilityPicker(game, p);
        List<SpellAbility> originalCandidates = originalPicker.getCandidateSpellsAndAbilities();

        // Copy game
        GameCopier copier = new GameCopier(game);
        Game copy = copier.makeCopy();
        Player pCopy = (Player) copier.find(p);

        // Get candidates in copied game
        SpellAbilityPicker copyPicker = new SpellAbilityPicker(copy, pCopy);
        List<SpellAbility> copyCandidates = copyPicker.getCandidateSpellsAndAbilities();

        AssertJUnit.assertEquals("Copied game should have same number of candidate spells",
                originalCandidates.size(), copyCandidates.size());
    }
}
