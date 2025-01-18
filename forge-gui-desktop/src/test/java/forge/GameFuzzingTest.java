
package forge;

import com.google.common.base.Function;
import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardType;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.generation.*;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

import static forge.view.SimulateMatch.simulateSingleGameOfMatch;

public class GameFuzzingTest {
    @Ignore
    @Test
    public void PlayGameWithRandomDecks() {
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>() {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(ForgePreferences.FPref.UI_LANGUAGE, "en-US");
                return null;
            }
        });

        // first deck
        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        final DeckGenerator5Color gen = new DeckGenerator5Color(cardDb, DeckFormat.Constructed);
        final Deck first_deck = new Deck("first", gen.getDeck(60, false));
        final Deck second_deck = new Deck("second", gen.getDeck(60, false));

        final RegisteredPlayer p1 = new RegisteredPlayer(first_deck);
        final RegisteredPlayer p2 = new RegisteredPlayer(second_deck);

        Set<AIOption> options = new HashSet<>();
        // options.add(AIOption.USE_SIMULATION);
        p1.setPlayer(new LobbyPlayerAi("p1", options));
        p2.setPlayer(new LobbyPlayerAi("p2", options));
        GameRules rules = new GameRules(GameType.Constructed);
        // need game rules, players, and title
        Match m = new Match(rules, Arrays.asList(p1, p2), "test");

        simulateSingleGameOfMatch(m, 120);
    }

//    @Ignore
    @Test
    public void ABTestAIWithRandomDecks() {
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>() {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(ForgePreferences.FPref.UI_LANGUAGE, "en-US");
                return null;
            }
        });
        // generate combinations of all two-color pairs
        List<String> colors = Arrays.asList("w", "u", "b", "r", "g");
        // randomly select a pair
        List<Pair<String, String>> pairs = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            for (int j = i + 1; j < colors.size(); j++) {
                pairs.add(Pair.of(colors.get(i), colors.get(j)));
            }
        }
        Random randomizer = new Random();

        int wins = 0;
        int draws = 0;
        int num_games = 1000;

        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        DeckGenPool pool = new DeckGenPool();
        // test with M13 for basic stuff
        CardEdition edition = StaticData.instance().getEditions().get("M13");
        pool.addAll(edition.getAllCardsInSet().stream().map(e -> cardDb.getCard(e.name)).collect(Collectors.toList()));

        // Add guildgates for sanity's sake
        pool.addAll(StaticData.instance().getEditions().get("DGM").getAllCardsInSet().stream()
                .map(e -> cardDb.getCard(e.name))
                        .filter(e -> !e.getRules().getType().getLandTypes().isEmpty()).collect(Collectors.toList()));

        for (int i = 0; i < num_games; i++) {
            Pair<String, String> pair1 = pairs.get(randomizer.nextInt(pairs.size()));
            Pair<String, String> pair2 = pairs.get(randomizer.nextInt(pairs.size()));
            System.out.println("Starting game " + i + ", wins: " + wins + ", Pair1: " + pair1 + ", Pair2: " + pair2);
            final DeckGeneratorBase gen = new DeckGenerator2Color(pool, DeckFormat.Constructed, pair1.getLeft(), pair1.getRight());
            final Deck first_deck = new Deck("first", gen.getDeck(60, false));
            final DeckGeneratorBase gen2 = new DeckGenerator2Color(pool, DeckFormat.Constructed, pair2.getLeft(), pair2.getRight());
            final Deck second_deck = new Deck("second", gen2.getDeck(60, false));

            System.out.println(first_deck.getAllCardsInASinglePool());

            final RegisteredPlayer p1 = new RegisteredPlayer(first_deck);
            final RegisteredPlayer p2 = new RegisteredPlayer(second_deck);

            Set<AIOption> options1 = new HashSet<>();
            Set<AIOption> options2 = new HashSet<>();
            options1.add(AIOption.AB_OPTION_ONE);
            p1.setPlayer(new LobbyPlayerAi("p1", options1));
            p2.setPlayer(new LobbyPlayerAi("p2", options2));
            GameRules rules = new GameRules(GameType.Constructed);
            // need game rules, players, and title
            Match m = new Match(rules, Arrays.asList(p1, p2), "test");

            Game g = simulateSingleGameOfMatch(m, 120);
            System.out.println(g.getOutcome().getOutcomeStrings() + " " + g.getPhaseHandler().getTurn());
            if (g.getOutcome().isWinner(p1)) {
                if (g.getOutcome().isWinner(p2)) {
                    draws += 1;
                } else {
                    wins += 1;
                }
            }
        }

        System.out.println("win percent " + wins / (float)num_games);
        System.out.println("draw percent " + draws / (float)num_games);
    }
}