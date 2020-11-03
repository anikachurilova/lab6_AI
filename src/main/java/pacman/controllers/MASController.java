package pacman.controllers;

import pacman.entries.ghostMAS.Blinky;
import pacman.entries.ghostMAS.Sue;
import pacman.entries.ghostMAS.Inky;
import pacman.entries.ghostMAS.Pinky;
import pacman.game.Constants.*;
import pacman.game.Game;

import java.util.EnumMap;

/**
 * Created by Piers on 11/11/2015.
 *
 * Makes the game PO for each controller
 */
public final class MASController extends Controller<EnumMap<GHOST, MOVE>> {

    private EnumMap<GHOST, MOVE> myMoves = new EnumMap<GHOST, MOVE>(GHOST.class);
    private EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

    public MASController(EnumMap<GHOST, IndividualGhostController> controllers){
        this.controllers = controllers;
    }

    @Override
    public final EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
        myMoves.clear();

        for (GHOST ghost : GHOST.values()) {
            myMoves.put(ghost, controllers.get(ghost).getMove(game.copy(ghost), timeDue));
        }
        return myMoves;
    }
}
