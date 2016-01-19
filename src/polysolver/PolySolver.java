package polysolver;

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

        public String toBernsteinString(ArrayList<String> varorder) {
            if (varorder.size() == 0) {
                return "";
            }
            String varstr = "COEF (";
            varstr += Integer.toString(this.vars.get(varorder.get(0)));
            for (int i=1; i<varorder.size(); i++) {
                varstr += "," + Integer.toString(this.vars.get(varorder.get(i)));
            }
            varstr += ") " + Double.toString(this.coeff);
            return varstr;
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


    public boolean testXADDExprDec(XADD.ExprDec dec, int rec_depth, XADD lxadd) {

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

        // get constraint relationship
        int propop = Bernstein.GE;


        // solve equation
        bernstein.Bernstein bern = new bernstein.Bernstein();
        bern.solveSimpleConstraint(polystr, propop, rhs_val, rec_depth);


        return true;
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

        int maxk = 2;
        int k = 0;
        int rec_depth=5;
        for (XADD.Decision dec : myxadd._alOrder) {
            log.info(dec.toString());
            if (dec instanceof XADD.ExprDec) {
                log.info("Is a decision expression.");

                // need to transform this decision variable into
                boolean test_value = ps.testXADDExprDec((XADD.ExprDec) dec, rec_depth, myxadd);
                log.info("Test Value: " + Boolean.toString(test_value));
                // break for now because we only want to do one
                k++;
                if (k > maxk) break;
            }

        }

    }


}
