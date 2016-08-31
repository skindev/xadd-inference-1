package cmomdp;

import camdp.CAMDP;
import xadd.optimization.MATLABNonLinear;
import xadd.optimization.Optimise;

import java.io.File;

/**
 * Created by skin on 2016-08-18.
 */
public class Main {

    private static Integer NUM_ARGUMENTS = 3;

    private static MATLABNonLinear instance = null;

    /**
     * @return  Instance of MATLABNonLinear
     */
    private static MATLABNonLinear GetMATLABInstance() {
        if (instance == null) {
            instance = new MATLABNonLinear();
        }

        return instance;
    }

    /**
     *
     */
    public static void Usage() {
        System.out.println("\nUsage: MDP-filepath #iter #Ddisplay(2or3)");
        System.exit(1);
    }

    public static void main(String args[]) {

        if (args.length != Main.NUM_ARGUMENTS) {
            Main.Usage();
        }

        // Parse problem filepath
        File file = new File(args[0]);
        Integer experimentNumber = Integer.parseInt(args[1]);
        Integer numIterations = Integer.parseInt(args[2]);


        // Build a CAMDP
        CAMDP camdp = new CAMDP(file.getAbsoluteFile().getAbsolutePath());

        // Initialise a connection to the Non-linear solver
        Optimise.RegisterOptimisationMethod(Main.GetMATLABInstance());

        // Instantiate VI with the camdp
        VI viSolver = new VI(camdp, numIterations);

        switch (experimentNumber) {
            case 1:
                Experiment.Robot1D(camdp, viSolver, numIterations);
                break;
            case 2:
                Experiment.OptimalExecution(camdp, viSolver, numIterations);
                break;
            case 3:
                Experiment.SIR(camdp, viSolver, numIterations);
                break;
            default:
                System.err.println("Unrecognised Experiment Number: " + experimentNumber);
        }

//        System.exit(0);

    }
}
