import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class CircuitRAM {
    protected int id;
    protected ResourceOrganizer resource;
    protected List<LogicalRAM> logicRAMList;
    protected List<RAMType> ramTypeList;

    private CircuitRAM(int id, int basicLUT, Set<RAMType> ramTypeSet){
        this.id = id;
        this.ramTypeList = new ArrayList<>(ramTypeSet);
        this.ramTypeList.sort(((o1, o2) -> o2.size() - o1.size())); // Place them in the descending order
        this.resource = new ResourceOrganizer(basicLUT, ramTypeSet);
    }

    /**
     * Parse all the rams in the given RAM list and generate a {@code CircuitRAM} with the specified logical RAM
     * @param id id of the circuit
     * @param basicLUT basic LUT usage of the circuit (excluding all memory related LUTs)
     * @param ramRecord the LogicalRAM list
     * @return a new instance of the {@code CircuitRAM} with logical RAM fully parsed
     */
    public static CircuitRAM parseCircuit(int id, int basicLUT, List<LogicalRAM> ramRecord, Set<RAMType> ramTypeSet){
        CircuitRAM circuitRAM = new CircuitRAM(id, basicLUT, ramTypeSet);
        circuitRAM.logicRAMList = ramRecord;
        LinkedList<LogicalRAM> unparsedRecord = new LinkedList<>(ramRecord); // Clone the instance since we will operate the list

        // First, place all logical RAM in size-order

        unparsedRecord.sort(((o1, o2) -> o1.d * o1.w - o2.d * o2.w)); // ascending order

        // Then, initialize all the dual-port RAM (BRAM type only)

        List<LogicalRAM> trueDualPortList = new ArrayList<>();

        for (LogicalRAM ram : unparsedRecord) {
            if (ram.mode == RAMMode.TRUEDUALPORT){
                trueDualPortList.add(ram);
            }
        }

        for (LogicalRAM ram : trueDualPortList) {
            if (ram.w * ram.d > 16 && circuitRAM.resource.ready(RAMType.BRAM128k, ram.peekSize(RAMType.BRAM128k))){
                ram.parse(RAMType.BRAM128k);
            } else {
                ram.parse(RAMType.BRAM8192);
            }
        }

        unparsedRecord.removeAll(trueDualPortList); // parsed, so remove from list

        // Start Generating the largest logical RAM with BRAM until reach the limit

        while(circuitRAM.resource.ready(RAMType.BRAM128k) && unparsedRecord.size() > 0){
            LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
            // Peek the size of this element as if implemented in such type of RAM
            if (!circuitRAM.resource.ready(RAMType.BRAM128k,  ram.peekSize(RAMType.BRAM128k))) break;
            ram.parse(RAMType.BRAM128k);
            circuitRAM.resource.addLUT(ram.additionalLUT);
            circuitRAM.resource.addRAM(ram.type, ram.serial * ram.parallel);
            unparsedRecord.removeLast();
        }

        // Generate the second largest BRAM from the largest one until the current limit reached

        // If capacity increases, we can even try to replace the second largest RAM with the largest RAM implementation
        Queue<LogicalRAM> secondLargestRAM = new LinkedList<>();

        while(circuitRAM.resource.ready(RAMType.BRAM8192) && unparsedRecord.size() > 0){
            LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
            // Peek the size of this element as if implemented in such type of RAM
            if (!circuitRAM.resource.ready(RAMType.BRAM8192,  ram.peekSize(RAMType.BRAM8192))) break;
            ram.parse(RAMType.BRAM8192);
            circuitRAM.resource.addLUT(ram.additionalLUT);
            circuitRAM.resource.addRAM(ram.type, ram.serial * ram.parallel);
            secondLargestRAM.offer(ram); // Record the second largest from the largest to the smallest
            unparsedRecord.removeLast();
        }

        // Generate LUTRAM from the logical RAM from least size, and produce second largest BRAM if capacity increases

        while(unparsedRecord.size() > 0){

            boolean ramParsed = false; 
            // If this flag is false after the loop block, this means all the resources
            // runs out. So we need to allocate some more LUT for this circuit (wastage)

            // parse a LUTRAM
            if(circuitRAM.resource.ready(RAMType.LUTRAM, unparsedRecord.getFirst().peekSize(RAMType.LUTRAM)) && 
            unparsedRecord.getFirst().peekSize(RAMType.LUTRAM) <= 16) {
                // When LUT allocation
                LogicalRAM ram = unparsedRecord.removeFirst();
                ram.parse(RAMType.LUTRAM);
                circuitRAM.resource.addLUT(ram.additionalLUT);
                circuitRAM.resource.addRAM(ram.type, ram.serial * ram.parallel);
                circuitRAM.resource.addLUT(ram.serial * ram.parallel * MemoryCAD.LOGICBLOCKLUT);
                ramParsed = true;
            }

            // Try to replace the second largest RAM with the largest one

            while (secondLargestRAM.size() > 0 &&
            circuitRAM.resource.ready(RAMType.BRAM128k, 
            secondLargestRAM.peek().peekSize(RAMType.BRAM128k))){ // The element with largest size
                LogicalRAM ram = secondLargestRAM.poll();
                // Remove the related resource usage record
                circuitRAM.resource.addLUT(-ram.additionalLUT);
                circuitRAM.resource.addRAM(ram.type, - ram.serial * ram.parallel);
                // Parse the ram again with the largest RAM type
                ram.parse(RAMType.BRAM128k);
                circuitRAM.resource.addLUT(ram.additionalLUT);
                circuitRAM.resource.addRAM(ram.type, ram.serial * ram.parallel);
                // No need to operate unparsedRecord list
                ramParsed = true;
            }
            // Try to generate second largest RAMs
            
            while(circuitRAM.resource.ready(RAMType.BRAM8192) && unparsedRecord.size() > 0){
                LogicalRAM ram = unparsedRecord.getLast(); // Get the element with largest size
                // Peek the size of this element as if implemented in such type of RAM
                if (!circuitRAM.resource.ready(RAMType.BRAM8192,  ram.peekSize(RAMType.BRAM8192))) break;
                ram.parse(RAMType.BRAM8192);
                circuitRAM.resource.addLUT(ram.additionalLUT);
                circuitRAM.resource.addRAM(ram.type, ram.serial * ram.parallel);
                secondLargestRAM.offer(ram); // Record the second largest from the largest to the smallest
                unparsedRecord.removeLast();
                ramParsed = true;
            }
            
            if (ramParsed == false){ // Allocate some temporal LUT so that the program will not stuck
                int lutTemp = unparsedRecord.getFirst().peekSize(RAMType.LUTRAM);
                if(unparsedRecord.getFirst().peekSize(RAMType.LUTRAM) >= 16) lutTemp = Integer.MAX_VALUE; // Not taken
                int bramTemp = unparsedRecord.getLast().peekSize(RAMType.BRAM8192) * RAMType.BRAM8192.lutRatio();
                int minTempLUTIncrease = lutTemp > bramTemp ? bramTemp : lutTemp;
                circuitRAM.resource.addTempLUT(minTempLUTIncrease * MemoryCAD.LOGICBLOCKLUT);
                // We can add a little bit more TempLUT, b/c we will clean the unused tempLUT anyway
            }
        }
        circuitRAM.resource.releaseTempLUT(); // try to release some of the tempLUT.

        return circuitRAM;
    }

    /**
     * Generate the mapping list for this RAM circuit. Each line represents the mapping for a
     * logical RAM. The list can be used to generate the mapping file.
     * @return a list of record
     */
    public List<String> generateRecord(){
        List<String> list = new ArrayList<>();
        for (LogicalRAM ram : this.logicRAMList) {
            int type = 0;
            switch(ram.type){
                case LUTRAM:    type = 1; break;
                case BRAM8192:  type = 2; break;
                case BRAM128k:  type = 3; break;
            }
            String mode = "";
            switch(ram.mode){
                case ROM:               mode = "ROM"; break;
                case SIMPLEDUALPORT:    mode = "SimpleDualPort"; break;
                case SINGLEPORT:        mode = "SinglePort"; break;
                case TRUEDUALPORT:      mode = "TrueDualPort"; break;
            }
            list.add(String.format("%d %d %d LW %d LD %d ID %d S %d P %d Type %d Mode %s W %d D %d", 
            this.id, ram.id, ram.additionalLUT, ram.w, ram.d, ram.id, ram.serial, ram.parallel, type,
            mode, ram.physicalWidth, ram.physicalDepth ));
        }
        return list;
    }
}
