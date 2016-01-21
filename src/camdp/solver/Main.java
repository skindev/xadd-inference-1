package camdp.solver;

import camdp.CAMDP;

import java.io.IOException;
import java.util.logging.*;

/**
 * Main Executable class for Continuous State and Action MDP (CAMDP)
 *
 * @author Luis Vianna
 * @author Zahra Zamani
 * @author Scott Sanner
 * @version 1.0
 * @language Java (JDK 1.5)
 **/

public class Main {

    //Running Configurations
    private static int VERBOSE = 1;
    private static double APPROX = 0d;
    private static String[] solvers = {"Invalid", "VI", "CRTDP", "RTSDP", "Aprox VI", "Approx RTSDP"};// 1 -> Value Iteration 2-> RTDP 3-> RTDPFH, Run all and Save All

    // Results Configurations
    private static final boolean SAVE_RESULTS = true;
    private static boolean PRINT_RESULTS = true;

    // Logger
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void Usage() {
        System.out.println("\nUsage: MDP-filename #solver #iter #Ddisplay(2or3) #trials [VERBOSE(int) SHOW_PLOT?(boolean) Approx]");
        System.exit(1);
    }

    /**
     *
     * @param args
     */
    public static void main(String args[]) {

        try {
            FileHandler fileHander = new FileHandler("./camdp.log");
            fileHander.setLevel(Level.ALL);

            LOGGER.addHandler(fileHander);
        } catch(IOException exception) {
            LOGGER.severe("Unable to create log file.");
        }

        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            handlers[index].setLevel(Level.ALL);
        }

        int nargs = args.length;
        if (nargs < 5 || nargs > 8) {
            Usage();
        }

        // Parse problem filename
        String filename = args[0];

        // Parse integers
        int solution = -1;
        int iter = -1;
        int display = -1;
        int trials = -1;

        try {
            solution = Integer.parseInt(args[1]);
            iter = Integer.parseInt(args[2]);
            display = Integer.parseInt(args[3]);
            trials = Integer.parseInt(args[4]);
        } catch (NumberFormatException nfe) {
            LOGGER.severe("Illegal integer parameters");
            Usage();
        }

        if (args.length > 5) {
            VERBOSE = Integer.parseInt(args[5]);
            if (args.length > 6) {
                CAMDPsolver.debugSetUp(VERBOSE, Boolean.parseBoolean(args[6]));
                if (args.length > 7) {
                    APPROX = Double.parseDouble(args[7]);
                }
            }
        }

        LOGGER.info("Solving " + filename + " with solver " + solvers[solution] + " for " + iter + " iterations and " + trials + " trials.");
        LOGGER.fine("Approximation Allowed Error: " + APPROX);

        // Build a CAMDP, display, solve
        CAMDP mdp = new CAMDP(filename);
        LOGGER.config(mdp.toString(false, false));

        mdp.DISPLAY_2D = (display == 2);
        mdp.DISPLAY_3D = (display == 3);

        CAMDPsolver solver = null;
        String methodName = "";
        int used = -1;

        switch (solution) {
            case 1:
                methodName = "Value Iteration";
                solver = new VI(mdp, iter);
                break;
//        case 2:
//            checkInitialS(mdp);
//            solver = new CRTDP(mdp, trials, iter);
//            used = solver.solve();
//            if (SAVE_RESULTS) solver.saveResults();
//            if (PRINT_RESULTS) solver.printResults();
//            if (VERBOSE > 0) System.out.println("\nContinuous RTDP Solution complete, " + used + " trials of depth " + iter + ".");
//            break;
            case 3:
                methodName = "RTSDP";
                checkInitialS(mdp);
                solver = new CRTDPFH(mdp, trials, iter);
                break;
            case 4:
                methodName = "Approximate Value Iteration";
                solver = new VI(mdp, iter, APPROX);
                break;
            case 5:
                methodName = "Approximate RTSDP";
                checkInitialS(mdp);
                solver = new CRTDPFH(mdp, trials, iter, APPROX);
                break;
            default:
                LOGGER.severe("Invalid Solution Method!");
//                System.err.println("\nInvalid Solution Method!");
                System.exit(1);
        }

        used = solver.solve();
        LOGGER.info(methodName + " Solution complete, " + used + " trials of depth " + iter + ".");

        if (SAVE_RESULTS) {
            solver.saveResults();
        }

        LOGGER.info(solver.printSummary());
    }

    /**
     *
     * @param mdp
     */
    private static void checkInitialS(CAMDP mdp) {
        if (!mdp._hsBoolIVars.isEmpty() || !mdp._hsContIVars.isEmpty() || !mdp._hsNoiseVars.isEmpty()) {
            System.err.println("CAMDP Solver Main Fail: RTDP Solution incompatible with intermediate and noise Vars!");
            System.exit(1);
        }
        if (mdp._initialS == null) {
            System.err.println("CAMDP Solver Main Fail: RTDP Solution incompatible with undefined initial state.");
            System.exit(1);
        }
    }
}