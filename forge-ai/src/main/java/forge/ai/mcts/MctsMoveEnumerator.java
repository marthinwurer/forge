package forge.ai.mcts;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtilAbility;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Enumerates legal moves at each decision point using existing Forge infrastructure.
 */
public class MctsMoveEnumerator {

    /**
     * Get legal moves during priority (main phase spell/ability casting).
     * Always includes PASS as an option.
     */
    public List<MctsMove> getLegalPriorityMoves(Game game, Player player) {
        List<MctsMove> moves = new ArrayList<>();

        // Check for land plays
        CardCollection landsToPlay = ComputerUtilAbility.getAvailableLandsToPlay(game, player);
        if (landsToPlay != null && !landsToPlay.isEmpty()) {
            for (int i = 0; i < landsToPlay.size(); i++) {
                Card land = landsToPlay.get(i);
                moves.add(MctsMove.spellAbility(-(i + 1), "Play land: " + land.getName()));
            }
        }

        // Get castable spells/abilities
        SpellAbilityPicker picker = new SpellAbilityPicker(game, player);
        List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();
        for (int i = 0; i < candidates.size(); i++) {
            SpellAbility sa = candidates.get(i);
            String desc = sa.getHostCard().getName() + " - " + sa.toString();
            moves.add(MctsMove.spellAbility(i, desc));
        }

        // Always include pass
        moves.add(MctsMove.pass());
        return moves;
    }

    /**
     * Get legal attack configurations.
     * Generates a pruned subset: no attack, all-in, each creature solo.
     */
    public List<MctsMove> getLegalAttackMoves(Game game, Player player) {
        List<MctsMove> moves = new ArrayList<>();
        CardCollection possibleAttackers = CombatUtil.getPossibleAttackers(player);

        if (possibleAttackers.isEmpty()) {
            moves.add(MctsMove.attackSet(0, "No attackers"));
            return moves;
        }

        // No attack
        moves.add(MctsMove.attackSet(0, "No attackers"));

        // All-in attack
        int allInMask = (1 << possibleAttackers.size()) - 1;
        moves.add(MctsMove.attackSet(allInMask, "All-in attack (" + possibleAttackers.size() + " creatures)"));

        // Each creature solo (if more than 1 attacker)
        if (possibleAttackers.size() > 1) {
            for (int i = 0; i < possibleAttackers.size(); i++) {
                int mask = 1 << i;
                moves.add(MctsMove.attackSet(mask, "Attack with " + possibleAttackers.get(i).getName()));
            }
        }

        return moves;
    }

    /**
     * Get the list of possible attackers for resolving attack bitmasks.
     */
    public CardCollection getPossibleAttackers(Player player) {
        return CombatUtil.getPossibleAttackers(player);
    }

    /**
     * Get legal block configurations.
     * For now, generates: no blocks, and block with all available blockers.
     */
    public List<MctsMove> getLegalBlockMoves(Game game, Player player) {
        List<MctsMove> moves = new ArrayList<>();

        // No blocks
        moves.add(MctsMove.blockAssignment(0, "No blocks"));

        // Block with all (delegate to AI heuristics for actual assignment)
        moves.add(MctsMove.blockAssignment(1, "Block (AI heuristic)"));

        return moves;
    }
}
