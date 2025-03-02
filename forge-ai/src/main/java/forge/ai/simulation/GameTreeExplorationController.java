package forge.ai.simulation;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
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
import java.util.function.Predicate;

public class GameTreeExplorationController extends PlayerController {
    public GameTreeExplorationController(Game game0, Player p, LobbyPlayer lp) {
        super(game0, p, lp);
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        throw new NotImplementedException();
    }

    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        throw new NotImplementedException();
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets) {
        throw new NotImplementedException();

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

    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix, boolean addMsgSuffix) {
        throw new NotImplementedException();
    }

    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix, boolean addMsgSuffix) {
        throw new NotImplementedException();
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
    public boolean playChosenSpellAbility(SpellAbility sa) {
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
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultChoice) {
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
        throw new NotImplementedException();
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

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect) {
        throw new NotImplementedException();
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

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider) {
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
