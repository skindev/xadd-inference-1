package polysolver;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.*;
import java.util.ArrayList;
import bernstein.Bernstein;
import xadd.ExprLib;
import xadd.XADD;


/**
 * Created by haroldsoh on 17/1/16.
 */
public class PolySolver {
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    public static final int UNKNOWN = -1;


    private static final Logger log = Logger.getLogger(PolySolver.class.getPackage().getName());

    public class PolyElem {
        java.util.HashMap<String,Integer> vars; //variables to some power
        double coeff = 0;   // coefficient for term
        public PolyElem() {
            vars = new java.util.HashMap<String,Integer>();
        }
        public PolyElem(ArrayList<String> variables) {
            vars = new java.util.HashMap<String,Integer>();
            for (String key: variables) {
                vars.put(key, 0);
            }
        }
        public PolyElem(PolyElem rhs) {
            vars = new java.util.HashMap<String,Integer>(rhs.vars);
            coeff = rhs.coeff;
        }

        // toString()
        public String toString() {
            String varstr = "";
            for (Map.Entry<String, Integer> entry : this.vars.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();
                if (value != 0) {
                    varstr += "*";
                    varstr += key + "^" + Integer.toString(value);
                }
            }
            return Double.toString(this.coeff) + varstr;
        }

        public String toBernsteinString(ArrayList<String> varorder, String coeff_suffix) {
            if (varorder.size() == 0) {
                return "";
            }
            String varstr = "COEF " + coeff_suffix + "(";
            varstr += Integer.toString(this.vars.get(varorder.get(0)));
            for (int i=1; i<varorder.size(); i++) {
                varstr += "," + Integer.toString(this.vars.get(varorder.get(i)));
            }
            varstr += ") " + Double.toString(this.coeff);
            return varstr;
        }


        public String toBernsteinString(ArrayList<String> varorder) {
            return toBernsteinString(varorder, "");
        }

        // compares two PolyElem to see if they have identical variables
        // but possibly different coefficients
        // e.g., 3*x^2 is equalVars to 4*x^2 but NOT equalVars to 3*x^3
        public boolean equalVars(PolyElem rhs) {
            for (Map.Entry<String, Integer> entry : rhs.vars.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();
                if (this.vars.containsKey(key)) {
                    if (!this.vars.get(key).equals(value)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }
    }

    public class PolyExpr {
        ArrayList<PolyElem> elems;  // an array of polynomial elements
        public PolyExpr() {
            elems = new ArrayList<PolyElem>();
        }
        public PolyExpr(PolyExpr rhs) {
            elems = new ArrayList<PolyElem>();
            for (int i = 0; i < rhs.elems.size(); i++) {
                this.elems.add(i, new PolyElem(rhs.elems.get(i)));
            }
        }

        public String toString() {
            String exprstr = "";
            if (elems.size() == 0) {
                return exprstr;
            }
            exprstr += "(" + elems.get(0).toString() + ")";
            for (int i=1; i<elems.size(); i++) {
                exprstr += " + (" + elems.get(i).toString() + ")";
            }

            return exprstr;
        }

        public String toBernsteinString(ArrayList<String> varorder) {
            if (varorder.size() == 0) {
                return "";
            }
            // get number of variables.
            String varstr = "VAR " + Integer.toString(varorder.size()) + "\n";
            // get coefficients
            for (int i=0; i<this.elems.size(); i++) {
                varstr += this.elems.get(i).toBernsteinString(varorder) + "\n";
            }

            return varstr;
        }

        public String toBernsteinString(ArrayList<String> varorder, String coeff_suffix, boolean show_nvar) {
            if (varorder.size() == 0) {
                return "";
            }
            // get number of variables.
            String varstr = "";
            if (show_nvar) {
                varstr = "VAR " + Integer.toString(varorder.size()) + "\n";
            }
            // get coefficients
            for (int i=0; i<this.elems.size(); i++) {
                varstr += this.elems.get(i).toBernsteinString(varorder, coeff_suffix) + "\n";
            }

            return varstr;
        }



        public void addToExpr(PolyElem elem) {
            // go through current elements to see if we find a match
            for (int i = 0; i < this.elems.size(); i++) {
                if (this.elems.get(i).equalVars(elem)) {
                    this.elems.get(i).coeff += elem.coeff;
                    return;
                }
            }

            // no match found
            // add this polynomial element to the expression
            this.elems.add(0, elem);
        }

        public void addToExpr(PolyExpr expr) {
            for (PolyElem elem : expr.elems) {
                this.addToExpr(elem);
            }
        }

        public void prodToExpr(PolyElem elem) {
            for (int i=0; i< this.elems.size(); i++) {
                // multiply the coefficient
                this.elems.get(i).coeff *= elem.coeff;

                // multiply the terms
                for (Map.Entry<String, Integer> entry : elem.vars.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    int new_value = value;
                    if (this.elems.get(i).vars.containsKey(key)) {
                        new_value = this.elems.get(i).vars.get(key) + value;
                    }
                    this.elems.get(i).vars.put(key, new_value);
                }
            }
        }

        public void prodToExpr(PolyExpr expr) {
            PolyExpr ori_expr = new PolyExpr(this);
            this.elems.clear();
            for (PolyElem elem : expr.elems) {
                PolyExpr curr_expr = new PolyExpr(ori_expr);
                curr_expr.prodToExpr(elem);
                this.addToExpr(curr_expr);
            }
        }

    }

    // constructor
    public PolySolver() {}

    // parses a string generated by XADD ExprLib and returns a set of PolyExpr
//    public PolyExpr parseXADDExprStr(String str) {
//        PolyExpr pexpr;
//
//        return pexpr;
//    }


    public PolyExpr parseXADDArithExpr(ExprLib.ArithExpr expr,
                                       ArrayList<String> vars) {
        PolyExpr pexpr = new PolyExpr();

        // check type of expression
        if (expr instanceof ExprLib.OperExpr) {
            ExprLib.OperExpr o = ((ExprLib.OperExpr) expr);
            // Currently only product and sum arithmetic operations are supported
            if (o._type == ExprLib.ArithOperation.PROD) {
                // handle products
                // log.info("handling prod");
                PolyElem one = new PolyElem();
                one.coeff = 1.0;
                pexpr.addToExpr(one);
                for (ExprLib.ArithExpr aexpr : o._terms) {
                    // log.info("prod term: " + aexpr.toString());
                    PolyExpr term_expr = parseXADDArithExpr(aexpr, vars);
                    pexpr.prodToExpr(term_expr);
                    // log.info("prod result: " + pexpr.toString());
                }

            } else if (o._type == ExprLib.ArithOperation.SUM) {
                // handle sum
                // log.info("handling sum");
                for (ExprLib.ArithExpr aexpr : o._terms) {
                    // log.info("sum term: " + aexpr.toString());
                    PolyExpr term_expr = parseXADDArithExpr(aexpr, vars);
                    pexpr.addToExpr(term_expr);
                    // log.info("add result: " + pexpr.toString());
                }
            }
        } else if (expr instanceof ExprLib.DoubleExpr) {
            // simply assign the coefficient since it is a constant
            PolyElem pelem = new PolyElem(vars);
            // log.info("double term: " + expr.toString());
            pelem.coeff = ((ExprLib.DoubleExpr) expr)._dConstVal;
            pexpr.addToExpr(pelem);
            // log.info("In doublexpr: " + pexpr.toString());
        } else if (expr instanceof ExprLib.VarExpr) {
            // assign variable ename
            PolyElem pelem = new PolyElem(vars);
            // log.info("var term: " + expr.toString());
            pelem.coeff = 1.0;
            pelem.vars.put(((ExprLib.VarExpr) expr)._sVarName, 1);
            pexpr.addToExpr(pelem);
            // log.info("In varexpr: " + pexpr.toString());
        } else {
            log.severe("Unexpected LHS of Expression");
        }


        return pexpr;
    }

    public int getXADDExprDecConstraintType(XADD.ExprDec dec, boolean flip_operator) throws UnsupportedOperationException {
        // get constraint relationship
        int propop = -1;
        ExprLib.CompOperation type = flip_operator ?
                ExprLib.CompExpr.flipCompOper(dec._expr._type) :
                dec._expr._type;

        switch (type) {
            case GT:
                propop = Bernstein.GT;
                break;
            case GT_EQ:
                propop = Bernstein.GE;
                break;
            case LT:
                propop = Bernstein.LT;
                break;
            case LT_EQ:
                propop = Bernstein.LE;
                break;
            default:
                log.severe("Unsupported comparison type.");
                throw new UnsupportedOperationException("Unsupported comparison type");
        }
        return propop;
    }


    public int testXADDExprDec(XADD.ExprDec dec, int rec_depth, XADD lxadd) {
        return testXADDExprDec(dec,  rec_depth, lxadd, false);
    }

    public int testXADDExprDec(XADD.ExprDec dec, int rec_depth, XADD lxadd, boolean flip_operator) {

        // get variables
        ArrayList<String> vars = lxadd.getContinuousVarList();

        // make into canonical form
        XADD.Decision d = dec.makeCanonical();
        log.info("Canonical form: " + d.toString());

        // get expression on the lhs
        ExprLib.ArithExpr lhs = dec._expr._lhs;
        PolyExpr pexpr = parseXADDArithExpr(lhs, vars);
        log.info(pexpr.toString());

        // get expression on the rhs
        ExprLib.DoubleExpr rhs = (ExprLib.DoubleExpr) dec._expr._rhs;
        double rhs_val = rhs._dConstVal;

        // convert to Bernstein string format
        // TODO: instead of converting to string format, directly create Bernstein polynomial.
        String polystr =  pexpr.toBernsteinString(vars);

        // get local bounds
        String bounds = "";
        int i=0;
        for (String v : vars) {
            bounds += "BOUND x" + Integer.toString(i) + " [" +
                    lxadd.lowerBounds[ lxadd._cvar2ID.get(v) ] + "," +
                    lxadd.upperBounds[ lxadd._cvar2ID.get(v) ] + "]\n";
            i++;
        }

        polystr += "\n" + bounds;

        // print bounds
        log.info(polystr);

        // get constraint type
        int propop = getXADDExprDecConstraintType(dec, flip_operator);

        // solve equation
        bernstein.Bernstein bern = new bernstein.Bernstein();
        String truth_val = bern.solveSimpleConstraint(polystr, propop, rhs_val, rec_depth);
        log.info("polysolver truth value: " + truth_val);
        if (truth_val.startsWith("T")) {
            return this.TRUE;
        } else if (truth_val.startsWith("F")) {
            return this.FALSE;
        } else {
            log.info("Unknown result");
        }
        return this.UNKNOWN; // default to returning false if unknown
    }


    public String getDecIdString(Integer dec_id, String coef_suffix, ArrayList<String> vars, XADD lxadd, boolean assump) {

        boolean flip_operator = false;
        if (dec_id < 0) {
            flip_operator = true;
            dec_id = -dec_id;
        }

        XADD.Decision dec = lxadd._alOrder.get(dec_id);
        if (dec instanceof XADD.ExprDec) {

            // make into canonical form
            XADD.ExprDec d = (XADD.ExprDec) dec.makeCanonical();
            //log.info("Canonical form: " + d.toString());

            // get expression on the lhs
            ExprLib.ArithExpr lhs = d._expr._lhs;
            PolyExpr pexpr = parseXADDArithExpr(lhs, vars);

            // get expression on the rhs
            ExprLib.DoubleExpr rhs = (ExprLib.DoubleExpr) d._expr._rhs;
            double rhs_val = rhs._dConstVal;

            // convert to Bernstein string format
            // TODO: instead of converting to string format, directly create Bernstein polynomial.
            String polystr =  pexpr.toBernsteinString(vars, coef_suffix, false);

            // add Sign
            polystr += "\n";
            polystr += "SIGN " + coef_suffix;
            int propop = getXADDExprDecConstraintType(d, flip_operator);
            switch (propop) {
                case Bernstein.GT:
                    polystr += "GT\n";
                    break;
                case Bernstein.GE:
                    polystr += "GE\n";
                    break;
                case Bernstein.LT:
                    polystr += "LT\n";
                    break;
                case Bernstein.LE:
                    polystr += "LE\n";
                    break;
            }
            polystr += "VALUE " + coef_suffix + Double.toString(rhs_val) + "\n";
            return polystr;
        } else {
            throw new UnsupportedOperationException("Unsupported expression type");
        }
    }

    public int testXADDImplication(HashSet<Integer> assump_dec_ids, int dec_id, int rec_depth, XADD lxadd ) {
        if (assump_dec_ids.size() == 0) {
            return  PolySolver.UNKNOWN;
        }

        // get variables
        ArrayList<String> vars = lxadd.getContinuousVarList();

        String teststr = "VAR " + Integer.toString(vars.size()) + "\n";
        teststr += "CONJUNCTION 1\n";
        teststr += "ASSUMP 0 " + Integer.toString(assump_dec_ids.size()) + "\n";
        teststr += "\n######\n";
        int k = 0;
        for (Integer id : assump_dec_ids) {
            String coeff_suffix = "A0_" + Integer.toString(k) + " ";
            teststr += getDecIdString(id, coeff_suffix, vars, lxadd, true);
            teststr += "\n######\n";
            k++;
        }
        //System.out.println("DEC_ID: " + dec_id);
        teststr += getDecIdString(dec_id, "G0 ", vars, lxadd, false);

        // get local bounds
        String bounds = "";
        int i=0;
        for (String v : vars) {
            bounds += "BOUND x" + Integer.toString(i) + " [" +
                    lxadd.lowerBounds[ lxadd._cvar2ID.get(v) ] + "," +
                    lxadd.upperBounds[ lxadd._cvar2ID.get(v) ] + "]\n";
            i++;
        }
        teststr += "\n" + bounds;

        // print teststr
        //log.info(teststr);

        // solve
        bernstein.Bernstein bern = new bernstein.Bernstein();
        String truth_val = bern.solveAssumeGuaranteeConstraints(teststr, rec_depth);
        //log.info("Bernstein Result: " + truth_val);
        if (truth_val.equals("TRUE for the constraint system \n")) {
            return PolySolver.TRUE;
        } else if (truth_val.startsWith("F")) {
            return PolySolver.FALSE;
        } else {
            log.info("Unknown result");
        }
        return PolySolver.UNKNOWN; // default to returning false if unknown
    }


    public static void main(String[] args) {
        System.out.println("PolySolver test");

        // load a sample XADD from file
        if (args.length != 1) {
          //log.severe(Integer.toString(args.length));
          System.out.println("Usage Error: PolySolver XADDFilename");
          return;
        }
        //String filename = "/home/haroldsoh/GoogleDrive/Development/MOMDP/xadd-inference-1/src/cmomdp/domain/results/si_2a.cmdp.V_3-000.xadd";
        String filename = args[0];
        XADD myxadd = new XADD();
        myxadd.importXADDFromFile(filename);
        log.info(myxadd.toString());

        // Get list of variables
        ArrayList<String> varlist = myxadd.getContinuousVarList();
        java.util.HashMap<String,Integer> vars = new java.util.HashMap<String,Integer>();
        for (String v : varlist) {
            log.info(v);
            vars.put(v, 0);
        }

        PolySolver ps = new PolySolver();

        int maxk = 3;
        int k = 0;
        int rec_depth=5;
        for (XADD.Decision dec : myxadd._alOrder) {
            if (dec instanceof XADD.ExprDec) {

                // need to transform this decision variable into
                final long startTime = System.currentTimeMillis();
                int test_value = ps.testXADDExprDec((XADD.ExprDec) dec, rec_depth, myxadd);
                final long endTime = System.currentTimeMillis();
                log.info("Total execution time: " + (endTime - startTime) + "ms");
                log.info(dec.toString());
                log.info("Test Value: " + Integer.toString(test_value));

                // break for now because we only want to do one
                k++;
                if (k > maxk) break;
            }
        }

    }


}
