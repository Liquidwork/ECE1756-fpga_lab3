import java.util.HashMap;
import java.util.List;

/**
 * The class {@code ResourceOrganizer} supervise the usage of all resources
 * including basic logic LUT, LUTRAM, and BRAM.
 */
public class ResourceOrganizer {
    protected int basicLUT;
    protected int additionalLUT;
    protected int tempLUT; // Space added for more memory resources
    protected HashMap<RAMType, Integer> ramCount;

    /**
     * Generate an instance of {@link ResourceOrganizer} with the basic LUT count
     * @param basicLUT the LUT count without ram related usage
     * @apiNote this is the LUT count not logic block count
     */
    public ResourceOrganizer(int basicLUT, List<RAMType> ramTypeSet){
        this.basicLUT = basicLUT;
        this.additionalLUT = 0;
        this.ramCount = new HashMap<>(ramTypeSet.size());
        // Initialize the ramCount Hash map
        for(RAMType type : ramTypeSet){
            this.ramCount.put(type, 0); // Initialize all entry as 0
        }
    }

    /**
     * Check whether this type of RAM is available to add several corresponding RAM
     * @param type type of the RAM
     * @param count number of RAM to be added
     * @return if it is ready to add, return true
     */
    public boolean ready(RAMType type, int count){
        if (ramCount.get(type) + count > getLUTRequired() / type.getLutRatio()){
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
    public long getTotalArea(){
        long area = 0;
        for(RAMType type : ramCount.keySet()){
            if (type.getLutImpl() == 0) { // not a LUTRAM
                long singleRamSize = 9000 + 5 * type.getSize() + 90 * (int) Math.ceil(Math.sqrt(type.getSize())) + 600 * 2 * type.getMaxWidth();

                area += ceilDiv(getLUTRequired(), type.getLutRatio()) * singleRamSize;
            }
        }
        // half of the LUT can implement LUT RAM, simply average the area usage
        area += (long) ceilDiv(getLUTRequired(), MemoryCAD.LOGICBLOCKLUT) * (35000 + 40000) / 2;
        return area;
    }

    /**
     * Add RAM usage to the resource organizer
     * @param type
     * @param count
     */
    public void addRAM(RAMType type, int count){
        ramCount.replace(type, ramCount.get(type) + count);
    }

    /**
     * Add lut usage to the resource organizer
     * @param count
     */
    public void addLUT(int count){
        additionalLUT += count;
    }

    /**
     * Add a temporal LUT, to increase the capacity of each resource
     * @param count
     */
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
        for (RAMType type : ramCount.keySet()){
            int needed = ramCount.get(type) * type.getLutRatio();
            if(needed > minNeededLUT) minNeededLUT = needed;
        }
        tempLUT -= getLUTRequired() - minNeededLUT;
        tempLUT = tempLUT < 0 ? 0 : tempLUT; // The value should not drop less than 0
    }

    /**
     * Introduced after java 11. Added to support ug machine...
     * @param x
     * @param y
     * @return
     */
    private static int ceilDiv(int x, int y){
        int result = x / y;
        if (x % y == 0) return result;
        return result + 1;
    }

    @Override
    public String toString() {
        String s =  "Basic LUT: " + basicLUT + "\n" +
                    "Additional LUT: " + additionalLUT + "\n" +
                    "Margin LUT: " + tempLUT + "\n" +
                    "RAM usage: \n";
        for (RAMType type : ramCount.keySet()){
            s += " -" + type.getClass() + "(" + type.getSize() + " bits): " + ramCount.get(type) + "/" + 
            getLUTRequired() / type.getLutRatio()+ "\n";
        }
        return s;
    }
}
