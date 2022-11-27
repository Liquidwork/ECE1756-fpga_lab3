import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class MemoryCAD {

    public static final int LOGICBLOCKLUT = 10;

    public static void main(String[] args){
        long startTime = System.nanoTime();
        try {
            File file = new File("logical_rams.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            // Get total number of circuit
            String line = reader.readLine();
            Scanner scanner = new Scanner(line);
            scanner.next(); // Skip useless word
            int circuitNum = scanner.nextInt();
            scanner.close();

            // Generate the record list
            ArrayList<LogicalRAM>[] ramRecordsList = new ArrayList[circuitNum];
            for (int i = 0; i < circuitNum; i++) {
                ramRecordsList[i] = new ArrayList<>();
            }

            reader.readLine(); // skip second line

            while (reader.ready()) {
                line = reader.readLine();
                scanner = new Scanner(line);
                int circuitId = scanner.nextInt();
                int ramId = scanner.nextInt();
                RAMMode mode = RAMMode.parseMode(scanner.next());
                int depth = scanner.nextInt();
                int width = scanner.nextInt();
                scanner.close();
                ramRecordsList[circuitId].add(new LogicalRAM(ramId, mode, width, depth));
            }
            reader.close();

            // Get the total logic block count of each circuit
            int[] logicBlockCount = new int[circuitNum];
            file = new File("logic_block_count.txt");
            reader = new BufferedReader(new FileReader(file));
            reader.readLine(); // skip the first line
            while (reader.ready()) {
                line = reader.readLine();
                String[] lineSegment = line.split("\t");
                logicBlockCount[Integer.parseInt(lineSegment[0])] = Integer.parseInt(lineSegment[1]);
            }
            reader.close();

            // Set RAM Type 
            ArrayList<RAMType> typeSet = new ArrayList<>(3);
            typeSet.add(new LUTRAM(1, 64 * 10, 10, 20, 2 * MemoryCAD.LOGICBLOCKLUT));
            typeSet.add(new BRAM(2, 8192, 32, 10 * MemoryCAD.LOGICBLOCKLUT));
            typeSet.add(new BRAM(3, 1024 * 128, 128, 300 * MemoryCAD.LOGICBLOCKLUT));

            // Map file name
            SimpleDateFormat format = new SimpleDateFormat("yy_MM_dd_HH_mm_ss");
            String timestamp = format.format(new Date());
            // Make an output dir with timestamp
            File dir = new File("output");
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
            dir = new File(dir, timestamp);
            dir.mkdir();

            file = new File(dir, "map.txt");
            PrintWriter writer = new PrintWriter(file);
            // Start the execution
            CircuitRAM[] circuits = new CircuitRAM[circuitNum];
            double accProduct = 1.;
            for (int i = 0; i < circuits.length; i++) {
                System.out.println("Fitting circuit " + i);
                circuits[i] = CircuitRAM.parseCircuit(i, logicBlockCount[i] * 10, ramRecordsList[i], typeSet);
                System.out.println(circuits[i].resource);
                for (String s : generateRecord(circuits[i])) {
                    writer.println(s);
                }
                accProduct *= Math.pow((double) circuits[i].resource.getTotalArea(), 1 / (double) circuits.length);
            }
            writer.close();
            System.out.println("Average area usage (Geometric): " + accProduct);
            long runtime = System.nanoTime() - startTime;
            // Denote runtime in fixed point (x.3) notation.
            int runtimeMs = (int) (runtime / 10000000);
            int runtimePoint = (int) ((runtime / 1000) % 1000);
            System.out.println("Runtime: " + runtimeMs + "." + runtimePoint + "ms");
            // Print the stats of resource usage
            file = new File(dir, "stats.csv");
            writer = new PrintWriter(file);
            // print the line title at first line in csv format
            writer.println("ciruit_id,lutram,8kBRAM,128kBRAM,regularLB,requiredLB,TotalArea");
            for (int i = 0; i < circuits.length; i++) {
                line = circuits[i].id + ",";
                line += circuits[i].resource.ramCount.get(typeSet.get(0)) + ",";
                line += circuits[i].resource.ramCount.get(typeSet.get(1)) + ",";
                line += circuits[i].resource.ramCount.get(typeSet.get(2)) + ",";
                line += ceilDiv(circuits[i].resource.getLUTRegular(), MemoryCAD.LOGICBLOCKLUT) + ",";
                line += ceilDiv(circuits[i].resource.getLUTRequired(), MemoryCAD.LOGICBLOCKLUT) + ",";
                line += circuits[i].resource.getTotalArea();
                writer.println(line);
            }
            writer.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }


    /**
     * Generate the mapping list for this RAM circuit. Each line represents the mapping for a
     * logical RAM. The list can be used to generate the mapping file.
     * @return a list of record
     */
    public static List<String> generateRecord(CircuitRAM circuit){
        List<String> list = new ArrayList<>();
        for (LogicalRAM ram : circuit.logicRAMList) {       
            String mode = "";
            switch(ram.mode){
                case ROM:               mode = "ROM"; break;
                case SIMPLEDUALPORT:    mode = "SimpleDualPort"; break;
                case SINGLEPORT:        mode = "SinglePort"; break;
                case TRUEDUALPORT:      mode = "TrueDualPort"; break;
            }
            list.add(String.format("%d %d %d LW %d LD %d ID %d S %d P %d Type %d Mode %s W %d D %d", 
            circuit.id, ram.id, ram.additionalLUT, ram.w, ram.d, ram.id, ram.serial, ram.parallel, ram.type.getId(),
            mode, ram.physicalWidth, ram.physicalDepth ));
        }
        return list;
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
}
