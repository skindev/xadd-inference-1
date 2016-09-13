package cmomdp;

import camdp.CAMDP;
import camdp.solver.CAMDPsolver;
import com.sun.org.apache.xpath.internal.operations.Bool;
import xadd.ExprLib;
import xadd.XADD;
import xadd.optimization.OptimisationDirection;
import xadd.optimization.OptimisationResult;
import xadd.optimization.Optimise;

import java.io.*;
import java.util.ArrayList;
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
     * @param numIterations
     */
    public static void Robot1D_IRL(CAMDP camdp, Integer numIterations) {

        HashMap<String, Boolean> subsMapBoolean = new HashMap<String, Boolean>();
        HashMap<String, ExprLib.ArithExpr> subsMap = new HashMap<String, ExprLib.ArithExpr>();
        File problemFile = new File(camdp._problemFile);

        subsMapBoolean.put("eps", Boolean.TRUE);
        subsMap.put("location", new ExprLib.DoubleExpr(5.0));

        /*
                Run Value Iteration for numIterations - 1
         */

        Integer penultimateIteration = numIterations - 1;

        System.out.println("VI");

        Integer valueFuncXADD = null;
        VI valueIteration = null;

        for(Integer exp = 1; exp <= 1; exp++) {
            System.out.println("Experiment: " + exp);
            System.out.println("------------");

            valueIteration = new VI(camdp, numIterations);
            valueFuncXADD = valueIteration.solve(penultimateIteration, false, OptimisationDirection.MAXIMISE, subsMap, subsMapBoolean);
            Experiment.WriteComputationResults(valueIteration, problemFile, exp);
        }

        subsMap.remove("location");

        /*
                Plot the results of VI
         */

//        System.out.println("Plotting Value Functions H " +  penultimateIteration);
//        valueIteration.plotXADD(valueFuncXADD, "XADD H " + penultimateIteration);
//        valueIteration.plotFunction(valueFuncXADD, "Value Function H " +  penultimateIteration, 3);

        /*
                Backup for an extra horizon
         */

        HashMap<String, Integer> qValueFuncMap = valueIteration.tempBackup(valueFuncXADD, subsMap, subsMapBoolean);

        /*
                Run the optimisation method.
         */

        System.out.println("Optimising");

        HashMap<Integer, ArrayList<Double>> locationBounds = new HashMap<Integer, ArrayList<Double>>();

        ArrayList<Double> bounds1 = new ArrayList<Double>();
        ArrayList<Double> bounds2 = new ArrayList<Double>();
        ArrayList<Double> bounds3 = new ArrayList<Double>();

        bounds1.add(-20.0);
        bounds1.add(0.0);

        bounds2.add(0.0);
        bounds2.add(10.0);

        bounds3.add(10.0);
        bounds3.add(20.0);

        locationBounds.put(1, bounds1);
        locationBounds.put(2, bounds2);
        locationBounds.put(3, bounds3);

//        Integer location_number = 1;
        Integer noOp = qValueFuncMap.get("noop");
        Integer right = qValueFuncMap.get("right");
        Integer max = qValueFuncMap.get("max");

        for(Integer location_number = 2; location_number <= 2; location_number++) {

            Integer locationIndicator = CAMDPsolver.context.buildCanonicalXADDFromFile("/Users/skin/repository/xadd-inference-1/src/cmomdp/domain/aaai2017/location_indicator" + location_number + ".xadd");

            Integer noOpLocation = CAMDPsolver.context.apply(noOp, locationIndicator, XADD.PROD);
            Integer rightLocation = CAMDPsolver.context.apply(right, locationIndicator, XADD.PROD);
            Integer maxLocation = CAMDPsolver.context.apply(max, locationIndicator, XADD.PROD);

//            valueIteration.plotFunction(maxLocation, "max location_" + location_number, 3);
//            valueIteration.plotXADD(noOpLocation, "noOpLocation location_" + location_number);
//            valueIteration.plotXADD(rightLocation, "rightLocation location_" + location_number);

            Integer diff_noOp_right = CAMDPsolver.context.apply(noOpLocation, rightLocation, XADD.MINUS);
            Integer diff_right_noOp = CAMDPsolver.context.apply(rightLocation, noOpLocation, XADD.MINUS);

            diff_noOp_right = CAMDPsolver.context.apply(diff_noOp_right, locationIndicator, XADD.PROD);
            diff_right_noOp = CAMDPsolver.context.apply(diff_right_noOp, locationIndicator, XADD.PROD);

//            valueIteration.plotXADD(diff_noOp_right, "diff_noOp_right location_" + location_number);
//            valueIteration.plotXADD(diff_right_noOp, "diff_right_noOp location_" + location_number);

//            valueIteration.plotFunction(diff_noOp_right, "diff_noOp_right location_" + location_number, 3);
//            valueIteration.plotFunction(diff_right_noOp, "diff_right_noOp location_" + location_number, 3);

            ArrayList<Double> bounds = locationBounds.get(location_number);

            if(location_number == 2) {
                System.out.println("move - noop");
                valueIteration.plotFunction(diff_right_noOp, "diff_right_noOp location_" + location_number, 3);
                Experiment.OptimiseValueFunction(OptimisationDirection.MAXIMISE, valueIteration, diff_right_noOp, subsMap,
                        subsMapBoolean, "location", bounds.get(0), bounds.get(1), 0.25, problemFile,
                        "_max_right_noop_location_" + location_number);
            } else {
                System.out.println("noop - move");
                valueIteration.plotFunction(diff_noOp_right, "diff_noOp_right location_" + location_number, 3);
                Experiment.OptimiseValueFunction(OptimisationDirection.MAXIMISE, valueIteration, diff_noOp_right, subsMap,
                        subsMapBoolean, "location", bounds.get(0), bounds.get(1), 1.0, problemFile,
                        "_max_noop_right_location_" + location_number);
            }
        }

        System.out.println("COMPLETE");
    }

    /**
     *
     * @param camdp
     * @param numIterations
     */
    public static void OptimalExecution(CAMDP camdp, Integer numIterations) {

        HashMap<String, Boolean> subsMapBoolean = new HashMap<String, Boolean>();
        HashMap<String, ExprLib.ArithExpr> subsMap = new HashMap<String, ExprLib.ArithExpr>();
        File problemFile = new File(camdp._problemFile);

        subsMapBoolean.put("eps", Boolean.TRUE);

        subsMap.put("inventory", new ExprLib.DoubleExpr(500.0));
        subsMap.put("price", new ExprLib.DoubleExpr(55.0));
        subsMap.put("market_impact", new ExprLib.DoubleExpr(0.165));

        /*
                Run Value Iteration
         */

        System.out.println("VI");

        Integer valueFuncXADD = null;
        VI valueIteration = null;

        for(Integer exp = 1; exp <= 1; exp++) {
            valueIteration = new VI(camdp, numIterations);
            valueFuncXADD = valueIteration.solve(numIterations, true, OptimisationDirection.MAXIMISE, subsMap, subsMapBoolean);
//            Experiment.WriteComputationResults(valueIteration, problemFile, exp);
        }
        /*
                Plot the results of Value Iteration
         */

//        System.out.println("Plotting");

//        valueIteration.plotXADD(valueFuncXADD, "Final XADD");
//        valueIteration.plotFunction(valueFuncXADD, "Final Value Function", 3);

         // Take the derivative of the value function w.r.t. sell_prop
//        valueIteration.plotXADD(valueFuncXADD, "Deriv XADD Before");

//        Integer derivValueDD = valueIteration.context.computeDerivative(valueFuncXADD, "sell_prop");
//        Integer marketImpact_derivValueDD = valueIteration.context.computeDerivative(valueFuncXADD, "market_impact");

//        valueIteration.plotXADD(derivValueDD, "Deriv XADD");
//        valueIteration.plotFunction(derivValueDD, "Sell Prop Deriv Func 3D", 3);

//        valueIteration.plotXADD(marketImpact_derivValueDD, "Market Impact Deriv Final XADD");
//        valueIteration.plotFunction(marketImpact_derivValueDD, "Market Impact Deriv Func 3D", 3);

//        Integer secondDeriv = valueIteration.context.computeDerivative(derivValueDD, "sell_prop");
//        valueIteration.plotFunction(secondDeriv, "Sell Prop 2nd Deriv Func 3D", 3);

        /*
                Run the optimisation method.
         */


//        System.out.println("Optimising");

//        Experiment.OptimiseValueFunction(OptimisationDirection.MAXIMISE, valueIteration, valueFuncXADD, subsMap,
//                subsMapBoolean, "inventory", 0.0, 1000.0, 10.0, problemFile, "_max");
//
//        Experiment.OptimiseValueFunction(OptimisationDirection.MINIMISE, valueIteration, valueFuncXADD, subsMap,
//                subsMapBoolean, "inventory", 0.0, 1000.0, 10.0, problemFile, "_min");

//        Experiment.OptimiseValueFunction(OptimisationDirection.MAXIMISE, valueIteration, derivValueDD, subsMap,
//                subsMapBoolean, "inventory", 0.0, 1000.0, 10.0, problemFile, "_deriv_max");
//
//        Experiment.OptimiseValueFunction(OptimisationDirection.MINIMISE, valueIteration, derivValueDD, subsMap,
//                subsMapBoolean, "inventory", 0.0, 1000.0, 10.0, problemFile, "_deriv_min");

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

        Experiment.WriteComputationResults(valueIteration, problemFile, 1);

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
                subsMapBoolean, "beta", 0.0, 1.0, 0.05, problemFile, "");

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
                                              Double lowerBound, Double uppperBound, Double increment, File problemFile, String suffix) {

        try {
            String computationStatsFile = Experiment.GenerateCSVFileName(problemFile, "opt" + suffix);
            FileOutputStream fOut = new FileOutputStream(computationStatsFile);

            PrintWriter writer = new PrintWriter(fOut);

            writer.println(variable + ",argmax,max");
            for(Double var = lowerBound; var <= uppperBound; var += increment) {
                subsMap.put(variable, new ExprLib.DoubleExpr(var));
                System.out.println("- " + variable + ": " + var);

                Integer optXADD = CAMDPsolver.context.substituteBoolVars(valueFuncXADD, subsMapBoolean);
                optXADD = CAMDPsolver.context.substitute(optXADD, subsMap);

//                valueIteration.plotXADD(optXADD, "OptXADD " + variable + ": " + var);
//                valueIteration.plotFunction(optXADD, "OptXADD " + variable + ": " + var + " 3D", 3);

                Integer approxXADDId = CAMDPsolver.context.linPruneRel(optXADD, 1.0e-6);
//                valueIteration.plotXADD(approxXADDId, "approxXADDId " + variable + ": " + var);
//                valueIteration.plotFunction(approxXADDId, "approxXADDId " + variable + ": " + var + " 3D", 3);

                OptimisationResult optimalResult = Optimise.optimisePaths(maxMin, CAMDPsolver.context, approxXADDId, null, null);
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
    public static void WriteComputationResults(VI valueIteration, File problemFile, Integer runNumber) {

        String computationStatsFile = Experiment.GenerateCSVFileName(problemFile, "stats");
        Boolean append = runNumber > 1;

        try {

            FileOutputStream fOut = new FileOutputStream(computationStatsFile, append);
            valueIteration.writeSolutionStatistics(new PrintStream(fOut), append);

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
