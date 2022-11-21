import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class MemoryCAD {

    public static final int LOGICBLOCKLUT = 10;

    public static void main(String[] args){
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
            for (int i = 0; i < circuits.length; i++) {
                System.out.println("Fitting circuit " + i);
                circuits[i] = CircuitRAM.parseCircuit(i, logicBlockCount[i] * 10, ramRecordsList[i]);
                System.out.println(circuits[i].resource);
                for (String s : circuits[i].generateRecord()) {
                    writer.println(s);
                }
            }
            writer.close();
            // Print the stats of resource usage
            file = new File(dir, "stats.csv");
            writer = new PrintWriter(file);
            // print the line title at first line in csv format
            writer.println("ciruit_id,lutram,8kBRAM,128kBRAM,regularLB,requiredLB,TotalArea");
            for (int i = 0; i < circuits.length; i++) {
                line = circuits[i].id + ",";
                line += circuits[i].resource.ramCount[RAMType.LUTRAM.ordinal()] + ",";
                line += circuits[i].resource.ramCount[RAMType.BRAM8192.ordinal()] + ",";
                line += circuits[i].resource.ramCount[RAMType.BRAM128k.ordinal()] + ",";
                line += Math.ceilDiv(circuits[i].resource.getLUTRegular(), MemoryCAD.LOGICBLOCKLUT) + ",";
                line += Math.ceilDiv(circuits[i].resource.getLUTRequired(), MemoryCAD.LOGICBLOCKLUT) + ",";
                line += circuits[i].resource.getTotalArea();
                writer.println(line);
            }
            writer.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}
