package camdp;

import graph.Graph;

import java.util.*;

import util.IntTriple;
import xadd.XADD;
import xadd.XADD.*;

public class ComputeQFunction {

	public XADD _xadd   = null;
	public CAMDP _camdp = null;

	public ComputeQFunction(XADD context ,CAMDP camdp)
	{
		_xadd  = context;
		_camdp = camdp;
	}
	
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);
	
	/**
	 * Regress a DD through an action
	 **/
	public int regress(int vfun, CAction a) {
      
		XADDNode n = _xadd.getNode(vfun);
		
		// What state variables in vfun do we have to regress?  
		// Note: no action params in vfun.
		HashSet<String> state_vars_in_vfun  = n.collectVars();
		
		// Prime the value function
		int q = _xadd.substitute(vfun, _camdp._hmPrimeSubs); 

		// Regress continuous variables first in order given in 
		for (String var : _camdp._alContSVars) {
			if (!_camdp._hsContSVars.contains(var))
				continue; // Not regressing boolean variables yet, skip

			// Get cpf for continuous var'
			String var_prime = var + "'";
			int var_id = _xadd.getVarIndex( _xadd.new BoolDec(var_prime), false);
			Integer dd_conditional_sub = a._hmVar2DD.get(var_prime);
	
			// Check cache
			_contRegrKey.set(var_id, dd_conditional_sub, q);
			Integer result = null;
			if ((result = _camdp._hmContRegrCache.get(_contRegrKey)) != null) {
				q = result;
				continue;
			}
			
			// Perform regression via delta function substitution
			q = _xadd.reduceProcessXADDLeaf(dd_conditional_sub, 
					_xadd.new DeltaFunctionSubstitution(var_prime, q), true);
			
			// Cache result
			_camdp._hmContRegrCache.put(new IntTriple(_contRegrKey), result);
		}
		
		// Regress boolean variables second
		for (String var : state_vars_in_vfun) {
			if (_camdp._hsContSVars.contains(var))
				continue; // Continuous variables already regressed, skip
		
			// Get cpf for boolean var'
			String var_prime = var + "'";
			int var_id = _xadd.getVarIndex( _xadd.new BoolDec(var_prime), false);
			Integer dd_cpf = a._hmVar2DD.get(var_prime);
			
			_camdp._logStream.println("- Summing out: " + var_prime + "/" + var_id + " in\n" + _xadd.getString(dd_cpf));
			q = _xadd.apply(q, dd_cpf, XADD.PROD);
			
			// Following is a safer way to marginalize (instead of using opOut
			// based on apply) in the event that two branches of a boolean variable 
			// had equal probability and were collapsed.
			int restrict_high = _xadd.opOut(q, var_id, XADD.RESTRICT_HIGH);
			int restrict_low  = _xadd.opOut(q, var_id, XADD.RESTRICT_LOW);
			q = _xadd.apply(restrict_high, restrict_low, XADD.SUM);
		}
		
		// TODO: Policy maintenance: currently unfinished in this version
		// - if no action variables, can just annotate each Q-function with action
		// - if action variables then need to maintain action name along with
		//   the substitutions made at the leaves (which can occur recursively for
		//   multivariable problems)
		// if (_camdp.MAINTAIN_POLICY) { 
		//      ... 
		// }
			
		// Multiply in discount and add reward
    	q = _xadd.apply(a._reward,  
				_xadd.scalarOp(q, _camdp._bdDiscount.doubleValue(), XADD.PROD), 
				XADD.SUM);	

    	// Ensure Q-function is properly constrained and minimal (e.g., subject to constraints)
		for (Integer constraint : _camdp._alConstraints)
			q = _xadd.apply(q, constraint, XADD.PROD);
		if (_camdp._alConstraints.size() > 0)
			q = _xadd.reduceLP(q, _camdp._alContAllVars);
		
		// Continuous action parameter maximization
		for (int i=0; i < a._actionParam.size(); i++) {
			String var = a._actionParam.get(i);
			double lb  = a._contBounds.get(i*2);
			double ub  = a._contBounds.get(i*2+1);
			q = maxOutVar(q, var, lb, ub);
			
			// Can be computational expensive (max-out) so flush caches if needed
			_camdp.flushCaches(Arrays.asList(q) /* additional node to save */);
		}

		return q;
	}
	
	public int maxOutVar(int ixadd, String var, double lb, double ub) {
		XADDLeafMax max = _xadd.new XADDLeafMax(var, lb, ub);
		ixadd  = _xadd.reduceProcessXADDLeaf(ixadd, max, false);
		return max._runningMax;
	}
}
