# LAB 3

*Author*: Dongyu Wang (1009218525)

The input files are already inside the project and linked by the program.

## How to compile

Before running the code, first compile the code by running the following command:

> javac -Xlint:unchecked *.java

Please make sure the java version is higher than java11 (enough for ug machine to run)

## How to run the code

To run the memory CAD (solver for problem D)

> java MemoryCAD

The output will be in ./output folder, organized with timestamp.

---

To run the optimizer of single BRAM case (solver for problem E)

> java Optimizer1

The output will be in ./output_e folder, organized with timestamp.

---

To run the optimizer of single BRAM with LUTRAM (solver for problem F)

> java Optimizer2

The output will be in ./output_f folder, organized with timestamp.

---

To run the optimizer of two BRAM with LUTRAM (solver for problem G)

> java Optimizer3 [n]

where 1 of n logic blocks can be used as LUTRAM, the default value is 2 (if not specified any value).

The output will be in ./output_g folder, organized with timestamp.
