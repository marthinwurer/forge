package forge.ai.mcts;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import forge.LobbyPlayer;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilAbility;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollectionView;
import forge.game.GameEntity;

/**
 * PlayerController for MCTS that extends PlayerControllerAi.
 *
 * Two modes:
 * - ROLLOUT: delegates everything to super (existing AI heuristics)
 * - DIRECTED: pops pre-determined MctsMove from a queue for the 3 critical decisions
 *   (chooseSpellAbilityToPlay, declareAttackers, declareBlockers);
 *   delegates everything else to super
 */
public class PlayerControllerMcts extends PlayerControllerAi {

    public enum Mode {
        ROLLOUT,
        DIRECTED
    }

    private Mode mode = Mode.ROLLOUT;
    private final Deque<MctsMove> moveQueue = new ArrayDeque<>();
    private boolean directedMovePlayed = false;

    public PlayerControllerMcts(Game game, Player p, LobbyPlayer lp) {
        super(game, p, lp);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        this.directedMovePlayed = false;
    }

    public void queueMove(MctsMove move) {
        moveQueue.addLast(move);
    }

    public void clearMoveQueue() {
        moveQueue.clear();
        directedMovePlayed = false;
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        if (mode == Mode.ROLLOUT) {
            return super.chooseSpellAbilityToPlay();
        }

        // DIRECTED mode: play one move from queue, then pass
        if (directedMovePlayed || moveQueue.isEmpty()) {
            return null; // pass
        }

        MctsMove move = moveQueue.peekFirst();
        if (move.getType() != MctsMove.MoveType.SPELL_ABILITY && move.getType() != MctsMove.MoveType.PASS) {
            // Not a spell/pass move - leave it for attack/block handlers
            return null;
        }

        moveQueue.pollFirst();
        directedMovePlayed = true;

        if (move.getType() == MctsMove.MoveType.PASS) {
            return null;
        }

        int index = move.getIndex();

        // Negative index means land play
        if (index < 0) {
            int landIndex = -(index + 1);
            CardCollection landsToPlay = ComputerUtilAbility.getAvailableLandsToPlay(
                    getGame(), getPlayer());
            if (landsToPlay != null && landIndex < landsToPlay.size()) {
                Card land = landsToPlay.get(landIndex);
                List<SpellAbility> abilities = land.getAllPossibleAbilities(getPlayer(), true);
                abilities.removeIf(sa -> !sa.isLandAbility());
                if (!abilities.isEmpty()) {
                    return abilities;
                }
            }
            return null; // land play failed, pass
        }

        // Positive index means spell/ability from candidates list
        SpellAbilityPicker picker = new SpellAbilityPicker(getGame(), getPlayer());
        List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();
        if (index < candidates.size()) {
            SpellAbility sa = candidates.get(index);
            // Must call canPlaySa to set up targets (targeting is a side effect)
            AiPlayDecision decision = getAi().canPlaySa(sa);
            if (decision == AiPlayDecision.WillPlay) {
                return List.of(sa);
            }
        }

        return null; // index out of range or targeting failed, pass
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        if (mode == Mode.ROLLOUT) {
            super.declareAttackers(attacker, combat);
            return;
        }

        // DIRECTED mode: check if there's an attack move queued
        if (!moveQueue.isEmpty() && moveQueue.peekFirst().getType() == MctsMove.MoveType.ATTACK_SET) {
            MctsMove move = moveQueue.pollFirst();
            int bitmask = move.getIndex();

            if (bitmask == 0) {
                // No attackers
                return;
            }

            CardCollection possibleAttackers = CombatUtil.getPossibleAttackers(attacker);
            FCollectionView<GameEntity> defenders = CombatUtil.getAllPossibleDefenders(attacker);
            GameEntity defender = defenders.isEmpty() ? null : defenders.getFirst();

            if (defender == null) {
                return;
            }

            for (int i = 0; i < possibleAttackers.size(); i++) {
                if ((bitmask & (1 << i)) != 0) {
                    combat.addAttacker(possibleAttackers.get(i), defender);
                }
            }
        } else {
            // No attack move queued, delegate to AI
            super.declareAttackers(attacker, combat);
        }
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        if (mode == Mode.ROLLOUT) {
            super.declareBlockers(defender, combat);
            return;
        }

        // DIRECTED mode: check if there's a block move queued
        if (!moveQueue.isEmpty() && moveQueue.peekFirst().getType() == MctsMove.MoveType.BLOCK_ASSIGNMENT) {
            MctsMove move = moveQueue.pollFirst();
            if (move.getIndex() == 0) {
                // No blocks
                return;
            }
            // Delegate to AI heuristics for actual block assignment
            super.declareBlockers(defender, combat);
        } else {
            // No block move queued, delegate to AI
            super.declareBlockers(defender, combat);
        }
    }
}
