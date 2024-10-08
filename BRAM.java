public class BRAM extends RAMType {
    protected int id;
    protected int size;
    protected int maxWidth;
    protected int lutRatio;

    public BRAM(int id, int size, int maxWidth, int lutRatio){
        this.id = id;
        this.size = size;
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
        return 1;
    }

    @Override
    public int getLutRatio() {
        return lutRatio;
    }

    @Override
    public int getLutImpl() {
        return 0;
    }
    
}
