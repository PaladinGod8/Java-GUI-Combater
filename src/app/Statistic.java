package app;
public class Statistic {
    private int maxValue;
    private int curValue;

    public int getMax() {
        return this.maxValue;
    }

    public void setMax(int setValue) { 
        this.maxValue = setValue; 
    }
    
    public void modifyMax(int value) { 
        this.maxValue += value; 
    }

    public int getCurrent() { 
        return this.curValue; 
    }

    public void setCurrent(int setValue) { 
        this.curValue = setValue;
    }

    public void modifyCurrent(int value) { 
        this.curValue += value; 
    } 

    public Statistic(int max) { 
        this(max, max); 
    }

    public Statistic(int max, int cur) { 
        this.maxValue = max; 
        this.curValue = cur; 
    }
}
