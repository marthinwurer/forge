package forge.ai;

import com.google.common.collect.*;
import forge.LobbyPlayer;
import forge.card.CardStateName;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.*;
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
import forge.game.player.*;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;

/**
 * Random PlayerController implementation that makes random legal choices for every decision.
 * 
 * This controller is designed for testing purposes and makes completely random decisions
 * within the constraints of legal game actions. Each decision point is marked with
 * the comment "RANDOM_CHOICE_POINT" for easy searching.
 */
public class RandomPlayerController extends PlayerController {
    private final Random random;
    
    public RandomPlayerController(Game game, Player p, LobbyPlayer lp) {
        super(game, p, lp);
        this.random = new Random();
    }
    
    public RandomPlayerController(Game game, Player p, LobbyPlayer lp, long seed) {
        super(game, p, lp);
        this.random = new Random(seed);
    }
    
    @Override
    public boolean isAI() {
        return true;
    }
    
    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        // RANDOM_CHOICE_POINT: Choose random ability from available abilities
        if (abilities.isEmpty()) {
            return null;
        }
        return abilities.get(random.nextInt(abilities.size()));
    }
    
    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to choose new targets if allowed
        if (mayChoseNewTargets && random.nextBoolean()) {
            chooseTargetsFor(effectSA);
        }
        effectSA.resolve();
    }
    
    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        // RANDOM_CHOICE_POINT: Randomly shuffle the order of simultaneous abilities
        List<SpellAbility> shuffled = new ArrayList<>(activePlayerSAs);
        Collections.shuffle(shuffled, random);
        
        for (SpellAbility sa : shuffled) {
            if (sa.canPlay()) {
                sa.setActivatingPlayer(player);
                getGame().getStack().add(sa);
            }
        }
    }
    
    @Override
    public boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to play optional triggers
        if (isMandatory) {
            return true;
        }
        return random.nextBoolean();
    }
    
    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to cast spells from play effects
        return random.nextBoolean();
    }
    
    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType, String message) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to sideboard in/out
        List<PaperCard> sideboardCards = new ArrayList<>();
        if (deck.get(DeckSection.Sideboard) != null) {
            List<PaperCard> availableSideboard = new ArrayList<>(deck.get(DeckSection.Sideboard).toFlatList());
            int numToSideboard = random.nextInt(Math.min(availableSideboard.size(), 15) + 1);
            Collections.shuffle(availableSideboard, random);
            sideboardCards.addAll(availableSideboard.subList(0, numToSideboard));
        }
        return sideboardCards;
    }
    
    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        // RANDOM_CHOICE_POINT: Randomly choose which ante cards to add to deck
        List<PaperCard> chosen = new ArrayList<>();
        for (PaperCard card : losses) {
            if (random.nextBoolean()) {
                chosen.add(card);
            }
        }
        return chosen;
    }
    
    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, 
            CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder) {
        // RANDOM_CHOICE_POINT: Randomly assign combat damage (following lethal damage rules)
        Map<Card, Integer> assignment = new HashMap<>();
        int remainingDamage = damageDealt;
        
        List<Card> targets = new ArrayList<>(remaining);
        if (!overrideOrder) {
            // Use established blocking order
        } else {
            // RANDOM_CHOICE_POINT: Randomly order damage assignment when override is allowed
            Collections.shuffle(targets, random);
        }
        
        // Assign damage following rules (lethal to each blocker before next)
        for (Card blocker : targets) {
            if (remainingDamage <= 0) break;
            
            int lethalDamage = Math.max(1, blocker.getNetToughness() - blocker.getDamage());
            int assignedDamage = Math.min(remainingDamage, lethalDamage);
            
            // RANDOM_CHOICE_POINT: Randomly decide to assign more than lethal damage sometimes
            if (remainingDamage > lethalDamage && random.nextBoolean()) {
                int extraDamage = random.nextInt(remainingDamage - lethalDamage + 1);
                assignedDamage += extraDamage;
            }
            
            assignment.put(blocker, assignedDamage);
            remainingDamage -= assignedDamage;
        }
        
        // Assign remaining damage to defender if any left
        if (remainingDamage > 0 && defender != null) {
            // For planeswalkers, convert to card if needed
            if (defender instanceof Card) {
                assignment.put((Card) defender, remainingDamage);
            }
        }
        
        return assignment;
    }
    
    @Override
    public Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount) {
        // RANDOM_CHOICE_POINT: Randomly distribute shield counters among targets
        Map<GameEntity, Integer> result = new HashMap<>();
        List<GameEntity> targets = new ArrayList<>(affected.keySet());
        int remaining = shieldAmount;
        
        while (remaining > 0 && !targets.isEmpty()) {
            GameEntity target = targets.get(random.nextInt(targets.size()));
            int amount = random.nextInt(remaining) + 1;
            result.put(target, result.getOrDefault(target, 0) + amount);
            remaining -= amount;
        }
        
        return result;
    }
    
    @Override
    public Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different) {
        // RANDOM_CHOICE_POINT: Randomly choose mana color combination
        Map<Byte, Integer> result = new HashMap<>();
        List<Byte> availableColors = new ArrayList<>();
        
        for (byte color : MagicColor.WUBRG) {
            if (colorSet.hasAnyColor(color)) {
                availableColors.add(color);
            }
        }
        
        int remaining = manaAmount;
        Set<Byte> usedColors = new HashSet<>();
        
        while (remaining > 0) {
            List<Byte> validColors = new ArrayList<>(availableColors);
            if (different) {
                validColors.removeAll(usedColors);
                if (validColors.isEmpty()) break;
            }
            
            byte chosenColor = validColors.get(random.nextInt(validColors.size()));
            int amount = different ? 1 : random.nextInt(remaining) + 1;
            
            result.put(chosenColor, result.getOrDefault(chosenColor, 0) + amount);
            usedColors.add(chosenColor);
            remaining -= amount;
        }
        
        return result;
    }
    
    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, 
            CardCollectionView validTargets, String message) {
        // RANDOM_CHOICE_POINT: Randomly choose permanents to sacrifice
        CardCollection result = new CardCollection();
        List<Card> available = new ArrayList<>(validTargets);
        Collections.shuffle(available, random);
        
        int numToSacrifice = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToSacrifice = Math.min(numToSacrifice, available.size());
        
        for (int i = 0; i < numToSacrifice; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, 
            CardCollectionView validTargets, String message) {
        // RANDOM_CHOICE_POINT: Randomly choose permanents to destroy
        CardCollection result = new CardCollection();
        List<Card> available = new ArrayList<>(validTargets);
        Collections.shuffle(available, random);
        
        int numToDestroy = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToDestroy = Math.min(numToDestroy, available.size());
        
        for (int i = 0; i < numToDestroy; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public Integer announceRequirements(SpellAbility ability, String announce) {
        // RANDOM_CHOICE_POINT: Randomly choose X value or other announced requirement
        // Cap at reasonable values to avoid infinite loops or game breaks
        return random.nextInt(10);
    }
    
    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        // RANDOM_CHOICE_POINT: Randomly choose new targets for spell redirection
        if (optional && random.nextBoolean()) {
            return null; // Choose not to retarget
        }
        
        TargetChoices newTargets = new TargetChoices();
        SpellAbility rootAbility = ability.getRootAbility();
        
        for (TargetRestrictions tgt : rootAbility.getAllTargetRestrictions()) {
            List<GameObject> validTargets = new ArrayList<>();
            
            // Find valid targets that match the filter
            for (GameObject obj : getGame().getCardsIn(ZoneType.Battlefield)) {
                if (obj.canBeTargetedBy(ability) && filter.test(obj)) {
                    validTargets.add(obj);
                }
            }
            
            // Add players as potential targets
            for (Player p : getGame().getPlayers()) {
                if (p.canBeTargetedBy(ability) && filter.test(p)) {
                    validTargets.add(p);
                }
            }
            
            if (!validTargets.isEmpty()) {
                GameObject chosen = validTargets.get(random.nextInt(validTargets.size()));
                newTargets.add(chosen);
            }
        }
        
        return newTargets;
    }
    
    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        // RANDOM_CHOICE_POINT: Randomly choose targets for spell or ability
        boolean hasValidTargets = false;
        
        for (TargetRestrictions tgt : currentAbility.getAllTargetRestrictions()) {
            List<GameObject> validTargets = new ArrayList<>();
            
            // Find all valid targets
            for (GameObject obj : getGame().getCardsIn(ZoneType.Battlefield)) {
                if (obj.canBeTargetedBy(currentAbility)) {
                    validTargets.add(obj);
                }
            }
            
            for (Player p : getGame().getPlayers()) {
                if (p.canBeTargetedBy(currentAbility)) {
                    validTargets.add(p);
                }
            }
            
            if (!validTargets.isEmpty()) {
                // RANDOM_CHOICE_POINT: Randomly select from valid targets
                int numTargets = Math.min(tgt.getMaxTargets(currentAbility.getHostCard(), currentAbility), validTargets.size());
                Collections.shuffle(validTargets, random);
                
                TargetChoices targets = new TargetChoices();
                for (int i = 0; i < numTargets; i++) {
                    targets.add(validTargets.get(i));
                }
                currentAbility.setTargets(targets);
                hasValidTargets = true;
            }
        }
        
        return hasValidTargets;
    }
    
    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, 
            List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        // RANDOM_CHOICE_POINT: Randomly choose target from stack instances
        if (allTargets.isEmpty()) {
            return null;
        }
        return allTargets.get(random.nextInt(allTargets.size()));
    }
    
    @Override
    public boolean helpPayForAssistSpell(ManaCostBeingPaid cost, SpellAbility sa, int max, int requested) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to help pay for assist spells
        return random.nextBoolean();
    }
    
    @Override
    public Player choosePlayerToAssistPayment(FCollectionView<Player> optionList, SpellAbility sa, String title, int max) {
        // RANDOM_CHOICE_POINT: Randomly choose player to assist with payment
        if (optionList.isEmpty()) {
            return null;
        }
        return optionList.get(random.nextInt(optionList.size()));
    }
    
    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, 
            int min, int max, boolean isOptional, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly choose cards for various effects
        if (isOptional && random.nextBoolean()) {
            return CardCollection.EMPTY;
        }
        
        CardCollection result = new CardCollection();
        List<Card> available = new ArrayList<>(sourceList);
        Collections.shuffle(available, random);
        
        int numToChoose = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToChoose = Math.min(numToChoose, available.size());
        
        for (int i = 0; i < numToChoose; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap, SpellAbility sa, String title, boolean isOptional) {
        // RANDOM_CHOICE_POINT: Randomly choose cards from multiple collections
        if (isOptional && random.nextBoolean()) {
            return new CardCollection();
        }
        
        CardCollection result = new CardCollection();
        for (CardCollection cards : validMap.values()) {
            if (!cards.isEmpty() && random.nextBoolean()) {
                result.add(cards.get(random.nextInt(cards.size())));
            }
        }
        return result;
    }
    
    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, 
            SpellAbility sa, String title, boolean isOptional, Player relatedPlayer, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly choose single entity from options
        if (isOptional && random.nextBoolean()) {
            return null;
        }
        if (optionList.isEmpty()) {
            return null;
        }
        return optionList.get(random.nextInt(optionList.size()));
    }
    
    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, 
            DelayedReveal delayedReveal, SpellAbility sa, String title, Player relatedPlayer, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly choose multiple entities from options
        List<T> result = new ArrayList<>();
        List<T> available = new ArrayList<>(optionList);
        Collections.shuffle(available, random);
        
        int numToChoose = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToChoose = Math.min(numToChoose, available.size());
        
        for (int i = 0; i < numToChoose; i++) {
            result.add(available.get(i));
        }
        return result;
    }
    
    @Override
    public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title, int num, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly choose spell abilities from list
        List<SpellAbility> result = new ArrayList<>();
        List<SpellAbility> available = new ArrayList<>(spells);
        Collections.shuffle(available, random);
        
        int numToChoose = Math.min(num, available.size());
        for (int i = 0; i < numToChoose; i++) {
            result.add(available.get(i));
        }
        return result;
    }
    
    @Override
    public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly choose single spell ability
        if (spells.isEmpty()) {
            return null;
        }
        return spells.get(random.nextInt(spells.size()));
    }
    
    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly confirm or deny actions
        return random.nextBoolean();
    }
    
    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner) {
        // RANDOM_CHOICE_POINT: Randomly confirm bid actions
        return random.nextBoolean();
    }
    
    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question) {
        // RANDOM_CHOICE_POINT: Randomly confirm replacement effects
        return random.nextBoolean();
    }
    
    @Override
    public boolean confirmStaticApplication(Card hostCard, PlayerActionConfirmMode mode, String message, String logic) {
        // RANDOM_CHOICE_POINT: Randomly confirm static ability applications
        return random.nextBoolean();
    }
    
    @Override
    public boolean confirmTrigger(WrappedAbility sa) {
        // RANDOM_CHOICE_POINT: Randomly confirm optional triggers
        return random.nextBoolean();
    }
    
    @Override
    public List<Card> exertAttackers(List<Card> attackers) {
        // RANDOM_CHOICE_POINT: Randomly choose which attackers to exert
        List<Card> result = new ArrayList<>();
        for (Card attacker : attackers) {
            if (random.nextBoolean()) {
                result.add(attacker);
            }
        }
        return result;
    }
    
    @Override
    public List<Card> enlistAttackers(List<Card> attackers) {
        // RANDOM_CHOICE_POINT: Randomly choose which attackers to enlist
        List<Card> result = new ArrayList<>();
        for (Card attacker : attackers) {
            if (random.nextBoolean()) {
                result.add(attacker);
            }
        }
        return result;
    }
    
    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        // RANDOM_CHOICE_POINT: Randomly choose attackers for combat
        CardCollection creatures = attacker.getCreaturesInPlay();
        for (Card creature : creatures) {
            if (creature.canAttack() && random.nextBoolean()) {
                GameEntity defender = getRandomDefender(attacker);
                if (defender != null) {
                    combat.addAttacker(creature, defender);
                }
            }
        }
    }
    
    @Override
    public void declareBlockers(Player defender, Combat combat) {
        // RANDOM_CHOICE_POINT: Randomly choose blockers for combat
        CardCollection creatures = defender.getCreaturesInPlay();
        for (Card creature : creatures) {
            if (creature.canBlock() && random.nextBoolean()) {
                CardCollection attackers = combat.getUnblocked();
                if (!attackers.isEmpty()) {
                    Card attacker = attackers.get(random.nextInt(attackers.size()));
                    if (combat.canBlock(attacker, creature)) {
                        combat.addBlocker(attacker, creature);
                    }
                }
            }
        }
    }
    
    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        // RANDOM_CHOICE_POINT: Randomly order blockers for damage assignment
        CardCollection ordered = new CardCollection(blockers);
        Collections.shuffle(ordered, random);
        return ordered;
    }
    
    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
        // RANDOM_CHOICE_POINT: Randomly order single blocker among existing blockers
        CardCollection newOrder = new CardCollection(oldBlockers);
        newOrder.add(random.nextInt(newOrder.size() + 1), blocker);
        return newOrder;
    }
    
    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        // RANDOM_CHOICE_POINT: Randomly order attackers blocked by this creature
        CardCollection ordered = new CardCollection(attackers);
        Collections.shuffle(ordered, random);
        return ordered;
    }
    
    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix, boolean addMsgSuffix) {
        // No random choice needed - just reveal the cards
    }
    
    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix, boolean addMsgSuffix) {
        // No random choice needed - just reveal the cards
    }
    
    @Override
    public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value) {
        // No random choice needed - just notification
    }
    
    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        // RANDOM_CHOICE_POINT: Randomly choose which cards to put on top vs bottom for scry
        CardCollection toTop = new CardCollection();
        CardCollection toBottom = new CardCollection();
        
        for (Card card : topN) {
            if (random.nextBoolean()) {
                toTop.add(card);
            } else {
                toBottom.add(card);
            }
        }
        
        // Randomly shuffle the order within each pile
        Collections.shuffle(toTop, random);
        Collections.shuffle(toBottom, random);
        
        return ImmutablePair.of(toTop, toBottom);
    }
    
    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        // RANDOM_CHOICE_POINT: Randomly choose which cards to put in graveyard vs on top for surveil
        CardCollection toGraveyard = new CardCollection();
        CardCollection toTop = new CardCollection();
        
        for (Card card : topN) {
            if (random.nextBoolean()) {
                toGraveyard.add(card);
            } else {
                toTop.add(card);
            }
        }
        
        Collections.shuffle(toTop, random);
        return ImmutablePair.of(toGraveyard, toTop);
    }
    
    @Override
    public boolean willPutCardOnTop(Card c) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to put card on top of library
        return random.nextBoolean();
    }
    
    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source) {
        // RANDOM_CHOICE_POINT: Randomly order cards moving to a zone
        CardCollection ordered = new CardCollection(cards);
        Collections.shuffle(ordered, random);
        return ordered;
    }
    
    @Override
    public CardCollectionView chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, CardCollection validCards, int min, int max) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to discard
        CardCollection result = new CardCollection();
        List<Card> available = new ArrayList<>(validCards);
        Collections.shuffle(available, random);
        
        int numToDiscard = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToDiscard = Math.min(numToDiscard, available.size());
        
        for (int i = 0; i < numToDiscard; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int min, CardCollectionView hand, String param, SpellAbility sa) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to discard unless they match type
        CardCollection result = new CardCollection();
        List<Card> available = new ArrayList<>(hand);
        Collections.shuffle(available, random);
        
        for (int i = 0; i < min && i < available.size(); i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to discard to hand size
        CardCollection result = new CardCollection();
        List<Card> hand = new ArrayList<>(player.getCardsIn(ZoneType.Hand));
        Collections.shuffle(hand, random);
        
        for (int i = 0; i < numDiscard && i < hand.size(); i++) {
            result.add(hand.get(i));
        }
        
        return result;
    }
    
    @Override
    public CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to delve from graveyard
        CardCollection result = new CardCollection();
        List<Card> available = new ArrayList<>(grave);
        Collections.shuffle(available, random);
        
        int numToDelve = Math.min(genericAmount, available.size());
        for (int i = 0; i < numToDelve; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to tap for convoke/improvise
        Map<Card, ManaCostShard> result = new HashMap<>();
        List<Card> available = new ArrayList<>(untappedCards);
        Collections.shuffle(available, random);
        
        List<ManaCostShard> shards = new ArrayList<>(manaCost);
        Collections.shuffle(shards, random);
        
        int maxCards = Math.min(available.size(), shards.size());
        for (int i = 0; i < maxCards && random.nextBoolean(); i++) {
            result.put(available.get(i), shards.get(i));
        }
        
        return result;
    }
    
    @Override
    public List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to splice onto arcane spell
        List<Card> result = new ArrayList<>();
        for (Card card : cards) {
            if (random.nextBoolean()) {
                result.add(card);
            }
        }
        return result;
    }
    
    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to reveal from hand
        CardCollection result = new CardCollection();
        List<Card> available = new ArrayList<>(valid);
        Collections.shuffle(available, random);
        
        int numToReveal = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToReveal = Math.min(numToReveal, available.size());
        
        for (int i = 0; i < numToReveal; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        // RANDOM_CHOICE_POINT: Randomly choose abilities to activate from opening hand
        List<SpellAbility> result = new ArrayList<>();
        for (SpellAbility sa : usableFromOpeningHand) {
            if (random.nextBoolean()) {
                result.add(sa);
            }
        }
        return result;
    }
    
    @Override
    public Player chooseStartingPlayer(boolean isFirstGame) {
        // RANDOM_CHOICE_POINT: Randomly choose starting player
        List<Player> players = new ArrayList<>(getGame().getPlayers());
        return players.get(random.nextInt(players.size()));
    }
    
    @Override
    public PlayerZone chooseStartingHand(List<PlayerZone> zones) {
        // RANDOM_CHOICE_POINT: Randomly choose starting hand zone
        if (zones.isEmpty()) {
            return null;
        }
        return zones.get(random.nextInt(zones.size()));
    }
    
    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        // RANDOM_CHOICE_POINT: Randomly choose mana from pool
        if (manaChoices.isEmpty()) {
            return null;
        }
        return manaChoices.get(random.nextInt(manaChoices.size()));
    }
    
    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, boolean isOptional) {
        // RANDOM_CHOICE_POINT: Randomly choose type (creature type, card type, etc.)
        if (isOptional && random.nextBoolean()) {
            return null;
        }
        if (validTypes.isEmpty()) {
            return null;
        }
        List<String> types = new ArrayList<>(validTypes);
        return types.get(random.nextInt(types.size()));
    }
    
    @Override
    public String chooseSector(Card assignee, String ai, List<String> sectors) {
        // RANDOM_CHOICE_POINT: Randomly choose sector for Unfinity mechanics
        if (sectors.isEmpty()) {
            return null;
        }
        return sectors.get(random.nextInt(sectors.size()));
    }
    
    @Override
    public List<Card> chooseContraptionsToCrank(List<Card> contraptions) {
        // RANDOM_CHOICE_POINT: Randomly choose contraptions to crank
        List<Card> result = new ArrayList<>();
        for (Card contraption : contraptions) {
            if (random.nextBoolean()) {
                result.add(contraption);
            }
        }
        return result;
    }
    
    @Override
    public int chooseSprocket(Card assignee, boolean forceDifferent) {
        // RANDOM_CHOICE_POINT: Randomly choose sprocket for Unfinity mechanics
        return random.nextInt(6) + 1; // 1-6 like a die roll
    }
    
    @Override
    public PlanarDice choosePDRollToIgnore(List<PlanarDice> rolls) {
        // RANDOM_CHOICE_POINT: Randomly choose planar die roll to ignore
        if (rolls.isEmpty()) {
            return null;
        }
        return rolls.get(random.nextInt(rolls.size()));
    }
    
    @Override
    public Integer chooseRollToIgnore(List<Integer> rolls) {
        // RANDOM_CHOICE_POINT: Randomly choose die roll to ignore
        if (rolls.isEmpty()) {
            return null;
        }
        return rolls.get(random.nextInt(rolls.size()));
    }
    
    @Override
    public List<Integer> chooseDiceToReroll(List<Integer> rolls) {
        // RANDOM_CHOICE_POINT: Randomly choose dice to reroll
        List<Integer> result = new ArrayList<>();
        for (Integer roll : rolls) {
            if (random.nextBoolean()) {
                result.add(roll);
            }
        }
        return result;
    }
    
    @Override
    public Integer chooseRollToModify(List<Integer> rolls) {
        // RANDOM_CHOICE_POINT: Randomly choose die roll to modify
        if (rolls.isEmpty()) {
            return null;
        }
        return rolls.get(random.nextInt(rolls.size()));
    }
    
    @Override
    public RollDiceEffect.DieRollResult chooseRollToSwap(List<RollDiceEffect.DieRollResult> rolls) {
        // RANDOM_CHOICE_POINT: Randomly choose die roll result to swap
        if (rolls.isEmpty()) {
            return null;
        }
        return rolls.get(random.nextInt(rolls.size()));
    }
    
    @Override
    public String chooseRollSwapValue(List<String> swapChoices, Integer currentResult, int power, int toughness) {
        // RANDOM_CHOICE_POINT: Randomly choose value to swap die roll with
        if (swapChoices.isEmpty()) {
            return null;
        }
        return swapChoices.get(random.nextInt(swapChoices.size()));
    }
    
    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer, boolean optional) {
        // RANDOM_CHOICE_POINT: Randomly vote on council's dilemma effects
        if (optional && random.nextBoolean()) {
            return null;
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }
    
    @Override
    public boolean mulliganKeepHand(Player player, int cardsToReturn) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to keep opening hand
        return random.nextBoolean();
    }
    
    @Override
    public CardCollectionView londonMulliganReturnCards(Player mulliganingPlayer, int cardsToReturn) {
        // RANDOM_CHOICE_POINT: Randomly choose cards to put back for London mulligan
        CardCollection result = new CardCollection();
        List<Card> hand = new ArrayList<>(mulliganingPlayer.getCardsIn(ZoneType.Hand));
        Collections.shuffle(hand, random);
        
        for (int i = 0; i < cardsToReturn && i < hand.size(); i++) {
            result.add(hand.get(i));
        }
        
        return result;
    }
    
    @Override
    public boolean confirmMulliganScry(Player p) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to scry after mulligan
        return random.nextBoolean();
    }
    
    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        // RANDOM_CHOICE_POINT: Randomly choose spell abilities to play during priority
        List<SpellAbility> playable = new ArrayList<>();
        
        // Get all playable spells and abilities
        for (Card card : player.getAllCards()) {
            for (SpellAbility sa : card.getSpellAbilities()) {
                if (sa.canPlay()) {
                    playable.add(sa);
                }
            }
        }
        
        List<SpellAbility> result = new ArrayList<>();
        for (SpellAbility sa : playable) {
            if (random.nextBoolean()) {
                result.add(sa);
                break; // Only play one at a time for simplicity
            }
        }
        
        return result;
    }
    
    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to actually play the chosen spell
        return random.nextBoolean();
    }
    
    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat) {
        // RANDOM_CHOICE_POINT: Randomly choose modes for modal spells
        List<AbilitySub> result = new ArrayList<>();
        List<AbilitySub> available = new ArrayList<>(possible);
        
        for (int i = 0; i < num && !available.isEmpty(); i++) {
            AbilitySub chosen = available.get(random.nextInt(available.size()));
            result.add(chosen);
            if (!allowRepeat) {
                available.remove(chosen);
            }
        }
        
        return result;
    }
    
    @Override
    public int chooseNumberForCostReduction(SpellAbility sa, int min, int max) {
        // RANDOM_CHOICE_POINT: Randomly choose amount for cost reduction
        return min + random.nextInt(max - min + 1);
    }
    
    @Override
    public int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max) {
        // RANDOM_CHOICE_POINT: Randomly choose number for keyword costs (kicker, multikicker, etc.)
        return random.nextInt(max + 1);
    }
    
    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        // RANDOM_CHOICE_POINT: Randomly choose number in range
        return min + random.nextInt(max - min + 1);
    }
    
    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer) {
        // RANDOM_CHOICE_POINT: Randomly choose number from specific values
        if (values.isEmpty()) {
            return 0;
        }
        return values.get(random.nextInt(values.size()));
    }
    
    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultChoice) {
        // RANDOM_CHOICE_POINT: Randomly make binary choices (heads/tails, tap/untap, etc.)
        return random.nextBoolean();
    }
    
    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        // RANDOM_CHOICE_POINT: Randomly call coin flip result
        return random.nextBoolean();
    }
    
    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        // RANDOM_CHOICE_POINT: Randomly choose color from available colors
        List<Byte> availableColors = new ArrayList<>();
        for (byte color : MagicColor.WUBRG) {
            if (colors.hasAnyColor(color)) {
                availableColors.add(color);
            }
        }
        if (availableColors.isEmpty()) {
            return 0;
        }
        return availableColors.get(random.nextInt(availableColors.size()));
    }
    
    @Override
    public byte chooseColorAllowColorless(String message, Card c, ColorSet colors) {
        // RANDOM_CHOICE_POINT: Randomly choose color including colorless option
        List<Byte> availableColors = new ArrayList<>();
        availableColors.add((byte) 0); // Colorless option
        for (byte color : MagicColor.WUBRG) {
            if (colors.hasAnyColor(color)) {
                availableColors.add(color);
            }
        }
        return availableColors.get(random.nextInt(availableColors.size()));
    }
    
    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        // RANDOM_CHOICE_POINT: Randomly choose multiple colors from options
        List<String> result = new ArrayList<>();
        List<String> available = new ArrayList<>(options);
        Collections.shuffle(available, random);
        
        int numToChoose = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToChoose = Math.min(numToChoose, available.size());
        
        for (int i = 0; i < numToChoose; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, String message, Predicate<ICardFace> cpp, String name) {
        // RANDOM_CHOICE_POINT: Randomly choose card face matching predicate
        List<ICardFace> validFaces = new ArrayList<>();
        
        // This would need access to all card faces in the game, simplified implementation
        for (Card card : getGame().getCardsInGame()) {
            for (CardStateName state : card.getStates()) {
                ICardFace face = card.getState(state);
                if (cpp.test(face)) {
                    validFaces.add(face);
                }
            }
        }
        
        if (validFaces.isEmpty()) {
            return null;
        }
        return validFaces.get(random.nextInt(validFaces.size()));
    }
    
    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, List<ICardFace> faces, String message) {
        // RANDOM_CHOICE_POINT: Randomly choose card face from list
        if (faces.isEmpty()) {
            return null;
        }
        return faces.get(random.nextInt(faces.size()));
    }
    
    @Override
    public CardState chooseSingleCardState(SpellAbility sa, List<CardState> states, String message, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly choose card state
        if (states.isEmpty()) {
            return null;
        }
        return states.get(random.nextInt(states.size()));
    }
    
    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp) {
        // RANDOM_CHOICE_POINT: Randomly choose between two piles of cards
        return random.nextBoolean();
    }
    
    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, Map<String, Object> params) {
        // RANDOM_CHOICE_POINT: Randomly choose counter type
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }
    
    @Override
    public String chooseKeywordForPump(List<String> options, SpellAbility sa, String prompt, Card tgtCard) {
        // RANDOM_CHOICE_POINT: Randomly choose keyword to grant
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }
    
    @Override
    public boolean confirmPayment(CostPart costPart, String string, SpellAbility sa) {
        // RANDOM_CHOICE_POINT: Randomly confirm payment of costs
        return random.nextBoolean();
    }
    
    @Override
    public ReplacementEffect chooseSingleReplacementEffect(List<ReplacementEffect> possibleReplacers) {
        // RANDOM_CHOICE_POINT: Randomly choose replacement effect to apply
        if (possibleReplacers.isEmpty()) {
            return null;
        }
        return possibleReplacers.get(random.nextInt(possibleReplacers.size()));
    }
    
    @Override
    public StaticAbility chooseSingleStaticAbility(String prompt, List<StaticAbility> possibleReplacers) {
        // RANDOM_CHOICE_POINT: Randomly choose static ability
        if (possibleReplacers.isEmpty()) {
            return null;
        }
        return possibleReplacers.get(random.nextInt(possibleReplacers.size()));
    }
    
    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        // RANDOM_CHOICE_POINT: Randomly choose protection type
        if (choices.isEmpty()) {
            return null;
        }
        return choices.get(random.nextInt(choices.size()));
    }
    
    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        // No random choice needed - just reveal ante
    }
    
    @Override
    public void revealAISkipCards(String message, Map<Player, Map<DeckSection, List<? extends PaperCard>>> deckCards) {
        // No random choice needed - just reveal AI skip cards
    }
    
    @Override
    public void resetAtEndOfTurn() {
        // No random choice needed - cleanup method
    }
    
    @Override
    public List<OptionalCostValue> chooseOptionalCosts(SpellAbility choosen, List<OptionalCostValue> optionalCostValues) {
        // RANDOM_CHOICE_POINT: Randomly choose which optional costs to pay
        List<OptionalCostValue> result = new ArrayList<>();
        for (OptionalCostValue cost : optionalCostValues) {
            if (random.nextBoolean()) {
                result.add(cost);
            }
        }
        return result;
    }
    
    @Override
    public List<CostPart> orderCosts(List<CostPart> costs) {
        // RANDOM_CHOICE_POINT: Randomly order costs for payment
        List<CostPart> ordered = new ArrayList<>(costs);
        Collections.shuffle(ordered, random);
        return ordered;
    }
    
    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to pay cost to prevent effect
        return random.nextBoolean();
    }
    
    @Override
    public boolean payCostDuringRoll(Cost cost, SpellAbility sa, FCollectionView<Player> allPayers) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to pay cost during die roll
        return random.nextBoolean();
    }
    
    @Override
    public boolean payCombatCost(Card card, Cost cost, SpellAbility sa, String prompt) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to pay combat-related costs
        return random.nextBoolean();
    }
    
    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect) {
        // RANDOM_CHOICE_POINT: Randomly decide whether to pay mana costs
        return random.nextBoolean();
    }
    
    @Override
    public String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message) {
        // RANDOM_CHOICE_POINT: Randomly choose card name matching criteria
        List<String> cardNames = new ArrayList<>();
        
        // This would need access to all card names, simplified to common ones
        String[] commonNames = {"Lightning Bolt", "Counterspell", "Giant Growth", "Dark Ritual", "Healing Salve"};
        for (String name : commonNames) {
            cardNames.add(name);
        }
        
        if (cardNames.isEmpty()) {
            return "Lightning Bolt"; // Default fallback
        }
        return cardNames.get(random.nextInt(cardNames.size()));
    }
    
    @Override
    public String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message) {
        // RANDOM_CHOICE_POINT: Randomly choose card name from specific faces
        if (faces.isEmpty()) {
            return "Lightning Bolt"; // Default fallback
        }
        ICardFace chosen = faces.get(random.nextInt(faces.size()));
        return chosen.getName();
    }
    
    @Override
    public Card chooseDungeon(Player player, List<PaperCard> dungeonCards, String message) {
        // RANDOM_CHOICE_POINT: Randomly choose dungeon to venture into
        if (dungeonCards.isEmpty()) {
            return null;
        }
        PaperCard chosen = dungeonCards.get(random.nextInt(dungeonCards.size()));
        return Card.fromPaperCard(chosen, player);
    }
    
    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, 
            CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider) {
        // RANDOM_CHOICE_POINT: Randomly choose single card for zone change
        if (isOptional && random.nextBoolean()) {
            return null;
        }
        if (fetchList.isEmpty()) {
            return null;
        }
        return fetchList.get(random.nextInt(fetchList.size()));
    }
    
    @Override
    public List<Card> chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, 
            CardCollection fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider) {
        // RANDOM_CHOICE_POINT: Randomly choose multiple cards for zone change
        List<Card> result = new ArrayList<>();
        List<Card> available = new ArrayList<>(fetchList);
        Collections.shuffle(available, random);
        
        int numToChoose = min + random.nextInt(Math.max(1, Math.min(max, available.size()) - min + 1));
        numToChoose = Math.min(numToChoose, available.size());
        
        for (int i = 0; i < numToChoose; i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
    
    @Override
    public void autoPassCancel() {
        // No random choice needed - cancel auto-pass
    }
    
    @Override
    public void awaitNextInput() {
        // No random choice needed - wait for input
    }
    
    @Override
    public void cancelAwaitNextInput() {
        // No random choice needed - cancel input wait
    }
    
    // Helper method to randomly choose a defender for combat
    private GameEntity getRandomDefender(Player attacker) {
        List<GameEntity> defenders = new ArrayList<>();
        
        // Add opponent players
        for (Player opponent : getGame().getPlayers()) {
            if (opponent != attacker) {
                defenders.add(opponent);
            }
        }
        
        // Add planeswalkers opponents control
        for (Player opponent : getGame().getPlayers()) {
            if (opponent != attacker) {
                for (Card planeswalker : opponent.getCardsIn(ZoneType.Battlefield)) {
                    if (planeswalker.isPlaneswalker()) {
                        defenders.add(planeswalker);
                    }
                }
            }
        }
        
        if (defenders.isEmpty()) {
            return null;
        }
        
        return defenders.get(random.nextInt(defenders.size()));
    }
}