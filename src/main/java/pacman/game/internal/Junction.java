package pacman.game.internal;

import pacman.game.Constants;

import java.util.EnumMap;

class Junction {
    public int jctId, nodeId;
    public EnumMap<Constants.MOVE, int[]>[] paths;

    public void computeShortestPaths() {
        Constants.MOVE[] moves = Constants.MOVE.values();

        for (int i = 0; i < paths.length; i++) {
            if (i == jctId)
                paths[i].put(Constants.MOVE.NEUTRAL, new int[]{});
            else {
                int distance = Integer.MAX_VALUE;
                int[] path = null;

                for (int j = 0; j < moves.length; j++) {
                    if (paths[i].containsKey(moves[j])) {
                        int[] tmp = paths[i].get(moves[j]);

                        if (tmp.length < distance) {
                            distance = tmp.length;
                            path = tmp;
                        }
                    }
                }

                paths[i].put(Constants.MOVE.NEUTRAL, path);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Junction(int jctId, int nodeId, int numJcts) {
        this.jctId = jctId;
        this.nodeId = nodeId;

        paths = new EnumMap[numJcts];

        for (int i = 0; i < paths.length; i++)
            paths[i] = new EnumMap<Constants.MOVE, int[]>(Constants.MOVE.class);
    }

    // store the shortest path given the last move made
    public void addPath(int toJunction, Constants.MOVE firstMoveMade, int[] path) {
        paths[toJunction].put(firstMoveMade, path);
    }

    public String toString() {
        return jctId + "\t" + nodeId;
    }
}