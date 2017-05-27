package color.guard.state;

import squidpony.squidmath.LightRNG;
import squidpony.squidmath.StatefulRNG;

/**
 * Tiny storage for all other parts of state, so this can be handed off to SquidStorage to save.
 * Created by Tommy Ettinger on 10/3/2016.
 */
public class GameState {
    public StatefulRNG masterRandom;
    public WorldState world;
    public GameState()
    {
    }
    public GameState(long seed)
    {
        masterRandom = new StatefulRNG(new LightRNG(seed));
        world = new WorldState(192, 192, masterRandom.nextLong());
    }
}
