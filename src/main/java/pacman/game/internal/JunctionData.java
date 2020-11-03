package pacman.game.internal;

import pacman.game.Constants;

import java.util.Arrays;

class JunctionData {
    public int nodeID, nodeStartedFrom;
    public Constants.MOVE firstMove, lastMove;
    public int[] path, reversePath;

    public JunctionData(int nodeID, Constants.MOVE firstMove, int nodeStartedFrom, int[] path, Constants.MOVE lastMove) {
        this.nodeID = nodeID;
        this.nodeStartedFrom = nodeStartedFrom;
        this.firstMove = firstMove;
        this.path = path;
        this.lastMove = lastMove;

        if (path.length > 0)
            this.reversePath = getReversePath(path);
        else
            reversePath = new int[]{};
    }

    public int[] getReversePath(int[] path) {
        int[] reversePath = new int[path.length];

        for (int i = 1; i < reversePath.length; i++)
            reversePath[i - 1] = path[path.length - 1 - i];

        reversePath[reversePath.length - 1] = nodeStartedFrom;

        return reversePath;
    }

    public String toString() {
        return nodeID + "\t" + firstMove.toString() + "\t" + Arrays.toString(path);
    }
}