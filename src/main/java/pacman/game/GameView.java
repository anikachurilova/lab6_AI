package pacman.game;

import pacman.controllers.examples.po.mcts.prediction.GhostLocation;
import pacman.controllers.examples.po.mcts.prediction.GhostPredictions;
import pacman.game.Constants.*;
import pacman.game.internal.PacMan;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Vector;

import static pacman.game.Constants.*;

/**
 * This class is the view that displays the game. The only thing contestants might need to know
 * about are the helper methods that allow one to add colours and lines to the game view. This
 * could be useful for debugging.
 */
@SuppressWarnings("serial")
public final class GameView extends JComponent {
	public static Vector<DebugPointer> debugPointers = new Vector<DebugPointer>();
	public static Vector<DebugLine> debugLines = new Vector<DebugLine>();
	//for debugging/illustration purposes only: draw colors in the maze to check whether controller is working
	//correctly or not; can draw squares and lines (see NearestPillPacManVS for demonstration).
	private static boolean isVisible = true;
	private static boolean saveImage = false;
	private static String imageFileName = "";
	private final Game game;
	private Images images;
	private MOVE lastPacManMove;
	private int time;
	private GameFrame frame;
	private Graphics bufferGraphics;
	private BufferedImage offscreen;
	private GhostPredictions predictions;

	private Color[] redAlphas;

	///////////////////////////////////////////////
	////// Visual aids for debugging: start ///////
	///////////////////////////////////////////////
	private int predictionTicks;

	/**
	 * Instantiates a new game view.
	 *
	 * @param game The game that is played.
	 */
	public GameView(Game game) {
		this.game = game;

		images = new Images();
		lastPacManMove = game.getPacmanLastMoveMade();
		time = game.getTotalTime();

		redAlphas = new Color[256];
		for(int i = 0; i < 256; i++){
			redAlphas[i] = new Color(255, 0, 0, i);
		}
	}

	/**
	 * Adds a node to be highlighted using the color specified
	 *
	 * @param game        Copy of the current game
	 * @param color       Colour to be used
	 * @param nodeIndices The node indices to be highlighted by the chosen colour
	 */
	public synchronized static void addPoints(Game game, Color color, int... nodeIndices) {
		if (isVisible)
			for (int i = 0; i < nodeIndices.length; i++)
				debugPointers.add(new DebugPointer(game.getNodeXCood(nodeIndices[i]), game.getNodeYCood(nodeIndices[i]), color));
	}

	/**
	 * Adds a set of lines to be drawn using the color specified (fromNnodeIndices.length must be equals toNodeIndices.length)
	 *
	 * @param game             Copy of the current game
	 * @param color            Colour to be used
	 * @param fromNnodeIndices The node indices where the lines start
	 * @param toNodeIndices    The node indices where the lines end
	 */
	public synchronized static void addLines(Game game, Color color, int[] fromNnodeIndices, int[] toNodeIndices) {
		if (isVisible)
			for (int i = 0; i < fromNnodeIndices.length; i++)
				debugLines.add(new DebugLine(game.getNodeXCood(fromNnodeIndices[i]), game.getNodeYCood(fromNnodeIndices[i]), game.getNodeXCood(toNodeIndices[i]), game.getNodeYCood(toNodeIndices[i]), color));
	}

	/**
	 * Adds a line to be drawn using the color specified
	 *
	 * @param game           the game
	 * @param color          the color
	 * @param fromNnodeIndex the from nnode index
	 * @param toNodeIndex    the to node index
	 */
	public synchronized static void addLines(Game game, Color color, int fromNnodeIndex, int toNodeIndex) {
		if (isVisible)
			debugLines.add(new DebugLine(game.getNodeXCood(fromNnodeIndex), game.getNodeYCood(fromNnodeIndex), game.getNodeXCood(toNodeIndex), game.getNodeYCood(toNodeIndex), color));
	}

	/**
	 * Allows one to save the image of the current game state using the file name specified.
	 *
	 * @param fileName Name of the image.
	 */
	public synchronized static void saveImage(String fileName) {
		saveImage = true;
		imageFileName = fileName;
	}

	/////////////////////////////////////////////
	////// Visual aids for debugging: end ///////
	/////////////////////////////////////////////

	/**
	 * Draw the debug info and the clear it - it is shown for a single time step only.
	 */
	private void drawDebugInfo() {
		for (int i = 0; i < debugPointers.size(); i++) {
			DebugPointer dp = debugPointers.get(i);
			bufferGraphics.setColor(dp.color);
			bufferGraphics.fillRect(dp.x * MAG + 1, dp.y * MAG + 5, 10, 10);
		}

		for (int i = 0; i < debugLines.size(); i++) {
			DebugLine dl = debugLines.get(i);
			bufferGraphics.setColor(dl.color);
			bufferGraphics.drawLine(dl.x1 * MAG + 5, dl.y1 * MAG + 10, dl.x2 * MAG + 5, dl.y2 * MAG + 10);
		}

		debugPointers.clear();
		debugLines.clear();
	}

	/*
	 * Saves the actual image.
	 */
	private void saveImage() {
		try {
			ImageIO.write(offscreen, "png", new File("myData/" + imageFileName + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		saveImage = false;
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
		time = game.getTotalTime();

		if (offscreen == null) {
			offscreen = (BufferedImage) createImage(this.getPreferredSize().width, this.getPreferredSize().height);
			bufferGraphics = offscreen.getGraphics();
		}

		drawMaze();
		drawDebugInfo();    //this will be used during testing only and will be disabled in the competition itself
		drawPills();
		drawPowerPills();
		drawPacMan();
		drawGhosts();
		drawLives();
		drawGameInfo();
//        drawPacManVisibility();
//        drawPacManPredictions();
		if (game.gameOver()) {
			drawGameOver();
		}

		g.drawImage(offscreen, 0, 0, this);

		if (saveImage)
			saveImage();
	}

	/**
	 * Draw maze.
	 */
	private void drawMaze() {
		bufferGraphics.setColor(Color.BLACK);
		bufferGraphics.fillRect(0, 0, GV_WIDTH * MAG, GV_HEIGHT * MAG + 20);

		bufferGraphics.drawImage(images.getMaze(game.getMazeIndex()), 2, 6, null);
	}

	/**
	 * Draw pills.
	 */
	private void drawPills() {
		int[] pillIndices = game.getPillIndices();

		bufferGraphics.setColor(Color.white);

		for (int i = 0; i < pillIndices.length; i++)
			if (game.isPillStillAvailable(i))
				bufferGraphics.fillOval(game.getNodeXCood(pillIndices[i]) * MAG + 4, game.getNodeYCood(pillIndices[i]) * MAG + 8, 3, 3);
	}

	/**
	 * Draw power pills.
	 */
	private void drawPowerPills() {
		int[] powerPillIndices = game.getPowerPillIndices();

		bufferGraphics.setColor(Color.white);

		for (int i = 0; i < powerPillIndices.length; i++)
			if (game.isPowerPillStillAvailable(i))
				bufferGraphics.fillOval(game.getNodeXCood(powerPillIndices[i]) * MAG + 4, game.getNodeYCood(powerPillIndices[i]) * MAG + 8, 3, 3);
	}

	/**
	 * Draw pac man.
	 */
	private void drawPacMan() {
		int pacLoc = game.getPacmanCurrentNodeIndex();

		MOVE tmpLastPacManMove = game.getPacmanLastMoveMade();

		if (tmpLastPacManMove != MOVE.NEUTRAL)
			lastPacManMove = tmpLastPacManMove;

		bufferGraphics.drawImage(images.getPacMan(lastPacManMove, time), game.getNodeXCood(pacLoc) * MAG - 1, game.getNodeYCood(pacLoc) * MAG + 3, null);
	}

	private void drawPacManPredictions() {
		if (predictions == null) {
			predictions = new GhostPredictions(game.getCurrentMaze());
			predictionTicks = game.getCurrentLevelTime();
		}

		// Update them
		while(game.getCurrentLevelTime() > predictionTicks) {
//            System.out.println("Updating");
			long time = System.currentTimeMillis();
			predictions.update();
			System.out.println("Took: " + (System.currentTimeMillis() - time));
			predictionTicks++;
		}


		// Make observations
		Game po = game.copy(new PacMan(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade(), game.getPacmanNumberOfLivesRemaining(), true));
		// Check the 4 ghosts
		for (GHOST ghost : GHOST.values()) {
			int ghostIndex = po.getGhostCurrentNodeIndex(ghost);
			if (ghostIndex != -1) {
				predictions.observe(ghost, ghostIndex, po.getGhostLastMoveMade(ghost));
			} else {
				LinkedList<GhostLocation> locationList = new LinkedList<>(predictions.getGhostLocations(ghost));
				for (GhostLocation location : locationList) {
					if (po.isNodeObservable(location.getIndex()))
						predictions.observeNotPresent(ghost, location.getIndex());
				}
			}
		}

		// Draw it
		for (int i = 0; i < game.getNumberOfNodes(); i++) {
			double probability = predictions.calculate(i);
			if (probability > 1E-2) {
//                System.out.println("Have a probability: " + probability + " alpha: " + (int)(128 * probability));
//                bufferGraphics.setColor(new Color(255, 0, 0, (int) (Math.max(128 * probability, 255))));
				bufferGraphics.setColor(redAlphas[(int)Math.min(255*probability, 255)]);
				bufferGraphics.fillRect(
						game.getNodeXCood(i) * MAG - 1,
						game.getNodeYCood(i) * MAG + 3,
						14, 14
				);
			}
		}

//        System.out.println(predictions);
	}

	private void drawPacManVisibility() {
		Game pacmanGame = game.copy(new PacMan(0, game.getPacmanLastMoveMade(), game.getPacmanNumberOfLivesRemaining(), false));
		BufferedImage image = new BufferedImage(GV_WIDTH * MAG, GV_HEIGHT * MAG, BufferedImage.TYPE_4BYTE_ABGR);

		Graphics2D overlay = (Graphics2D) image.getGraphics();

		int pacmanLocation = game.getPacmanCurrentNodeIndex();
//        overlay.fillRect(0, 0, GV_WIDTH * MAG, GV_HEIGHT * MAG);

		overlay.setColor(Color.GRAY);
		for (int i = 0; i < game.getNumberOfNodes(); i++) {
			if (!pacmanGame.isNodeObservable(i)) {
				overlay.fillRect(
						game.getNodeXCood(i) * MAG - 1,
						game.getNodeYCood(i) * MAG + 3,
						14, 14);
			}
		}

//        original.setColor(Color.BLACK);
		overlay.setColor(Color.WHITE);
//        overlay.setColor(new Color(0, 0, 0, 0));

		overlay.setComposite(AlphaComposite.Clear);
		int totalVisisble = 0;
		for (MOVE move : MOVE.values()) {
			int nextPoint = pacmanLocation;
			while (pacmanGame.isNodeObservable(nextPoint)) {
				overlay.fillRect(
						game.getNodeXCood(nextPoint) * MAG - 1,
						game.getNodeYCood(nextPoint) * MAG + 3,
						14, 14
				);
//                Rectangle2D.Float rectangle = new Rectangle2D.Float(game.getNodeXCood(nextPoint) * MAG - 1, game.getNodeYCood(nextPoint) * MAG + 3, 14, 14);
//                bufferGraphics.clipRect(game.getNodeXCood(nextPoint) * MAG - 1, game.getNodeYCood(nextPoint) * MAG + 3, 14, 14);
				totalVisisble++;
				nextPoint = game.getNeighbour(nextPoint, move);
			}
		}
//
		System.out.println("Total: " + game.getNumberOfNodes() + " Visible: " + totalVisisble);
//        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OUT, 0.5f);
//        Graphics2D original = (Graphics2D) bufferGraphics;
//        overlay.setComposite(composite);
//        overlay.drawImage(offscreen, 0, 0, null);
		bufferGraphics.drawImage(image, 0, 0, null);

	}

	/**
	 * Draw ghosts.
	 */
	private void drawGhosts() {
		for (GHOST ghostType : GHOST.values()) {
			int currentNodeIndex = game.getGhostCurrentNodeIndex(ghostType);
			int nodeXCood = game.getNodeXCood(currentNodeIndex);
			int nodeYCood = game.getNodeYCood(currentNodeIndex);

//			if (game.getGhostEdibleTime(ghostType) > 0) {
//				//what is the second clause for????
//				if (game.getGhostEdibleTime(ghostType) < EDIBLE_ALERT && ((time % 6) / 3) == 0)
//					bufferGraphics.drawImage(images.getEdibleGhost(true, time), nodeXCood * MAG - 1, nodeYCood * MAG + 3, null);
//				else
//					bufferGraphics.drawImage(images.getEdibleGhost(false, time), nodeXCood * MAG - 1, nodeYCood * MAG + 3, null);
//			} else {
				int index = ghostType.ordinal();

				if (game.getGhostLairTime(ghostType) > 0)
					bufferGraphics.drawImage(images.getGhost(ghostType, game.getGhostLastMoveMade(ghostType), time), nodeXCood * MAG - 1 + (index * 5), nodeYCood * MAG + 3, null);
				else
					bufferGraphics.drawImage(images.getGhost(ghostType, game.getGhostLastMoveMade(ghostType), time), nodeXCood * MAG - 1, nodeYCood * MAG + 3, null);
			//}
		}
	}

	/**
	 * Draw lives.
	 */
	private void drawLives() {
		for (int i = 0; i < game.getPacmanNumberOfLivesRemaining() - 1; i++) //-1 as lives remaining includes the current life
			bufferGraphics.drawImage(images.getPacManForExtraLives(), 210 - (30 * i) / 2, 260, null);
	}

	/**
	 * Draw game info.
	 */
	private void drawGameInfo() {
		bufferGraphics.setColor(Color.WHITE);
		bufferGraphics.drawString("S: ", 4, 271);
		bufferGraphics.drawString("" + game.getScore(), 16, 271);
		bufferGraphics.drawString("L: ", 78, 271);
		bufferGraphics.drawString("" + (game.getCurrentLevel() + 1), 90, 271);
		bufferGraphics.drawString("T: ", 116, 271);
		bufferGraphics.drawString("" + game.getCurrentLevelTime(), 129, 271);
	}

	/**
	 * Draw game over.
	 */
	private void drawGameOver() {
		bufferGraphics.setColor(Color.WHITE);
		bufferGraphics.drawString("Game Over", 80, 150);
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#getPreferredSize()
	 */
	public Dimension getPreferredSize() {
		return new Dimension(GV_WIDTH * MAG, GV_HEIGHT * MAG + 20);
	}

	/**
	 * Show game.
	 *
	 * @return the game view
	 */
	public GameView showGame() {
		this.frame = new GameFrame(this);

		//just wait for a bit for player to be ready
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
		}

		return this;
	}

	/**
	 * Gets the frame.
	 *
	 * @return the frame
	 */
	public GameFrame getFrame() {
		return frame;
	}

	private static class DebugPointer {
		public int x, y;
		public Color color;

		public DebugPointer(int x, int y, Color color) {
			this.x = x;
			this.y = y;
			this.color = color;
		}
	}

	private static class DebugLine {
		public int x1, y1, x2, y2;
		public Color color;

		public DebugLine(int x1, int y1, int x2, int y2, Color color) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.color = color;
		}
	}

	/**
	 * The Class GameFrame.
	 */
	public class GameFrame extends JFrame {
		/**
		 * Instantiates a new game frame.
		 *
		 * @param comp the comp
		 */
		public GameFrame(JComponent comp) {
			getContentPane().add(BorderLayout.CENTER, comp);
			pack();
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			this.setLocation((int) (screen.getWidth() * 3 / 8), (int) (screen.getHeight() * 3 / 8));
			this.setVisible(true);
			this.setResizable(false);
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			repaint();
		}
	}

	public class Images {
		private EnumMap<MOVE, BufferedImage[]> pacman;
		private EnumMap<GHOST, EnumMap<MOVE, BufferedImage[]>> ghosts;
		private BufferedImage[] edibleGhosts, edibleBlinkingGhosts, mazes;

		public Images() {
			pacman = new EnumMap<MOVE, BufferedImage[]>(MOVE.class);

			pacman.put(MOVE.UP, new BufferedImage[]{_loadImage("mspacman-up-normal.png"),
					_loadImage("mspacman-up-open.png"),
					_loadImage("mspacman-up-closed.png")});

			pacman.put(MOVE.RIGHT, new BufferedImage[]{_loadImage("mspacman-right-normal.png"),
					_loadImage("mspacman-right-open.png"),
					_loadImage("mspacman-right-closed.png")});

			pacman.put(MOVE.DOWN, new BufferedImage[]{_loadImage("mspacman-down-normal.png"),
					_loadImage("mspacman-down-open.png"),
					_loadImage("mspacman-down-closed.png")});

			pacman.put(MOVE.LEFT, new BufferedImage[]{_loadImage("mspacman-left-normal.png"),
					_loadImage("mspacman-left-open.png"),
					_loadImage("mspacman-left-closed.png")});

			ghosts = new EnumMap<GHOST, EnumMap<MOVE, BufferedImage[]>>(GHOST.class);

			ghosts.put(GHOST.BLINKY, new EnumMap<MOVE, BufferedImage[]>(MOVE.class));
			ghosts.get(GHOST.BLINKY).put(MOVE.UP, new BufferedImage[]{_loadImage("blinky-up-1.png"), _loadImage("blinky-up-2.png")});
			ghosts.get(GHOST.BLINKY).put(MOVE.RIGHT, new BufferedImage[]{_loadImage("blinky-right-1.png"), _loadImage("blinky-right-2.png")});
			ghosts.get(GHOST.BLINKY).put(MOVE.DOWN, new BufferedImage[]{_loadImage("blinky-down-1.png"), _loadImage("blinky-down-2.png")});
			ghosts.get(GHOST.BLINKY).put(MOVE.LEFT, new BufferedImage[]{_loadImage("blinky-left-1.png"), _loadImage("blinky-left-2.png")});

			ghosts.put(GHOST.PINKY, new EnumMap<MOVE, BufferedImage[]>(MOVE.class));
			ghosts.get(GHOST.PINKY).put(MOVE.UP, new BufferedImage[]{_loadImage("pinky-up-1.png"), _loadImage("pinky-up-2.png")});
			ghosts.get(GHOST.PINKY).put(MOVE.RIGHT, new BufferedImage[]{_loadImage("pinky-right-1.png"), _loadImage("pinky-right-2.png")});
			ghosts.get(GHOST.PINKY).put(MOVE.DOWN, new BufferedImage[]{_loadImage("pinky-down-1.png"), _loadImage("pinky-down-2.png")});
			ghosts.get(GHOST.PINKY).put(MOVE.LEFT, new BufferedImage[]{_loadImage("pinky-left-1.png"), _loadImage("pinky-left-2.png")});

			ghosts.put(GHOST.INKY, new EnumMap<MOVE, BufferedImage[]>(MOVE.class));
			ghosts.get(GHOST.INKY).put(MOVE.UP, new BufferedImage[]{_loadImage("inky-up-1.png"), _loadImage("inky-up-2.png")});
			ghosts.get(GHOST.INKY).put(MOVE.RIGHT, new BufferedImage[]{_loadImage("inky-right-1.png"), _loadImage("inky-right-2.png")});
			ghosts.get(GHOST.INKY).put(MOVE.DOWN, new BufferedImage[]{_loadImage("inky-down-1.png"), _loadImage("inky-down-2.png")});
			ghosts.get(GHOST.INKY).put(MOVE.LEFT, new BufferedImage[]{_loadImage("inky-left-1.png"), _loadImage("inky-left-2.png")});

			ghosts.put(GHOST.SUE, new EnumMap<MOVE, BufferedImage[]>(MOVE.class));
			ghosts.get(GHOST.SUE).put(MOVE.UP, new BufferedImage[]{_loadImage("sue-up-1.png"), _loadImage("sue-up-2.png")});
			ghosts.get(GHOST.SUE).put(MOVE.RIGHT, new BufferedImage[]{_loadImage("sue-right-1.png"), _loadImage("sue-right-2.png")});
			ghosts.get(GHOST.SUE).put(MOVE.DOWN, new BufferedImage[]{_loadImage("sue-down-1.png"), _loadImage("sue-down-2.png")});
			ghosts.get(GHOST.SUE).put(MOVE.LEFT, new BufferedImage[]{_loadImage("sue-left-1.png"), _loadImage("sue-left-2.png")});

			edibleGhosts = new BufferedImage[2];
			edibleGhosts[0] = _loadImage("edible-ghost-1.png");
			edibleGhosts[1] = _loadImage("edible-ghost-2.png");

			edibleBlinkingGhosts = new BufferedImage[2];
			edibleBlinkingGhosts[0] = _loadImage("edible-ghost-blink-1.png");
			edibleBlinkingGhosts[1] = _loadImage("edible-ghost-blink-2.png");

			mazes = new BufferedImage[4];
			for (int i = 0; i < mazes.length; i++)
				mazes[i] = _loadImage(mazeNames[i]);
		}

		public BufferedImage getPacMan(MOVE move, int time) {
			return pacman.get(move)[(time % 6) / 2];
		}

		public BufferedImage getPacManForExtraLives() {
			return pacman.get(MOVE.RIGHT)[0];
		}

		public BufferedImage getGhost(GHOST ghost, MOVE move, int time) {
			if (move == MOVE.NEUTRAL)
				return ghosts.get(ghost).get(MOVE.UP)[(time % 6) / 3];
			else
				return ghosts.get(ghost).get(move)[(time % 6) / 3];
		}

		public BufferedImage getEdibleGhost(boolean blinking, int time) {
			if (!blinking)
				return edibleGhosts[(time % 6) / 3];
			else
				return edibleBlinkingGhosts[(time % 6) / 3];
		}

		public BufferedImage getMaze(int mazeIndex) {
			return mazes[mazeIndex];
		}

		private BufferedImage _loadImage(String fileName) {
			BufferedImage image = null;

			try {
				InputStream in = getClass().getResourceAsStream(pathImages + "/" + fileName);
				image = ImageIO.read(in);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return image;
		}
	}
}