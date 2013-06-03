/**
 * SPINdle (version 2.2.0)
 * Copyright (C) 2009-2012 NICTA Ltd.
 *
 * This file is part of SPINdle project.
 * 
 * SPINdle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SPINdle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SPINdle.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory 
 */
package spindle.engine.tdl;


import spindle.core.dom.TheoryType;
import spindle.engine.TheoryNormalizerException;
import spindle.sys.AppConst;
import spindle.sys.message.ErrorMessage;

/**
 * TDL Theory Normalizer.
 * <p>
 * Provides methods that can be used to transform a defeasible theory into an equivalent theory without superiority
 * relation or defeater using the algorithms described in:
 * <ul>
 * <li>G. Antoniou, D. Billington, G. Governatori and M.J. Maher (2001) Representation Results for Defeasible Logic,
 * <i>ACM Transactions on Computational Logic</i>, Vol. 2 (2), pp. 255-287</li>
 * </ul>
 * </p>
 * <p>
 * Rule/literal modal conversions and conflict resolutions are based on description presented in:
 * <ul>
 * <li>G. Governatori and A. Rotolo (2008) BIO Logical Agents: Norms, Beliefs, Intentions in Defeasible Logic,
 * <i>Journal of Autonomous Agents and Multi Agent Systems</i>, Vol. 17 (1), pp. 36--69</li>
 * </ul>
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public class TdlTheoryNormalizer2 extends spindle.engine.mdl.MdlTheoryNormalizer2 {

	public TdlTheoryNormalizer2() throws TheoryNormalizerException {
		super();
		if (AppConst.isDeploy)
			throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_NORMALIZER_NOT_SUPPORTED,
					new Object[] { TheoryType.TDL });
	}

	/**
	 * transform the theory to regular form and defeasible rules with multiple heads to single headed rules.
	 */
	@Override
	protected void transformTheoryToRegularFormImpl() throws TheoryNormalizerException {
		// if (AppConst.isVerifyConflictRules) {
		// List<Superiority> superiorities = theory.getAllSuperiority();
		// for (Superiority superiority : superiorities) {
		// String superiorRuleId = superiority.getSuperior();
		// String inferiorRuleId = superiority.getInferior();
		//
		// Rule superiorRule = factsAndRules.get(superiorRuleId);
		// Rule inferiorRule = factsAndRules.get(inferiorRuleId);
		//
		// if (null == superiorRule) throw new TheoryNormalizerException(getClass(),
		// ErrorMessage.THEORY_SUPERIOR_RULE_NOT_FOUND_IN_THEORY, new Object[] { superiorRuleId });
		// if (null == inferiorRule) throw new TheoryNormalizerException(getClass(),
		// ErrorMessage.THEORY_INFERIOR_RULE_NOT_FOUND_IN_THEORY, new Object[] { inferiorRuleId });
		//
		// boolean isConflictRule = false;
		// List<Literal> superiorRuleHead = superiorRule.getHeadLiterals();
		// List<Literal> inferiorRuleHead = inferiorRule.getHeadLiterals();
		// for (int j = 0; j < superiorRuleHead.size() && !isConflictRule; j++) {
		// Set<Literal> conflictLiterals = new TreeSet<Literal>(theory.getConflictLiterals(superiorRuleHead
		// .get(j)));
		// for (int i = 0; i < inferiorRuleHead.size() && !isConflictRule; i++) {
		// if (conflictLiterals.contains(inferiorRuleHead.get(i))) isConflictRule = true;
		// }
		// }
		// if (!isConflictRule) throw new TheoryNormalizerException(getClass(),
		// ErrorMessage.SUPERIORITY_UNCONFLICTING_RULES, new Object[] { superiorRuleId, inferiorRuleId });
		// }
		// }

		super.transformTheoryToRegularFormImpl();
	}
}
