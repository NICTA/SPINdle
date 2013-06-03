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
import spindle.core.dom.Mode;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleException;
import spindle.core.dom.RuleType;
import spindle.engine.TheoryNormalizer;
import spindle.engine.TheoryNormalizerException;
import spindle.sys.AppConst;
import spindle.sys.message.ErrorMessage;

/**
 * MDL Theory Normalizer.
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
public class MdlTheoryNormalizer extends TheoryNormalizer {
	Map<String, Set<String>> ruleModesConversionsRules = null;

	public MdlTheoryNormalizer() {
		super();
	}

	/**
	 * expand defeasible rule with head more than one literal
	 * 
	 * <pre>
	 * e.g. a =&gt; b,c 
	 * will becomes 
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
				Mode ruleMode = rule.getMode();
				for (Literal literal : headLiterals) {
					if (null == lastLiteral) {
					} else {
						Literal lc = lastLiteral.getComplementClone();
						bodyLiterals.add(lc);
					}

					String newRuleLabel = rule.getLabel() + "_" + count;
					Rule newRule = DomUtilities.getRule(newRuleLabel, RuleType.DEFEASIBLE);
newRule.setOriginalLabel(originalRuleLabel);
					newRule.setMode(ruleMode);
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

	@Override
	protected void transformTheoryToRegularFormImpl() throws TheoryNormalizerException {
		ruleModesConversionsRules = theory.getAllModeConversionRules();

		List<Rule> rulesToAdd = new ArrayList<Rule>();
		Set<String> rulesToDelete = new TreeSet<String>();
		Map<String, List<Rule>> oldNewRuleMapping = new Hashtable<String, List<Rule>>();

		List<Literal> headLiterals = null;
		List<Literal> origHeadLiterals = null;
		Rule newRule1 = null;
		Rule newRule2 = null;
		Rule newRule3 = null;
		List<Rule> newRules = null;
		try {
			for (Rule rule : factsAndRules.values()) {
				logMessage(Level.FINER, 1, "transforming ", rule.getRuleType().getLabel(), " [", rule.getLabel(), "]");
				newRule1 = null;
				newRule2 = null;
				newRule3 = null;
				headLiterals = null;

				String originalRuleLabel=rule.getOriginalLabel();
				Mode ruleMode = rule.getMode();
				if ("".equals(ruleMode.getName())) ruleMode = null;

				switch (rule.getRuleType()) {
				case STRICT:
					origHeadLiterals = rule.getHeadLiterals();
					headLiterals = rule.getHeadLiterals();

					String profixStr = (factsAndRules.containsKey(rule.getLabel() + TRANSFORM_POSTFIX)) ? (Utilities
							.getRandomString(AppConst.LOG_FILE_ID_LENGTH) + TRANSFORM_POSTFIX) : TRANSFORM_POSTFIX;

					newRule1 = DomUtilities.getRule(rule.getLabel() + profixStr, RuleType.STRICT);
newRule1.setOriginalLabel(originalRuleLabel);
					if (null != ruleMode) newRule1.setMode(ruleMode);
					addBodyLiterals(newRule1, rule.getBodyLiterals(), TRANSFORM_POSTFIX);
					addHeadLiterals(newRule1, headLiterals, TRANSFORM_POSTFIX);

					newRule2 = DomUtilities.getRule(rule.getLabel() + profixStr + TRANSFORM_POSTFIX2, RuleType.STRICT);
newRule2.setOriginalLabel(originalRuleLabel)	;				
					if (null != ruleMode) newRule2.setMode(ruleMode);
					addBodyLiterals(newRule2, headLiterals, TRANSFORM_POSTFIX);
					addHeadLiterals(newRule2, origHeadLiterals, "");
					if (null != ruleMode) newRule2.setHeadLiteralMode(ruleMode);

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
					if (null != ruleMode) newRule1.setMode(ruleMode);
					addHeadLiterals(newRule1, headLiterals, TRANSFORM_POSTFIX);

					newRule2 = DomUtilities.getRule(newFactStrictRuleLabel + TRANSFORM_POSTFIX, RuleType.STRICT);
newRule2.setOriginalLabel(originalRuleLabel)	;				
					if (null != ruleMode) newRule2.setMode(ruleMode);
					addBodyLiterals(newRule2, headLiterals, TRANSFORM_POSTFIX);
					addHeadLiterals(newRule2, origHeadLiterals, "");
					if (null != ruleMode) newRule2.setHeadLiteralMode(ruleMode);

					// delete the fact, and
					// add the two new strict rules to the theory
					rulesToDelete.add(rule.getLabel());
					rulesToAdd.add(newRule1);
					rulesToAdd.add(newRule2);
					break;
				case DEFEASIBLE:
					// - modification start
					List<Rule> expandedModeRules = convertRuleMode(rule);
					if (!AppConst.isDeploy) {
						for (Rule r : expandedModeRules) {
							logMessage(Level.FINEST, 2, "[", rule.getLabel(), "] expandRuleWithModeConversion: ", r);
						}
					}
					newRules = new ArrayList<Rule>();
					// expand the defeasible rule if number of head literal is greater than 1
					// do nothing otherwise
					for (Rule expendedRule : expandedModeRules) {
						headLiterals = expendedRule.getHeadLiterals();
						if (headLiterals.size() > 1) {
							logMessage(Level.FINEST, 1, "expending ", expendedRule.getRuleType().getLabel(), " [",
									expendedRule.getLabel(), "]");
							newRules.addAll(expandDefeasibleRule(expendedRule));
						} else {
							logMessage(Level.FINEST, 1, "no transform performed to ", rule.getRuleType().getLabel(),
									" [", rule.getLabel(), "]");
							newRules.add(expendedRule);
						}
					}
					// - modification end

					// delete the old rule and
					// add the expanded rules to the theory
					rulesToDelete.add(rule.getLabel());
					rulesToAdd.addAll(newRules);
					if (newRules.size() > 1) oldNewRuleMapping.put(rule.getLabel(), newRules);
					break;
				default:
					logMessage(Level.FINEST, 2, "no transform performed to ", rule.getRuleType().getLabel(), " [",
							rule.getLabel(), "]");
				}
			}
			theory.updateTheory(rulesToAdd, rulesToDelete, oldNewRuleMapping);
		} catch (Exception e) {
			throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_UPDATE_ERROR,
					"Theory update exception!", e);
		} finally {
			if (!AppConst.isDeploy) {
				logMessage(Level.INFO, 0, "=== expandRuleWithModeConversion - start ===");
				logMessage(Level.INFO, 0, theory.toString());
				logMessage(Level.INFO, 0, "=== expandRuleWithModeConversion -  end  ===");
			}
		}
	}

	private List<Rule> convertRuleMode(Rule rule) throws TheoryNormalizerException {
		List<Rule> expandedRules = new ArrayList<Rule>();
		Mode ruleMode = rule.getMode();
		String ruleModeName = ruleMode.getName();
		if ("".equals(ruleModeName) && rule.getModeUsedInHead().size() > 1) {
			expandedRules.add(rule);
			return expandedRules;
		}
		Rule modifiedRule = null;
		if ("".equals(ruleModeName)) {
			modifiedRule = rule;
		} else {
			try {
				modifiedRule = DomUtilities.getRule(rule.getLabel(), rule.getRuleType());
				addBodyLiterals(modifiedRule, rule.getBodyLiterals(), "");
				for (Literal literal : rule.getHeadLiterals()) {
					Literal l = literal.clone();
					l.setMode(ruleMode);
					modifiedRule.addHeadLiteral(l);
				}
			} catch (RuleException e) {
				throw new TheoryNormalizerException(getClass(), "exception throw while converting rule mode", e);
			}
		}
		expandedRules.add(modifiedRule);
		List<Mode> modeUsedInBody = modifiedRule.getModeUsedInBody();

		if (!AppConst.isDeploy) {
			logMessage(Level.INFO, 0, "convertRuleMode.rule=", modifiedRule);
			logMessage(Level.INFO, 1, "convertRuleMode.rule.getBodyLiterals().size()="
					+ modifiedRule.getBodyLiterals().size());
			logMessage(Level.INFO, 1, "convertRuleMode.modeUsedInBody.size()=" + modeUsedInBody.size());
			logMessage(Level.INFO, 1, "convertRuleMode.modeUsedInBody=", modeUsedInBody);
		}
		
		if (modifiedRule.getBodyLiterals().size() == 0 || modeUsedInBody.size() == 0) {
			//ruleModeName= modifiedRule.getHeadLiterals().get(0).getMode().getName();
//			ruleModeName =headLiteralMode.getName();
//			Set<String> conversionRule = theory.getModeConversionRules(ruleModeName);
			Mode headLiteralMode=modifiedRule.getHeadLiterals().get(0).getMode();
			Set<String> conversionRule = theory.getModeConversionRules(headLiteralMode.getName());
			if (!AppConst.isDeploy) logMessage(Level.INFO, 1, "convertRuleMode.1, ruleModeName=", ruleModeName,
					",conversionRule =", conversionRule);
			if (null != conversionRule) {
				for (String cm : conversionRule) {
					try {
						Rule newRule = modifiedRule.cloneWithModeChange(new Mode(cm, headLiteralMode.isNegation()));
						newRule.setLabel(modifiedRule.getLabel() + "_[" + cm + "]");
						expandedRules.add(newRule);
						logMessage(Level.FINEST, 3, "newRule=", newRule);
					} catch (Exception e) {
						logMessage(Level.SEVERE, 0, "[ERROR] ", e.getMessage());
						throw new TheoryNormalizerException(getClass(), "exception throw while converting rule mode", e);
					}
				}
			}
		} else if (modeUsedInBody.size() == 1) {
			String bodyMode = (modeUsedInBody.size() == 0) ? "" : modeUsedInBody.get(0).getName();
		//	ruleModeName = modifiedRule.getHeadLiterals().get(0).getMode().getName();
			//Set<String> conversionRule = theory.getModeConversionRules(ruleModeName);
			Mode headLiteralMode=modifiedRule.getHeadLiterals().get(0).getMode();
			Set<String> conversionRule=theory.getModeConflictRules(headLiteralMode.getName());
			if (!AppConst.isDeploy) logMessage(Level.INFO, 1, "convertRuleMode.2, ruleModeName=", ruleModeName,
					",conversionRule =", conversionRule);
			if (null != conversionRule && conversionRule.contains(bodyMode)) {
				try {
					Rule newRule = modifiedRule.cloneWithModeChange(new Mode(bodyMode, headLiteralMode.isNegation()));
					newRule.setLabel(modifiedRule.getLabel() + "_[" + bodyMode + "]");
					expandedRules.add(newRule);
					logMessage(Level.FINEST, 3, "newRule=", newRule);
				} catch (Exception e) {
					logMessage(Level.SEVERE, 0, "[ERROR] " + e.getMessage());
					throw new TheoryNormalizerException(getClass(), "exception throw while converting rule mode", e);
				}
			}
		}
		return expandedRules;
	}
}
