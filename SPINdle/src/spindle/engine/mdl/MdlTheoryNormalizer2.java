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
package spindle.engine.mdl;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.Superiority;
import spindle.engine.TheoryNormalizerException;
import spindle.sys.message.ErrorMessage;

public class MdlTheoryNormalizer2 extends MdlTheoryNormalizer {

	public MdlTheoryNormalizer2() {
		super();
	}

	/**
	 * transform the theory to regular form and normalize the defeasible rule to single literal head
	 */
	@Override
	protected void transformTheoryToRegularFormImpl() throws TheoryNormalizerException {
		List<Superiority> superiorities = theory.getAllSuperiority();
		for (Superiority superiority : superiorities) {
			String superiorRuleId = superiority.getSuperior();
			String inferiorRuleId = superiority.getInferior();

			Rule superiorRule = factsAndRules.get(superiorRuleId);
			Rule inferiorRule = factsAndRules.get(inferiorRuleId);

			if (null == superiorRule) throw new TheoryNormalizerException(getClass(),
					ErrorMessage.THEORY_SUPERIOR_RULE_NOT_FOUND_IN_THEORY, new Object[] { superiorRuleId });
			if (null == inferiorRule) throw new TheoryNormalizerException(getClass(),
					ErrorMessage.THEORY_INFERIOR_RULE_NOT_FOUND_IN_THEORY, new Object[] { inferiorRuleId });

			boolean isConflictRule = false;
			List<Literal> superiorRuleHead = superiorRule.getHeadLiterals();
			List<Literal> inferiorRuleHead = inferiorRule.getHeadLiterals();
			for (int j = 0; j < superiorRuleHead.size() && !isConflictRule; j++) {
				Set<Literal> conflictLiterals = new TreeSet<Literal>(
						theory.getConflictLiterals(superiorRuleHead.get(j)));
				for (int i = 0; i < inferiorRuleHead.size() && !isConflictRule; i++) {
					if (conflictLiterals.contains(inferiorRuleHead.get(i))) isConflictRule = true;
				}
			}
			if (!isConflictRule) { throw new TheoryNormalizerException(getClass(),
					ErrorMessage.SUPERIORITY_UNCONFLICTING_RULES, new Object[] { superiorRuleId, inferiorRuleId }); }

		}
		super.transformTheoryToRegularFormImpl();
	}

}
