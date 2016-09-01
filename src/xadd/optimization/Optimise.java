package xadd.optimization;

import camdp.CAMDP;
import graph.Graph;
import xadd.ExprLib;
import xadd.XADD;
import xadd.XADDUtils;

import java.util.*;

public class Optimise {

    private static HashSet<IOptimisationTechnique> optimisers;

    /**
     *
     * @param optimiser
     */
    public static void RegisterOptimisationMethod(IOptimisationTechnique optimiser) {

        if(Optimise.optimisers == null) {
            Optimise.optimisers = new HashSet<IOptimisationTechnique>();
        }

        Optimise.optimisers.add(optimiser);
    }

    /**
     *
     * @param maxMin
     * @param objective
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
    private static OptimisationResult RunOptimisationMethod(OptimisationDirection maxMin, String objective, Set<String> variables,
                                                 Collection<String> constraints, Collection<String> lowerBounds,
                                                                                 Collection<String> upperBounds) {

        OptimisationResult result = null;

        for(IOptimisationTechnique optimiser : Optimise.optimisers) {

            result = optimiser.run(maxMin, objective, variables, constraints, lowerBounds, upperBounds);
            System.out.println(objective + ". optimalValue: " + result.getMaxValue() + " argMax: " + result.getArgMax());
        }

        return result;
    }

    /**
     *
     * @param maxMin
     * @param context
     * @param xaddID
     * @param varSet
     * @param constraintsMap
     * @return
     */
    public static OptimisationResult optimisePaths(OptimisationDirection maxMin, XADD context, Integer xaddID,
                                                   HashSet<String> varSet,
                                       HashMap<Integer, String> constraintsMap) {

        OptimisationResult lowMResult = null;
        OptimisationResult highMResult = null;

        XADD.XADDNode rootNode = context.getExistNode(xaddID);
        if(constraintsMap == null) {
            constraintsMap = new HashMap<Integer, String>();
        }

        if(varSet == null) {
            varSet = new HashSet<String>();
        }

        if(rootNode instanceof XADD.XADDTNode) {
            XADD.XADDTNode tNode = (XADD.XADDTNode) rootNode;

            // Extract the ExprLib.ArithExpr contained in the node
            ExprLib.ArithExpr expression = tNode._expr;

            // If the expression is a DoubleExpr, then return the _dConstVal and skip
            // evaluating the mathematical program
            if(expression instanceof ExprLib.DoubleExpr) {
                return new OptimisationResult(((ExprLib.DoubleExpr) expression)._dConstVal);
            }

            HashSet<String> lowerBounds = new HashSet<String>();
            HashSet<String> upperBounds = new HashSet<String>();

            // Convert the lower and upper bounds into "canonical" form i.e. c(x) > 0
            for(Map.Entry<String, Double> entry : context._hmMinVal.entrySet()) {
                String var = entry.getKey();

                lowerBounds.add(var + " - " + " (" + entry.getValue().toString() + ")");
                upperBounds.add("-1 * (" + var + " - " + context._hmMaxVal.get(var) + ")");
            }

            HashSet<String> vars = new HashSet<String>();
            expression.collectVars(vars);
            expression.collectVars(varSet);

            OptimisationResult result = Optimise.RunOptimisationMethod(maxMin, expression.toString(), varSet,
                                            constraintsMap.values(), lowerBounds, upperBounds);
            return result;
        } else {
            XADD.XADDINode iNode = (XADD.XADDINode) rootNode;

            int iNodeVar = iNode._var;
            XADD.Decision iNodeDecision = iNode.getDecision();
            ExprLib.CompExpr compExpr = ((XADD.ExprDec) iNodeDecision)._expr;
            String iNodeDecisionStr = null;

            // Convert the comparison expression in the iNodeDecision into canonical form i.e. c(x) > 0
            ExprLib.Expr tmp;
            if(compExpr.isGreater()) {
                tmp = compExpr.makeCanonical();
                iNodeDecisionStr = ((ExprLib.CompExpr) tmp)._lhs.toString();
            }

            compExpr.collectVars(varSet);

            int iNodeLow = iNode._low;
            int iNodeHigh = iNode._high;

            if (context._alOrder.get(iNodeVar) instanceof XADD.ExprDec) {

                // Low branch
                constraintsMap.put(iNodeVar, "-1 * " + iNodeDecisionStr);
                lowMResult = Optimise.optimisePaths(maxMin, context, iNodeLow, varSet, constraintsMap);
                constraintsMap.remove(iNodeVar);

                // High branch
                constraintsMap.put(iNodeVar, iNodeDecisionStr);
                highMResult = Optimise.optimisePaths(maxMin, context, iNodeHigh, varSet, constraintsMap);
                constraintsMap.remove(iNodeVar);

            } else {
                // TODO: Raise Exception?
            }

        }

        OptimisationResult returnResult;
        if(maxMin == OptimisationDirection.MAXIMISE) {
            returnResult = lowMResult.getMaxValue() > highMResult.getMaxValue() ? lowMResult : highMResult;
        } else {
            returnResult = lowMResult.getMaxValue() < highMResult.getMaxValue() ? lowMResult : highMResult;
        }

        return returnResult;
    }

    public static void main(String[] args) {

        // Register the MATLABNonLinear optimiser with the class
        Optimise.RegisterOptimisationMethod(new MATLABNonLinear());

        XADD xadd_context = new XADD();
        HashMap<String, Boolean> subsMapBoolean = new HashMap<String, Boolean>();
        HashMap<String, ExprLib.ArithExpr> subsMap = new HashMap<String, ExprLib.ArithExpr>();

        subsMap.put("ax", new ExprLib.DoubleExpr(0.0));
        subsMap.put("ay", new ExprLib.DoubleExpr(1.0));
        subsMap.put("y", new ExprLib.DoubleExpr(1.0));

        Integer ixadd = xadd_context.buildCanonicalXADDFromFile("./src/xadd/optimization/test6.xadd");
        CAMDP.FileOptions opt = new CAMDP.FileOptions("/Users/skin/repository/xadd-inference-1/src/xadd/optimization/test6.2d");

        Graph gc = xadd_context.getGraph(ixadd);
        gc.launchViewer("Before subs");

        Integer newIXADD = xadd_context.substitute(ixadd, subsMap);

        gc = xadd_context.getGraph(newIXADD);
        gc.launchViewer("After subs");

//        XADDUtils.PlotXADD(xadd_context, ixadd, opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0),
//                opt._bassign, opt._dassign, opt._var.get(0), "2D");

        OptimisationResult result = Optimise.optimisePaths(OptimisationDirection.MINIMISE, xadd_context, newIXADD, null, null);
        System.out.println(result.getMaxValue());
//        System.out.println(result.getArgMax());
//        System.out.println(result.getCpuTime());

//        subsMap.put("x", new ExprLib.DoubleExpr(result.getMaxValue()));
//        ixadd = xadd_context.substitute(ixadd, subsMap);

//        gc = xadd_context.getGraph(ixadd);
//        gc.launchViewer("Result");


    }
}
