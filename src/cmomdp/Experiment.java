package cmomdp;

import camdp.CAMDP;
import xadd.ExprLib;
import xadd.optimization.OptimisationDirection;
import xadd.optimization.OptimisationResult;
import xadd.optimization.Optimise;

import java.io.*;
import java.util.HashMap;

/**
 * Experiments for AAAI 2017
 * Created by skin on 2016-08-31.
 */
public class Experiment {

    private static String RESULTS_FOLDER_NAME = "results";

    /**
     *
     * @param camdp
     * @param valueIteration
     * @param numIterations
     */
    public static void Robot1D(CAMDP camdp, VI valueIteration, Integer numIterations) {

        HashMap<String, Boolean> subsMapBoolean = new HashMap<String, Boolean>();
        HashMap<String, ExprLib.ArithExpr> subsMap = new HashMap<String, ExprLib.ArithExpr>();
        File problemFile = new File(camdp._problemFile);

        subsMapBoolean.put("eps", Boolean.TRUE);

        /*
                Run Value Iteration
         */
        System.out.println("VI");
        Integer valueFuncXADD = valueIteration.solve(numIterations, true, OptimisationDirection.MAXIMISE, subsMap, subsMapBoolean);

        Experiment.WriteComputationResults(valueIteration, problemFile);
        /*
                Plot the results of Value Iteration
         */

        System.out.println("Plotting");
        valueIteration.plotXADD(valueFuncXADD, "Final XADD");
        valueIteration.plotFunction(valueFuncXADD, "Final Value Function", 3);

        /*
                Run the optimisation method.
         */

        System.out.println("Optimising");
        Experiment.OptimiseValueFunction(OptimisationDirection.MAXIMISE, valueIteration, valueFuncXADD, subsMap,
                subsMapBoolean, "location", -20.0, 20.0, 1.0, problemFile);

        System.out.println("COMPLETE");
    }

    /**
     *
     * @param camdp
     * @param valueIteration
     * @param numIterations
     */
    public static void OptimalExecution(CAMDP camdp, VI valueIteration, Integer numIterations) {

        HashMap<String, Boolean> subsMapBoolean = new HashMap<String, Boolean>();
        HashMap<String, ExprLib.ArithExpr> subsMap = new HashMap<String, ExprLib.ArithExpr>();
        File problemFile = new File(camdp._problemFile);

        subsMapBoolean.put("eps", Boolean.TRUE);
        subsMapBoolean.put("eta", Boolean.TRUE);

        subsMap.put("inventory", new ExprLib.DoubleExpr(1000.0));
        subsMap.put("temp_price", new ExprLib.DoubleExpr(0.01));
        subsMap.put("inventory", new ExprLib.DoubleExpr(500.0));
        subsMap.put("capture", new ExprLib.DoubleExpr(0.01));

        /*
                Run Value Iteration
         */

        System.out.println("VI");

        Integer valueFuncXADD = valueIteration.solve(numIterations, true, OptimisationDirection.MAXIMISE, subsMap, subsMapBoolean);
        Experiment.WriteComputationResults(valueIteration, problemFile);

        /*
                Plot the results of Value Iteration
         */

//        valueIteration.plotXADD(valueFuncXADD, "Final XADD");
//        valueIteration.plotFunction(valueFuncXADD, "Final Value Function", 3);

         // Take the derivative of the value function w.r.t. sell_prop
        System.out.println("Plotting");
//        valueIteration.plotXADD(valueFuncXADD, "Deriv XADD Before");
        Integer derivValueDD = valueIteration.context.computeDerivative(valueFuncXADD, "sell_prop");
//        valueIteration.plotXADD(derivValueDD, "Deriv XADD");
        valueIteration.plotFunction(derivValueDD, "Deriv Func 3D", 3);

        /*
                Run the optimisation method.
         */
        subsMap.put("perm_price", new ExprLib.DoubleExpr(35.0));
        subsMap.put("temp_price", new ExprLib.DoubleExpr(30.0));
        subsMap.put("capture", new ExprLib.DoubleExpr(0.01));

        System.out.println("Optimising");
        Experiment.OptimiseValueFunction(OptimisationDirection.MAXIMISE, valueIteration, derivValueDD, subsMap,
                subsMapBoolean, "inventory", 0.0, 1000.0, 5.0, problemFile);

        System.out.println("COMPLETE");
    }

    /**
     *
     * @param camdp
     * @param valueIteration
     * @param numIterations
     */
    public static void SIR(CAMDP camdp, VI valueIteration, Integer numIterations) {

        HashMap<String, Boolean> subsMapBoolean = new HashMap<String, Boolean>();
        HashMap<String, ExprLib.ArithExpr> subsMap = new HashMap<String, ExprLib.ArithExpr>();
        File problemFile = new File(camdp._problemFile);

//        subsMapBoolean.put("eps", Boolean.TRUE);
//        subsMapBoolean.put("eta", Boolean.TRUE);

        subsMap.put("s", new ExprLib.DoubleExpr(1000.0));
        subsMap.put("i", new ExprLib.DoubleExpr(100.0));
        subsMap.put("r", new ExprLib.DoubleExpr(0.0));

        /*
                Run Value Iteration
         */

        System.out.println("VI");
        Integer valueFuncXADD = valueIteration.solve(numIterations, false, OptimisationDirection.MAXIMISE, subsMap, subsMapBoolean);

        Experiment.WriteComputationResults(valueIteration, problemFile);

        /*
                Plot the results of Value Iteration
         */

        System.out.println("Plotting");
//        valueIteration.plotXADD(valueFuncXADD, "Final XADD");
//        valueIteration.plotFunction(valueFuncXADD, "Final Value Function", 3);

        /*
                Run the optimisation method.
         */

        System.out.println("Optimising");
        Experiment.OptimiseValueFunction(OptimisationDirection.MAXIMISE, valueIteration, valueFuncXADD, subsMap,
                subsMapBoolean, "beta", 0.0, 1.0, 0.05, problemFile);

        System.out.println("COMPLETE");
    }

    /**
     *
     * @param valueIteration
     * @param valueFuncXADD
     * @param subsMap
     * @param subsMapBoolean
     * @param variable
     * @param lowerBound
     * @param uppperBound
     * @param increment
     * @param problemFile
     */
    private static void OptimiseValueFunction(OptimisationDirection maxMin, VI valueIteration, Integer valueFuncXADD,
                                              HashMap<String, ExprLib.ArithExpr> subsMap,
                                              HashMap<String, Boolean> subsMapBoolean, String variable,
                                              Double lowerBound, Double uppperBound, Double increment, File problemFile) {

        try {
            String computationStatsFile = Experiment.GenerateCSVFileName(problemFile, "opt");
            FileOutputStream fOut = new FileOutputStream(computationStatsFile);

            PrintWriter writer = new PrintWriter(fOut);

            writer.println(variable + ",argmax,max");
            for(Double var = lowerBound; var <= uppperBound; var += increment) {
                subsMap.put(variable, new ExprLib.DoubleExpr(var));

                Integer optXADD = valueIteration.context.substituteBoolVars(valueFuncXADD, subsMapBoolean);
                optXADD = valueIteration.context.substitute(optXADD, subsMap);

//            valueIteration.plotXADD(optXADD, "OptXADD " + variable + ": " + var);
//            valueIteration.plotFunction(optXADD, "OptXADD " + variable + ": " + var + " 3D", 3);

                OptimisationResult optimalResult = Optimise.optimisePaths(maxMin, valueIteration.context, optXADD, null, null);
                writer.format("%f,%f,%f\n", var, optimalResult.getArgMax(), optimalResult.getMaxValue());
            }

            writer.close();

        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param valueIteration
     * @param problemFile
     */
    public static void WriteComputationResults(VI valueIteration, File problemFile) {

        try {
            String computationStatsFile = Experiment.GenerateCSVFileName(problemFile, "stats");
            FileOutputStream fOut = new FileOutputStream(computationStatsFile);

            valueIteration.writeSolutionStatistics(new PrintStream(fOut));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param problemFile
     * @param dataType
     * @return
     */
    private static String GenerateCSVFileName(File problemFile, String dataType) {
        String computationStatsFile = problemFile.getParent() + File.separator + Experiment.RESULTS_FOLDER_NAME +
                File.separator + problemFile.getName().replaceFirst(".cmdp", "_" + dataType.toLowerCase() + ".csv");
        return computationStatsFile;
    }
}
