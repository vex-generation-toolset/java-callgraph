
class Blocks {
    static Block air;
}

class Color {
    Color z;
    public Color rgba() {
        return z;
    }
}

class Team {
    public Color color;
    public Team t;
}

class Block {
    boolean solid;
    Color color;
    
    public boolean synthetic() {
        return true;
    }
}

public class Z {
    public static int colorFor(Block floor, Block wall, Block ore, Team team){
        if(wall.synthetic()){
            return team.color.rgba();
        }
        Color i = (wall.solid? wall.color : ore == Blocks.air ? floor.color : ore.color).rgba();
        return i;/*<<<<<28,4,34,4,callee,Block:synthetic()Z,Blocks:Blocks()VSC,Color:rgba()QColor;*/
    }
    
}

//Issue 1403
//We include the virtual static constructor in call-graph.
//In marker we add suffix VSC (Virtual Static Constructor)
