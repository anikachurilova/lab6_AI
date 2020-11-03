
import pacman.Executor;
import pacman.controllers.examples.RandomGhosts;

public class Main
{
    public static void main(String[] args) {
        Executor executor = new Executor(false, true);
        executor.runGameTimed(new MiniMaxAlgorithm(), new RandomGhosts(), true);
    }
}
