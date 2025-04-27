package app; 

import java.util.Random;
import java.util.Stack; 

/**
 * Class used to store all data about Combatants in a Combat Encounter to be put into CombatFrame.
 * 
 * @author PaladinGod8
 * @since version 1.0
 */
public class Combatant { 
    private String name; 
    private int initiative; 

    private Statistic hitPoints; 
    private Statistic manaPoints; 
    private Statistic actionPoints; 
    
    private int trueAC; 
    private Stack<Statistic> shield = new Stack<>(); //index: layer | Statistic.getCurrent() <- value (shield)
    private Stack<Statistic> armour = new Stack<>(); //index: layer | Statistic.getCurrent() <- value (armour)
    private int overhealHitPoints; 

    private String tokenImagePath; 

    // private int currentSpeedSpent; 
    // private Map<String, Integer> movementSpeeds; 

    // private int x_coordinate;
    // private int y_coordinate; //vertical coordinate
    // private int z_coordinate; 
    // private int elevation;    //relative to 0m on each individual combat terrain
    
    // private int hoverDuration; 
    // private boolean isHovering; 
    // private int turnsSpentFalling; 
    // private int fallHeightPerTurn; 
    
    // private int effectiveWeight; 
    

    //TODO: for movement feature later on: 
    /* 
     * private Statistic basicMovementSpeed; 
     * private Statistic currentUsedSpeed; 
     * private List<Statistic> uniqueMovementSpeeds; 
     * 
     * private int x_coordinate; 
     * private int y_coordinate;
     * private int z_coordinate; 
     * private int elevation;  
     * private int movementSPDspent = 0; 
     * 
     * private Statistic hoverDuration;    // ticks upwards until max is reached -> once reached - stop hovering unless replenish 
     * private boolean isHoveringState;    //always considered hovering while moving via FLYSPD. but once FLYSPD on turn all used - rely on hoverDuration else fall. 
     * 
     * private int turnsSpentFalling;      //every turn is 10s. So. All 10s x number of Turns spent falling
     * private int effectiveWeight;        //from inventory + base weight from race etc.
     * private int fallHeightPerTurn;      //generally 395m / 10s. so... always apply fall damage depending on height and calculation. take 0 damage if within falling threshold. 
     * 
     */

    public Combatant(String name, int maxHP, int maxMP, int maxAP) { 
        Random rand = new Random(); //if no initiative. roll it
        int minInitiative = 1; 
        int maxInitiative = 20; 

        int initiative = rand.nextInt(maxInitiative - minInitiative + 1) + minInitiative; 

        this.name = name; 
        this.initiative = initiative; 
        this.hitPoints = new Statistic(maxHP); 
        this.manaPoints = new Statistic(maxMP); 
        this.actionPoints = new Statistic(maxAP); 
        
        // this.currentSpeedSpent = 0; 
    }

    public Combatant(String name, int initiative, int maxHP, int maxMP, int maxAP) { 
        this.name = name; 
        this.initiative = initiative; 
        this.hitPoints = new Statistic(maxHP); 
        this.manaPoints = new Statistic(maxMP); 
        this.actionPoints = new Statistic(maxAP); 

        // this.currentSpeedSpent = 0; 
    }

    /** 
     * getter method for initiative
     * 
     * @return The initiative value of the combatant
     * @since version 1.0
     */
    public int getInitiative() { 
        return initiative; 
    }

    /** 
     * setter method for initiative
     * 
     * @param setValue The initiative value you wish to set this combatant to
     * @since version 1.0
     */
    public void setInitiative(int setValue) { 
        this.initiative = setValue;
    }

    /** 
     * getter method for name
     * 
     * @return The name of the combatant
     * @since version 1.0
     */
    public String getName() { 
        return this.name; 
    }

    /** 
     * getter method for HP 
     * 
     * @return The HP of the combatant as a Statistic
     * @since version 1.0
     */
    public Statistic getHP() { 
        return this.hitPoints; 
    }

    /** 
     * getter method for MP
     * 
     * @return The MP of the combatant as a Statistic
     * @since version 1.0
     */
    public Statistic getMP() { 
        return this.manaPoints; 
    }

    /** 
     * getter method for AP
     * 
     * @return The AP of the combatant as a Statistic
     * @since version 1.0
     */
    public Statistic getAP() { 
        return this.actionPoints; 
        
    }

    /** 
     * getter method for trueAC
     * 
     * @return The trueAC of the combatant as an integer
     * @since version 1.0
     */
    public int getTAC() { 
        return this.trueAC; 
    }

    /** 
     * setter method for trueAC
     * 
     * @param setValue The trueAC you wish to set as the trueAC of this combatant
     * @since version 1.0
     */
    public void setTAC(int setValue) { 
        this.trueAC = setValue; 
    }

    /**
     * method used to modify the trueAC value by the specified input. 
     * e.g. modifyTAC(-1) to trueAC = 12 means that tAC: 12 -> tAC: 11. 
     * 
     * @param value The value you want to modify the value of trueAC by.
     * @since version 1.0
     */
    public void modifyTAC(int value) { 
        this.trueAC += value;
    }

    /**
     * method used to add a Shield Layer to the combatant
     * e.g. 
     * before addShield(10):
     * Layer 1: 10
     * 
     * after addShield(10): 
     * Layer 1: 10 <- shield added here - pushed onto the shield Stack. 
     * Layer 2: 10
     * 
     * @param value The value of the shield layer added.
     * @since version 1.0
     */
    public void addShield(int value) { 
        this.shield.push(new Statistic(0, value));     //no max 
    }

    /**
     * getter method to obtain shield as a Stack<Statistic>
     * 
     * return cloned Stack<Statistic> to prevent direct mutation
     * 
     * @return The shield as a Stack<Statistic>
     * @since version 1.0
     */
    @SuppressWarnings("unchecked") 
    public Stack<Statistic> getShield() { 
        return (Stack<Statistic>) this.shield.clone(); //prevent direction mutation
    }

    /**
     * setter method to set a completely new shield in Stack<Statistic> form as this combatant's shield layers.
     * 
     * @param newShield The new shield to be used to store all Shield Layers for the Combatant 
     * @since version 1.0
     */
    public void setShield(Stack<Statistic> newShield) { 
        this.shield = newShield; 
    }

    /**
     * method used to calculate and return the total amount of shielding provided by all shield layers for a given combatant.
     * 
     * @return Total amount of shielding provided by all shield layers of the combatant
     * @since version 1.0
     */
    public int getTotalShield() { 
        return getShield().stream().mapToInt(Statistic::getCurrent).sum();
    }

    /**
     * method used to add a Armour Layer to the combatant
     * e.g. 
     * before addArmour(20):
     * Layer 1: 10
     * 
     * after addShield(20): 
     * Layer 1: 20 <- Armour added here - pushed onto the armour Stack. 
     * Layer 2: 10
     * 
     * @param maxValue The maximum value of the armour layer added - it will intialise to e.g. maxValue = 20 -> 20/20.
     * @since version 1.0
     */
    public void addArmour(int maxValue) { 
        this.armour.push(new Statistic(maxValue));     //no max
    }

    /**
     * getter method to obtain armour as a Stack<Statistic>
     * 
     * return cloned Stack<Statistic> to prevent direct mutation
     * 
     * @return The armour as a Stack<Statistic>
     * @since version 1.0
     */
    @SuppressWarnings("unchecked") 
    public Stack<Statistic> getArmour() { 
        return (Stack<Statistic>) this.armour.clone(); //prevent direction mutation
    }

    /**
     * setter method to set a completely new armour in Stack<Statistic> form as this combatant's armour layers.
     * 
     * @param newShield The new armour to be used to store all Armour Layers for the Combatant 
     * @since version 1.0
     */
    public void setArmour(Stack<Statistic> newArmour) { 
        this.armour = newArmour; 
    }

    /**
     * method used to calculate and return the total amount of current armour provided by all armour layers for a given combatant.
     * 
     * @return Total amount of current armour provided by all armour layers of the combatant. i.e, the amount of damage armour can take before reaching overheal and HP
     * @since version 1.0
     */
    public int getTotalAC() { 
        return getArmour().stream().mapToInt(Statistic::getCurrent).sum();
    }

    /**
     * method used to calculate and return the total amount of maximum armour provided by all armour layers for a given combatant.
     * 
     * @return Total amount of maximum armour provided by all armour layers of the combatant.
     * @since version 1.0
     */
    public int getTotalMaxAC() { 
        return getArmour().stream().mapToInt(Statistic::getMax).sum();
    }

    /** 
     * getter method for overheal HP
     * 
     * @return The overheal HP of the combatant as an integer
     * @since version 1.0
     */
    public int getOverheal() { 
        return this.overhealHitPoints; 
    }
    
    /** 
     * setter method for overheal HP
     * 
     * @param setValue The overheal HP you wish to set as the overheal HP of this combatant
     * @since version 1.0
     */
    public void setOverheal(int setValue) { 
        this.overhealHitPoints = setValue; 
    }

    /**
     * method used to modify the overheal HP value by the specified input. 
     * e.g. modifyOverheal(-1) to overheal = 20 means that overheal: 20 -> overheal: 19. 
     * 
     * @param value The value you want to modify the value of overheal HP by.
     * @since version 1.0
     */
    public void modifyOverheal(int value) { 
        this.overhealHitPoints += value; 
    }

    public String getTokenImagePath() {
        return tokenImagePath;
    }

    public void setTokenImagePath(String tokenImagePath) {
        this.tokenImagePath = tokenImagePath;
    }

    /**
     * overloaded method of takeDamage() that specifies only purely damage. 
     * 
     * it is assumed that this means there is no armour penetration. 
     * 
     * @param damage The amount of initial damage received by the combatant. 
     * @since version 1.0
     */
    public void takeDamage(int damage) { 
        takeDamage(damage, 0.0); 
    }

    /**
     * primary method of takeDamage() that specifies both damage and armour penetration.
     * 
     * this method is used to modify the Health Layers of a given combatant in the following order: 
     * 0. Damage Reduction (via e.g. TrueAC, Layer-0 Resistances, etc.) 
     * 1. Shield Layers (from Top-to-Bottom Layer - ignored by ArmourPenetration if it used)
     * 2. Armour Layers (from Top-to-Bottom Layer - ignored by ArmourPenetration if it used)
     * 3. Overheal (Overheal HP - set to 0 if ArmourPenetration is used)
     * 4. HP (normal curHP/maxHP - directly affected by ArmourPenetration if it is used)
     * 
     * Damage overflows from Shield Layer -> Armour Layer -> Overheal HP -> HP unless otherwise specified.
     * 
     * @param damage The amount of initial damage received by the combatant. 
     * @param armourPenetration the percentage of damage that penetrates armour and shield
     * @since version 1.0
     */
    public void takeDamage(int damage, double armourPenetration) { 
        //handle trueAC:
        int damageAfterReduction = damage - this.trueAC; 

        int damagePenetratingArmour = (int) (damageAfterReduction * armourPenetration); 
        int standardWorkflowDamage = (int) (damageAfterReduction * (1.0 - armourPenetration));

        //handle armour penetration: 
        if(armourPenetration > 0.0) { 
            this.overhealHitPoints = 0;
        }
        this.hitPoints.modifyCurrent(-damagePenetratingArmour);

        //handle shieldLayers: 
        while(!this.shield.isEmpty() && standardWorkflowDamage > 0) { 
            Statistic topShield = this.shield.peek(); 
            int smallerValue = Math.min(standardWorkflowDamage, topShield.getCurrent()); 
            topShield.modifyCurrent(-smallerValue);
            standardWorkflowDamage -= smallerValue; 
            
            if(topShield.getCurrent() <= 0) { 
                this.shield.pop(); //remove broken shields
            }
        }

        //handleArmourLayers:
        Stack<Statistic> tempArmourStack = new Stack<>();
        while(!this.armour.isEmpty() && standardWorkflowDamage > 0) {
            Statistic topArmour = this.armour.pop(); 
            int smallerValue = Math.min(standardWorkflowDamage, topArmour.getCurrent()); 
            topArmour.modifyCurrent(-smallerValue);
            standardWorkflowDamage -= smallerValue; 
    
            if(topArmour.getCurrent() <= 0) { 
                topArmour.setCurrent(0);
            }

            // Always keep armor, even if it hits 0. (minValue for Armour is 0)
            tempArmourStack.push(topArmour);
        } 

        // Restore armor stack in reverse to maintain order
        while (!tempArmourStack.isEmpty()) {
            this.armour.push(tempArmourStack.pop()); 
        }

        //handle overheal if applicable: 
        int damageAfterOverheal = standardWorkflowDamage - this.overhealHitPoints;
        int overhealAfterdamage = this.overhealHitPoints - standardWorkflowDamage; 

        this.overhealHitPoints = overhealAfterdamage; 
        
        if(this.overhealHitPoints <= 0) { 
            this.overhealHitPoints = 0; 
        }

        //handle damageToHP: 
        if(damageAfterOverheal >= 0) {
            hitPoints.modifyCurrent(-damageAfterOverheal);
        }
    }

    public boolean isDefeated() { 
        return this.hitPoints.getCurrent() <= 0;
    }

    public boolean isDead() { 
        int deathHP = this.hitPoints.getMax() * -2;
        boolean isAtDeathHP = false; 

        if(this.hitPoints.getCurrent() <= deathHP) { 
            isAtDeathHP = true; 
        }

        return isAtDeathHP; 
    }

    //Draft 1: in CombatFrame WIP
    // public int attack(int damage) { 
    //     //Note: check for 0AP is already in the CombatFrame

    //     this.actionPoints.modifyCurrent(-1);
    //     return damage; 
    // }

    //WIP for movement: 
    //getter and setter methods for coordinates and elevation etc.
    // public void move(int amountMoved, String direction) { 
    //     //WIP - account for 3D/Vertical Movemene etc. 

    //     // movementSPDspent -= amountMoved; 
    // }

    // public void resetCurrentSpeed() {
    //     this.currentSpeedSpent = 0; 
    // }

    // public void move(String movementType, int amountMoved, MovementDirection direction) {
    //     int currentUsedSpeed += amountMoved; 

    //     movementSpeeds.get(movementType); 

    //     return; 
    // }


    // private int currentSpeedSpent; 
    // private Map<String, Integer> movementSpeeds; 

    // private int x_coordinate;
    // private int y_coordinate; //vertical coordinate
    // private int z_coordinate; 
    // private int elevation;    //relative to 0m on each individual combat terrain
    
    // private int hoverDuration; 
    // private boolean isHovering; 
    // private int turnsSpentFalling; 
    // private int fallHeightPerTurn; 
    
    // private int effectiveWeight; 


    //when switching speed - do not replenish to full, decrease by max - movementSPDspent already. 
    // public void switchSpeed(Statistic newlySelectedSpeed) { 
    //     int newMaxSPD = newlySelectedSpeed.getMax(); 
    //     this.currentUsedSpeed = newlySelectedSpeed; 
    //     int newCurSPD = this.currentUsedSpeed.getCurrent() - movementSPDspent; 
    //     this.currentUsedSpeed.setCurrent(newCurSPD); 
    // }

    //how much does this combatant fall in 10s (1 turn = 10s) - ticks every turn. 
    // private static final double gravitationalConstant = 9.8
    // public void calculateFallHeight() { 
        


    // }
    //roughly 490m for every turn regardless of mass. 
    //surface area reduces the fall height. 

    

    @Override
    public String toString() {
        return name;  // Just name — label logic moves to renderer
    }
}



// LEGACY toString() Method: 
// @Override
// public String toString() {
//     return name;  // Just name — label logic moves to renderer
//     // String formattedString = "%s (Initiative: %d | HP: %d/%d (+%d) | MP :%d/%d | AP: %d/%d | AC: %d/%d | Shielding: %d)";
//     // String result = String.format(formattedString, name, initiative, this.hitPoints.getCurrent(), this.hitPoints.getMax(), this.getOverheal(), this.manaPoints.getCurrent(), this.manaPoints.getMax(), this.actionPoints.getCurrent(), this.actionPoints.getMax(), this.getTotalAC(), this.getTotalMaxAC(), this.getTotalShield()); 
//     // return result; 
// }
// }
