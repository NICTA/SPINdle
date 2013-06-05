/**
 * SPINdle (version 2.2.2)
 * Copyright (C) 2009-2013 NICTA Ltd.
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
package spindle.engine.sdl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import com.app.utils.Utilities;

import spindle.core.dom.DomUtilities;
import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleException;
import spindle.core.dom.RuleType;
import spindle.engine.TheoryNormalizer;
import spindle.engine.TheoryNormalizerException;
import spindle.sys.AppConst;
import spindle.sys.message.ErrorMessage;

/**
 * SDL Theory Normalizer.
 * <p>
 * Provides methods that can be used to transform a defeasible theory into an equivalent theory without superiority
 * relation or defeater using the algorithms described in:
 * <ul>
 * <li>G. Antoniou, D. Billington, G. Governatori and M.J. Maher (2001) Representation Results for Defeasible Logic,
 * <i>ACM Transactions on Computational Logic</i>, Vol. 2 (2), pp. 255-287</li>
 * </ul>
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public class SdlTheoryNormalizer extends TheoryNormalizer {

	public SdlTheoryNormalizer() {
		super();
	}

	/**
	 * expand defeasible rule with head more than one literal, for example:
	 * 
	 * <pre>
	 *     a =&gt; b,c
	 * </pre>
	 * 
	 * will becomes
	 * 
	 * <pre>
	 *     a =&gt; b 
	 * a, -b =&gt; c
	 * </pre>
	 * 
	 * @param rule
	 *            original defeasible rule
	 * @return list of expanded rules
	 * @throws RuleException
	 */
	protected List<Rule> expandDefeasibleRule(final Rule rule) throws RuleException {
		if (rule.getRuleType() != RuleType.DEFEASIBLE) return null;

		String originalRuleLabel=rule.getOriginalLabel();
		List<Rule> newRules = new ArrayList<Rule>();
		List<Literal> headLiterals = rule.getHeadLiterals();
		try {
			if (headLiterals.size() < 2) {
				Rule newRule = rule.clone();
				newRules.add(newRule);
			} else {
				List<Literal> bodyLiterals = rule.getBodyLiterals();
				Literal lastLiteral = null;
				int count = 1;
				String newRuleLabel = null;
				for (Literal literal : headLiterals) {
					if (null == lastLiteral) {
					} else {
						Literal lc = lastLiteral.getComplementClone();
						bodyLiterals.add(lc);
					}

					newRuleLabel = rule.getLabel() + "_" + count;
					Rule newRule = DomUtilities.getRule(newRuleLabel, RuleType.DEFEASIBLE);
newRule.setOriginalLabel(originalRuleLabel);
					addBodyLiterals(newRule, bodyLiterals, "");
					newRule.addHeadLiteral(literal);

					newRules.add(newRule);
					lastLiteral = literal;
					count++;
				}
			}
			return newRules;
		} catch (Exception e) {
			throw new RuleException("exception throw while expanding rule [" + rule.getLabel() + "]", e);
		}
	}

	/**
	 * transform the theory to regular form and normalize the defeasible rule to single literal head
	 */
	@Override
	protected void transformTheoryToRegularFormImpl() throws TheoryNormalizerException {		
		List<Rule> rulesToAdd = new ArrayList<Rule>();
		Set<String> rulesToDelete = new TreeSet<String>();

		List<Literal> headLiterals = null;
		List<Literal> origHeadLiterals = null;
		Rule newRule1 = null;
		Rule newRule2 = null;
		Rule newRule3 = null;
		List<Rule> newRules = null;
		Map<String, List<Rule>> oldNewRuleMapping = new Hashtable<String, List<Rule>>();

		try {
			for (Rule rule : factsAndRules.values()) {
				logMessage(Level.FINER, 1, "transforming ", rule.getRuleType().getLabel(), " [", rule.getLabel(), "]");

				String originalRuleLabel=rule.getOriginalLabel();
				newRule1 = null;
				newRule2 = null;
				newRule3 = null;

				switch (rule.getRuleType()) {
				case STRICT:
					origHeadLiterals = rule.getHeadLiterals();
					headLiterals = rule.getHeadLiterals();

					String profixStr = (factsAndRules.containsKey(rule.getLabel() + TRANSFORM_POSTFIX)) ? (Utilities.getRandomString(5) + TRANSFORM_POSTFIX)
							: TRANSFORM_POSTFIX;

					newRule1 = DomUtilities.getRule(rule.getLabel() + profixStr, RuleType.STRICT);
newRule1.setOriginalLabel(originalRuleLabel);					
					addBodyLiterals(newRule1, rule.getBodyLiterals(), TRANSFORM_POSTFIX);
					addHeadLiterals(newRule1, headLiterals, TRANSFORM_POSTFIX);

					newRule2 = DomUtilities.getRule(rule.getLabel() + profixStr + TRANSFORM_POSTFIX2, RuleType.STRICT);
newRule2.setOriginalLabel(originalRuleLabel);
					addBodyLiterals(newRule2, headLiterals, TRANSFORM_POSTFIX);
					addHeadLiterals(newRule2, origHeadLiterals, "");

					// change the strict rule to defeasible rule, and
					// add the two new strict rules to the theory
					newRule3 = rule.clone();
newRule3.setOriginalLabel(originalRuleLabel);
					newRule3.setRuleType(RuleType.DEFEASIBLE);

					rulesToAdd.add(newRule1);
					rulesToAdd.add(newRule2);
					rulesToAdd.add(newRule3);
					rulesToDelete.add(rule.getLabel());
					break;
				case FACT:
					origHeadLiterals = rule.getHeadLiterals();
					headLiterals = rule.getHeadLiterals();

					String newFactStrictRuleLabel = theory.getUniqueRuleLabel(FACT_RULE_TRANSFORM_PREFIX);

					newRule1 = DomUtilities.getRule(newFactStrictRuleLabel, RuleType.STRICT);
newRule1.setOriginalLabel(originalRuleLabel);
					addHeadLiterals(newRule1, headLiterals, TRANSFORM_POSTFIX);

					newRule2 = DomUtilities.getRule(newFactStrictRuleLabel + TRANSFORM_POSTFIX, RuleType.STRICT);
newRule2.setOriginalLabel(originalRuleLabel);
					addBodyLiterals(newRule2, headLiterals, TRANSFORM_POSTFIX);
					addHeadLiterals(newRule2, origHeadLiterals, "");

					// delete the fact, and
					// add the two new strict rules to the theory
					rulesToDelete.add(rule.getLabel());
					rulesToAdd.add(newRule1);
					rulesToAdd.add(newRule2);
					break;
				case DEFEASIBLE:
					// expand the defeasible rule if number of head literal is
					// greater than 1
					// do nothing otherwise
					headLiterals = rule.getHeadLiterals();
					if (headLiterals.size() > 1) {
						logMessage(Level.FINER, 1, "expending ", rule.getRuleType().getLabel(), " [", rule.getLabel(), "]");
						newRules = expandDefeasibleRule(rule);

						// delete the old rule and add the expanded rules to the theory
						rulesToDelete.add(rule.getLabel());
						rulesToAdd.addAll(newRules);
						oldNewRuleMapping.put(rule.getLabel(), newRules);
					} else {
						logMessage(Level.FINER, 2, "no transform performed to ", rule.getRuleType().getLabel(), " [", rule.getLabel(), "]");
					}
					break;
				default:
					logMessage(Level.FINER, 2, "no transform performed to ", rule.getRuleType().getLabel(), " [", rule.getLabel(), "]");
				}
			}

			theory.updateTheory(rulesToAdd, rulesToDelete, oldNewRuleMapping);
		} catch (Exception e) {
			throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_UPDATE_ERROR, e);
		} finally {
			if (!AppConst.isDeploy) {
				logMessage(Level.INFO, 0, "=== transformTheoryToRegularFormImpl - start ===");
				logMessage(Level.INFO, 0, null, theory);
				logMessage(Level.INFO, 0, "=== transformTheoryToRegularFormImpl -  end  ===");
			}
		}
	}

}
