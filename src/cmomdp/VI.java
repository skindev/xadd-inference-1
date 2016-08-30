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
     * @param subsMap
     * @param subsMapBoolean
     * @return
     */
    public Integer solve(Integer numIterations, Boolean optimiseValueFunction, HashMap<String, ExprLib.ArithExpr> subsMap,
                         HashMap<String, Boolean> subsMapBoolean) {
        int RUN_DEPTH = 1;

        Integer _prevDD;

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

            super.bellmanBackup();

            checkLinearApprox(); //Approximation at the end of Iter

            super.updateSolutionStatistics("time", new Double(CAMDP.getElapsedTime(RUN_DEPTH) + (curIter > 1 ? super.solutionTimeList[curIter - 1] : 0)));
            super.updateSolutionStatistics("node_count", new Double(context.getNodeCount(valueDD)));
            super.updateSolutionStatistics("branch_count", new Double(context.getBranchCount(valueDD)));

            super.solutionDDList[curIter] = valueDD;
            super.solutionTimeList[curIter] = CAMDP.getElapsedTime(RUN_DEPTH) + (curIter > 1 ? solutionTimeList[curIter - 1] : 0);
            super.solutionNodeList[curIter] = context.getNodeCount(valueDD);
            //if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curIter] = context.linMaxVal(valueDD);

            if (mdp._initialS != null) {
                Double val = mdp.evaluateInitialS(valueDD);
                System.out.println("evaluateInitialS: " + val);
                solutionInitialSValueList[curIter] = val;
            }

            if(optimiseValueFunction) {

//                this.plotXADD(valueDD, "before opt subs");
                // Substitute variables in the valueDD to make the optimisation easier
                int optXADD = context.substituteBoolVars(valueDD, subsMapBoolean);
                optXADD = context.substitute(optXADD, subsMap);
//                this.plotXADD(valueDD, "after opt subs");

                OptimisationResult optimalValue;

                // If on the first iteration of VI, then run the optimisation method twice and discard start-up time
                Integer numOpts = curIter == 1 ? 2 : 1;
                for(Integer n = 1; n <= numOpts; n++) {
                    optimalValue = Optimise.optimisePaths(context, optXADD, null, null);
                    if(n.equals(numOpts)) {
                        super.updateSolutionStatistics("cpu_time", optimalValue.getCpuTime());
                    }
                }
            }

            if (_prevDD.equals(valueDD)) {
                break;
            }
        }

        flushCaches();
        
        return valueDD;        
    }

    /**
     *
     * @param xaddID
     * @param plotTitle
     * @param displayDimension
     */
    public void plotFunction(Integer xaddID, String plotTitle, Integer displayDimension) {

        if(displayDimension == 3) {
            mdp.display3D(xaddID, plotTitle);
        } else if(displayDimension == 2) {
            mdp.display2D(xaddID, plotTitle);
        }
    }
}
