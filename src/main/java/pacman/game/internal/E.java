package pacman.game.internal;

import pacman.game.Constants;
import pacman.game.internal.N;

class E {
    public N node;
    public Constants.MOVE move;
    public double cost;

    public E(N node, Constants.MOVE move, double cost) {
        this.node = node;
        this.move = move;
        this.cost = cost;
    }
}