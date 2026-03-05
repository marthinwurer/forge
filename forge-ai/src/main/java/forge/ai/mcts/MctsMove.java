package forge.ai.mcts;

/**
 * Represents a single decision at any choice point in the MCTS tree.
 */
public class MctsMove {

    public enum MoveType {
        SPELL_ABILITY,
        PASS,
        ATTACK_SET,
        BLOCK_ASSIGNMENT
    }

    private final MoveType type;
    private final int index; // Index into candidates list or bitmask for attacks
    private final String description;

    private MctsMove(MoveType type, int index, String description) {
        this.type = type;
        this.index = index;
        this.description = description;
    }

    public static MctsMove pass() {
        return new MctsMove(MoveType.PASS, -1, "Pass");
    }

    public static MctsMove spellAbility(int index, String description) {
        return new MctsMove(MoveType.SPELL_ABILITY, index, description);
    }

    public static MctsMove attackSet(int bitmask, String description) {
        return new MctsMove(MoveType.ATTACK_SET, bitmask, description);
    }

    public static MctsMove blockAssignment(int index, String description) {
        return new MctsMove(MoveType.BLOCK_ASSIGNMENT, index, description);
    }

    public MoveType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return type + ": " + description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MctsMove)) return false;
        MctsMove other = (MctsMove) o;
        return type == other.type && index == other.index;
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode() + index;
    }
}
