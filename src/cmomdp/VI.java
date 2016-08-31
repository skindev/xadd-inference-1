package cmomdp;

import camdp.CAMDP;
import xadd.ExprLib;
import xadd.optimization.OptimisationResult;
import xadd.optimization.Optimise;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by skin on 2016-08-23.
 */
public class VI extends camdp.solver.VI {

    private HashMap<String, ArrayList<Double>> solutionStatistics;

    public VI(CAMDP camdp, Integer numIterations) {
        super(camdp, numIterations);
        this.solutionStatistics = new HashMap<String, ArrayList<Double>>();
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

        super.setupResults();

        // Perform value iteration for specified number of iterations, or until convergence detected
        while (curIter < numIterations) {
            
            ++curIter;
            CAMDP.resetTimer(RUN_DEPTH);

            // Prime diagram
            _prevDD = valueDD;

            super.bellmanBackup();

            checkLinearApprox(); //Approximation at the end of Iter

            this.updateSolutionStatistics("time", new Double(CAMDP.getElapsedTime(RUN_DEPTH) + (curIter > 1 ? super.solutionTimeList[curIter - 1] : 0)));
            this.updateSolutionStatistics("node_count", new Double(context.getNodeCount(valueDD)));
            this.updateSolutionStatistics("branch_count", new Double(context.getBranchCount(valueDD)));

            super.solutionDDList[curIter] = valueDD;

            if (mdp._initialS != null) {
                Double val = mdp.evaluateInitialS(valueDD);
                System.out.println("evaluateInitialS: " + val);
            }

            if(optimiseValueFunction) {
                // Substitute variables in the valueDD to make the optimisation easier
                int optXADD = context.substituteBoolVars(valueDD, subsMapBoolean);
                optXADD = context.substitute(optXADD, subsMap);

                OptimisationResult optimalValue;

                // If on the first iteration of VI, then run the optimisation method twice and discard start-up time
                Integer numOpts = curIter == 1 ? 2 : 1;
                for(Integer n = 1; n <= numOpts; n++) {
                    optimalValue = Optimise.optimisePaths(context, optXADD, null, null);
                    if(n.equals(numOpts)) {
                        this.updateSolutionStatistics("cpu_time", optimalValue.getCpuTime());
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
     * @param categoryName
     * @param value
     */
    protected void updateSolutionStatistics(String categoryName, Double value) {

        if(!this.solutionStatistics.containsKey(categoryName)) {
            this.solutionStatistics.put(categoryName, new ArrayList<Double>());
        }

        ArrayList<Double> values = this.solutionStatistics.get(categoryName);
        values.add(value);
    }

    /**
     *
     * @param out
     */
    public void writeSolutionStatistics(PrintStream out) {

        if(out == null) {
            out = System.out;
        }

        PrintWriter writer = new PrintWriter(out);

        Integer finalIter = this.solutionStatistics.get("node_count").size();

        writer.println("Horizon,SDPTime,Nodes,Branches,OptTime");
        for(Integer index = 0; index < finalIter; index++) {
            Double numNodes = this.solutionStatistics.get("node_count").get(index);
            Double numBranches = this.solutionStatistics.get("branch_count").get(index);
            Double time = this.solutionStatistics.get("time").get(index);

            String writeString = index+1 + "," + time + "," + numNodes + "," + numBranches;

            Double cpuTime;
            if(this.solutionStatistics.containsKey("cpu_time")) {
                cpuTime = this.solutionStatistics.get("cpu_time").get(index);
                writeString += "," + cpuTime;
            }

            writer.println(writeString);
        }

        writer.close();
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
