package bangsparks.com.jarchess;

/**
 *
 * @author mylesbartlett72
 */
// and StackOverflow
import bangsparks.com.jarchess.exceptions.BadClickPointerException;
import bangsparks.com.jarchess.movevalidate.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.*;
import javax.imageio.ImageIO;

public class Main {
    
    private final JPanel gui = new JPanel(new BorderLayout(3, 3));
    private final JButton[][] chessBoardSquares = new JButton[8][8];
    private final Image[][] chessPieceImages = new Image[2][6];
    private JPanel chessBoard;
    private final JLabel message = new JLabel(
            ">ready to play");
    private static final String COLS = "ABCDEFGH";
    private static final int KING = 0, QUEEN = 1,
            ROOK = 2, KNIGHT = 3, BISHOP = 4, PAWN = 5;
    private static final int[] STARTING_ROW = {
        ROOK, KNIGHT, BISHOP, QUEEN, KING, BISHOP, KNIGHT, ROOK
    };
    private static final int BLACK = 0, WHITE = 1;
    private Object firstClickCol = null, firstClickRow = null, firstClickPiece = null, firstClickPieceColour = null;
    private boolean secondClick = false;
    private boolean isBlackTurn = false;
    private Spot firstClickAsSpot = null;
    private String squareDefaultColour = null;
    private String badMoveMessageSuffix = null;
    private boolean attemptingCastle = false;

    Board board = new Board(); // Thanks, jaeheonshim!  Sorry I can't make Maven or Gradle work :(.  But your code is MIT licensed, so it doesn't affect me!
    
    public final void initializeGui() {
        // create the images for the chess pieces
        createImages();

        // set up the main GUI
        gui.setBorder(new EmptyBorder(5, 5, 5, 5));
        JToolBar tools = new JToolBar();
        tools.setFloatable(false);
        gui.add(tools, BorderLayout.PAGE_START);
        Action newGameAction = new AbstractAction("setup a new match") {

            @Override
            public void actionPerformed(ActionEvent e) {
                setupNewGame();
            }
        };
        
        Action clearBoardAction = new AbstractAction("clear") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                clearBoard();
            }
        };
        
        Action gridClickAction;
        gridClickAction = new AbstractAction(){
            
            @Override
            public void actionPerformed(ActionEvent e) { // When a square is clicked
                JButton btn = (JButton) e.getSource(); // Get the button clicked
                Object clickCol = btn.getClientProperty("column"), clickRow = btn.getClientProperty("row"); // Get the column and row of the button clicked
                Object clickPiece = btn.getClientProperty("piece"), clickPieceColour = btn.getClientProperty("piececolour"); // Get the piece and the piece's colour
                System.out.println("clicked column " + btn.getClientProperty("column")
                        + ", row " + btn.getClientProperty("row")); // Debug code
                Spot clickSpot = board.getSpot((int) clickCol, rowTranslate((int) clickRow));
                System.out.println(clickSpot);
                if ((firstClickCol==clickCol && firstClickRow == clickRow) && secondClick) {
                    // Reset the clicked square by clicking it again
                    firstClickCol=null;
                    firstClickRow=null;
                    firstClickPieceColour=null;
                    firstClickPiece=null;
                    secondClick=false;
                    int targetCol = (int) clickCol;
                    int targetRow = (int) clickRow;
                    String squareDefaultColour = (String) chessBoardSquares[targetCol][targetRow].getClientProperty("colour");
                    // Reset square colour (it is green when selected)
                    if ("white".equals(squareDefaultColour)) {
                        chessBoardSquares[targetCol][targetRow].setBackground(Color.WHITE);
                    } else {
                        chessBoardSquares[targetCol][targetRow].setBackground(Color.BLACK);
                    }
                } else if (!secondClick && clickPieceColour==null) {
                    System.out.println("Nothing on square to click.");
                    message.setText(">clicked empty square");
                } else if ((clickCol!=firstClickCol || clickRow!=firstClickRow) && secondClick) {
                    // Attempt to move piece
                    int targetCol0 = (int) firstClickCol;
                    int targetRow0 = (int) firstClickRow;
                    int targetCol1 = (int) clickCol;
                    int targetRow1 = (int) clickRow;
                    int targetPiece = (int) firstClickPiece;
                    int targetPieceColour = (int) firstClickPieceColour;
                    String squareDefaultColour = (String) chessBoardSquares[targetCol0][targetRow0].getClientProperty("colour");
                    if (chessBoardSquares[targetCol1][targetRow1].getClientProperty("piececolour") != null && chessBoardSquares[targetCol1][targetRow1].getClientProperty("piececolour") == firstClickPieceColour) {
                        message.setText(">cannot take your own piece");
                    } else if (!board.canMove(firstClickAsSpot, clickSpot)) {
                        switch (targetPiece) {
                            case KING: badMoveMessageSuffix = "king";
                            case QUEEN: badMoveMessageSuffix = "queen";
                            case ROOK: badMoveMessageSuffix = "rook";
                            case KNIGHT: badMoveMessageSuffix = "knight";
                            case BISHOP: badMoveMessageSuffix = "bishop";
                            case PAWN: badMoveMessageSuffix = "pawn";
                        }
                        message.setText(">invalid move for " + badMoveMessageSuffix);
                    } else {
                        // Move piece
                        attemptingCastle = targetPiece == KING && !isMoveNear(targetCol0, targetRow0, targetCol1, targetRow1);
                        board.move(firstClickAsSpot, clickSpot);
                        chessBoardSquares[targetCol0][targetRow0].setIcon(null);
                        chessBoardSquares[targetCol1][targetRow1].setIcon(new ImageIcon(
                                chessPieceImages[targetPieceColour][targetPiece]));
                        chessBoardSquares[targetCol1][targetRow1].putClientProperty("piece", targetPiece);
                        chessBoardSquares[targetCol1][targetRow1].putClientProperty("piececolour", targetPieceColour);
                        chessBoardSquares[targetCol0][targetRow0].putClientProperty("piece", null);
                        chessBoardSquares[targetCol0][targetRow0].putClientProperty("piececolour", null);
                        if (attemptingCastle) {
                            if (targetCol1 == 2 && targetPieceColour == BLACK){
                                chessBoardSquares[0][0].setIcon(null);
                                chessBoardSquares[3][0].setIcon(new ImageIcon(
                                chessPieceImages[targetPieceColour][ROOK]));
                                chessBoardSquares[0][0].putClientProperty("piece", null);
                                chessBoardSquares[0][0].putClientProperty("piececolour", null);
                                chessBoardSquares[3][0].putClientProperty("piece", ROOK);
                                chessBoardSquares[3][0].putClientProperty("piececolour", targetPieceColour);
                            }
                            if (targetCol1 == 6 && targetPieceColour == BLACK){
                                chessBoardSquares[7][0].setIcon(null);
                                chessBoardSquares[5][0].setIcon(new ImageIcon(
                                chessPieceImages[targetPieceColour][ROOK]));
                                chessBoardSquares[7][0].putClientProperty("piece", null);
                                chessBoardSquares[7][0].putClientProperty("piececolour", null);
                                chessBoardSquares[5][0].putClientProperty("piece", ROOK);
                                chessBoardSquares[5][0].putClientProperty("piececolour", targetPieceColour);
                            }
                            if (targetCol1 == 2 && targetPieceColour == WHITE){
                                chessBoardSquares[0][7].setIcon(null);
                                chessBoardSquares[3][7].setIcon(new ImageIcon(
                                chessPieceImages[targetPieceColour][ROOK]));
                                chessBoardSquares[0][7].putClientProperty("piece", null);
                                chessBoardSquares[0][7].putClientProperty("piececolour", null);
                                chessBoardSquares[3][7].putClientProperty("piece", ROOK);
                                chessBoardSquares[3][7].putClientProperty("piececolour", targetPieceColour);
                            }
                            if (targetCol1 == 6 && targetPieceColour == WHITE){
                                chessBoardSquares[7][7].setIcon(null);
                                chessBoardSquares[5][7].setIcon(new ImageIcon(
                                chessPieceImages[targetPieceColour][ROOK]));
                                chessBoardSquares[7][7].putClientProperty("piece", null);
                                chessBoardSquares[7][7].putClientProperty("piececolour", null);
                                chessBoardSquares[5][7].putClientProperty("piece", ROOK);
                                chessBoardSquares[5][7].putClientProperty("piececolour", targetPieceColour);
                            }
                            
                        }
                        firstClickPieceColour=null;
                        firstClickPiece=null;
                        secondClick=false;
                        if (targetPieceColour==BLACK) {
                            isBlackTurn=false;
                            message.setText(">white turn");
                        } else {
                            isBlackTurn=true;
                            message.setText(">black turn");
                        }
                        if ("white".equals(squareDefaultColour)) {
                            chessBoardSquares[targetCol0][targetRow0].setBackground(Color.WHITE);
                        } else {
                            chessBoardSquares[targetCol0][targetRow0].setBackground(Color.BLACK);
                        }
                    }
                } else if (!secondClick) {
                    // First Click
                    int clickedPieceColour = (int) clickPieceColour;
                    if ((isBlackTurn && clickedPieceColour==BLACK)||((!isBlackTurn)&&clickedPieceColour==WHITE)){
                        firstClickAsSpot=clickSpot;
                        firstClickCol=clickCol;
                        firstClickRow=clickRow;
                        firstClickPieceColour=clickPieceColour;
                        firstClickPiece=clickPiece;
                        secondClick=true;
                        int targetCol = (int) firstClickCol;
                        int targetRow = (int) firstClickRow;
                        chessBoardSquares[targetCol][targetRow].setBackground(Color.GREEN);
                        String squareDefaultColour = (String) chessBoardSquares[targetCol][targetRow].getClientProperty("colour");
                    } else {
                        System.out.println("Not your turn.");
                        message.setText(">wait your turn");
                    }
                } else {
                    throw new BadClickPointerException("Invalid click pointers.");
                }
            }
        };
        
        tools.add(newGameAction);
        /*tools.add(new JButton("*alters magnetic fields of hard disk*")); // TODO - add functionality!
        tools.add(new JButton("today is international backup day")); // TODO - add functionality!
        tools.addSeparator();
        tools.add(new JButton("jump off tall building")); // TODO - add functionality!*/
        tools.add(clearBoardAction);
        tools.addSeparator();
        
        message.setBackground(Color.black);
        message.setForeground(Color.green);
        message.setOpaque(true);
        
        tools.add(message);
        
        //gui.add(new JLabel("?"), BorderLayout.LINE_START); // don't know what this was meant to do

        chessBoard = new JPanel(new GridLayout(0, 9)) {

            /**
             * Override the preferred size to return the largest it can, in
             * a square shape.  Must (must, must) be added to a GridBagLayout
             * as the only component (it uses the parent as a guide to size)
             * with no GridBagConstaint (so it is centered).
             */
            @Override
            public final Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                Dimension prefSize = null;
                Component c = getParent();
                if (c == null) {
                    prefSize = new Dimension(
                            (int)d.getWidth(),(int)d.getHeight());
                } else if (c!=null &&
                        c.getWidth()>d.getWidth() &&
                        c.getHeight()>d.getHeight()) {
                    prefSize = c.getSize();
                } else {
                    prefSize = d;
                }
                int w = (int) prefSize.getWidth();
                int h = (int) prefSize.getHeight();
                // the smaller of the two sizes
                int s = (w>h ? h : w);
                return new Dimension(s,s);
            }
        };
        chessBoard.setBorder(new CompoundBorder(
                new EmptyBorder(8,8,8,8),
                new LineBorder(Color.BLACK)
                ));
        // Set the BG to be ochre
        Color ochre = new Color(204,119,34);
        chessBoard.setBackground(ochre);
        JPanel boardConstrain = new JPanel(new GridBagLayout());
        boardConstrain.setBackground(ochre);
        boardConstrain.add(chessBoard);
        gui.add(boardConstrain);
        
        
        // create the chess board squares
        Insets buttonMargin = new Insets(0, 0, 0, 0);
        for (int ii = 0; ii < chessBoardSquares.length; ii++) {
            for (int jj = 0; jj < chessBoardSquares[ii].length; jj++) {
                JButton b = new JButton();
                b.setMargin(buttonMargin);
                // our chess pieces are 64x64 px in size, so we'll
                // 'fill this in' using a transparent icon.
                ImageIcon icon = new ImageIcon(
                        new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
                b.setIcon(icon);
                chessBoardSquares[jj][ii] = b;
                chessBoardSquares[jj][ii].putClientProperty("column", jj);
                chessBoardSquares[jj][ii].putClientProperty("row", ii);
                chessBoardSquares[jj][ii].addActionListener(gridClickAction);

            }
        }
        resetSquareColours();
        /*
         * fill the chess board
         */
        chessBoard.add(new JLabel(""));
        // fill the top row
        for (int ii = 0; ii < 8; ii++) {
            chessBoard.add(
                    new JLabel(COLS.substring(ii, ii + 1),
                    SwingConstants.CENTER));
        }
        // fill the black non-pawn piece row
        for (int ii = 0; ii < 8; ii++) {
            for (int jj = 0; jj < 8; jj++) {
                switch (jj) {
                    case 0:
                        chessBoard.add(new JLabel("" + (9-(ii + 1)),
                                SwingConstants.CENTER));
                    default:
                        chessBoard.add(chessBoardSquares[jj][ii]);
                }
            }
        }
    }

    public final JComponent getGui() {
        initializeGui(); // Property of tetra the programmer.  Unauthorized removal is prohibited, mainly because by removing it you will break the entire f***ing GUI.  Seriously.
        return gui; // Don't know.  Don't care.  Don't touch.
    }
    
    public final void resetSquareColours() {
        for (int ii = 0; ii < chessBoardSquares.length; ii++) {
            for (int jj = 0; jj < chessBoardSquares[ii].length; jj++) {
                if ((jj % 2 == 1 && ii % 2 == 1) || (jj % 2 == 0 && ii % 2 == 0)) {
                    chessBoardSquares[jj][ii].setBackground(Color.WHITE);
                    chessBoardSquares[jj][ii].putClientProperty("colour", "white");
                } else {
                    chessBoardSquares[jj][ii].setBackground(Color.BLACK);
                    chessBoardSquares[jj][ii].putClientProperty("colour", "black");
                }
            }
        }
    }
    
    private void createImages() {
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(this.getClass().getResourceAsStream("/chess_spritesheet.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        for (int ii = 0; ii < 2; ii++) {
            for (int jj = 0; jj < 6; jj++) {
                chessPieceImages[ii][jj] = bi.getSubimage(
                        jj * 64, ii * 64, 64, 64);
            }
        }
    }
    
    /**
     * Initializes the icons of the initial chess board piece places
     */
    private void setupNewGame() {
        message.setText(">awaiting move");
        board = new Board(); // Reset board.  The first definition is required to avoid a compiler error, even though it is never actually useful otherwise.
        firstClickCol = null;
        firstClickRow = null;
        firstClickPiece = null;
        firstClickPieceColour = null;
        secondClick = false;
        isBlackTurn = false;
        // set up the black pieces
        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
            chessBoardSquares[ii][0].setIcon(new ImageIcon(
                    chessPieceImages[BLACK][STARTING_ROW[ii]]));
            chessBoardSquares[ii][0].putClientProperty("piece", STARTING_ROW[ii]);
            chessBoardSquares[ii][0].putClientProperty("piececolour", BLACK);
        }
        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
            chessBoardSquares[ii][1].setIcon(new ImageIcon(
                    chessPieceImages[BLACK][PAWN]));
            chessBoardSquares[ii][1].putClientProperty("piece", PAWN);
            chessBoardSquares[ii][1].putClientProperty("piececolour", BLACK);
        }
        // set up the white pieces
        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
            chessBoardSquares[ii][6].setIcon(new ImageIcon(
                    chessPieceImages[WHITE][PAWN]));
            chessBoardSquares[ii][6].putClientProperty("piece", PAWN);
            chessBoardSquares[ii][6].putClientProperty("piececolour", WHITE);
        }
        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
            chessBoardSquares[ii][7].setIcon(new ImageIcon(
                    chessPieceImages[WHITE][STARTING_ROW[ii]]));
            chessBoardSquares[ii][7].putClientProperty("piece", STARTING_ROW[ii]);
            chessBoardSquares[ii][7].putClientProperty("piececolour", WHITE);
        }
        // Blank other squares
        resetSquareColours();
        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
            for (int jj = 2; jj < 6; jj++) {
                chessBoardSquares[ii][jj].setIcon(null);
                chessBoardSquares[ii][jj].putClientProperty("piece", null);
                chessBoardSquares[ii][jj].putClientProperty("piececolour", null);
            }
        }
    }
    
    public boolean isMoveNear(int sourcecol, int sourcerow, int destcol, int destrow){
        boolean retval = false;
        int[][] offsets = {
            {1,0},
            {1,1},
            {0,1},
            {1,-1},
            {-1,-1},
            {-1,1},
            {0,-1},
            {-1,0}
        };
        for (int[] offset : offsets) { // Protip: This is apparently how to iterate over arrays. (a multidimensional array is an array of arrays)
            if (sourcecol + offset[0] == destcol && sourcerow + offset[1] == destrow) {
                retval = true;
            }
        }
        return retval;
    }
    
    private int rowTranslate(int row) {
        // You are welcome to make this an array rather than a function, as it should be.
        int[] translations = {
            7,
            6,
            5,
            4,
            3,
            2,
            1,
            0
        };
        return translations[row];
    }
    
    private void clearBoard(){
        for (int ii = 0; ii < STARTING_ROW.length; ii++) {
            for (int jj = 0; jj < 8; jj++){
                chessBoardSquares[ii][jj].setIcon(null);
                chessBoardSquares[ii][jj].putClientProperty("piece", null);
                chessBoardSquares[ii][jj].putClientProperty("piececolour", null);
                message.setText(">board cleared");
            }
        }
    }
    
    public static void main(String[] args) {
        // Thank you StackOverflow, for writing this while I was still trying to understand Java.
        Runnable r;
        r = () -> {
            Main cg = new Main();
            
            JFrame f = new JFrame("Chess");
            f.add(cg.getGui());
            // Ensures JVM closes after frame(s) closed and
            // all non-daemon threads are finished
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.setLocationByPlatform(true); // See https://stackoverflow.com/a/7143398/418556 for demo.
            
            // ensures the frame is the minimum size it needs to be
            // in order display the components within it
            f.pack();
            // ensures the minimum size is enforced.
            f.setMinimumSize(f.getSize());
            f.setVisible(true);
        };
        // Swing GUIs should be created and updated on the EDT
        // http://docs.oracle.com/javase/tutorial/uiswing/concurrency
        SwingUtilities.invokeLater(r);
    }
}