public class BRAM extends RAMType {

    protected int size;
    protected int maxWidth;
    protected int lutRatio;

    public BRAM(int size, int maxWidth, int lutRatio){
        this.size = size;
        this.maxWidth = maxWidth;
        this.lutRatio = lutRatio;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int maxWidth() {
        return maxWidth;
    }

    @Override
    public int minWidth() {
        return 1;
    }

    @Override
    public int lutRatio() {
        return lutRatio;
    }

    @Override
    public int lutImpl() {
        return 0;
    }
    
}
