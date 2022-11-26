/**
 * The class {@code ResourceOrganizer} supervise the usage of all resources
 * including basic logic LUT, LUTRAM, and BRAM.
 */
public class ResourceOrganizer {
    protected int basicLUT;
    protected int additionalLUT;
    protected int tempLUT; // Space added for more memory resources
    protected int[] ramCount = new int[RAMType.values().length];

    /**
     * Generate an instance of {@link ResourceOrganizer} with the basic LUT count
     * @param basicLUT the LUT count without ram related usage
     * @apiNote this is the LUT count not logic block count
     */
    public ResourceOrganizer(int basicLUT){
        this.basicLUT = basicLUT;
        this.additionalLUT = 0;
    }

    /**
     * Check whether this type of RAM is available to add several corresponding RAM
     * @param type type of the RAM
     * @param count number of RAM to be added
     * @return if it is ready to add, return true
     */
    public boolean ready(RAMType type, int count){
        if (ramCount[type.ordinal()] + count > getLUTRequired() / type.lutRatio() / MemoryCAD.LOGICBLOCKLUT){
            return false;
        }
        return true;
    }

    /**
     * Check whether this type of RAM is available to add one corresponding RAM
     * @param type
     * @return if it is ready to add, return true
     */
    public boolean ready(RAMType type){
        return ready(type, 1);
    }

    /**
     * Get the total LUT required
     * @return total LUT required
     */
    public int getLUTRequired(){
        return basicLUT + additionalLUT + tempLUT;
    }

    /**
     * Get the total regular LUT usage. including all LUT blocks in function
     * @return total regular LUT used
     */
    public int getLUTRegular(){
        return  basicLUT + additionalLUT;
    }

    /**
     * Calculate the total area of the resource in use
     * @return total area
     */
    public int getTotalArea(){
        int area = 0;
        for(RAMType type : RAMType.values()){
            if (type.lutImpl() == 0) { // not a LUTRAM
                // half of the LUT can implement LUT RAM, simply average the area usage
                int singleRamSize = 9000 + 5 * type.size() + 90 * (int) Math.ceil(Math.sqrt(type.size())) + 600 * 2 * type.maxWidth();
                area += Math.ceilDiv(getLUTRequired(), type.lutRatio()) * singleRamSize;
            }
        }
        area += Math.ceilDiv(getLUTRequired(), MemoryCAD.LOGICBLOCKLUT) * (35000 + 40000) / 2;
        return area;
    }

    /**
     * Add RAM usage to the resource organizer
     * @param type
     * @param count
     */
    public void addRAM(RAMType type, int count){
        ramCount[type.ordinal()] += count;
    }

    /**
     * Add lut usage to the resource organizer
     * @param count
     */
    public void addLUT(int count){
        additionalLUT += count;
    }

    public void addTempLUT(int count){
        tempLUT += count;
    }

    /**
     * Try to release some TempLUT to its lowest value
     */
    public void releaseTempLUT(){
        if(tempLUT == 0) return;
        // Find minimum LUT needed (maximum among needed value)
        int minNeededLUT = 0;
        for (RAMType type : RAMType.values()){
            int needed = ramCount[type.ordinal()] * type.lutRatio() * MemoryCAD.LOGICBLOCKLUT;
            if(needed > minNeededLUT) minNeededLUT = needed;
        }
        tempLUT -= getLUTRequired() - minNeededLUT;
        tempLUT = tempLUT < 0 ? 0 : tempLUT; // The value should not drop less than 0
    }

    @Override
    public String toString() {
        String s =  "Basic LUT: " + basicLUT + "\n" +
                    "Additional LUT: " + additionalLUT + "\n" +
                    "Margin LUT: " + tempLUT + "\n" +
                    "RAM usage: \n";
        for (RAMType type : RAMType.values()){
            s += " -" + type + ": " + ramCount[type.ordinal()] + "/" + 
            getLUTRequired() / type.lutRatio() / MemoryCAD.LOGICBLOCKLUT+ "\n";
        }
        return s;
    }
}
