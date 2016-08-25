package cmomdp;

import camdp.CAMDP;
import xadd.ExprLib;
import xadd.optimization.OptimisationResult;
import xadd.optimization.Optimise;

import java.util.HashMap;

/**
 * Created by skin on 2016-08-23.
 */
public class VI extends camdp.solver.VI {


    public VI(CAMDP camdp, int iter) {
        super(camdp, iter);
    }

    public VI(CAMDP camdp, int iter, double approxError) {
        super(camdp, iter, approxError);
    }

    public int solve() {
        return 0;
    }

    /**
     * 
     * @param numIterations
     * @param optimiseValueFunction
     * @return
     */
    public Integer solve(Integer numIterations, Boolean optimiseValueFunction, HashMap<String, ExprLib.ArithExpr> subsMap,
                         HashMap<String, Boolean> subsMapBoolean) {
        int RUN_DEPTH = 1;

        Integer _prevDD = null;

        //Iteration counter
        curIter = 0;

        //Initialize value function to zero
        valueDD = context.ZERO;

        // Perform value iteration for specified number of iterations, or until convergence detected
        while (curIter < numIterations) {
            
            ++curIter;
            CAMDP.resetTimer(RUN_DEPTH);

            // Prime diagram
            _prevDD = valueDD;

//            valueDD = context.substitute(valueDD, subsMap);

            super.bellmanBackup();

//            checkLinearApprox(); //Approximation at the end of Iter

            super.updateSolutionStatistics("time", new Double(CAMDP.getElapsedTime(RUN_DEPTH) + (curIter > 1 ? super.solutionTimeList[curIter - 1] : 0)));
            super.updateSolutionStatistics("node_count", new Double(context.getNodeCount(valueDD)));
            super.updateSolutionStatistics("branch_count", new Double(context.getBranchCount(valueDD)));

            super.solutionDDList[curIter] = valueDD;
            super.solutionTimeList[curIter] = CAMDP.getElapsedTime(RUN_DEPTH) + (curIter > 1 ? solutionTimeList[curIter - 1] : 0);
            super.solutionNodeList[curIter] = context.getNodeCount(valueDD);
            //if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curIter] = context.linMaxVal(valueDD);

            if (mdp._initialS != null) {
                solutionInitialSValueList[curIter] = mdp.evaluateInitialS(valueDD);
            }

            if(optimiseValueFunction) {
//                plotXADD(valueDD, "valueDD: H " + curIter);
                int tempXADD = context.substituteBoolVars(valueDD, subsMapBoolean);
                tempXADD = context.substitute(tempXADD, subsMap);
//                plotXADD(tempXADD, "Optimise: " + curIter);

                OptimisationResult optimalValue = Optimise.optimisePaths(context, tempXADD, null, null);
                super.updateSolutionStatistics("cpu_time", optimalValue.getCpuTime());
            }

            if (_prevDD.equals(valueDD)) {
                break;
            }
        }

//        plotXADD(valueDD, "valueDD: H " + curIter);

        // Take the derivative of the valueDD w.r.t. the weight
//        Integer derivValueDD = context.computeDerivative(valueDD, "w1");
//        plotXADD(derivValueDD, "deriv: H " + curIter);

        flushCaches();
        
        return valueDD;        
    }

    /**
     *
     * @param xaddID
     * @param plotTitle
     */
    public void plotFunction(Integer xaddID, String plotTitle, Integer displayDimension) {

        if(displayDimension == 3) {
            mdp.display3D(xaddID, plotTitle);
        } else if(displayDimension == 2) {
            mdp.display2D(xaddID, plotTitle);
        }
    }
}
