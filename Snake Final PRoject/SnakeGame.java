import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {
    private class Tile {
        int x;
        int y;

        Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    int boardWidth;
    int boardHeight;
    int tileSize = 30; // Customize the size

    // Snake
    Tile snakeHead;
    ArrayList<Tile> snakeBody;
    BufferedImage snakeHeadImage; // Image for snake head

    // Food
    Tile food;
    Random random;

    // Game logic
    int velocityX;
    int velocityY;
    Timer gameLoop;

    boolean gameOver = false;

    Clip backgroundMusic;
    BufferedImage backgroundImage; // Image for background

    SnakeGame(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        setPreferredSize(new Dimension(this.boardWidth, this.boardHeight));
        setBackground(Color.black);
        addKeyListener(this);
        setFocusable(true);

        snakeHead = new Tile(5, 5);
        snakeBody = new ArrayList<>();

        food = new Tile(10, 10);
        random = new Random();
        placeFood();

        velocityX = 1;
        velocityY = 0;

        // Load background image
        try {
            backgroundImage = ImageIO.read(new File("Material/background.jpg")); // Update the path to your background image
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
            e.printStackTrace();
        }

        // Load snake head image
        try {
            // Use a relative path to load the image
            snakeHeadImage = ImageIO.read(new File("Material/Snakebluehead.jpg"));
        } catch (IOException e) {
            System.err.println("Error loading snake head image: " + e.getMessage());
            e.printStackTrace();
        }

        // Game timer
        gameLoop = new Timer(100, this);
        gameLoop.start();

        playBackgroundMusic("Material/Lobby Music (Original Soundtrack).wav");
    }

    public void playBackgroundMusic(String filePath) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInputStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Draw the background image
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, boardWidth, boardHeight, this);
        }

        // Draw the grid
        for (int i = 0; i < boardWidth / tileSize; i++) {
            g.drawLine(i * tileSize, 0, i * tileSize, boardHeight);
            g.drawLine(0, i * tileSize, boardWidth, i * tileSize);
        }

        // Draw the food
        g.setColor(Color.red);
        g.fill3DRect(food.x * tileSize, food.y * tileSize, tileSize, tileSize, true);

        // Draw the snake head
        if (snakeHeadImage != null) {
            BufferedImage rotatedHead = getRotatedImage(snakeHeadImage);
            g.drawImage(rotatedHead, snakeHead.x * tileSize, snakeHead.y * tileSize, tileSize, tileSize, null);
        } else {
            // Fallback to a rectangle if the image fails to load
            g.setColor(Color.blue);
            g.fill3DRect(snakeHead.x * tileSize, snakeHead.y * tileSize, tileSize, tileSize, true);
        }

        // Draw the snake body
        g.setColor(Color.blue);
        for (Tile snakePart : snakeBody) {
            g.fill3DRect(snakePart.x * tileSize, snakePart.y * tileSize, tileSize, tileSize, true);
        }

        // Draw the score and game over message
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        if (gameOver) {
            g.setColor(Color.red);
            g.drawString("Game Over: " + snakeBody.size(), tileSize - 16, tileSize);
        } else {
            g.setColor(Color.white); // Ensuring score text is visible
            g.drawString("Score: " + snakeBody.size(), tileSize - 16, tileSize);
        }
    }

    private BufferedImage getRotatedImage(BufferedImage image) {
        double rotationRequired = 0;
        if (velocityX == 1) {
            rotationRequired = Math.PI / 2;
        } else if (velocityX == -1) {
            rotationRequired = -Math.PI / 2;
        } else if (velocityY == 1) {
            rotationRequired = Math.PI;
        } else if (velocityY == -1) {
            rotationRequired = 0;
        }
        double locationX = image.getWidth() / 2;
        double locationY = image.getHeight() / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(image, null);
    }

    public void placeFood() {
        food.x = random.nextInt(boardWidth / tileSize);
        food.y = random.nextInt(boardHeight / tileSize);
    }

    public void move() {
        if (collision(snakeHead, food)) {
            snakeBody.add(new Tile(food.x, food.y));
            placeFood();

            // Play the eat sound effect
            playSoundEffect("Material/Eating sfx.wav");
        }

        for (int i = snakeBody.size() - 1; i >= 0; i--) {
            Tile snakePart = snakeBody.get(i);
            if (i == 0) {
                snakePart.x = snakeHead.x;
                snakePart.y = snakeHead.y;
            } else {
                Tile prevSnakePart = snakeBody.get(i - 1);
                snakePart.x = prevSnakePart.x;
                snakePart.y = prevSnakePart.y;
            }
        }

        snakeHead.x += velocityX;
        snakeHead.y += velocityY;

        for (Tile snakePart : snakeBody) {
            if (collision(snakeHead, snakePart)) {
                gameOver = true;
            }
        }

        if (snakeHead.x < 0 || snakeHead.x >= boardWidth / tileSize ||
                snakeHead.y < 0 || snakeHead.y >= boardHeight / tileSize) {
            gameOver = true;
        }
    }

    public void playSoundEffect(String filePath) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean collision(Tile tile1, Tile tile2) {
        return tile1.x == tile2.x && tile1.y == tile2.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
            stopBackgroundMusic();
            showRestartDialog();
        }
    }

    public void showRestartDialog() {
        int response = JOptionPane.showOptionDialog(
                this,
                "Game Over! Do you want to restart?",
                "Game Over",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[] {"Restart", "Exit"},
                "Restart"
        );

        if (response == JOptionPane.YES_OPTION) {
            resetGame();
        } else {
            System.exit(0);
        }
    }

    public void resetGame() {
        snakeHead = new Tile(5, 5);
        snakeBody.clear();
        placeFood();
        velocityX = 1;
        velocityY = 0;
        gameOver = false;
        gameLoop.start();
        playBackgroundMusic("Material/Lobby Music (Original Soundtrack).wav");
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP && velocityY != 1) {
            velocityX = 0;
            velocityY = -1;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && velocityY != -1) {
            velocityX = 0;
            velocityY = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && velocityX != 1) {
            velocityX = -1;
            velocityY = 0;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && velocityX != -1) {
            velocityX = 1;
            velocityY = 0;
        } else if (e.getKeyCode() == KeyEvent.VK_1) { // changing the  speed of the snake
            gameLoop.setDelay(200); // Slow
        } else if (e.getKeyCode() == KeyEvent.VK_2) {
            gameLoop.setDelay(100); // Normal
        } else if (e.getKeyCode() == KeyEvent.VK_3) {
            gameLoop.setDelay(50);  // Fast
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}
