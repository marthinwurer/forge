package forge.game.player;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.*;
import forge.game.GameOutcome.AnteResult;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.ITriggerEvent;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A prototype for player controller class
 *
 * Handles phase skips for now.
 */
public abstract class PlayerController {

    public enum BinaryChoiceType {
        HeadsOrTails, // coin
        TapOrUntap,
        PlayOrDraw,
        OddsOrEvens,
        UntapOrLeaveTapped,
        LeftOrRight,
        AddOrRemove,
        IncreaseOrDecrease
    }

    public enum FullControlFlag {
        ChooseCostOrder,
        ChooseCostReductionOrderAndVariableAmount,
        //ChooseManaPoolShard, // select shard with special properties
        NoPaymentFromManaAbility,
        NoFreeCombatCostHandling,
        AllowPaymentStartWithMissingResources,
        LayerTimestampOrder // for StaticEffect$, tokens later etc.
    }

    private Set<FullControlFlag> fullControls = EnumSet.noneOf(FullControlFlag.class);

    protected final GameView gameView;

    protected final Player player;
    protected final LobbyPlayer lobbyPlayer;

    public PlayerController(Game game0, Player p, LobbyPlayer lp) {
        gameView = game0.getView();
        player = p;
        lobbyPlayer = lp;
    }

    public boolean isAI() {
        return false;
    }

    public Game getGame() { return gameView.getGame(); }
    public Match getMatch() { return gameView.getMatch(); }
    public Player getPlayer() { return player; }
    public LobbyPlayer getLobbyPlayer() { return lobbyPlayer; }

    public void tempShowCards(final Iterable<Card> cards) { } // show cards in UI until ended
    public void endTempShowCards() { }

    public final SpellAbility getAbilityToPlay(final Card hostCard, final List<SpellAbility> abilities) { return getAbilityToPlay(hostCard, abilities, null); }
    /**
     * Called when a player needs to choose which ability to activate from a card with multiple abilities.
     * Used during priority windows when multiple abilities are available.
     *
     * @param hostCard The card whose abilities are being chosen from
     * @param abilities List of available abilities on the card
     * @param triggerEvent The trigger event that made these abilities available (null for normal activation)
     * @return The chosen SpellAbility to activate, or null if none chosen
     */
    public abstract SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent);

    /**
     * Executes a spell or ability without putting it on the stack (immediate resolution).
     * Used for effects that resolve immediately without giving opponents a chance to respond.
     *
     * @param effectSA The spell/ability to execute immediately
     * @param mayChoseNewTargets Whether the player can choose new targets when executing
     */
    public abstract void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets);
    /**
     * Orders and executes multiple abilities that trigger simultaneously for the active player.
     * Called when multiple triggered abilities would go on the stack at the same time.
     * The active player chooses the order in which they are put on the stack.
     *
     * @param activePlayerSAs List of triggered abilities controlled by the active player
     */
    public abstract void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs);
    /**
     * Decides whether to put a triggered ability on the stack when it triggers.
     * Called whenever a triggered ability would trigger for this player.
     *
     * @param host The card that has the triggered ability
     * @param wrapperAbility The wrapped triggered ability with targeting/mode choices
     * @param isMandatory Whether this trigger is mandatory (true) or optional (false)
     * @return true to put the ability on the stack, false to decline (only for optional triggers)
     */
    public abstract boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory);
    /**
     * Decides whether to cast a spell when given the opportunity by another effect.
     * Used by effects that allow playing spells from unusual zones or with modifications.
     * Examples: Cascade, cards that let you play from graveyard, "you may cast" effects.
     *
     * @param tgtSA The spell ability that can be cast
     * @return true to cast the spell, false to decline
     */
    public abstract boolean playSaFromPlayEffect(SpellAbility tgtSA);

    /**
     * Performs sideboarding between games in a match.
     * Called between games 1 and 2, and games 2 and 3 in best-of-3 matches.
     *
     * @param deck The player's current deck configuration
     * @param gameType The type of game being played (affects sideboarding rules)
     * @param message UI message explaining the sideboarding opportunity
     * @return List of cards to swap in from sideboard (implementation handles swapping out)
     */
    public abstract List<PaperCard> sideboard(final Deck deck, GameType gameType, String message);
    /**
     * Chooses which cards won from ante to add to the deck.
     * Called after winning an ante game - player can choose which ante cards to keep.
     *
     * @param losses Cards lost by opponents in the ante
     * @return Cards to add to this player's deck from the ante
     */
    public abstract List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses);

    /**
     * Assigns combat damage from an attacking creature to its blockers and/or the defending player/planeswalker.
     * Called during the combat damage step when creatures deal damage.
     *
     * @param attacker The attacking creature dealing damage
     * @param blockers All creatures blocking this attacker (in damage assignment order)
     * @param remaining Blockers that still need damage assigned (lethal damage removes them from this list)
     * @param damageDealt Total damage the attacker deals
     * @param defender The player or planeswalker being attacked (receives trample damage)
     * @param overrideOrder Whether to ignore normal damage assignment order (e.g., Crystalline Giant)
     * @return Map of targets to damage amounts - must assign lethal to each blocker before assigning to defender
     */
    public abstract Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder);
    /**
     * Divides shield counters or damage prevention among multiple targets.
     * Used by effects that distribute protection or damage prevention.
     * Example: Protean Hydra's damage prevention, or shield counter distribution effects.
     *
     * @param effectSource The card creating the shield effect
     * @param affected Map of entities that can receive shield amounts to their current amounts
     * @param shieldAmount Total amount of shield/prevention to distribute
     * @return Map of entities to shield amounts - total must equal shieldAmount
     */
    public abstract Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount);
    /**
     * Specifies a combination of mana colors for effects that produce variable colored mana.
     * Used by mana sources that produce "X mana in any combination of colors".
     * Examples: Gemstone Mine variants, Pillar of the Paruns, Coalition Relic.
     *
     * @param sa The mana ability being activated
     * @param colorSet The colors of mana that can be produced
     * @param manaAmount Total amount of mana to produce
     * @param different Whether each mana must be a different color
     * @return Map of color bytes to amounts - must total to manaAmount
     */
    public abstract Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different);

    /**
     * Chooses permanents to sacrifice for an effect.
     * Called when spells or abilities require sacrificing permanents as part of their effect.
     * Examples: Diabolic Intent (sacrifice creature), Natural Order, Goblin Bombardment activation.
     *
     * @param sa The spell/ability requiring the sacrifice
     * @param min Minimum number of permanents that must be sacrificed
     * @param max Maximum number of permanents that can be sacrificed
     * @param validTargets Permanents that can be sacrificed
     * @param message UI prompt describing the sacrifice requirement
     * @return Permanents chosen to be sacrificed
     */
    public abstract CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message);
    /**
     * Chooses permanents to destroy for an effect where the controller has a choice.
     * Used by effects where the affected player chooses what gets destroyed.
     * Examples: Wrath effects with choice, Pernicious Deed activation, Nevinyrral's Disk.
     *
     * @param sa The spell/ability causing the destruction
     * @param min Minimum number of permanents that must be destroyed
     * @param max Maximum number of permanents that can be destroyed
     * @param validTargets Permanents that can be destroyed
     * @param message UI prompt describing the destruction choice
     * @return Permanents chosen to be destroyed
     */
    public abstract CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message);

    /**
     * Announces a value for X or other variable requirements in spells.
     * Called when casting spells with variable costs or when choosing values for X spells.
     * Examples: Fireball (X damage), Hydras with X +1/+1 counters, Earthquake.
     *
     * @param ability The spell/ability being cast
     * @param announce Description of what value is being chosen
     * @return The announced value (typically for X in mana costs)
     */
    public abstract Integer announceRequirements(SpellAbility ability, String announce);
    /**
     * Chooses new targets for a spell or ability that is being retargeted.
     * Used by rare effects that change the targets of spells on the stack.
     * Examples: Redirect, Deflection, Ricochet Trap.
     *
     * @param ability The spell/ability being retargeted
     * @param filter Predicate defining what can be targeted
     * @param optional Whether retargeting is optional
     * @return New target choices for the ability
     */
    public abstract TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional);
    /**
     * Chooses targets for a spell or ability during casting/activation.
     * Called during the targeting step of casting spells or activating abilities.
     * Note: This method modifies the ability's target list directly.
     * Examples: Lightning Bolt (target creature or player), Counterspell (target spell).
     *
     * @param currentAbility The spell/ability that needs targets
     * @return true if valid targets were chosen, false if targeting failed
     */
    public abstract boolean chooseTargetsFor(SpellAbility currentAbility);

    /**
     * Chooses a target to redirect to when an effect allows changing targets.
     * Used primarily by Spellskite and similar redirection effects.
     * Examples: Spellskite (redirect target to itself), Willbender (redirect target).
     *
     * @param sa The ability allowing the redirection
     * @param allTargets List of spell-target pairs that can be redirected
     * @return The chosen spell and its new target
     */
    public abstract Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets);

    /**
     * Decides whether to help pay for another player's spell with the Assist mechanic.
     * Called when another player casts a spell with Assist and this player can contribute mana.
     * Examples: Counsel of the Soratami, Nantuko Cultivator.
     *
     * @param cost The mana cost being paid (shows what's already paid)
     * @param sa The spell with Assist being cast
     * @param max Maximum mana this player can contribute
     * @param requested Amount of mana the caster is asking for help with
     * @return true to help pay, false to decline
     */
    public abstract boolean helpPayForAssistSpell(ManaCostBeingPaid cost, SpellAbility sa, int max, int requested);
    /**
     * Chooses which player to ask for Assist help when casting a spell with Assist.
     * Called by the player casting the Assist spell to select who to ask for mana help.
     *
     * @param optionList Players who can potentially assist with payment
     * @param sa The spell with Assist being cast
     * @param title UI prompt for the choice
     * @param max Maximum mana that can be assisted
     * @return The player to ask for assistance
     */
    public abstract Player choosePlayerToAssistPayment(FCollectionView<Player> optionList, SpellAbility sa, String title, int max);

    /**
     * Chooses cards from a list for a spell or ability effect.
     * One of the most commonly used choice methods - handles card selection with constraints.
     * Examples: Buried Alive (choose 3 creatures), Fact or Fiction (choose a pile).
     * Note: min/max and optional can coexist for cases like "choose 3 to 5 cards or none at all".
     *
     * @param sourceList Available cards to choose from
     * @param sa The spell/ability requesting the choice
     * @param title UI prompt describing the choice
     * @param min Minimum number of cards that must be chosen
     * @param max Maximum number of cards that can be chosen
     * @param isOptional Whether the entire choice can be declined (overrides min requirement)
     * @param params Additional context (e.g., "ChosenCards" for previously selected cards)
     * @return The chosen cards
     */
    public abstract CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params);
    /**
     * Chooses cards from multiple categorized lists for complex effects.
     * Used when an effect offers choices from different categories or zones.
     * Examples: Gifts Ungiven (choose from library with different restrictions), complex modal spells.
     *
     * @param validMap Map of category names to available cards in each category
     * @param sa The spell/ability requesting the choice
     * @param title UI prompt describing the choice
     * @param isOptional Whether the choice can be declined entirely
     * @return The chosen cards from across all categories
     */
    public abstract CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap, SpellAbility sa, String title, boolean isOptional);

    public final <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, SpellAbility sa, String title, Map<String, Object> params) { return chooseSingleEntityForEffect(optionList, null, sa, title, false, null, params); }
    public final <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, SpellAbility sa, String title, boolean isOptional, Map<String, Object> params) { return chooseSingleEntityForEffect(optionList, null, sa, title, isOptional, null, params); }
    /**
     * Chooses a single game entity (card, player, etc.) for a spell or ability effect.
     * Universal choice method that works with any game entity type - cards, players, planeswalkers.
     * Examples: Demonic Tutor (choose card), Lightning Bolt (choose target), Diabolic Edict (choose creature to sacrifice).
     *
     * @param optionList Available entities to choose from
     * @param delayedReveal Cards to reveal to this player before choosing (null if none)
     * @param sa The spell/ability requesting the choice
     * @param title UI prompt describing the choice
     * @param isOptional Whether the choice can be declined
     * @param relatedPlayer Context player (e.g., "choose a creature target player controls")
     * @param params Additional context ("Voter" for vote effects, "AILogic" for AI hints)
     * @return The chosen entity, or null if declined (when optional)
     */
    public abstract <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player relatedPlayer, Map<String, Object> params);

    /**
     * Chooses multiple game entities for a spell or ability effect.
     * Like chooseSingleEntityForEffect but allows selecting multiple entities.
     * Examples: Council's Judgment (choose multiple permanents), mass bounce spells with choice.
     *
     * @param optionList Available entities to choose from
     * @param min Minimum number of entities that must be chosen
     * @param max Maximum number of entities that can be chosen
     * @param delayedReveal Cards to reveal before choosing (null if none)
     * @param sa The spell/ability requesting the choice
     * @param title UI prompt describing the choice
     * @param relatedPlayer Context player for the choice
     * @param params Additional context parameters
     * @return List of chosen entities
     */
    public abstract <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title, Player relatedPlayer, Map<String, Object> params);

    /**
     * Chooses multiple spell abilities from a list for an effect.
     * Used by effects that let you choose several spells or abilities to copy/activate.
     * Examples: Isochron Scepter variants, effects that copy multiple spells.
     *
     * @param spells Available spell abilities to choose from
     * @param sa The spell/ability requesting the choice
     * @param title UI prompt describing the choice
     * @param num Number of spell abilities to choose
     * @param params Additional context parameters
     * @return List of chosen spell abilities
     */
    public abstract List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title, int num, Map<String, Object> params);

    /**
     * Chooses a single spell ability from a list for an effect.
     * Used when an effect lets you choose one spell to copy, counter, or otherwise affect.
     * Examples: Fork (choose spell to copy), Counterspell (choose spell to counter).
     *
     * @param spells Available spell abilities to choose from
     * @param sa The spell/ability requesting the choice
     * @param title UI prompt describing the choice
     * @param params Additional context parameters
     * @return The chosen spell ability
     */
    public abstract SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title, Map<String, Object> params);

    public final boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return confirmAction(sa, mode, message, Lists.newArrayList(), null, params);
    }
    public final boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, Card cardToShow, Map<String, Object> params) {
        return confirmAction(sa, mode, message, Lists.newArrayList(), cardToShow, params);
    }
    /**
     * Confirms an action with the player before proceeding.
     * Used for optional effects, confirmation dialogs, and "may" abilities.
     * Examples: "You may draw a card", "Do you want to pay the additional cost?"
     *
     * @param sa The spell/ability requesting confirmation
     * @param mode The type of confirmation being requested
     * @param message The question or prompt to show the player
     * @param options List of additional text options (can be empty)
     * @param cardToShow Card to display for context (can be null)
     * @param params Additional context parameters
     * @return true to confirm the action, false to decline
     */
    public abstract boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params);
    /**
     * Confirms a bid action in auction-style effects.
     * Used by effects that involve bidding life or other resources.
     * Examples: Contract from Below, auction effects in Un-sets.
     *
     * @param sa The spell/ability involving bidding
     * @param bidlife The confirmation mode for the bid
     * @param string Description of what is being bid on
     * @param bid The current winning bid amount
     * @param winner The player who made the current winning bid
     * @return true to accept/continue the bid, false to decline
     */
    public abstract boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner);
    /**
     * Confirms whether to apply an optional replacement effect.
     * Called when a replacement effect can optionally replace an event.
     * Examples: "You may have [creature] enter with +1/+1 counters instead", Shield Sphere's prevention.
     *
     * @param replacementEffect The replacement effect that could apply
     * @param effectSA The spell/ability that created the replacement effect
     * @param affected The game entity that would be affected
     * @param question The prompt asking whether to apply the replacement
     * @return true to apply the replacement effect, false to let the original event happen
     */
    public abstract boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question);
    /**
     * Confirms whether to apply a static ability that requires player choice.
     * Used by static abilities that have optional or conditional applications.
     * Examples: Spells that cost less if you control certain permanents.
     *
     * @param hostCard The card with the static ability
     * @param mode The type of confirmation being requested
     * @param message The prompt describing the static ability choice
     * @param logic Additional logic context for the choice
     * @return true to apply the static ability, false to decline
     */
    public abstract boolean confirmStaticApplication(Card hostCard, PlayerActionConfirmMode mode, String message, String logic);
    /**
     * Confirms whether to put an optional triggered ability on the stack.
     * Called when an optional trigger would trigger ("whenever X, you may Y").
     * Examples: "Whenever a creature enters, you may draw a card", Rhystic Study.
     *
     * @param sa The wrapped triggered ability that would go on the stack
     * @return true to put the trigger on the stack, false to decline
     */
    public abstract boolean confirmTrigger(WrappedAbility sa);

    /**
     * Chooses which attacking creatures to exert during the declare attackers step.
     * Exerted creatures provide a benefit but don't untap during the next untap step.
     * Examples: Glory-Bound Initiate, Combat Celebrant, Always Watching (prevents exertion).
     *
     * @param attackers List of creatures that can be exerted while attacking
     * @return List of creatures chosen to be exerted
     */
    public abstract List<Card> exertAttackers(List<Card> attackers);
    /**
     * Chooses which non-attacking creatures to enlist with attacking creatures.
     * Enlisted creatures help attackers by providing their power as bonus damage.
     * Used by the Enlist mechanic from Dominaria United.
     *
     * @param attackers List of creatures that can be enlisted to help attackers
     * @return List of creatures chosen to be enlisted
     */
    public abstract List<Card> enlistAttackers(List<Card> attackers);

    /**
     * Declares which creatures attack and what they attack during the declare attackers step.
     * Called during the Combat Phase for the attacking player.
     * Must populate the Combat object with attacking creatures and their targets.
     *
     * @param attacker The player declaring attackers (the active player)
     * @param combat The Combat object to populate with attack declarations
     */
    public abstract void declareAttackers(Player attacker, Combat combat);
    /**
     * Declares which creatures block attacking creatures during the declare blockers step.
     * Called for each player being attacked during the Combat Phase.
     * Must add blocking assignments to the Combat object.
     *
     * @param defender The player declaring blockers (the defending player)
     * @param combat The Combat object containing attackers and to be populated with blockers
     */
    public abstract void declareBlockers(Player defender, Combat combat);

    /**
     * Orders multiple creatures blocking a single attacker for damage assignment.
     * Called when multiple creatures block the same attacker.
     * The attacking player chooses the order in which damage is assigned.
     *
     * @param attacker The creature being blocked
     * @param blockers The creatures blocking the attacker
     * @return The blockers in the order damage should be assigned to them
     */
    public abstract CardCollection orderBlockers(Card attacker, CardCollection blockers);

    /**
     * Adds a new blocker to an existing blocking order.
     * Called when a creature becomes a blocker after blockers have already been ordered.
     * Examples: Flash creatures, activated abilities that make creatures block.
     *
     * @param attacker The attacking creature being blocked
     * @param blocker The new creature joining the block
     * @param oldBlockers The creatures already blocking in their current order
     * @return The new complete order of all blockers (including the new one)
     */
    public abstract CardCollection orderBlocker(final Card attacker, final Card blocker, final CardCollection oldBlockers);
    /**
     * Orders multiple attackers when they're blocked by a single creature.
     * Called when one blocker blocks multiple attackers (rare, but possible with special effects).
     * The defending player chooses the order for damage assignment.
     *
     * @param blocker The creature blocking multiple attackers
     * @param attackers The attacking creatures being blocked
     * @return The attackers in the order damage should be assigned from them
     */
    public abstract CardCollection orderAttackers(Card blocker, CardCollection attackers);

    /**
     * Shows cards to this player for information purposes.
     * Used when effects reveal cards from libraries, hands, or other hidden zones.
     * Examples: Gitaxian Probe, Telepathy, library tutoring effects.
     */
    public final void reveal(CardCollectionView cards, ZoneType zone, Player owner) {
        reveal(cards, zone, owner, null);
    }
    public final void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix) {
        reveal(cards, zone, owner, null, true);
    }
    /**
     * Shows cards to this player with full control over the reveal message.
     *
     * @param cards The cards being revealed
     * @param zone The zone the cards are being revealed from
     * @param owner The player who owns/controls the revealed cards
     * @param messagePrefix Custom prefix for the reveal message (can be null)
     * @param addMsgSuffix Whether to add a standard suffix to the message
     */
    public abstract void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix, boolean addMsgSuffix);
    public final void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix) {
        reveal(cards, zone, owner, null, true);
    }
    /**
     * Shows card views to this player (used for GUI display optimization).
     * Similar to the Card version but uses CardView objects for efficient network transmission.
     *
     * @param cards The card views being revealed
     * @param zone The zone the cards are from
     * @param owner The player view who owns the cards
     * @param messagePrefix Custom prefix for the reveal message
     * @param addMsgSuffix Whether to add a standard suffix to the message
     */
    public abstract void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix, boolean addMsgSuffix);

    /**
     * Notifies the player of a value that was chosen or determined by an effect.
     * Used to inform players of hidden information that becomes public.
     * Examples: Chosen card names from Cabal Therapy, creature types from Tribal spells,
     * numbers chosen for Elemental Blast, colors chosen for Circle of Protection effects.
     *
     * @param saSource The spell/ability that determined the value
     * @param realtedTarget The game object the value relates to (can be null)
     * @param value The string representation of the chosen/determined value
     */
    public abstract void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value);

    /**
     * Arranges cards when scrying - decides which cards go on top and which go to bottom.
     * Called when Scry effects resolve (Scry X means look at X cards).
     * Examples: Serum Visions, Brainstorm effects, Preordain.
     *
     * @param topN The cards from the top of the library that can be arranged
     * @return Pair of (cards to put on top in order, cards to put on bottom in any order)
     */
    public abstract ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN);
    /**
     * Arranges cards when surveiling - decides which cards go to graveyard and which stay on top.
     * Called when Surveil effects resolve (Surveil X means look at X cards, put any in graveyard).
     * Examples: House Guildmage, Dimir cards from Guilds of Ravnica.
     *
     * @param topN The cards from the top of the library that can be arranged
     * @return Pair of (cards to leave on top in order, cards to put in graveyard)
     */
    public abstract ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN);

    /**
     * Decides whether to put a card on top of the library when given the choice.
     * Used by effects that say "you may put this card on top of your library instead".
     * Examples: Brainstorm (putting cards back), Scroll Rack effects.
     *
     * @param c The card that can be put on top of the library
     * @return true to put the card on top, false to leave it where it would normally go
     */
    public abstract boolean willPutCardOnTop(Card c);

    /**
     * Orders cards being moved to a zone when the order matters.
     * Called when multiple cards move to the same zone and their relative order is important.
     * Examples: Mass reanimation (order matters for enters-the-battlefield triggers),
     * putting multiple cards on top of library, graveyard ordering.
     * Note: When moving to top of library, this returns the order they should be moved,
     * which is the reverse of their final order (last moved ends up on top).
     *
     * @param cards The cards being moved to the zone
     * @param destinationZone The zone the cards are moving to
     * @param source The spell/ability causing the zone change
     * @return The cards in the order they should be moved to the zone
     */
    public abstract CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source);

    /**
     * Chooses cards to discard from a specific set of valid cards.
     * Used when discard effects have restrictions on what can be discarded.
     * Examples: "Discard a card with mana value 3 or less", "Discard an artifact card".
     *
     * @param playerDiscard The player whose cards are being discarded
     * @param sa The spell/ability causing the discard
     * @param validCards The cards that can legally be discarded
     * @param min Minimum number of cards that must be discarded
     * @param max Maximum number of cards that can be discarded
     * @return The cards chosen to be discarded
     */
    public abstract CardCollectionView chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, CardCollection validCards, int min, int max);
    /**
     * Chooses cards to discard unless they match a specific type or property.
     * Used by effects that force discard but allow keeping cards of certain types.
     * Examples: "Discard your hand unless you discard a land card", type-specific discard effects.
     *
     * @param min Minimum number of cards that must be discarded
     * @param hand The cards available to discard from
     * @param param The type or property that can be kept instead of discarding
     * @param sa The spell/ability causing the discard
     * @return The cards chosen to be discarded
     */
    public abstract CardCollectionView chooseCardsToDiscardUnlessType(int min, CardCollectionView hand, String param, SpellAbility sa);
    /**
     * Chooses cards to discard during cleanup step to reach maximum hand size.
     * Called at end of turn when a player has more than 7 cards in hand.
     * This is the normal "discard to hand size" rule, not an effect.
     *
     * @param numDiscard The number of cards that must be discarded
     * @return The cards chosen to be discarded
     */
    public abstract CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard);

    /**
     * Chooses cards to exile for the Delve mechanic to reduce casting costs.
     * Called when casting spells with Delve - each card exiled reduces generic cost by 1.
     * Examples: Treasure Cruise, Dig Through Time, Hooting Mandrills.
     *
     * @param genericAmount The amount of generic mana that can be reduced
     * @param grave The cards in graveyard available to exile for delve
     * @return The cards chosen to exile for delve cost reduction
     */
    public abstract CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave);
    /**
     * Chooses creatures/artifacts to tap for Convoke or Improvise cost reduction.
     * Convoke lets you tap creatures to help pay costs, Improvise lets you tap artifacts.
     * Each tapped permanent can pay for one mana of the corresponding type.
     * Examples: Chord of Calling (convoke), Inspiring Statuary effects (improvise).
     *
     * @param sa The spell being cast with convoke/improvise
     * @param manaCost The mana cost being paid
     * @param untappedCards The creatures (convoke) or artifacts (improvise) that can be tapped
     * @param improvise true for improvise, false for convoke
     * @return Map of cards to the mana cost shard they will pay for
     */
    public abstract Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise);
    /**
     * Chooses cards with splice to add their effects to an instant or sorcery being cast.
     * Used by the Splice mechanic from Kamigawa block.
     * Examples: Splice onto Arcane spells, Splice onto Instant/Sorcery.
     *
     * @param sa The instant or sorcery spell being cast
     * @param cards The cards with splice that can be spliced onto the spell
     * @return The cards chosen to splice onto the spell
     */
    public abstract List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards);

    /**
     * Chooses cards to reveal from hand when an effect requires hand revelation.
     * Used by effects that ask players to reveal cards with certain properties.
     * Examples: "Reveal any number of cards from your hand", "Reveal a creature card".
     *
     * @param min Minimum number of cards that must be revealed
     * @param max Maximum number of cards that can be revealed
     * @param valid The cards in hand that can be revealed
     * @return The cards chosen to be revealed
     */
    public abstract CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid);
    /**
     * Chooses which abilities to activate from the opening hand before the game begins.
     * Used by cards that have abilities usable before the first turn.
     * Examples: Leylines (start in play if in opening hand), Chancellor effects.
     *
     * @param usableFromOpeningHand Abilities that can be activated from the opening hand
     * @return The abilities chosen to be activated before the game starts
     */
    public abstract List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand);
    /**
     * Chooses who goes first at the start of a game.
     * In multiplayer, the randomly chosen starting player chooses the actual starting player.
     * In matches, the winner of the previous game chooses who goes first.
     *
     * @param isFirstGame Whether this is the first game of a match
     * @return The player who should take the first turn
     */
    public abstract Player chooseStartingPlayer(boolean isFirstGame);
    /**
     * Chooses which starting hand to keep when multiple options are available.
     * Used in special game modes or effects that provide multiple hand options.
     * Examples: Planechase variants, special draft formats with hand choices.
     *
     * @param zones List of possible starting hands (as PlayerZone objects)
     * @return The chosen starting hand zone
     */
    public abstract PlayerZone chooseStartingHand(List<PlayerZone> zones);
    /**
     * Chooses which specific mana to spend from the mana pool when multiple options exist.
     * Called when paying costs and the mana pool contains different types of mana.
     * Important for effects that care about mana sources or produce conditional mana.
     * Examples: Spending mana with restrictions, choosing between different colored mana.
     *
     * @param manaChoices The different mana options available in the pool
     * @return The specific mana to spend from the pool
     */
    public abstract Mana chooseManaFromPool(List<Mana> manaChoices);

    /**
     * Chooses a type from a list of valid types (creature type, card type, etc.).
     * Used by effects that ask players to name or choose types.
     * Examples: Goblin King (choose creature type), Door of Destinies, tribal spells.
     *
     * @param kindOfType Description of what kind of type is being chosen ("creature type", "card type")
     * @param sa The spell/ability requesting the type choice
     * @param validTypes Collection of valid type strings
     * @param isOptional Whether choosing a type is optional
     * @return The chosen type as a string, or null if optional and declined
     */
    public abstract String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, boolean isOptional);
    public final String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes) {
        return chooseSomeType(kindOfType, sa, validTypes, false);
    }

    /**
     * Chooses a sector for Contraption mechanics from Unstable.
     * Used when assembling contraptions that need to be assigned to sectors.
     * Sectors are Alpha, Beta, and Gamma in the contraption deck area.
     *
     * @param assignee The contraption card being assigned to a sector
     * @param ai AI hint for the choice
     * @param sectors Available sectors ("Alpha", "Beta", "Gamma")
     * @return The chosen sector name
     */
    public abstract String chooseSector(Card assignee, String ai, List<String> sectors);
    public final String chooseSector(Card assignee, String ai) {
        final List<String> sectors = Arrays.asList("Alpha", "Beta", "Gamma");
        return chooseSector(assignee, ai, sectors);
    }

    /**
     * Chooses which contraptions to crank during contraption cranking.
     * Used by the Contraption mechanic from Unstable - contraptions in each sector can be cranked.
     * Examples: Any effect that cranks contraptions, Steamflogger effects.
     *
     * @param contraptions List of contraptions available to crank
     * @return The contraptions chosen to be cranked
     */
    public abstract List<Card> chooseContraptionsToCrank(List<Card> contraptions);

    /**
     * Chooses a sprocket number for Contraption assembly.
     * Used when assembling contraptions - each contraption has sprocket numbers that affect connections.
     * Part of the Unstable Contraption mechanic.
     *
     * @param assignee The contraption being assembled
     * @param forceDifferent Whether this sprocket must be different from existing ones
     * @return The chosen sprocket number
     */
    public abstract int chooseSprocket(Card assignee, boolean forceDifferent);
    public final int chooseSprocket(Card assignee) {
        return chooseSprocket(assignee, false);
    }

    /**
     * Chooses which planar dice roll to ignore when effects allow rerolling or ignoring dice.
     * Used in Planechase format when effects let you ignore or modify planar dice rolls.
     * Examples: Effects that let you ignore chaos symbols or planar die results.
     *
     * @param rolls List of planar dice rolls that can be ignored
     * @return The planar dice roll chosen to be ignored
     */
    public abstract PlanarDice choosePDRollToIgnore(List<PlanarDice> rolls);
    /**
     * Chooses which dice roll result to ignore when effects allow ignoring rolls.
     * Used by effects that let you ignore dice rolls or choose which roll to keep.
     * Examples: Reroll effects, "ignore the lowest roll" effects.
     *
     * @param rolls List of dice roll results that can be ignored
     * @return The roll result chosen to be ignored
     */
    public abstract Integer chooseRollToIgnore(List<Integer> rolls);
    /**
     * Chooses which dice to reroll when effects allow rerolling some or all dice.
     * Used by effects that let you reroll dice after seeing the initial results.
     * Examples: "You may reroll any number of those dice", partial reroll effects.
     *
     * @param rolls List of current dice roll results
     * @return The rolls chosen to be rerolled (by their values)
     */
    public abstract List<Integer> chooseDiceToReroll(List<Integer> rolls);
    /**
     * Chooses which dice roll to modify when effects allow changing dice results.
     * Used by effects that let you add to or modify specific dice rolls.
     * Examples: "Add 1 to target die roll", "Set target die to 6".
     *
     * @param rolls List of dice rolls that can be modified
     * @return The roll chosen to be modified
     */
    public abstract Integer chooseRollToModify(List<Integer> rolls);
    /**
     * Chooses which dice roll result to swap when effects allow swapping dice.
     * Used by effects that let you exchange dice results between different dice.
     * Examples: "Swap two target dice rolls", effects that let you rearrange dice results.
     *
     * @param rolls List of dice roll results that can be swapped
     * @return The dice roll result chosen to be swapped
     */
    public abstract RollDiceEffect.DieRollResult chooseRollToSwap(List<RollDiceEffect.DieRollResult> rolls);
    /**
     * Chooses what value to swap a dice roll to when given specific swap options.
     * Used when dice swapping effects provide predetermined values to swap to.
     * Examples: Effects that let you set dice to specific values based on creature stats.
     *
     * @param swapChoices List of possible values to swap the dice to
     * @param currentResult The current value of the dice being swapped
     * @param power Power of related creature (if applicable)
     * @param toughness Toughness of related creature (if applicable)
     * @return The chosen swap value
     */
    public abstract String chooseRollSwapValue(List<String> swapChoices, Integer currentResult, int power, int toughness);

    /**
     * Casts a vote in Council's Dilemma and other voting mechanics.
     * Used by Will of the Council, Council's Dilemma, and similar voting effects.
     * Examples: Tivit, Seller of Secrets (evidence/bribery), Ballot Broker, Council's Judgment.
     *
     * @param sa The spell/ability causing the vote
     * @param prompt Description of what is being voted on
     * @param options Available voting choices
     * @param votes Current vote tallies from other players
     * @param forPlayer The player this vote is for (may differ from the voter)
     * @param optional Whether this vote is optional
     * @return The chosen vote option
     */
    public abstract Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer, boolean optional);

    /**
     * Decides whether to keep the current hand or take a mulligan.
     * Called during the mulligan phase at the start of each game.
     * Used with both old and new mulligan rules.
     *
     * @param player The player considering the mulligan
     * @param cardsToReturn Number of cards that would be returned if mulliganing
     * @return true to keep the hand, false to take a mulligan
     */
    public abstract boolean mulliganKeepHand(Player player, int cardsToReturn);
    /**
     * Chooses which cards to put back when taking a London mulligan.
     * With London mulligan rules, you draw 7, then put back cards equal to mulligan count.
     * Used in current official Magic rules (replaced Paris mulligan).
     *
     * @param mulliganingPlayer The player taking the mulligan
     * @param cardsToReturn Number of cards that must be put back
     * @return The cards chosen to be put back on the library
     */
    public abstract CardCollectionView londonMulliganReturnCards(Player mulliganingPlayer, int cardsToReturn);
    /**
     * Confirms whether to scry after mulliganing (if applicable by game rules).
     * Some mulligan rules include a scry 1 after keeping a mulliganed hand.
     * Used in formats that include post-mulligan scrying.
     *
     * @param p The player who can scry after mulliganing
     * @return true to scry, false to skip the scry
     */
    public abstract boolean confirmMulliganScry(final Player p);

    /**
     * Chooses which spell abilities to play during priority windows.
     * Called during main phase and other priority windows when player can act.
     * Returns multiple abilities for AI batching and complex decision making.
     *
     * @return List of spell abilities chosen to be played (can be empty)
     */
    public abstract List<SpellAbility> chooseSpellAbilityToPlay();
    /**
     * Confirms whether to actually play a spell ability that was chosen.
     * Called after targets are chosen to give final confirmation before playing.
     * Allows backing out if targeting fails or circumstances change.
     *
     * @param sa The spell ability ready to be played
     * @return true to play the ability, false to cancel
     */
    public abstract boolean playChosenSpellAbility(SpellAbility sa);

    /**
     * Chooses modes for modal spells and abilities.
     * Used by spells with "Choose one" or "Choose two" or similar modal effects.
     * Examples: Cryptic Command, Primal Command, Charms, modal double-faced cards.
     *
     * @param sa The modal spell/ability being cast
     * @param possible List of available modes to choose from
     * @param min Minimum number of modes that must be chosen
     * @param num Total number of modes to choose
     * @param allowRepeat Whether the same mode can be chosen multiple times
     * @return List of chosen modes (AbilitySub objects)
     */
    public abstract List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat);

    /**
     * Chooses how much to reduce a spell's cost when variable cost reduction is available.
     * Used by effects that can reduce costs by a chosen amount within a range.
     * Examples: Artifacts that reduce costs by X, cost reduction with choices.
     *
     * @param sa The spell whose cost is being reduced
     * @param min Minimum amount of reduction
     * @param max Maximum amount of reduction
     * @return The chosen amount of cost reduction
     */
    public abstract int chooseNumberForCostReduction(final SpellAbility sa, final int min, final int max);
    /**
     * Chooses how much to pay for variable keyword costs.
     * Used by keywords that have variable costs like Kicker, Multikicker, Entwine.
     * Examples: Multikicker (choose how many times to kick), Buyback with variable costs.
     *
     * @param sa The spell/ability with the keyword cost
     * @param cost The base cost of the spell
     * @param keyword The keyword interface defining the variable cost
     * @param prompt UI prompt describing the choice
     * @param max Maximum amount that can be paid
     * @return The chosen amount to pay for the keyword
     */
    public abstract int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max);
    public boolean addKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt) {
        return chooseNumberForKeywordCost(sa, cost, keyword, prompt, 1) == 1;
    }

    /**
     * Chooses a number within a specified range for various game effects.
     * One of the most general choice methods - used for many different numeric choices.
     * Examples: "Choose a number between 1 and 5", damage distribution, counter placement.
     *
     * @param sa The spell/ability requesting the number choice
     * @param title UI prompt describing what the number is for
     * @param min Minimum number that can be chosen
     * @param max Maximum number that can be chosen
     * @return The chosen number
     */
    public abstract int chooseNumber(SpellAbility sa, String title, int min, int max);
    /**
     * Chooses a number from a specific list of valid values.
     * Used when only certain numbers are valid choices (not a continuous range).
     * Examples: "Choose 1, 3, or 5", specific values based on game state.
     *
     * @param sa The spell/ability requesting the number choice
     * @param title UI prompt describing what the number is for
     * @param values List of valid numbers to choose from
     * @param relatedPlayer Player related to this choice (context)
     * @return The chosen number from the list
     */
    public abstract int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer);
    public int chooseNumber(SpellAbility sa, String string, int min, int max, Map<String, Object> params) {
        return chooseNumber(sa, string, min, max);
    }

    public final boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice) { return chooseBinary(sa, question, kindOfChoice, (Boolean) null); }
    /**
     * Makes a binary choice (two options) for various game effects.
     * Handles common yes/no, heads/tails, and either/or decisions.
     * Examples: Coin flips, "tap or untap", "play or draw", "odds or evens".
     *
     * @param sa The spell/ability requesting the choice
     * @param question The prompt describing the choice
     * @param kindOfChoice The type of binary choice (affects UI presentation)
     * @param defaultChoice Default value if no choice is made (can be null)
     * @return true for the first option, false for the second option
     */
    public abstract boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultChoice);
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Map<String, Object> params)  { return chooseBinary(sa, question, kindOfChoice); }

    /**
     * Chooses the result of a coin flip when effects allow choosing the outcome.
     * Used by effects that let you choose or call the result of coin flips.
     * Examples: Krark's Thumb, Chance Encounter, effects that rig coin flips.
     *
     * @param sa The spell/ability involving the coin flip
     * @param flipper The player flipping the coin
     * @param results Array of possible flip results
     * @param call Whether this is calling the flip (true) or choosing the result (false)
     * @return true for heads, false for tails
     */
    public abstract boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call);

    /**
     * Chooses a color from a set of available colors.
     * Used by many effects that ask players to choose colors.
     * Examples: "Choose a color", mana production choices, protection color choices.
     *
     * @param message UI prompt describing the color choice
     * @param sa The spell/ability requesting the color choice
     * @param colors The set of colors available to choose from
     * @return The chosen color as a byte value (Magic color constants)
     */
    public abstract byte chooseColor(String message, SpellAbility sa, ColorSet colors);
    /**
     * Chooses a color from available colors, with colorless as an option.
     * Similar to chooseColor but allows selecting colorless/generic.
     * Examples: Eldrazi effects, colorless mana production, generic color effects.
     *
     * @param message UI prompt describing the color choice
     * @param c The card context for the choice
     * @param colors The set of colors available (may include colorless)
     * @return The chosen color as a byte value (including possible colorless)
     */
    public abstract byte chooseColorAllowColorless(String message, Card c, ColorSet colors);
    /**
     * Chooses multiple colors from a list of color options.
     * Used when effects require selecting several colors at once.
     * Examples: "Choose two colors", multiple protection colors, Coalition Victory.
     *
     * @param message UI prompt describing the color choices
     * @param sa The spell/ability requesting the color choices
     * @param min Minimum number of colors that must be chosen
     * @param max Maximum number of colors that can be chosen
     * @param options List of color strings available to choose from
     * @return List of chosen color strings
     */
    public abstract List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options);

    /**
     * Chooses a card face when multiple faces are available (filtering by predicate).
     * Used for double-faced cards, split cards, and other multi-face cards.
     * Examples: Modal double-faced cards, split card selection, flip card choices.
     *
     * @param sa The spell/ability requesting the face choice
     * @param message UI prompt describing the choice
     * @param cpp Predicate filtering which faces are valid choices
     * @param name Context name for the choice
     * @return The chosen card face
     */
    public abstract ICardFace chooseSingleCardFace(SpellAbility sa, String message, Predicate<ICardFace> cpp, String name);
    /**
     * Chooses a card face from a specific list of available faces.
     * Similar to the predicate version but with a pre-filtered list.
     * Used when the valid faces have already been determined.
     *
     * @param sa The spell/ability requesting the face choice
     * @param faces List of card faces available to choose from
     * @param message UI prompt describing the choice
     * @return The chosen card face
     */
    public abstract ICardFace chooseSingleCardFace(SpellAbility sa, List<ICardFace> faces, String message);
    /**
     * Chooses a card state when a card has multiple possible states.
     * Used for effects that can affect different states of the same card.
     * Examples: Double-faced cards, cards with different printed states.
     *
     * @param sa The spell/ability requesting the state choice
     * @param states List of possible card states
     * @param message UI prompt describing the choice
     * @param params Additional context parameters
     * @return The chosen card state
     */
    public abstract CardState chooseSingleCardState(SpellAbility sa, List<CardState> states, String message, Map<String, Object> params);

    /**
     * Chooses between two piles of cards when given the choice.
     * Used by effects that separate cards into piles and let a player choose one.
     * Examples: Fact or Fiction, Gifts Ungiven, Truth or Tale.
     *
     * @param sa The spell/ability creating the pile choice
     * @param pile1 The first pile of cards
     * @param pile2 The second pile of cards
     * @param faceUp Whether the cards in the piles are face up
     * @return true to choose pile1, false to choose pile2
     */
    public abstract boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp);

    /**
     * Chooses a type of counter when multiple counter types are available.
     * Used by effects that can add or remove different types of counters.
     * Examples: Clockwork creatures, Vivid lands, Power Conduit, counter manipulation.
     *
     * @param options List of counter types available to choose from
     * @param sa The spell/ability involving counters
     * @param prompt UI prompt describing the counter choice
     * @param params Additional context parameters
     * @return The chosen counter type
     */
    public abstract CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, Map<String, Object> params);

    /**
     * Chooses a keyword ability to grant to a creature from pump effects.
     * Used by effects that can grant one of several keyword abilities.
     * Examples: "Choose one - flying, first strike, or trample", keyword-granting effects.
     *
     * @param options List of keyword strings available to grant
     * @param sa The spell/ability granting the keyword
     * @param prompt UI prompt describing the keyword choice
     * @param tgtCard The creature that will receive the keyword
     * @return The chosen keyword string
     */
    public abstract String chooseKeywordForPump(List<String> options, SpellAbility sa, String prompt, Card tgtCard);

    /**
     * Confirms whether to pay a specific part of a cost.
     * Used for optional cost components and "you may pay" effects.
     * Examples: Kicker costs, optional additional costs, Buyback.
     *
     * @param costPart The specific cost component to pay
     * @param string Description of what paying this cost will do
     * @param sa The spell/ability with the optional cost
     * @return true to pay the cost, false to decline
     */
    public abstract boolean confirmPayment(CostPart costPart, String string, SpellAbility sa);
    /**
     * Chooses which replacement effect to apply when multiple effects could replace the same event.
     * Called when multiple replacement effects are trying to replace the same event.
     * The affected player (or controller) chooses which replacement applies.
     * Examples: Multiple damage prevention effects, multiple enters-the-battlefield replacements.
     *
     * @param possibleReplacers List of replacement effects that could apply
     * @return The chosen replacement effect to apply
     */
    public abstract ReplacementEffect chooseSingleReplacementEffect(List<ReplacementEffect> possibleReplacers);
    /**
     * Chooses which static ability to apply when multiple static abilities conflict.
     * Called when multiple static abilities could apply and a choice must be made.
     * Examples: Multiple cost reduction effects, conflicting continuous effects.
     *
     * @param prompt UI prompt describing the choice
     * @param possibleReplacers List of static abilities that could apply
     * @return The chosen static ability to apply
     */
    public abstract StaticAbility chooseSingleStaticAbility(String prompt, List<StaticAbility> possibleReplacers);
    /**
     * Chooses what type of protection to grant when multiple options are available.
     * Used by effects that can grant protection from different things.
     * Examples: "Protection from the color of your choice", Akroma's Memorial variants.
     *
     * @param string Description of the protection choice
     * @param sa The spell/ability granting protection
     * @param choices List of protection types available (colors, card types, etc.)
     * @return The chosen protection type
     */
    public abstract String chooseProtectionType(String string, SpellAbility sa, List<String> choices);

    /**
     * Reveals ante cards that have been removed from the game.
     * Used in ante games to show what cards are being wagered.
     * Examples: Contract from Below, ante cards in old-school formats.
     *
     * @param message Description of why ante cards are being revealed
     * @param removedAnteCards Map of players to their ante cards
     */
    public abstract void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards);
    /**
     * Reveals cards that the AI has chosen to skip or exclude from play.
     * Used for AI deck optimization and to show players what the AI isn't using.
     * Helps with AI transparency and debugging.
     *
     * @param message Description of why AI cards are being revealed
     * @param deckCards Map of players to their deck sections and excluded cards
     */
    public abstract void revealAISkipCards(String message, Map<Player, Map<DeckSection, List<? extends PaperCard>>> deckCards);

    public abstract void revealUnsupported(Map<Player, List<PaperCard>> unsupported);

    // These 2 are for AI
    /**
     * Allows AI to "cheat" during shuffling for testing/debugging purposes.
     * Default implementation returns the list unchanged.
     * Only used by AI controllers for deterministic testing.
     *
     * @param list The list of cards to shuffle
     * @return The shuffled (or unchanged) list of cards
     */
    public CardCollectionView cheatShuffle(CardCollectionView list) { return list; }

    /**
     * Allows AI to report cards from its deck that it can't play well.
     * Used for AI deck analysis and optimization.
     * Returns null for human players.
     *
     * @param myDeck The deck to analyze
     * @return Map of deck sections to cards the AI struggles with, or null
     */
    public Map<DeckSection, List<? extends PaperCard>> complainCardsCantPlayWell(Deck myDeck) { return null; }

    /**
     * Resets controller state at the end of each turn.
     * Used primarily by AI to clear temporary memory and state.
     * Called during the cleanup step to prepare for the next turn.
     */
    public abstract void resetAtEndOfTurn();

    /**
     * Chooses which optional costs to pay when casting a spell.
     * Used when spells have multiple optional cost components.
     * Examples: Spells with both Kicker and Buyback, multiple optional costs.
     *
     * @param choosen The spell being cast
     * @param optionalCostValues List of optional costs that can be paid
     * @return List of optional costs chosen to be paid
     */
    public abstract List<OptionalCostValue> chooseOptionalCosts(SpellAbility choosen, List<OptionalCostValue> optionalCostValues);

    /**
     * Orders the payment of cost components when multiple costs must be paid.
     * Used when the order of paying costs matters for the game state.
     * Examples: Sacrificing creatures vs paying mana, order-dependent cost interactions.
     *
     * @param costs List of cost parts that need to be paid
     * @return The costs in the order they should be paid
     */
    public abstract List<CostPart> orderCosts(List<CostPart> costs);

    /**
     * Decides whether to pay a cost to prevent an effect from happening.
     * Used by effects that can be prevented by paying costs.
     * Examples: "Any player may pay {2} to counter this spell", prevention costs.
     *
     * @param cost The cost that can be paid to prevent the effect
     * @param sa The spell/ability that can be prevented
     * @param alreadyPaid Whether someone has already paid to prevent this
     * @param allPayers All players who can potentially pay the cost
     * @return true to pay the cost and prevent the effect, false to let it happen
     */
    public abstract boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers);
    /**
     * Decides whether to pay a cost during dice rolling effects.
     * Used by effects that allow paying costs to modify dice rolls.
     * Examples: "Pay {1}: Reroll this die", costs that affect dice outcomes.
     *
     * @param cost The cost that can be paid during the roll
     * @param sa The spell/ability involving dice rolling
     * @param allPayers All players who can potentially pay the cost
     * @return true to pay the cost, false to decline
     */
    public abstract boolean payCostDuringRoll(Cost cost, SpellAbility sa, FCollectionView<Player> allPayers);

    /**
     * Decides whether to pay additional costs related to combat.
     * Used for costs that must be paid during combat phases.
     * Examples: Attacking costs, blocking costs, combat-related activations.
     *
     * @param card The creature involved in combat
     * @param cost The combat-related cost to pay
     * @param sa The ability requiring the combat cost
     * @param prompt UI description of what the cost accomplishes
     * @return true to pay the combat cost, false to decline
     */
    public abstract boolean payCombatCost(Card card, Cost cost, SpellAbility sa, String prompt);

    public final boolean payManaCost(CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect) {
        return payManaCost(costPartMana.getManaCostFor(sa), costPartMana, sa, prompt, matrix, effect);
    }
    /**
     * Pays a mana cost interactively, choosing which mana sources to use.
     * The core mana payment method - handles all interactive mana payment.
     * Called for spells, abilities, and any effect requiring mana payment.
     *
     * @param toPay The mana cost that needs to be paid
     * @param costPartMana The cost component being paid (may have restrictions)
     * @param sa The spell/ability being cast/activated
     * @param prompt UI guidance for the payment
     * @param matrix Mana conversion rules (for hybrid costs, Phyrexian mana, etc.)
     * @param effect Whether this is paying for a spell (false) or an effect (true)
     * @return true if the cost was successfully paid, false if payment failed
     */
    public abstract boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect);

    /**
     * Chooses a card name from cards matching a predicate.
     * Used by effects that ask players to name cards with restrictions.
     * Examples: Cabal Therapy, Cranial Extraction, "name a card" effects with filters.
     *
     * @param sa The spell/ability requesting the card name
     * @param cpp Predicate defining which cards can be named
     * @param valid Additional validation string for the choice
     * @param message UI prompt for naming the card
     * @return The name of the chosen card
     */
    public abstract String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message);
    /**
     * Chooses a card name from a specific list of card faces.
     * Similar to the predicate version but with a pre-filtered list.
     * Used when the valid card names have already been determined.
     *
     * @param sa The spell/ability requesting the card name
     * @param faces List of card faces that can be named
     * @param message UI prompt for naming the card
     * @return The name of the chosen card
     */
    public abstract String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message);

    /**
     * Chooses which dungeon to venture into when venturing for the first time.
     * Used by the venture into the dungeon mechanic from Adventures in the Forgotten Realms.
     * Examples: Acererak the Archlich, Dungeon Crawler, any "venture into the dungeon" effect.
     *
     * @param player The player venturing into the dungeon
     * @param dungeonCards List of available dungeons to venture into
     * @param message UI prompt for choosing the dungeon
     * @return The chosen dungeon card
     */
    public abstract Card chooseDungeon(Player player, List<PaperCard> dungeonCards, String message);
    /**
     * Chooses a single card for a zone change operation (like library searching).
     * Used when effects move cards between zones and player choice is involved.
     * Examples: Tutoring effects, "search your library for a card", zone-to-zone transfers.
     * Note: This method exists to avoid complex playerType comparisons in ChangeZone.
     *
     * @param destination The zone the card will move to
     * @param origin List of zones the card can come from
     * @param sa The spell/ability causing the zone change
     * @param fetchList Cards available to choose from for the zone change
     * @param delayedReveal Cards to reveal before making the choice
     * @param selectPrompt UI prompt for the selection
     * @param isOptional Whether choosing a card is optional
     * @param decider The player making the choice (may differ from controller)
     * @return The chosen card to move between zones
     */
    public abstract Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider);
    /**
     * Chooses multiple cards for a zone change operation.
     * Like chooseSingleCardForZoneChange but allows selecting multiple cards.
     * Examples: "Search your library for up to two cards", mass tutoring effects.
     *
     * @param destination The zone the cards will move to
     * @param origin List of zones the cards can come from
     * @param sa The spell/ability causing the zone change
     * @param fetchList Cards available to choose from
     * @param min Minimum number of cards that must be chosen
     * @param max Maximum number of cards that can be chosen
     * @param delayedReveal Cards to reveal before making choices
     * @param selectPrompt UI prompt for the selection
     * @param decider The player making the choice
     * @return List of chosen cards to move between zones
     */
    public abstract List<Card> chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider);

    public Set<FullControlFlag> getFullControl() {
        return fullControls;
    }
    public boolean isFullControl(FullControlFlag f) {
        return fullControls.contains(f);
    }

    /**
     * Cancels any automatic passing of priority that may be in effect.
     * Used to interrupt AI or automatic play when manual intervention is needed.
     * Called when players want to regain control during automated sequences.
     */
    public abstract void autoPassCancel();

    /**
     * Waits for the next input from this player controller.
     * Used to pause game execution until the player makes a decision.
     * Part of the input/UI synchronization system.
     */
    public abstract void awaitNextInput();
    /**
     * Cancels waiting for input from this player controller.
     * Used to interrupt input waiting when game state changes or timeouts occur.
     * Part of the input/UI synchronization system.
     */
    public abstract void cancelAwaitNextInput();

    /**
     * Resets the input state of this controller.
     * Default implementation does nothing - overridden by subclasses that need input cleanup.
     * Used to clear pending inputs and reset UI state.
     */
    public void resetInputs() {
        // Do nothing unless overridden by a subclass
    }

    /**
     * Returns whether this controller represents a GUI (human) player.
     * Used to determine if this player uses a graphical interface vs AI.
     * Default implementation returns false (AI player).
     *
     * @return true if this is a GUI player, false for AI
     */
    public boolean isGuiPlayer() {
        return false;
    }

    /**
     * Returns whether this controller can play unlimited lands per turn.
     * Used for special game modes or testing where land drop limits are removed.
     * Default implementation returns false (normal land drop rules apply).
     *
     * @return true if unlimited land drops are allowed, false for normal rules
     */
    public boolean canPlayUnlimitedLands() {
        return false;
    }

    /**
     * Gets the ante result for this player from the current game.
     * Used in ante games to determine what cards this player has won or lost.
     *
     * @return The ante result showing cards won/lost in ante
     */
    public AnteResult getAnteResult() {
        return gameView.getAnteResult(player.getView());
    }

    /**
     * Returns whether this controller maintains ordered zones (like libraries).
     * Used to determine if zone ordering matters for this player type.
     * Default implementation returns false.
     *
     * @return true if zones should maintain strict ordering, false otherwise
     */
    public boolean isOrderedZone() { return false; }
}
