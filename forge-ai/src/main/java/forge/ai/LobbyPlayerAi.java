package forge.ai;

import java.util.Set;

import forge.LobbyPlayer;
import forge.ai.mcts.PlayerControllerMcts;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;

public class LobbyPlayerAi extends LobbyPlayer implements IGameEntitiesFactory {

    private String aiProfile = "";
    private boolean rotateProfileEachGame;
    private boolean allowCheatShuffle;
    private boolean useSimulation;
    private boolean useMcts;

    public LobbyPlayerAi(String name, Set<AIOption> options) {
        super(name);
        if (options != null) {
            if (options.contains(AIOption.USE_SIMULATION)) {
                this.useSimulation = true;
            }
            if (options.contains(AIOption.USE_MCTS)) {
                this.useMcts = true;
            }
        }
    }

    public boolean isAllowCheatShuffle() {
        return allowCheatShuffle;
    }
    public void setAllowCheatShuffle(boolean allowCheatShuffle) {
        this.allowCheatShuffle = allowCheatShuffle;
    }

    public void setAiProfile(String profileName) {
        aiProfile = profileName;
    }
    public String getAiProfile() {
        return aiProfile;
    }

    public void setRotateProfileEachGame(boolean rotateProfileEachGame) {
        this.rotateProfileEachGame = rotateProfileEachGame;
    }

    private PlayerControllerAi createControllerFor(Player ai) {
        PlayerControllerAi result;
        if (useMcts) {
            result = new PlayerControllerMcts(ai.getGame(), ai, this);
        } else {
            result = new PlayerControllerAi(ai.getGame(), ai, this);
        }
        result.setUseSimulation(useSimulation);
        result.setUseMcts(useMcts);
        result.allowCheatShuffle(allowCheatShuffle);
        return result;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return createControllerFor(slave);
    }

    @Override
    public Player createIngamePlayer(Game game, final int id) {
        Player ai = new Player(getName(), game, id);
        ai.setFirstController(createControllerFor(ai));

        if (rotateProfileEachGame) {
            setAiProfile(AiProfileUtil.getRandomProfile());
            /*System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", getAiProfile(), getName()));*/
        }
        return ai;
    }

    @Override
    public void hear(LobbyPlayer player, String message) { /* Local AI is deaf. */ }
}