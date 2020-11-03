package entrants.pacman.silvaw;

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

public class MyPacManMiniMax extends PacmanController
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