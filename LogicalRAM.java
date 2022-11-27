public class LogicalRAM {
    protected int id;
    protected RAMMode mode;
    protected RAMType type;
    protected int w, d;
    protected int serial, parallel;
    protected int physicalWidth, physicalDepth;
    protected int additionalLUT;
    // protected int size;

    public LogicalRAM(int id, RAMMode mode, int width, int depth){
        this.id = id;
        this.mode = mode;
        this.w = width;
        this.d = depth;
    }
    
    /**
     * Peek the size of this ram implemented by the specified RAMType, helping 
     * to determine the number of RAM used before parsing the logicLUT
     * @param type
     * @return the estimated size
     */
    public int peekSize(RAMType type){

        // Allocate physical RAM according to the type, mode and width

        // Satisfy depth first, b/c linking RAM parallel has no penalty
        int selectedWidth = type.getMinWidth();
        while (selectedWidth < type.getMaxWidth() * mode.widthUtilization() && type.getSize() / selectedWidth >= this.d){
            selectedWidth *= 2;
        }
        int physicalWidth = selectedWidth;
        int physicalDepth = type.getSize() / selectedWidth;
        
        int parallel = Math.ceilDiv(this.w, physicalWidth);
        int serial = Math.ceilDiv(this.d, physicalDepth);
        return parallel * serial;
    }

    public void parse(RAMType type){
        this.type = type;

        // Allocate physical RAM according to the type, mode and width

        // Satisfy depth first, b/c linking RAM parallel has no penalty
        int selectedWidth = type.getMinWidth();
        
        while (selectedWidth * 2 <= type.getMaxWidth() * mode.widthUtilization() && type.getSize() / (selectedWidth * 2) >= this.d){
            selectedWidth *= 2;
        }
        this.physicalWidth = selectedWidth;
        this.physicalDepth = type.getSize() / selectedWidth;
        
        this.parallel = Math.ceilDiv(this.w, this.physicalWidth);
        this.serial = Math.ceilDiv(this.d, this.physicalDepth);

        // Resolve additional LUT in a line (serial)
        int decLUT, muxLUT;
        switch(this.serial){
            case 1: // No serial
                decLUT = 0;
                muxLUT = 0;
                break; 
            case 2:
                decLUT = 1; // Special case
                muxLUT = 1;
                break;
            default: 
                decLUT = this.serial;
                muxLUT = Math.ceilDiv((this.serial-1), 3);
        }
        this.additionalLUT = (decLUT + muxLUT) * this.parallel + this.serial * this.parallel * this.type.getLutImpl();
    }
}
