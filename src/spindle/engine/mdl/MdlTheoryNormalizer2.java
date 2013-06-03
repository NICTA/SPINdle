/**
 * SPINdle (version 2.2.2)
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
package spindle.engine.mdl;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.Superiority;
import spindle.engine.TheoryNormalizerException;
import spindle.sys.AppFeatureConst;
import spindle.sys.message.ErrorMessage;

/**
 * MDL Theory Normalizer (version 2).
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
 * @since version 2.2.1
 * @version Last modified 2012.10.17
 * @version 2012.08.20
 */
public class MdlTheoryNormalizer2 extends MdlTheoryNormalizer {

	public MdlTheoryNormalizer2() {
		super();
	}

	/**
	 * transform the theory to regular form and defeasible rules with multiple heads to single headed rules.
	 */
	@Override
	protected void transformTheoryToRegularFormImpl() throws TheoryNormalizerException {
		if (AppFeatureConst.isVerifyConflictRules) verifyConflictRules();

		super.transformTheoryToRegularFormImpl();
	}

	protected void verifyConflictRules() throws TheoryNormalizerException {
		List<Superiority> superiorities = theory.getAllSuperiority();
		for (Superiority superiority : superiorities) {
			String superiorRuleId = superiority.getSuperior();
			String inferiorRuleId = superiority.getInferior();

			Rule superiorRule = factsAndRules.get(superiorRuleId);
			Rule inferiorRule = factsAndRules.get(inferiorRuleId);

			if (null == superiorRule)
				throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_SUPERIOR_RULE_NOT_FOUND_IN_THEORY,
						new Object[] { superiorRuleId });
			if (null == inferiorRule)
				throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_INFERIOR_RULE_NOT_FOUND_IN_THEORY,
						new Object[] { inferiorRuleId });

			boolean isConflictRule = isConflictRules(superiorRule, inferiorRule);
			if (!isConflictRule)
				throw new TheoryNormalizerException(getClass(), ErrorMessage.SUPERIORITY_UNCONFLICTING_RULES, new Object[] {
						superiorRuleId, inferiorRuleId });
		}
	}

	protected boolean isConflictRules(Rule r1, Rule r2) throws TheoryNormalizerException {
		logMessage(Level.FINER, 1, "verify conflict rules", r1.getLabel(), r2.getLabel());
		boolean isConflictRule = false;
		List<Literal> r1Heads = r1.getHeadLiterals();
		List<Literal> r2Heads = r2.getHeadLiterals();
		for (int j = 0; !isConflictRule && j < r1Heads.size(); j++) {
			Literal r1Head = r1Heads.get(j);
			Set<Literal> conflictLiterals = getConflictLiterals(r1Head);
			for (int i = 0; !isConflictRule && i < r2Heads.size(); i++) {
				Literal r2Head = r2Heads.get(i);
				if (conflictLiterals.contains(r2Head)) isConflictRule = true;
				logMessage(Level.FINEST, 2, "verify conflict literals", r1Head, r2Head, "isConflictRule=" + isConflictRule);
			}
		}
		return isConflictRule;
	}

	protected Set<Literal> getConflictLiterals(final Literal literal) {
		return theory.getConflictLiterals(literal);
	}

}
