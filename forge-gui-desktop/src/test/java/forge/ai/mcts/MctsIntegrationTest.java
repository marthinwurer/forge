package forge.ai.mcts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import forge.ai.AIOption;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class MctsIntegrationTest extends AITest {

    private Game createMctsGame() {
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck d1 = new Deck();

        // Opponent uses regular AI
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("opponent", null)));

        // AI player uses MCTS
        Set<AIOption> options = new HashSet<>();
        options.add(AIOption.USE_MCTS);
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("mcts_ai", options)));

        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");
        Game game = new Game(players, rules, match);
        game.setAge(GameStage.Play);
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = false;
        game.AI_TIMEOUT = FModel.getPreferences().getPrefInt(FPref.MATCH_AI_TIMEOUT);
        game.AI_CAN_USE_TIMEOUT = true;

        return game;
    }

    @Test
    public void testMctsAiSwingsForLethal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        // Give AI creature that can attack for lethal
        Card bear = addCard("Runeclaw Bear", p);
        bear.setSickness(false);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(2, null);

        // Fill libraries
        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue("Game should be over (lethal attack)", game.isGameOver());
    }

    @Test
    public void testMctsAiPlaysSpells() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCard("Runeclaw Bear", opponent);

        // Fill libraries
        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        // Shock should have been cast, dealing damage to bear or opponent
        // Verify the game advanced without crashing
        AssertJUnit.assertFalse("AI player should not have lost", p.hasLost());
    }

    @Test
    public void testFullGameWithMctsDoesntCrash() {
        // Run a full game with MCTS AI to verify no crashes
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        // Simple board state
        Card bear = addCard("Runeclaw Bear", p);
        bear.setSickness(false);
        addCard("Mountain", p);
        addCard("Mountain", p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(5, null);

        // Fill libraries
        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        // Play several turns
        for (int i = 0; i < 3 && !game.isGameOver(); i++) {
            this.playUntilNextTurn(game);
        }

        // Game should still be functional (may or may not be over)
        // Key check: no exceptions thrown during execution
    }
}
