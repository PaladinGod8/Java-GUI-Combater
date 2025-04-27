package app;

import java.awt.image.BufferedImage;

public class Token {
    private BufferedImage image;
    private int gridX;
    private int gridY;

    private Combatant associatedCombatant;

    public Token(BufferedImage image, Combatant combatant) {
        this.image = image;
        this.associatedCombatant = combatant;
    }
    
    public BufferedImage getImage() {
        return image;
    }

    public Combatant getAssociatedCombatant() {
        return associatedCombatant;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public void setGridPosition(int x, int y) {
        this.gridX = x;
        this.gridY = y;
    }
}

