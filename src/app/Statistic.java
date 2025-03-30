package app;

/**
 * Class used to store all data about Statistics like HP, MP, AP, SPD, AC, etc.
 * 
 * @author PaladinGod8
 * @since version 1.0
 */
public class Statistic {
    private int maxValue;
    private int curValue;

    /**
     * getter method for maximum value of a given statistic e.g. maxHP, maxMP, maxAP
     * 
     * @return The maxValue
     * @since version 1.0
     */
    public int getMax() {
        return this.maxValue;
    }

    /**
     * setter method for maximum value of a given statistic e.g. maxHP, maxMP, maxAP
     * 
     * @param setValue The value you want to set the maximum value of this statistic to be.
     * @since version 1.0
     */
    public void setMax(int setValue) { 
        this.maxValue = setValue; 
    }
    
    /**
     * method used to modify the statistic's maximum value by the specified input. 
     * e.g. modifyMax(-1) to maxHP = 20 means that HP: 20/20 -> HP: 19/19 
     * 
     * @param value The value you want to modify the maximum value of this statistic by.
     * @since version 1.0
     */
    public void modifyMax(int value) { 
        this.maxValue += value; 
    }

    /**
     * getter method for current value of a given statistic e.g. curHP, curMP, curAP. 
     * 
     * @return The curValue
     * @since version 1.0
     */
    public int getCurrent() { 
        return this.curValue; 
    }

    /**
     * setter method for current value of a given statistic e.g. curHP, curMP, curAP
     * 
     * @param setValue The value you want to set the current value of this statistic to be.
     * @since version 1.0
     */
    public void setCurrent(int setValue) { 
        this.curValue = setValue;
    }

    /**
     * method used to modify the statistic's current value by the specified input. 
     * e.g. modifyCurrent(-1) to maxHP = 20 means that HP: 20/20 -> HP: 19/20. 
     * 
     * usually used in methods like takeDamage(). 
     * 
     * @param value The value you want to modify the maximum value of this statistic by.
     * @since version 1.0
     */
    public void modifyCurrent(int value) { 
        this.curValue += value; 
    } 

    public Statistic(int max) { 
        this(max, max);           //if only max is input -> set current to max for a full statistic.
    }

    public Statistic(int max, int cur) { 
        this.maxValue = max; 
        this.curValue = cur; 
    }
}
