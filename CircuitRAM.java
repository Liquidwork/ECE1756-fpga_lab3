import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CircuitRAM {
    protected int id;
    protected ResourceOrganizer resource;
    protected List<LogicalRAM> logicRAMList;
    protected List<RAMType> ramTypeList;

    private CircuitRAM(int id, int basicLUT, List<RAMType> ramTypeSet){
        this.id = id;
        this.ramTypeList = new ArrayList<>(ramTypeSet);
        this.ramTypeList.sort(((o1, o2) -> o2.getSize() - o1.getSize())); // Place them in the descending order
        this.resource = new ResourceOrganizer(basicLUT, ramTypeSet);
    }

    /**
     * Parse all the rams in the given RAM list and generate a {@code CircuitRAM} with the specified logical RAM
     * @param id id of the circuit
     * @param basicLUT basic LUT usage of the circuit (excluding all memory related LUTs)
     * @param ramRecord the LogicalRAM list
     * @return a new instance of the {@code CircuitRAM} with logical RAM fully parsed
     */
    public static CircuitRAM parseCircuit(int id, int basicLUT, List<LogicalRAM> ramRecord, List<RAMType> ramTypeSet){
        CircuitRAM circuitRAM = new CircuitRAM(id, basicLUT, ramTypeSet);
        circuitRAM.logicRAMList = ramRecord;
        LinkedList<LogicalRAM> unparsedRecord = new LinkedList<>(ramRecord); // Clone the instance since we will operate the list
        ResourceOrganizer resource = circuitRAM.resource;

        // First, place all logical RAM in size-order

        unparsedRecord.sort(((o1, o2) -> o1.d * o1.w - o2.d * o2.w)); // ascending order

        List<LogicalRAM> trueDualPortList = new LinkedList<>();

        // Then, initialize all the dual-port RAM (BRAM type only)
        switch (circuitRAM.ramTypeList.size()){
            case 1:
            RAMType type = circuitRAM.ramTypeList.get(0);
            for (LogicalRAM ram : unparsedRecord) {
                int ramUsage = ram.peekSize(type);
                if(!resource.ready(type, ramUsage)){
                    resource.addTempLUT(ramUsage * type.getLutRatio());
                }
                ram.parse(type);
                resource.addLUT(ram.additionalLUT);
                resource.addRAM(ram.type, ram.serial * ram.parallel);
            }
            break;
            
            case 2:

            for (LogicalRAM ram : unparsedRecord) {
                if (ram.mode == RAMMode.TRUEDUALPORT){
                    trueDualPortList.add(ram);
                }
            }

            for (LogicalRAM ram : trueDualPortList) {
                int peekedSize = ram.peekSize(circuitRAM.ramTypeList.get(0));
                if (!resource.ready(circuitRAM.ramTypeList.get(0), peekedSize)){
                    resource.addTempLUT(peekedSize * circuitRAM.ramTypeList.get(0).getLutRatio());
                }
                ram.parse(circuitRAM.ramTypeList.get(0));
                resource.addLUT(ram.additionalLUT);
                resource.addRAM(ram.type, ram.serial * ram.parallel);
            }

            // Try to implement with BRAM until full
            while(resource.ready(circuitRAM.ramTypeList.get(0)) && unparsedRecord.size() > 0){
                LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
                // Peek the size of this element as if implemented in such type of RAM
                if (!resource.ready(circuitRAM.ramTypeList.get(0),  ram.peekSize(circuitRAM.ramTypeList.get(0)))) break;
                ram.parse(circuitRAM.ramTypeList.get(0));
                resource.addLUT(ram.additionalLUT);
                resource.addRAM(ram.type, ram.serial * ram.parallel);
                unparsedRecord.removeLast();
            }

            while(unparsedRecord.size() > 0){

                boolean ramParsed = false; 
                // If this flag is false after the loop block, this means all the resources
                // runs out. So we need to allocate some more LUT for this circuit (wastage)

                // parse a LUTRAM
                if(resource.ready(circuitRAM.ramTypeList.get(1), unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(1))) && 
                unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(1)) <= 16) {
                    // When LUT allocation
                    LogicalRAM ram = unparsedRecord.removeFirst();
                    ram.parse(circuitRAM.ramTypeList.get(1));
                    resource.addLUT(ram.additionalLUT);
                    resource.addRAM(ram.type, ram.serial * ram.parallel);
                    resource.addLUT(ram.serial * ram.parallel * MemoryCAD.LOGICBLOCKLUT);
                    ramParsed = true;
                }
                
                while(resource.ready(circuitRAM.ramTypeList.get(0)) && unparsedRecord.size() > 0){
                    LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
                    // Peek the size of this element as if implemented in such type of RAM
                    if (!resource.ready(circuitRAM.ramTypeList.get(0),  ram.peekSize(circuitRAM.ramTypeList.get(0)))) break;
                    ram.parse(circuitRAM.ramTypeList.get(0));
                    resource.addLUT(ram.additionalLUT);
                    resource.addRAM(ram.type, ram.serial * ram.parallel);
                    unparsedRecord.removeLast();
                    ramParsed = true;
                }
                
                if (ramParsed == false){ // Allocate some temporal LUT so that the program will not stuck
                    int lutTemp = unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(1)) * circuitRAM.ramTypeList.get(1).getLutRatio();
                    if(unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(1)) >= 16) lutTemp = Integer.MAX_VALUE; // Not taken
                    int bramTemp = unparsedRecord.getLast().peekSize(circuitRAM.ramTypeList.get(0)) * circuitRAM.ramTypeList.get(0).getLutRatio();
                    int minTempLUTIncrease = lutTemp > bramTemp ? bramTemp : lutTemp;
                    resource.addTempLUT(minTempLUTIncrease);
                    // We can add a little bit more TempLUT, b/c we will clean the unused tempLUT anyway
                }
            }
            break;

            case 3:

            for (LogicalRAM ram : unparsedRecord) {
                if (ram.mode == RAMMode.TRUEDUALPORT){
                    trueDualPortList.add(ram);
                }
            }

            for (LogicalRAM ram : trueDualPortList) {
                if (ram.w * ram.d > circuitRAM.ramTypeList.get(1).getSize() && 
                resource.ready(circuitRAM.ramTypeList.get(0), ram.peekSize(circuitRAM.ramTypeList.get(0)))){
                    ram.parse(circuitRAM.ramTypeList.get(0));
                } else {
                    int peekedSize = ram.peekSize(circuitRAM.ramTypeList.get(1));
                    if (!resource.ready(circuitRAM.ramTypeList.get(1), peekedSize)){
                        resource.addTempLUT(peekedSize * circuitRAM.ramTypeList.get(1).getLutRatio());
                    }
                    ram.parse(circuitRAM.ramTypeList.get(1));
                }
                resource.addLUT(ram.additionalLUT);
                resource.addRAM(ram.type, ram.serial * ram.parallel);
            }

            unparsedRecord.removeAll(trueDualPortList); // parsed, so remove from list

            // Start Generating the largest logical RAM with BRAM until reach the limit

            while(resource.ready(circuitRAM.ramTypeList.get(0)) && unparsedRecord.size() > 0){
                LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
                // Peek the size of this element as if implemented in such type of RAM
                if (!resource.ready(circuitRAM.ramTypeList.get(0),  ram.peekSize(circuitRAM.ramTypeList.get(0)))) break;
                ram.parse(circuitRAM.ramTypeList.get(0));
                resource.addLUT(ram.additionalLUT);
                resource.addRAM(ram.type, ram.serial * ram.parallel);
                unparsedRecord.removeLast();
            }

            // Generate the second largest BRAM from the largest one until the current limit reached

            // If capacity increases, we can even try to replace the second largest RAM with the largest RAM implementation
            Queue<LogicalRAM> secondLargestRAM = new LinkedList<>();

            while(resource.ready(circuitRAM.ramTypeList.get(1)) && unparsedRecord.size() > 0){
                LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
                // Peek the size of this element as if implemented in such type of RAM
                if (!resource.ready(circuitRAM.ramTypeList.get(1),  ram.peekSize(circuitRAM.ramTypeList.get(1)))) break;
                ram.parse(circuitRAM.ramTypeList.get(1));
                resource.addLUT(ram.additionalLUT);
                resource.addRAM(ram.type, ram.serial * ram.parallel);
                secondLargestRAM.offer(ram); // Record the second largest from the largest to the smallest
                unparsedRecord.removeLast();
            }

            // Generate LUTRAM from the logical RAM from least size, and produce second largest BRAM if capacity increases

            while(unparsedRecord.size() > 0){

                boolean ramParsed = false; 
                // If this flag is false after the loop block, this means all the resources
                // runs out. So we need to allocate some more LUT for this circuit (wastage)

                // parse a LUTRAM
                if(resource.ready(circuitRAM.ramTypeList.get(2), unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(2))) && 
                unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(2)) <= 16) {
                    // When LUT allocation
                    LogicalRAM ram = unparsedRecord.removeFirst();
                    ram.parse(circuitRAM.ramTypeList.get(2));
                    resource.addLUT(ram.additionalLUT);
                    resource.addRAM(ram.type, ram.serial * ram.parallel);
                    resource.addLUT(ram.serial * ram.parallel * MemoryCAD.LOGICBLOCKLUT);
                    ramParsed = true;
                }

                // Try to replace the second largest RAM with the largest one

                while (secondLargestRAM.size() > 0 &&
                resource.ready(circuitRAM.ramTypeList.get(0), 
                secondLargestRAM.peek().peekSize(circuitRAM.ramTypeList.get(0)))){ // The element with largest size
                    LogicalRAM ram = secondLargestRAM.poll();
                    // Remove the related resource usage record
                    resource.addLUT(-ram.additionalLUT);
                    resource.addRAM(ram.type, - ram.serial * ram.parallel);
                    // Parse the ram again with the largest RAM type
                    ram.parse(circuitRAM.ramTypeList.get(0));
                    resource.addLUT(ram.additionalLUT);
                    resource.addRAM(ram.type, ram.serial * ram.parallel);
                    // No need to operate unparsedRecord list
                    ramParsed = true;
                }
                // Try to generate second largest RAMs
                
                while(resource.ready(circuitRAM.ramTypeList.get(1)) && unparsedRecord.size() > 0){
                    LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
                    // Peek the size of this element as if implemented in such type of RAM
                    if (!resource.ready(circuitRAM.ramTypeList.get(1),  ram.peekSize(circuitRAM.ramTypeList.get(1)))) break;
                    ram.parse(circuitRAM.ramTypeList.get(1));
                    resource.addLUT(ram.additionalLUT);
                    resource.addRAM(ram.type, ram.serial * ram.parallel);
                    secondLargestRAM.offer(ram); // Record the second largest from the largest to the smallest
                    unparsedRecord.removeLast();
                    ramParsed = true;
                }
                
                if (ramParsed == false){ // Allocate some temporal LUT so that the program will not stuck
                    int lutTemp = unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(2)) * circuitRAM.ramTypeList.get(2).getLutRatio();
                    if(unparsedRecord.getFirst().peekSize(circuitRAM.ramTypeList.get(2)) >= 16) lutTemp = Integer.MAX_VALUE; // Not taken
                    int bramTemp = unparsedRecord.getLast().peekSize(circuitRAM.ramTypeList.get(1)) * circuitRAM.ramTypeList.get(1).getLutRatio();
                    int minTempLUTIncrease = lutTemp > bramTemp ? bramTemp : lutTemp;
                    resource.addTempLUT(minTempLUTIncrease);
                    // We can add a little bit more TempLUT, b/c we will clean the unused tempLUT anyway
                }
            }
            break;
            case 0:
            throw new RuntimeException("No valid input RAM type");
            default:
            throw new RuntimeException("At most three types of RAM is supported");
        }

        resource.releaseTempLUT(); // try to release some of the tempLUT.

        return circuitRAM;
    }
}
