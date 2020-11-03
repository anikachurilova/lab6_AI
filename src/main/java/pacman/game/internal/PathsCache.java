package pacman.game.internal;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.internal.DNode;
import pacman.game.internal.Junction;
import pacman.game.internal.JunctionData;
import pacman.game.internal.Maze;

import java.util.*;

public class PathsCache {
    public HashMap<Integer, Integer> junctionIndexConverter;
    public DNode[] nodes;
    public Junction[] junctions;
    public Game game;

    public PathsCache(int mazeIndex) {
        junctionIndexConverter = new HashMap<Integer, Integer>();

        this.game = new Game(0, mazeIndex);
        Maze m = game.getCurrentMaze();

        int[] jctIndices = m.junctionIndices;

        for (int i = 0; i < jctIndices.length; i++)
            junctionIndexConverter.put(jctIndices[i], i);

        nodes = assignJunctionsToNodes(game);
        junctions = junctionDistances(game);

        for (int i = 0; i < junctions.length; i++)
            junctions[i].computeShortestPaths();
    }

    //for Ms Pac-Man
    public int[] getPathFromA2B(int a, int b) {
        //not going anywhere
        if (a == b)
            return new int[]{};

        //junctions near the source
        ArrayList<JunctionData> closestFromJunctions = nodes[a].closestJunctions;

        //if target is on the way to junction, then we are done
        for (int w = 0; w < closestFromJunctions.size(); w++)
            for (int i = 0; i < closestFromJunctions.get(w).path.length; i++)
                if (closestFromJunctions.get(w).path[i] == b)
                    return Arrays.copyOf(closestFromJunctions.get(w).path, i + 1);

        //junctions near the target
        ArrayList<JunctionData> closestToJunctions = nodes[b].closestJunctions;

        int minFrom = -1;
        int minTo = -1;
        int minDistance = Integer.MAX_VALUE;
        int[] shortestPath = null;

        for (int i = 0; i < closestFromJunctions.size(); i++) {
            for (int j = 0; j < closestToJunctions.size(); j++) {
                //to the first junction
                int distance = closestFromJunctions.get(i).path.length;
                //junction to junction
                int[] tmpPath = junctions[junctionIndexConverter.get(closestFromJunctions.get(i).nodeID)]
                        .paths[junctionIndexConverter.get(closestToJunctions.get(j).nodeID)].get(Constants.MOVE.NEUTRAL);
                distance += tmpPath.length;
                //to the second junction
                distance += closestToJunctions.get(j).path.length;

                if (distance < minDistance) {
                    minDistance = distance;
                    minFrom = i;
                    minTo = j;
                    shortestPath = tmpPath;
                }
            }
        }

        return concat(closestFromJunctions.get(minFrom).path, shortestPath, closestToJunctions.get(minTo).reversePath);
    }

    /////// ghosts //////////

    //To be made more efficient shortly.
    public int getPathDistanceFromA2B(int a, int b, Constants.MOVE lastMoveMade) {
        return getPathFromA2B(a, b, lastMoveMade).length;
    }

    public int[] getPathFromA2B(int a, int b, Constants.MOVE lastMoveMade) {
        //not going anywhere
        if (a == b)
            return new int[]{};

        //first, go to closest junction (there is only one since we can't reverse)
        JunctionData fromJunction = nodes[a].getNearestJunction(lastMoveMade);

        //if target is on the way to junction, then we are done
        for (int i = 0; i < fromJunction.path.length; i++)
            if (fromJunction.path[i] == b)
                return Arrays.copyOf(fromJunction.path, i + 1);

        //we have reached a junction, fromJunction, which we entered with moveEnteredJunction
        int junctionFrom = fromJunction.nodeID;
        int junctionFromId = junctionIndexConverter.get(junctionFrom);
        Constants.MOVE moveEnteredJunction = fromJunction.lastMove.equals(Constants.MOVE.NEUTRAL) ? lastMoveMade : fromJunction.lastMove; //if we are at a junction, consider last move instead

        //now we need to get the 1 or 2 target junctions that enclose the target point
        ArrayList<JunctionData> junctionsTo = nodes[b].closestJunctions;

        int minDist = Integer.MAX_VALUE;
        int[] shortestPath = null;
        int closestJunction = -1;

        boolean onTheWay = false;

        for (int q = 0; q < junctionsTo.size(); q++) {
            int junctionToId = junctionIndexConverter.get(junctionsTo.get(q).nodeID);

            if (junctionFromId == junctionToId) {
                if (!game.getMoveToMakeToReachDirectNeighbour(junctionFrom, junctionsTo.get(q).reversePath[0]).equals(moveEnteredJunction.opposite())) {
                    int[] reversepath = junctionsTo.get(q).reversePath;
                    int cutoff = -1;

                    for (int w = 0; w < reversepath.length; w++)
                        if (reversepath[w] == b)
                            cutoff = w;

                    shortestPath = Arrays.copyOf(reversepath, cutoff + 1);
                    minDist = shortestPath.length;
                    closestJunction = q;
                    onTheWay = true;
                }
            } else {
                EnumMap<Constants.MOVE, int[]> paths = junctions[junctionFromId].paths[junctionToId];
                Set<Constants.MOVE> set = paths.keySet();

                for (Constants.MOVE move : set) {
                    if (!move.opposite().equals(moveEnteredJunction) && !move.equals(Constants.MOVE.NEUTRAL)) {
                        int[] path = paths.get(move);

                        if (path.length + junctionsTo.get(q).path.length < minDist)//need to take distance from toJunction to target into account
                        {
                            minDist = path.length + junctionsTo.get(q).path.length;
                            shortestPath = path;
                            closestJunction = q;
                            onTheWay = false;
                        }
                    }
                }
            }
        }

        if (!onTheWay)
            return concat(fromJunction.path, shortestPath, junctionsTo.get(closestJunction).reversePath);
        else
            return concat(fromJunction.path, shortestPath);
//			return concat(fromJunction.path, junctionsTo.get(closestJunction).reversePath);
    }

    private Junction[] junctionDistances(Game game) {
        Maze m = game.getCurrentMaze();
        int[] indices = m.junctionIndices;

        Junction[] junctions = new Junction[indices.length];

        for (int q = 0; q < indices.length; q++)// from
        {
            Constants.MOVE[] possibleMoves = m.graph[indices[q]].allPossibleMoves.get(Constants.MOVE.NEUTRAL);// all possible moves

            junctions[q] = new Junction(q, indices[q], indices.length);

            for (int z = 0; z < indices.length; z++)// to (we need to include distance to itself)
            {
                for (int i = 0; i < possibleMoves.length; i++) {
                    int neighbour = game.getNeighbour(indices[q], possibleMoves[i]);
                    int[] p = m.astar.computePathsAStar(neighbour, indices[z], possibleMoves[i], game);
                    m.astar.resetGraph();

                    junctions[q].addPath(z, possibleMoves[i], p);
                }
            }
        }

        return junctions;
    }

    private DNode[] assignJunctionsToNodes(Game game) {
        Maze m = game.getCurrentMaze();
        int numNodes = m.graph.length;

        DNode[] allNodes = new DNode[numNodes];

        for (int i = 0; i < numNodes; i++) {
            boolean isJunction = game.isJunction(i);
            allNodes[i] = new DNode(i, isJunction);

            if (!isJunction) {
                Constants.MOVE[] possibleMoves = m.graph[i].allPossibleMoves.get(Constants.MOVE.NEUTRAL);

                for (int j = 0; j < possibleMoves.length; j++) {
                    ArrayList<Integer> path = new ArrayList<Integer>();

                    Constants.MOVE lastMove = possibleMoves[j];
                    int currentNode = game.getNeighbour(i, lastMove);
                    path.add(currentNode);

                    while (!game.isJunction(currentNode)) {
                        Constants.MOVE[] newPossibleMoves = game.getPossibleMoves(currentNode);

                        for (int q = 0; q < newPossibleMoves.length; q++)
                            if (newPossibleMoves[q].opposite() != lastMove) {
                                lastMove = newPossibleMoves[q];
                                break;
                            }

                        currentNode = game.getNeighbour(currentNode, lastMove);
                        path.add(currentNode);
                    }

                    int[] array = new int[path.size()];

                    for (int w = 0; w < path.size(); w++)
                        array[w] = path.get(w);

                    allNodes[i].addPath(array[array.length - 1], possibleMoves[j], i, array, lastMove);
                }
            }
        }

        return allNodes;
    }

    private int[] concat(int[]... arrays) {
        int totalLength = 0;

        for (int i = 0; i < arrays.length; i++)
            totalLength += arrays[i].length;

        int[] fullArray = new int[totalLength];

        int index = 0;

        for (int i = 0; i < arrays.length; i++)
            for (int j = 0; j < arrays[i].length; j++)
                fullArray[index++] = arrays[i][j];

        return fullArray;
    }
}