package app;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class GridPanel extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener {
    private int rows;
    private int cols;
    private int cellSize = 50;
    private double scale = 1.0;
    private int offsetX = 0;
    private int offsetY = 0;

    private CombatFrame combatFrame;
    private List<Token> tokens = new ArrayList<>();
    private Token selectedToken = null;
    private Token tokenToPlace = null;
    private Point draggingGhostGridPosition = null;       // temp position while dragging
    private boolean panning = false;
    private Point dragStart = null;

    private Point mousePosition = null;                   // track where mouse is hovering

    private TokenSelectionListener selectionListener;

    public GridPanel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        setBackground(Color.WHITE);
    }
    
    public void initialize() {
        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void resetGrid(int newRows, int newCols) {
        this.rows = newRows;
        this.cols = newCols;
        tokens.clear();
        selectedToken = null;
        scale = 1.0;
        offsetX = 0;
        offsetY = 0;
        revalidate();
        repaint();
    }

    public void prepareToPlaceToken(Token token) {
        this.tokenToPlace = token;
        this.mousePosition = null; // reset mouse
    }
    

    public void addToken(Token token, int gridX, int gridY) {
        token.setGridPosition(gridX, gridY);
        tokens.add(token);
        repaint();
    }

    public boolean isPanning() {
        return panning;
    }

    public int getCellSize() {
        return cellSize;
    }
    
    public double getScale() {
        return scale;
    }

    public Token getTokenForCombatant(Combatant combatant) {
        for (Token token : tokens) {
            if (token.getAssociatedCombatant() == combatant) {
                return token;
            }
        }
        return null;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);

        // Draw grid
        g2d.setColor(Color.LIGHT_GRAY);
        for (int col = 0; col <= cols; col++) {
            int x = col * cellSize;
            g2d.drawLine(x, 0, x, rows * cellSize);
        }
        for (int row = 0; row <= rows; row++) {
            int y = row * cellSize;
            g2d.drawLine(0, y, cols * cellSize, y);
        }

        // Draw tokens
        for (Token token : tokens) {
            int x = token.getGridX() * cellSize;
            int y = token.getGridY() * cellSize;
            
            if (token == selectedToken && draggingGhostGridPosition != null) {
                // Draw ghost token
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // semi-transparent
                g2d.drawImage(token.getImage(), draggingGhostGridPosition.x * cellSize, draggingGhostGridPosition.y * cellSize, cellSize, cellSize, null);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // reset opacity
            
            } else {
                g2d.drawImage(token.getImage(), x, y, cellSize, cellSize, null);
            }

            // Draw name
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String name = token.getAssociatedCombatant().getName();
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(name);
            g2d.drawString(name, x + (cellSize - textWidth) / 2, y + cellSize + fm.getAscent());

            // Highlight selected token
            if (token == selectedToken) {
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, cellSize, cellSize);
            }
        }

        // Draw see-through token following mouse
        if (tokenToPlace != null && mousePosition != null) {
            Composite oldComposite = g2d.getComposite(); // save old state
        
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // 50% transparent
        
            int mouseX = (int) ((mousePosition.x - offsetX) / scale);
            int mouseY = (int) ((mousePosition.y - offsetY) / scale);
        
            int gridX = (mouseX / cellSize) * cellSize;
            int gridY = (mouseY / cellSize) * cellSize;
        
            g2d.drawImage(tokenToPlace.getImage(), gridX, gridY, cellSize, cellSize, null);
        
            g2d.setComposite(oldComposite); // restore normal opacity
        }

        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        int width = (int) (cols * cellSize * scale);
        int height = (int) (rows * cellSize * scale);
        return new Dimension(width, height);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        if (notches < 0) {
            scale *= 1.1;
        } else {
            scale /= 1.1;
        }
        scale = Math.max(0.1, Math.min(scale, 5.0));
        revalidate();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (tokenToPlace != null) {
            int mouseX = (int) ((e.getX() - offsetX) / scale);
            int mouseY = (int) ((e.getY() - offsetY) / scale);
    
            int gridX = mouseX / cellSize;
            int gridY = mouseY / cellSize;
    
            tokenToPlace.setGridPosition(gridX, gridY);
            tokens.add(tokenToPlace);
    
            tokenToPlace = null; // clear
            repaint();
            return;
        }

        dragStart = e.getPoint();

        // Adjust for zoom and offset
        int mouseX = (int) ((e.getX() - offsetX) / scale);
        int mouseY = (int) ((e.getY() - offsetY) / scale);

        int gridX = mouseX / cellSize;
        int gridY = mouseY / cellSize;

        selectedToken = null;
        for (Token token : tokens) {
            if (token.getGridX() == gridX && token.getGridY() == gridY) {
                selectedToken = token;
                break;
            }
        }
        
        // Notify CombatFrame about selection
        if (selectionListener != null) {
            selectionListener.onTokenSelected(selectedToken);
        }
        
        repaint();        
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) {
            // Panning
            int dx = e.getX() - dragStart.x;
            int dy = e.getY() - dragStart.y;
            offsetX += dx;
            offsetY += dy;
            dragStart = e.getPoint();
            panning = true;              // Start panning
            repaint();

        } else if (selectedToken != null && SwingUtilities.isLeftMouseButton(e)) {
            // Smooth dragging ghost
            int mouseX = (int) ((e.getX() - offsetX) / scale);
            int mouseY = (int) ((e.getY() - offsetY) / scale);
    
            int gridX = mouseX / cellSize;
            int gridY = mouseY / cellSize;
    
            // Update ghost position
            draggingGhostGridPosition = new Point(gridX, gridY);
            repaint();
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (draggingGhostGridPosition != null && selectedToken != null) {
            int newX = Math.max(0, Math.min(draggingGhostGridPosition.x, cols - 1));
            int newY = Math.max(0, Math.min(draggingGhostGridPosition.y, rows - 1));
            selectedToken.setGridPosition(newX, newY);
        }
        panning = false; //Done panning
        draggingGhostGridPosition = null; // clear ghost
        dragStart = null;
        repaint();
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        if (tokenToPlace != null) {
            mousePosition = e.getPoint();
            repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    public void setCombatFrame(CombatFrame frame) {
        this.combatFrame = frame;
    }

    public void resetCamera() {
        offsetX = 0;
        offsetY = 0;
        scale = 1.0;
    }

    public void setTokenSelectionListener(TokenSelectionListener listener) {
        this.selectionListener = listener;
    }
}
