package cmomdp;

import camdp.CAMDP;
import camdp.solver.VI;
import xadd.optimization.MATLABNonLinear;
import xadd.optimization.Optimise;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Created by skin on 2016-08-18.
 */
public class Main {

    private static Integer NUM_ARGUMENTS = 3;

    /**
     *
     */
    public static void Usage() {
        System.out.println("\nUsage: MDP-filename #iter #Ddisplay(2or3)");
        System.exit(1);
    }

    public static void main(String args[]) {

        if (args.length != Main.NUM_ARGUMENTS) {
            Main.Usage();
        }

        // Parse problem filename
        String filename = args[0];
        Integer numIterations = Integer.parseInt(args[1]);
        Integer displayDimension = Integer.parseInt(args[2]);

        // Build a CAMDP, display, solve
        CAMDP camdp = new CAMDP(filename);

        camdp.DISPLAY_2D = displayDimension == 2;
        camdp.DISPLAY_3D = displayDimension == 3;

        // Initialise a connection to the Non-linear solver
        Optimise.RegisterOptimisationMethod(new MATLABNonLinear());

        VI viSolver = new VI(camdp, numIterations);
        viSolver.solve();

        try {

            File f = new File(filename);
            System.out.println(f.getName());

            FileOutputStream fOut = new FileOutputStream(f.getParent() + File.separator + f.getName() + "_stats.csv");
            viSolver.writeSolutionStatistics(new PrintStream(fOut));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
