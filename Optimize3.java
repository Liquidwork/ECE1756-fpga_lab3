import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class Optimize3 {

    public static void main(String[] args){
        try {
            int lbRatio = 2;
            if (args.length >= 1){
                lbRatio = Integer.parseInt(args[0]);
            }

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
            File dir = new File("output_g");
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
            dir = new File(dir, timestamp);
            dir.mkdir();

            file = new File(dir, "optimization_result.txt");
            PrintWriter writer = new PrintWriter(file);
            writer.println("Size 1, width 1, ratio 1, Size 2, width 2, ratio 2, average area");
            // Prepare the LUTRAM instance
            LUTRAM lutram = new LUTRAM(1, 64 * 10, 10, 20, lbRatio * MemoryCAD.LOGICBLOCKLUT);
            // Start the execution
            CircuitRAM[] circuits = new CircuitRAM[circuitNum];
            for (int sizeSmall = 1; sizeSmall <=64; sizeSmall *= 2){
                for (int sizeLarge = sizeSmall * 2; sizeLarge <=128; sizeLarge *= 2){
                    System.out.print("Size:" + sizeSmall + ", " + sizeLarge + ": ");
                    int optimalWidthSmall = 0, optimalRatioSmall = 0;
                    int optimalWidthLarge = 0, optimalRatioLarge = 0;
                    double minimumArea = Double.MAX_VALUE;
                    for (int widthSmall = 1; widthSmall <= 512; widthSmall *= 2){
                        for (int ratioSmall = 1; ratioSmall <= 32; ratioSmall *= 2){
                            for (int widthLarge = widthSmall; widthLarge <= 512; widthLarge *= 2){
                                for (int ratioLarge = ratioSmall; ratioLarge <= 4096; ratioLarge *= 2){
                                    RAMType ramTypeSmall = new BRAM(2, sizeSmall * 1024, widthSmall, ratioSmall * MemoryCAD.LOGICBLOCKLUT);
                                    RAMType ramTypeLarge = new BRAM(3, sizeLarge * 1024, widthLarge, ratioLarge * MemoryCAD.LOGICBLOCKLUT);
                                    ArrayList<RAMType> ramTypes = new ArrayList<>();
                                    ramTypes.add(lutram);
                                    ramTypes.add(ramTypeSmall);
                                    ramTypes.add(ramTypeLarge);
                                    double accProduct = 1.;
                                    long area = 0;
                                    for (int i = 0; i < circuits.length; i++) {
                                        circuits[i] = CircuitRAM.parseCircuit(i, logicBlockCount[i] * 10, ramRecordsList[i], ramTypes);
                                        area = circuits[i].resource.getTotalArea();
                                        accProduct *= Math.pow((double) area, 1 / (double)circuits.length);
                                    }
                                    if (accProduct < minimumArea){
                                        minimumArea = accProduct;
                                        optimalWidthSmall = widthSmall;
                                        optimalRatioSmall = ratioSmall;
                                        optimalWidthLarge = widthLarge;
                                        optimalRatioLarge = ratioLarge;
                                    }
                                }
                            }
                        }
                    }
                    System.out.println(minimumArea);
                    writer.println(sizeSmall + ", " + optimalWidthSmall + ", " + optimalRatioSmall + ", " + 
                    sizeLarge + ", " + optimalWidthLarge + ", " + optimalRatioLarge + ", " + minimumArea);
                }
            }
            writer.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}