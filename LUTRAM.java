public class LUTRAM extends RAMType {

    protected int id;
    protected int size;
    protected int minWidth;
    protected int maxWidth;
    protected int lutRatio;
    protected int lutImpl;

    public LUTRAM(int id, int size, int minWidth, int maxWidth, int lutRatio){
        this.id = id;
        this.size = size;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.lutRatio = lutRatio;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getMaxWidth() {
        return maxWidth;
    }

    @Override
    public int getMinWidth() {
        return minWidth;
    }

    @Override
    public int getLutRatio() {
        return lutRatio;
    }

    @Override
    public int getLutImpl() {
        return MemoryCAD.LOGICBLOCKLUT;
    }
    
}
