package forge.ai.simulation;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.*;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.*;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.ITriggerEvent;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static forge.ai.ComputerUtilMana.calculateManaCost;

public class RandomController extends PlayerController {
    private static final Random RANDOM = new Random();

    public RandomController(Game game0, Player p, LobbyPlayer lp) {
        super(game0, p, lp);
    }

    @Override
    public boolean isAI() {
        return true;
    }

    /**
     * Called when given priority. null return is a pass.
     * Spells and abilities chosen get proposed
     *
     * @return
     */
    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        CardCollection cards = ComputerUtilAbility.getAvailableCards(this.getGame(), player);
        cards = ComputerUtilCard.dedupeCards(cards);
        List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, player);
        List<SpellAbility> candidateSAs = ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player);
        // list all land abilities if possible
        // list all spells if possible
        // list all activated abilities if possible
        // pick one at random
        candidateSAs.add(null);
        SpellAbility chosen = candidateSAs.get(RANDOM.nextInt(candidateSAs.size()));
        if (chosen == null) {
            return null;
        }
        chosen.setActivatingPlayer(player);
        return List.of(chosen);
    }

    /**
     * This function covers the proposal/resolution part of playing a sa
     * @param sa
     * @return
     */
    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        // play land
        if (sa.isLandAbility()) {
            if (sa.canPlay()) {
                sa.resolve();
            }
            return true;
        }

        // 601.2b:
        // If the spell is modal, the player announces the mode choice

        // If the player wishes to splice any cards onto the spell,

        // If the spell has alternative or additional costs that will be paid as it’s being cast

        // If the spell has a variable cost that will be paid as it’s being cast

        // If a cost that will be paid as the spell is being cast includes hybrid mana symbols,
        // the player announces the nonhybrid equivalent cost they intend to pay.

        // If a cost that will be paid as the spell is being cast includes Phyrexian mana symbols, the
        // player announces whether they intend to pay 2 life or a corresponding colored mana cost for
        // each of those symbols

        // 601.2c
        // The player announces their choice of an appropriate object or player for each target the spell requires

        // 601.2d If the spell requires the player to divide or distribute an effect (such as damage or counters)
        // among one or more targets, the player announces the division.

        // 601.2e The game checks to see if the proposed spell can legally be cast.

        // 601.2f The player determines the total cost of the spell.

        // 601.2g If the total cost includes a mana payment, the player then has a chance to activate mana abilities
        // TODO good lord this is gonna be nested ability activation and 

        // 601.2h The player pays the total cost

        // First, they pay all costs that don’t involve random elements or moving objects from the library to a
        // public zone, in any order.

        // then they pay all remaining costs in any order.

        // TODO for now just go with the heuristics for this one
        return ComputerUtil.handlePlayingSpellAbility(player, sa, getGame());
    }

    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (abilities.isEmpty()) {
            return null;
        }
        return abilities.get(RANDOM.nextInt(abilities.size()));
    }

    /**
     * Used for pregame actions, replacement effects, intervening if clauses
     * Also some triggers, maybe?
     * @param effectSA
     * @param mayChoseNewTargets
     */
    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets) {
        effectSA.setActivatingPlayer(this.player);
        System.out.println(effectSA.toString());

        throw new NotImplementedException();
        // ai code
//        if (mayChoseNewTargets)
//            brains.doTrigger(effectSA, true); // first parameter does not matter, since return value won't be used
//        ComputerUtil.playNoStack(player, effectSA, getGame(), true);
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        throw new NotImplementedException();
    }

    @Override
    public boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        throw new NotImplementedException();
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        throw new NotImplementedException();
    }

    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType, String message) {
        throw new NotImplementedException();
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        throw new NotImplementedException();
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder) {
        throw new NotImplementedException();
    }

    @Override
    public Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount) {
        throw new NotImplementedException();
    }

    @Override
    public Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        throw new NotImplementedException();
    }

    @Override
    public Integer announceRequirements(SpellAbility ability, String announce) {
        throw new NotImplementedException();
    }

    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        throw new NotImplementedException();
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        throw new NotImplementedException();
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        throw new NotImplementedException();
    }

    @Override
    public boolean helpPayForAssistSpell(ManaCostBeingPaid cost, SpellAbility sa, int max, int requested) {
        throw new NotImplementedException();
    }

    @Override
    public Player choosePlayerToAssistPayment(FCollectionView<Player> optionList, SpellAbility sa, String title, int max) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap, SpellAbility sa, String title, boolean isOptional) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player relatedPlayer, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title, Player relatedPlayer, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title, int num, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner) {
        throw new NotImplementedException();
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question) {
        throw new NotImplementedException();
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, PlayerActionConfirmMode mode, String message, String logic) {
        throw new NotImplementedException();
    }

    @Override
    public boolean confirmTrigger(WrappedAbility sa) {
        throw new NotImplementedException();
    }

    @Override
    public List<Card> exertAttackers(List<Card> attackers) {
        throw new NotImplementedException();
    }

    @Override
    public List<Card> enlistAttackers(List<Card> attackers) {
        throw new NotImplementedException();
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        throw new NotImplementedException();
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        throw new NotImplementedException();
    }

    /**
     * Called when cards are revealed
     * @param cards
     * @param zone
     * @param owner
     * @param messagePrefix
     * @param addMsgSuffix
     */
    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix, boolean addMsgSuffix) {
        // TODO keep track of these
    }

    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix, boolean addMsgSuffix) {
        // TODO keep track of these
    }

    @Override
    public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value) {
        throw new NotImplementedException();
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        throw new NotImplementedException();
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        throw new NotImplementedException();
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, CardCollection validCards, int min, int max) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int min, CardCollectionView hand, String param, SpellAbility sa) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave) {
        throw new NotImplementedException();
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise) {
        throw new NotImplementedException();
    }

    @Override
    public List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        throw new NotImplementedException();
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        throw new NotImplementedException();
    }

    @Override
    public Player chooseStartingPlayer(boolean isFirstGame) {
        throw new NotImplementedException();
    }

    @Override
    public PlayerZone chooseStartingHand(List<PlayerZone> zones) {
        throw new NotImplementedException();
    }

    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        throw new NotImplementedException();
    }

    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, boolean isOptional) {
        throw new NotImplementedException();
    }

    @Override
    public String chooseSector(Card assignee, String ai, List<String> sectors) {
        throw new NotImplementedException();
    }

    @Override
    public List<Card> chooseContraptionsToCrank(List<Card> contraptions) {
        throw new NotImplementedException();
    }

    @Override
    public int chooseSprocket(Card assignee, boolean forceDifferent) {
        throw new NotImplementedException();
    }

    @Override
    public PlanarDice choosePDRollToIgnore(List<PlanarDice> rolls) {
        throw new NotImplementedException();
    }

    @Override
    public Integer chooseRollToIgnore(List<Integer> rolls) {
        throw new NotImplementedException();
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer, boolean optional) {
        throw new NotImplementedException();
    }

    @Override
    public boolean mulliganKeepHand(Player player, int cardsToReturn) {
        throw new NotImplementedException();
    }

    @Override
    public CardCollectionView londonMulliganReturnCards(Player mulliganingPlayer, int cardsToReturn) {
        throw new NotImplementedException();
    }

    @Override
    public boolean confirmMulliganScry(Player p) {
        throw new NotImplementedException();
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat) {
        throw new NotImplementedException();
    }

    @Override
    public int chooseNumberForCostReduction(SpellAbility sa, int min, int max) {
        throw new NotImplementedException();
    }

    @Override
    public int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max) {
        throw new NotImplementedException();
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        throw new NotImplementedException();
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer) {
        throw new NotImplementedException();
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question, PlayerController.BinaryChoiceType kindOfChoice, Boolean defaultChoice) {
        throw new NotImplementedException();
    }

    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        throw new NotImplementedException();
    }

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        throw new NotImplementedException();
    }

    @Override
    public byte chooseColorAllowColorless(String message, Card c, ColorSet colors) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        throw new NotImplementedException();
    }

    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, String message, Predicate<ICardFace> cpp, String name) {
        throw new NotImplementedException();
    }

    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, List<ICardFace> faces, String message) {
        throw new NotImplementedException();
    }

    @Override
    public CardState chooseSingleCardState(SpellAbility sa, List<CardState> states, String message, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp) {
        throw new NotImplementedException();
    }

    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, Map<String, Object> params) {
        throw new NotImplementedException();
    }

    @Override
    public String chooseKeywordForPump(List<String> options, SpellAbility sa, String prompt, Card tgtCard) {
        throw new NotImplementedException();
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String string, SpellAbility sa) {
        throw new NotImplementedException();
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(List<ReplacementEffect> possibleReplacers) {
        return possibleReplacers.get(RANDOM.nextInt(possibleReplacers.size()));
    }

    @Override
    public StaticAbility chooseSingleStaticAbility(String prompt, List<StaticAbility> possibleReplacers) {
        throw new NotImplementedException();
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        throw new NotImplementedException();
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        throw new NotImplementedException();

    }

    @Override
    public void revealAISkipCards(String message, Map<Player, Map<DeckSection, List<? extends PaperCard>>> deckCards) {
        throw new NotImplementedException();

    }

    @Override
    public List<OptionalCostValue> chooseOptionalCosts(SpellAbility choosen, List<OptionalCostValue> optionalCostValues) {
        throw new NotImplementedException();
    }

    @Override
    public List<CostPart> orderCosts(List<CostPart> costs) {
        throw new NotImplementedException();
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers) {
        throw new NotImplementedException();
    }

    @Override
    public boolean payCombatCost(Card card, Cost cost, SpellAbility sa, String prompt) {
        throw new NotImplementedException();
    }

    public SpellAbility pickManaAbilityToPlay() {
        // pick a random mana ability that can pay towards the mana cost, and iterate until done
        CardCollection cards = ComputerUtilAbility.getAvailableCards(this.getGame(), player);
        cards = ComputerUtilCard.dedupeCards(cards);
        List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, player);
        List<SpellAbility> candidateSAs = ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player);
        candidateSAs.removeIf(c -> !c.isManaAbility());

        SpellAbility chosen = candidateSAs.get(RANDOM.nextInt(candidateSAs.size()));
        return chosen;
    }

    /**
     *
     * @param toPay
     * @param costPartMana
     * @param sa
     * @param prompt
     * @param matrix
     * @param effect
     * @return false if unable to pay the full mana cost, otherwise true
     */
    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect) {
        return ComputerUtilMana.payManaCost(new Cost(toPay, effect), player, sa, effect);
//        // taken from ComputerUtilMana.payManaCost
//        if ((sa.isOffering() && sa.getSacrificedAsOffering() == null) || (sa.isEmerge() && sa.getSacrificedAsEmerge() == null)) {
//            // nothing was chosen
//            return false;
//        }
//
//        // pick a random mana ability that can pay towards the mana cost, and iterate until done
//
//        // if there's not enough mana in the mana pool to pay the cost, activate mana abilities until you can.
//        // pick a combination of mana that can pay for the cost or activate a mana ability to add to the cost
//        Cost cost = new Cost(toPay, effect);
//        ManaCostBeingPaid manaCost = calculateManaCost(cost, sa, test, extraMana, effect);
//        while (!cost.())
//        SpellAbility chosen = pickManaAbilityToPlay();
//        throw new NotImplementedException();
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message) {
        throw new NotImplementedException();
    }

    @Override
    public String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message) {
        throw new NotImplementedException();
    }

    @Override
    public Card chooseDungeon(Player player, List<PaperCard> dungeonCards, String message) {
        throw new NotImplementedException();
    }

    /**
     * Used for learn,
     * @param destination
     * @param origin
     * @param sa
     * @param fetchList
     * @param delayedReveal
     * @param selectPrompt
     * @param isOptional
     * @param decider
     * @return
     */
    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider) {
        System.out.println(sa.toString());
        throw new NotImplementedException();
    }

    @Override
    public List<Card> chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider) {
        throw new NotImplementedException();
    }

    @Override
    public void autoPassCancel() {
        // Do nothing
    }

    @Override
    public void awaitNextInput() {
        // Do nothing
    }

    @Override
    public void cancelAwaitNextInput() {
        // Do nothing
    }

    @Override
    public void resetAtEndOfTurn() {
        // Do nothing
    }

}
