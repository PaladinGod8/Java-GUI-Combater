package app; 

import java.util.Random;
import java.util.Stack; 

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

    public Combatant(String name, int maxHP, int maxMP, int maxAP) { 
        Random rand = new Random(); //if no initiative. roll
        int minInitiative = 1; 
        int maxInitiative = 20; 

        int initiative = rand.nextInt(maxInitiative - minInitiative + 1) + minInitiative; 

        this.name = name; 
        this.initiative = initiative; 
        this.hitPoints = new Statistic(maxHP); 
        this.manaPoints = new Statistic(maxMP); 
        this.actionPoints = new Statistic(maxAP); 
    }

    public Combatant(String name, int initiative, int maxHP, int maxMP, int maxAP) { 
        this.name = name; 
        this.initiative = initiative; 
        this.hitPoints = new Statistic(maxHP); 
        this.manaPoints = new Statistic(maxMP); 
        this.actionPoints = new Statistic(maxAP); 

    }

    public int getInitiative() { 
        return initiative; 
    }

    public void setInitiative(int setValue) { 
        this.initiative = setValue;
    }

    public String getName() { 
        return this.name; 
    }

    public Statistic getHP() { 
        return this.hitPoints; 
    }

    public Statistic getMP() { 
        return this.manaPoints; 
    }

    public Statistic getAP() { 
        return this.actionPoints; 
        
    }

    public int getTAC() { 
        return this.trueAC; 
    }

    public void setTAC(int setValue) { 
        this.trueAC = setValue; 
    }

    public void modifyTAC(int value) { 
        this.trueAC += value;
    }

    public void addShield(int value) { 
        this.shield.push(new Statistic(0, value));     //no max 
    }

    @SuppressWarnings("unchecked") 
    public Stack<Statistic> getShield() { 
        return (Stack<Statistic>) this.shield.clone(); //prevent direction mutation
    }

    public void setShield(Stack<Statistic> newShield) { 
        this.shield = newShield; 
    }

    public int getTotalShield() { 
        return getShield().stream().mapToInt(Statistic::getCurrent).sum();
    }

    public void addArmour(int maxValue) { 
        this.armour.push(new Statistic(maxValue));     //no max
    }

    @SuppressWarnings("unchecked") 
    public Stack<Statistic> getArmour() { 
        return (Stack<Statistic>) this.armour.clone(); //prevent direction mutation
    }

    public void setArmour(Stack<Statistic> newArmour) { 
        this.armour = newArmour; 
    }

    public int getTotalAC() { 
        return getArmour().stream().mapToInt(Statistic::getCurrent).sum();
    }

    public int getTotalMaxAC() { 
        return getArmour().stream().mapToInt(Statistic::getMax).sum();
    }

    public int getOverheal() { 
        return this.overhealHitPoints; 
    }

    public void setOverheal(int setValue) { 
        this.overhealHitPoints = setValue; 
    }

    public void modifyOverheal(int value) { 
        this.overhealHitPoints += value; 
    }

    public void takeDamage(int damage) { 
        takeDamage(damage, 0.0); 
    }

    //0. Damage Reduction (e.g. TrueAC, Resistances, etc.)
    //1. Shields
    //2. Armour Layers
    //3. Overheal
    //4. HP
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

    @Override
    public String toString() {
        return name;  // Just name — label logic moves to renderer
    }
}

//DRAFT 1: 
// import java.util.Stack; 

// public class Combatant {
//     private String name;
//     private int initiative;
//     private boolean isMyTurn; 

//     //primary statistics: 
//     private Statistic hitPoints; 
//     private Statistic manaPoints; 
//     private Statistic actionPoints; 

//     //damage reduction
//     private int trueAC; 

//     //health layers: 
//     private int overhealHitPoints; 

//     //index = layer ; armourValue (AC)
//     private Stack<Statistic> armour; 

//     //index = layer ; shieldValue is in int
//     private Stack<Integer> shield; 

//     public Combatant(String name, int initiative) {
//         this.name = name;
//         this.initiative = initiative;
//     }

//     public int getInitiative() {
//         return initiative;
//     }
    
//     public int damageToShield(int damage) { 
//         int damageOnFirstShield = damage - shield.peek().intValue(); 
//         int shieldHealth = shield.peek().intValue() - damage; 
//         int shieldLastIndex = shield.size() - 1; 

//         if(shieldHealth <= 0) { 
//             shield.pop(); 
//         }

//         if(damageOnFirstShield <= 0) { 
//             return 0; 
//         }

//         Integer tempIntOne = shieldHealth; 

//         shield.set(shieldLastIndex, shieldHealth); 
//         int damageToShieldLayer = damageOnFirstShield; 

//         for (int i = shieldLastIndex - 1 ; i > 0 ; i--) { 
//             int currentShieldLayerHealth = shield.peek().intValue() - damageToShieldLayer; 

//             damageToShieldLayer -= shield.peek().intValue(); 

//             if(currentShieldLayerHealth <= 0) { 
//                 shield.pop(); 

//             }

//             if(damageToShieldLayer <= 0) { 
//                 return 0; 

//             } else { 
//                 Integer tempIntTwo = currentShieldLayerHealth; 
//                 shield.set(i, tempIntTwo); 

//                 return 0; 
//             }
//         }

//         return damageToShieldLayer; 
//     }

//     public int damageToArmour(int damage) { 
//         int damageOnFirstAC = damage - armour.peek().getCurrent(); 
//         int armourHealth = armour.peek().getCurrent() - damage; 
//         int armourLastIndex = armour.size() - 1; 

//         if(armourHealth <= 0) { 
//             armour.pop(); 
//         }

//         if(damageOnFirstAC <= 0) { 
//             return 0; 
//         }

//         Statistic firstArmourLayer = armour.get(armourLastIndex); 
//         firstArmourLayer.setCurrent(armourHealth); 
//         shield.set(armourLastIndex, firstArmourLayer); 
 
//         int damageToArmourLayer = damageOnFirstAC; 

//         for (int i = shieldLastIndex - 1 ; i > 0 ; i--) { 
//             int currentArmourLayerHealth = armour.peek().getCurrent() - damageToArmourLayer; 

//             damageToArmourLayer -= armour.peek().getCurrent(); 

//             if(currentArmourLayerHealth <= 0) { 
//                 shield.pop(); 

//             }

//             if(damageToArmourLayer <= 0) { 
//                 return 0; 

//             } else { 
//                 Statistic currentArmour = armour.get(i); 
//                 currentArmour.setCurrent(currentArmourLayerHealth);
//                 shield.set(i, currentArmour); 

//                 return 0; 
//             }
//         }

//         return damageToArmourLayer; 
//     }

//     public void takeDamage(int damage, double armourPenetration) { 
//         //go through TrueAC and DamageReduction first: 
//         int initialDamage = damage - trueAC; 

//         if(initialDamage <= 0) { 
//             return; 
//         }

//         if(armourPenetration > 0) { 
//             overhealHitPoints = 0; 
//         }
        
//         int directDamageToHealth = (int)(initialDamage * armourPenetration); 
//         int damageThroughHealthLayers = (int)(initialDamage * (armourPenetration - 1)); 

//         takeDamage(damageThroughHealthLayers); 
//     }

//     private void takeDamage(int damage) {    
//         //1. Shield Layers
//         //2. Armour Layers
//         //3. Overheal
//         //4. HP
        
//         //shield layers:
//         int remainingDamageFromShield = damageToShield(damage); 
 
//         if(remainingDamageFromShield <= 0) { 
//             return; 
//         }

//         //armour layers:
//         int remainingDamageFromAC = damageToArmour(remainingDamageFromShield); 
 
//         if(remainingDamageFromAC <= 0) { 
//             return; 
//         }

//         //overheal: 
//         int overhealAfterDamage = overhealHitPoints - remainingDamageFromAC; 
//         if(overhealAfterDamage <= 0) { 
//             overhealHitPoints = 0;

//         } else { 
//             overhealHitPoints = overhealAfterDamage; 
//             return; 

//         }

//         //HP: 
//         hitPoints.modifyCurrent(overhealAfterDamage);


//     }

//     public void setMyTurn(boolean turnStatus) { 
//         this.isMyTurn = turnStatus; 
//         return; 
//     }

//     //HP:
//     public Statistic getHP() { 
//         return this.hitPoints; 
//     }

//     public void setCurHP(int setValue) { 
//         this.hitPoints.setCurrent(setValue);
//         return; 
//     }

//     public void setMaxHP(int setValue) { 
//         this.hitPoints.setCurrent(setValue);
//         return; 
//     }

//     //MP:
//     public Statistic getMP() { 
//         return this.manaPoints; 
//     }

//     public void setCurMP(int setValue) { 
//         this.manaPoints.setCurrent(setValue);
//         return; 
//     }

//     public void setMaxMP(int setValue) { 
//         this.manaPoints.setCurrent(setValue);
//         return; 
//     }    

//     //AP: 
//     public Statistic getAP() { 
//         return this.actionPoints; 
//     }

//     public void setCurAP(int setValue) { 
//         this.actionPoints.setCurrent(setValue);
//         return; 
//     }

//     public void setMaxAP(int setValue) { 
//         this.actionPoints.setCurrent(setValue);
//         return; 
//     }    

//     public int getAC() { 
//         int totalAC = 0;
//         for(int i = this.armour.size() ; i > 0 ; i--) { 
//             totalAC += armour.get(i).getCurrent(); 
//         }

//         return totalAC; 
//     }

//     public void setCurACForLayer(int layer, int setValue) { 
//         this.armour.get(layer).setCurrent(setValue);
//         return; 
//     }

//     public void setMaxACForLayer(int layer, int setValue) { 
//         this.armour.get(layer).setMax(setValue);
//         return; 
//     }

//     public void modifyACForLayer(int layer, int value) { 
//         Statistic currentAC = this.armour.get(layer);

//         int currentACValue = currentAC.getCurrent();
//         currentACValue += value; 

//         currentAC.setCurrent(currentACValue); 
//         this.armour.set(layer, currentAC); 

//         return; 
//     }

// @Override
// public String toString() {
//     return name;  // Just name — label logic moves to renderer
//     // String formattedString = "%s (Initiative: %d | HP: %d/%d (+%d) | MP :%d/%d | AP: %d/%d | AC: %d/%d | Shielding: %d)";
//     // String result = String.format(formattedString, name, initiative, this.hitPoints.getCurrent(), this.hitPoints.getMax(), this.getOverheal(), this.manaPoints.getCurrent(), this.manaPoints.getMax(), this.actionPoints.getCurrent(), this.actionPoints.getMax(), this.getTotalAC(), this.getTotalMaxAC(), this.getTotalShield()); 
//     // return result; 
// }
// }
