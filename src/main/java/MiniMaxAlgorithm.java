
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import pacman.controllers.PacmanController;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class MiniMaxAlgorithm extends PacmanController
{

    private static final int MINIMAX_DEPTH = 4;

    public MOVE getMove(Game game, long timeDue)
    {
        Tree miniMaxTree = createMiniMaxTree(game, MINIMAX_DEPTH, true);
        return bestMoveFromTree(miniMaxTree);
    }
    private Tree createMiniMaxTree(Game game, int depth, boolean isPacMan)
    {

        if (depth == 0 || isEndGameState(game)) {
            return new Leaf(heuristicVal(game));
        }

        if (isPacMan) {

            Map<MOVE, Tree> branches = new HashMap<>();
            Game leftGame = stateAfterPacMove(MOVE.LEFT, game);
            branches.put(MOVE.LEFT, createMiniMaxTree(leftGame, depth - 1, false));
            Game rightGame = stateAfterPacMove(MOVE.RIGHT, game);
            branches.put(MOVE.RIGHT, createMiniMaxTree(rightGame, depth - 1, false));
            Game upGame = stateAfterPacMove(MOVE.UP, game);
            branches.put(MOVE.UP, createMiniMaxTree(upGame, depth - 1, false));
            Game downGame = stateAfterPacMove(MOVE.DOWN, game);
            branches.put(MOVE.DOWN, createMiniMaxTree(downGame, depth - 1, false));

            return new PacNode(branches);
        } else {

            Set<MOVE> possibleBlinkyMoves = getPossibleGhostMoves(game, GHOST.BLINKY);
            Set<MOVE> possibleInkyMoves = getPossibleGhostMoves(game, GHOST.INKY);
            Set<MOVE> possiblePinkyMoves = getPossibleGhostMoves(game, GHOST.PINKY);
            Set<MOVE> possibleSueMoves = getPossibleGhostMoves(game, GHOST.SUE);

            Set<Map<GHOST, MOVE>> possibleGhostCombinations = calculateGhostCombinations(possibleBlinkyMoves,
                    possibleInkyMoves,
                    possiblePinkyMoves,
                    possibleSueMoves);

            Set<Tree> ghostBranches = new HashSet<>();
            for (Map<GHOST, MOVE> possibleGhostMoves : possibleGhostCombinations) {
                Game gameStateAfterGhosts = gameStateAfterGhosts(game, possibleGhostMoves);
                ghostBranches.add(createMiniMaxTree(gameStateAfterGhosts, depth - 1, true));
            }

            return new GhostNode(ghostBranches);
        }
    }


    private boolean isEndGameState(Game game)
    {
        return (game.getNumberOfActivePills() == 0 && game.getNumberOfActivePowerPills() == 0) ||
                game.wasPacManEaten() ||
                game.gameOver();
    }

    private int heuristicVal(Game game)
    {
        if (game.wasPacManEaten()) {

            return Integer.MIN_VALUE;
        }

        int totalPills = game.getNumberOfActivePills() + game.getNumberOfActivePowerPills();
        int score = game.getScore();


        int distanceToBlinky = shortestPathDistanceToGhost(game, GHOST.BLINKY);
        int distanceToInky = shortestPathDistanceToGhost(game, GHOST.INKY);
        int distanceToPinky = shortestPathDistanceToGhost(game, GHOST.PINKY);
        int distanceToSue = shortestPathDistanceToGhost(game, GHOST.SUE);
        Map<GHOST, Integer> ghostsToDistance = new HashMap<>();
        ghostsToDistance.put(GHOST.BLINKY, distanceToBlinky);
        ghostsToDistance.put(GHOST.INKY, distanceToInky);
        ghostsToDistance.put(GHOST.PINKY, distanceToPinky);
        ghostsToDistance.put(GHOST.SUE, distanceToSue);

        int distanceToNearestGhost = Collections.min(Lists.newArrayList(distanceToBlinky, distanceToInky,
                distanceToPinky, distanceToSue));
        GHOST nearestGhost = null;

        for (Map.Entry<GHOST, Integer> ghostDistance : ghostsToDistance.entrySet()) {
            if (ghostDistance.getValue() == distanceToNearestGhost) {
                nearestGhost = ghostDistance.getKey();
            }
        }


        int weightedGhostScore = -500 * (20 - distanceToNearestGhost);
        if (distanceToNearestGhost >= 20 || game.isGhostEdible(nearestGhost)) {
            weightedGhostScore = 0;
        }

        int weightedEatingGhostScore = 0;
        if (game.isGhostEdible(nearestGhost) && distanceToNearestGhost <= 40) {
            weightedEatingGhostScore = 50 * (40 - distanceToNearestGhost);
        }


        List<Integer> activePillIndices = new ArrayList<>();
        activePillIndices.addAll(Ints.asList(game.getActivePillsIndices()));
        activePillIndices.addAll(Ints.asList(game.getActivePowerPillsIndices()));

        List<Integer> distancesToPills = new ArrayList<>();
        for (int pillIndice : activePillIndices) {
            distancesToPills.add(game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(), pillIndice));
        }
        int distanceToNearestPill = 0;
        if (!distancesToPills.isEmpty()) {

            distanceToNearestPill = Collections.min(distancesToPills);
        }

        return -1 * totalPills + -1 * distanceToNearestPill + 100 * score + weightedGhostScore +
                weightedEatingGhostScore;
    }

    private int shortestPathDistanceToGhost(Game game, GHOST ghost)
    {
        return game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(ghost));
    }

    private Game stateAfterPacMove(MOVE pacMove, Game curGame)
    {
        Game copyOfGame = curGame.copy();
        copyOfGame.updatePacMan(pacMove);
        return copyOfGame;
    }

    private Set<Map<GHOST, MOVE>> calculateGhostCombinations(Set<MOVE> possibleBlinkyMoves,
                                                             Set<MOVE> possibleInkyMoves,
                                                             Set<MOVE> possiblePinkyMoves,
                                                             Set<MOVE> possibleSueMoves)
    {
        Set<Map<GHOST, MOVE>> result = new HashSet<>();

        for (MOVE blinkyMove : possibleBlinkyMoves) {
            for (MOVE inkyMove : possibleInkyMoves) {
                for (MOVE pinkyMove : possiblePinkyMoves) {
                    for (MOVE sueMove : possibleSueMoves) {

                        Map<GHOST, MOVE> possibleMoveSet = new HashMap<>();
                        possibleMoveSet.put(GHOST.BLINKY, blinkyMove);
                        possibleMoveSet.put(GHOST.INKY, inkyMove);
                        possibleMoveSet.put(GHOST.PINKY, pinkyMove);
                        possibleMoveSet.put(GHOST.SUE, sueMove);

                        result.add(possibleMoveSet);
                    }
                }
            }
        }

        return result;
    }


    private Game gameStateAfterGhosts(Game game, Map<GHOST, MOVE> ghostMoves)
    {
        Game copyOfGame = game.copy();

        EnumMap<GHOST, MOVE> enumMap = new EnumMap<>(GHOST.class);

        for (Map.Entry<GHOST, MOVE> ghostMove : ghostMoves.entrySet()) {
            enumMap.put(ghostMove.getKey(), ghostMove.getValue());
        }
        copyOfGame.updateGhosts(enumMap);

        return copyOfGame;
    }
    private Set<MOVE> getPossibleGhostMoves(Game game, GHOST ghost)
    {
        Set<MOVE> result = Sets.newHashSet(game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost),
                game.getGhostLastMoveMade(ghost)));

        if (result.isEmpty()) {

            return Sets.newHashSet(MOVE.NEUTRAL);
        }
        return result;
    }

    private MOVE bestMoveFromTree(Tree miniMaxTree)
    {
        return bestMoveFromTreeHelper(miniMaxTree, true).move;
    }

    private MoveNumber bestMoveFromTreeHelper(Tree miniMaxTree, boolean maximizingPlayer)
    {
        if (miniMaxTree.isLeaf()) {
            // There is no possible move for just a leaf so just leave as null
            return new MoveNumber(null, miniMaxTree.getHeuristic());
        } else if (maximizingPlayer) {
            // This should be a PacNode. Retrieve the children and moves and find the one with the largest heuristic val
            Optional<MoveNumber> moveNumberOptional = Optional.absent();

            for (Map.Entry<MOVE, Tree> entry : miniMaxTree.getChildrenAndMoves().entrySet()) {
                MoveNumber moveNumber = bestMoveFromTreeHelper(entry.getValue(), false);
                moveNumber.setMove(entry.getKey());

                if (!moveNumberOptional.isPresent()) {
                    moveNumberOptional = Optional.of(moveNumber);
                } else if (moveNumber.hValue > moveNumberOptional.get().hValue) {
                    moveNumberOptional = Optional.of(moveNumber);
                }
            }
            return moveNumberOptional.get();
        } else {

            Optional<MoveNumber> moveNumberOptional = Optional.absent();

            for (Tree tree : miniMaxTree.getChildren()) {
                MoveNumber moveNumber = bestMoveFromTreeHelper(tree, true);

                if (!moveNumberOptional.isPresent()) {
                    moveNumberOptional = Optional.of(moveNumber);
                } else if (moveNumber.hValue < moveNumberOptional.get().hValue) {
                    moveNumberOptional = Optional.of(moveNumber);
                }
            }
            return moveNumberOptional.get();
        }
    }

    interface Tree
    {
        boolean isLeaf();
        int getHeuristic();
        Map<MOVE, Tree> getChildrenAndMoves();
        Set<Tree> getChildren();

    }

    private static class PacNode implements Tree
    {
        private Map<MOVE, Tree> branches;

        PacNode(Map<MOVE, Tree> branches)
        {
            this.branches = checkNotNull(branches);
        }

        @Override
        public boolean isLeaf()
        {
            return false;
        }

        @Override
        public int getHeuristic()
        {
            throw new RuntimeException();
        }

        @Override
        public Map<MOVE, Tree> getChildrenAndMoves()
        {
            return branches;
        }

        @Override
        public Set<Tree> getChildren()
        {
            throw new RuntimeException();
        }
    }
    private static class GhostNode implements Tree
    {

        private Set<Tree> ghostBranches;

        GhostNode(Set<Tree> ghostBranches)
        {
            this.ghostBranches = checkNotNull(ghostBranches);
        }

        @Override
        public boolean isLeaf()
        {
            return false;
        }

        @Override
        public int getHeuristic()
        {
            throw new RuntimeException();
        }

        @Override
        public Map<MOVE, Tree> getChildrenAndMoves()
        {
            throw new RuntimeException();
        }

        @Override
        public Set<Tree> getChildren()
        {
            return ghostBranches;
        }
    }


    private static class Leaf implements Tree
    {

        int heuristic;

        Leaf(int heuristic)
        {
            this.heuristic = checkNotNull(heuristic);
        }

        @Override
        public boolean isLeaf()
        {
            return true;
        }

        @Override
        public int getHeuristic()
        {
            return heuristic;
        }

        @Override
        public Map<MOVE, Tree> getChildrenAndMoves()
        {
            throw new RuntimeException();
        }

        @Override
        public Set<Tree> getChildren()
        {
            throw new RuntimeException();
        }
    }

    private static class MoveNumber
    {

        MOVE move;

        int hValue;

        MoveNumber(@Nullable MOVE move, int hValue)
        {
            this.move = move;
            this.hValue = checkNotNull(hValue);
        }

        public void setMove(MOVE move)
        {
            this.move = checkNotNull(move);
        }
    }
}