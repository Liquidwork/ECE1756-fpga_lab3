import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class Optimize1 {

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
            File dir = new File("output_e");
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
            dir = new File(dir, timestamp);
            dir.mkdir();

            file = new File(dir, "optimization_result.txt");
            PrintWriter writer = new PrintWriter(file);
            writer.println("Size, width, ratio, average area");
            // Start the execution
            CircuitRAM[] circuits = new CircuitRAM[circuitNum];
            for (int size = 1; size <=128; size *= 2){
                int optimal_width = 0, optimal_ratio = 0;
                double minimumArea = Double.MAX_VALUE;
                for (int width = 1; width <= 512; width *= 2){
                    for (int ratio = 1; ratio <= 512; ratio *= 2){
                        RAMType ramType = new BRAM(1, size * 1024, width, ratio * MemoryCAD.LOGICBLOCKLUT);
                        ArrayList<RAMType> ramTypes = new ArrayList<>();
                        ramTypes.add(ramType);
                        double accProduct = 1;
                        long area = 0;
                        for (int i = 0; i < circuits.length; i++) {
                            circuits[i] = CircuitRAM.parseCircuit(i, logicBlockCount[i] * 10, ramRecordsList[i], ramTypes);
                            area = circuits[i].resource.getTotalArea();
                            accProduct *= Math.pow(area, 1 / (double)circuits.length);
                        }
                        if (accProduct < minimumArea){
                            minimumArea = accProduct;
                            optimal_width = width;
                            optimal_ratio = ratio;
                        }
                        System.out.println(size + ", " + width + ", " + ratio + ", " + accProduct);
                    }
                }
                writer.println(size + ", " + optimal_width + ", " + optimal_ratio + ", " + minimumArea);
            }
            writer.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}

