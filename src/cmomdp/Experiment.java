package cmomdp;

import camdp.CAMDP;
import xadd.ExprLib;
import xadd.optimization.OptimisationResult;
import xadd.optimization.Optimise;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
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

        Integer valueFuncXADD = valueIteration.solve(numIterations, true, subsMap, subsMapBoolean);

        /*
                Plot the results of Value Iteration
         */

        valueIteration.plotXADD(valueFuncXADD, "Final XADD");
        valueIteration.plotFunction(valueFuncXADD, "Final Value Function", 3);

        Experiment.WriteComputationResults(valueIteration, problemFile);

        /*
                Run the optimisation method.
         */

        Experiment.OptimiseValueFunction(valueIteration, valueFuncXADD, subsMap, subsMapBoolean, "location", -20.0, 20.0, 1.0);
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

        Integer valueFuncXADD = valueIteration.solve(numIterations, true, subsMap, subsMapBoolean);

        /*
                Plot the results of Value Iteration
         */

//        valueIteration.plotXADD(valueFuncXADD, "Final XADD");
//        valueIteration.plotFunction(valueFuncXADD, "Final Value Function", 3);

        Experiment.WriteComputationResults(valueIteration, problemFile);

        /*
                Take the derivative of the value function w.r.t. sell_prop
         */

        valueIteration.plotXADD(valueFuncXADD, "Deriv XADD Before");
        Integer derivValueDD = valueIteration.context.computeDerivative(valueFuncXADD, "sell_prop");
        valueIteration.plotXADD(derivValueDD, "Deriv XADD");
        valueIteration.plotFunction(derivValueDD, "Deriv Func 3D", 3);

        /*
                Run the optimisation method.
         */

        subsMap.put("perm_price", new ExprLib.DoubleExpr(35.0));
        subsMap.put("temp_price", new ExprLib.DoubleExpr(30.0));
        subsMap.put("capture", new ExprLib.DoubleExpr(0.01));

        Experiment.OptimiseValueFunction(valueIteration, derivValueDD, subsMap, subsMapBoolean, "inventory", 1.0, 10.0, 1.0);
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

        subsMapBoolean.put("eps", Boolean.TRUE);
        subsMapBoolean.put("eta", Boolean.TRUE);

        subsMap.put("i", new ExprLib.DoubleExpr(100.0));
        subsMap.put("r", new ExprLib.DoubleExpr(0.01));

        /*
                Run Value Iteration
         */

        Integer valueFuncXADD = valueIteration.solve(numIterations, true, subsMap, subsMapBoolean);

        /*
                Plot the results of Value Iteration
         */

        valueIteration.plotXADD(valueFuncXADD, "Final XADD");
        valueIteration.plotFunction(valueFuncXADD, "Final Value Function", 3);

        Experiment.WriteComputationResults(valueIteration, problemFile);

        /*
                Run the optimisation method.
         */

        Experiment.OptimiseValueFunction(valueIteration, valueFuncXADD, subsMap, subsMapBoolean, "inventory", 1.0, 10.0, 1.0);
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
     */
    private static void OptimiseValueFunction(VI valueIteration, Integer valueFuncXADD, HashMap<String, ExprLib.ArithExpr> subsMap,
                                              HashMap<String, Boolean> subsMapBoolean, String variable,
                                              Double lowerBound, Double uppperBound, Double increment) {

        for(Double location = lowerBound; location <= uppperBound; location += increment) {
            subsMap.put(variable, new ExprLib.DoubleExpr(location));

            Integer optXADD = valueIteration.context.substituteBoolVars(valueFuncXADD, subsMapBoolean);
            optXADD = valueIteration.context.substitute(optXADD, subsMap);

//            valueIteration.plotXADD(optXADD, "OptXADD " + variable + ": " + location);
//            valueIteration.plotFunction(optXADD, "OptXADD " + variable + ": " + location + " 3D", 3);

            OptimisationResult optimalResult = Optimise.optimisePaths(valueIteration.context, optXADD, null, null);
            System.out.format("(%f, %f) = %f\n", location, optimalResult.getArgMax(), optimalResult.getMaxValue());
        }
    }

    /**
     *
     * @param valueIteration
     * @param problemFile
     */
    public static void WriteComputationResults(VI valueIteration, File problemFile) {

        try {
            String computationStatsFile = Experiment.GenerateComputationalStatsFileName(problemFile);
            FileOutputStream fOut = new FileOutputStream(computationStatsFile);

            valueIteration.writeSolutionStatistics(new PrintStream(fOut));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param problemFile
     * @return
     */
    private static String GenerateComputationalStatsFileName(File problemFile) {
        String computationStatsFile = problemFile.getParent() + File.separator + Experiment.RESULTS_FOLDER_NAME +
                File.separator + problemFile.getName().replaceFirst(".cmdp", "_stats.csv");
        return computationStatsFile;
    }
}
