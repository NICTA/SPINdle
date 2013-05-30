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
package spindle.tools.evaluator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.script.ScriptEngine;

import com.app.utils.ComparableEntry;

import spindle.core.MessageType;
import spindle.core.dom.AppConstants;
import spindle.core.dom.DomConst;
import spindle.core.dom.DomUtilities;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.io.ParserException;
import spindle.io.parser.DflTheoryParser2;
import spindle.sys.AppConst;
import spindle.sys.AppModuleBase;
import spindle.sys.AppModuleListener;
import spindle.sys.Conf;
import spindle.sys.NullValueException;
import spindle.sys.message.ErrorMessage;

/**
 * Literal variable evaluator.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.07.26
 * @since version 2.1.0
 */
public class LiteralVariablesEvaluator extends AppModuleBase {

	private static ScriptEngine evaluator = Conf.getScriptEngine();
	private AppConstants appConstants = AppConstants.getInstance(null);

	private Theory theory = null;

	Map<LiteralVariable, LiteralVariable> simplifiedLiteralVariables = null;
	Map<LiteralVariable, String> literalFunctionAnswers = null;
	Map<LiteralVariable,Literal>literalVariablesUpdate=null;
	Set<Rule> rulesToAdd = null;

	public Theory evaluateLiteralVariables(Theory theory) throws LiteralVariablesEvaluatorException {
		if (null == theory) throw new LiteralVariablesEvaluatorException(ErrorMessage.THEORY_NULL_THEORY);
		this.theory = theory;

		logMessage(Level.FINE, 0, "LiteralVariableEvaluator.evaluateLiteralVariables - start");

		simplifiedLiteralVariables = new TreeMap<LiteralVariable, LiteralVariable>();
		literalFunctionAnswers = new TreeMap<LiteralVariable, String>();
		literalVariablesUpdate=new TreeMap<LiteralVariable, Literal>();
		rulesToAdd = new TreeSet<Rule>();

		simplifyLiteralVariables();
		validateLiteralVariables();

		theory.clearLiteralVariables();

		simplifyTheoryBooleanFunctions(theory.getLiteralBooleanFunctions());

		if (theory.getLiteralBooleanFunctionsInRulesCount() > 0) {
			Map<LiteralVariable, LiteralVariable> theoryBooleanFunctionsInRules = new TreeMap<LiteralVariable, LiteralVariable>();
			for (LiteralVariable literalVariable : theory.getLiteralBooleanFunctionsInRules()) {
				theoryBooleanFunctionsInRules.put(literalVariable, literalVariable);
			}
			simplifyTheoryBooleanFunctions(theoryBooleanFunctionsInRules);
		}
		if (!AppConst.isDeploy) {
			for (java.util.Map.Entry<LiteralVariable, String> entry : literalFunctionAnswers.entrySet()) {
				logMessage(Level.INFO, 1, "* literalFunctionAnswers.entry:- ", entry);
			}
		}

		theory.clearLiteralBooleanFunctions();

		try {
			for (java.util.Map.Entry<LiteralVariable,Literal>entry:literalVariablesUpdate.entrySet()){
				LiteralVariable lv=entry.getKey();
				Literal l=entry.getValue();
				logMessage(Level.FINER, 1, "update literal variable to regular literal:", lv, " => ", l);
				this.theory.updateLiteralVariableInRule(lv,l);
			}
			for (Rule rule : rulesToAdd) {
				logMessage(Level.FINER, 1, "add new rule to theory:", rule);
				this.theory.addRule(rule);
			}
		} catch (TheoryException e) {
			throw new LiteralVariablesEvaluatorException(e);
		}

		logMessage(Level.FINE, 0, "LiteralVariableEvaluator.evaluateLiteralVariables - end");
		return this.theory;
	}

	/**
	 * Validate the correctness of the literal variables generated.
	 * 
	 * @throws LiteralVariablesEvaluatorException Signals that exception of some sort has occurred for a literal
	 *             variable.
	 */
	private void validateLiteralVariables() throws LiteralVariablesEvaluatorException {
		LiteralVariable lvName = null;
		try {
			for (java.util.Map.Entry<LiteralVariable, LiteralVariable> entry : simplifiedLiteralVariables.entrySet()) {
				lvName = entry.getKey();
				LiteralVariable lv = entry.getValue();
				// verify the correctness of a literal variable if it is an application constant
				if (appConstants.isAppConstant(lv)) appConstants.getAppConstantAsLiteralVariable(lv);
			}
		} catch (Exception e) {
			throw new LiteralVariablesEvaluatorException(lvName.toString() + " :- " + e.getMessage(), e);
		}
	}

	/**
	 * Simplify the literal variable values using application constants.
	 * 
	 * @throws LiteralVariablesEvaluatorException Signals that either a literal variable cannot be simplified using
	 *             application constants or an exception of some sort has occured.
	 */
	private void simplifyLiteralVariables() throws LiteralVariablesEvaluatorException {
		if (theory.getLiteralVariableCount() == 0) return;

		// simplify the literal variable values using application constants
		Map<LiteralVariable, ComparableEntry<LiteralVariable, Set<LiteralVariable>>> literalVariables = new TreeMap<LiteralVariable, ComparableEntry<LiteralVariable, Set<LiteralVariable>>>();
		for (java.util.Map.Entry<LiteralVariable, LiteralVariable> entry : theory.getLiteralVariables().entrySet()) {
			Set<LiteralVariable> lvEncountered = new TreeSet<LiteralVariable>();
			lvEncountered.add(entry.getValue());
			ComparableEntry<LiteralVariable, Set<LiteralVariable>> entryRecord = new ComparableEntry<LiteralVariable, Set<LiteralVariable>>(entry.getValue(), lvEncountered);
			literalVariables.put(entry.getKey(), entryRecord);
		}
		Map<LiteralVariable, LiteralVariable> updateSet = new TreeMap<LiteralVariable, LiteralVariable>();
		Set<LiteralVariable> removeSet = new TreeSet<LiteralVariable>();

		do {
			updateSet.clear();
			removeSet.clear();
			for (java.util.Map.Entry<LiteralVariable, ComparableEntry<LiteralVariable, Set<LiteralVariable>>> entry : literalVariables.entrySet()) {
				logMessage(Level.FINER, 1, "evaluating: ", entry);
				LiteralVariable lvValue = entry.getValue().getKey();
				if (null != isUserDefinedVariable(lvValue)) {
					logMessage(Level.FINEST, 1, AppConst.IDENTATOR + "==> contains user defined variable!");
					LiteralVariable lvValueC = lvValue.getComplementClone();
					if (simplifiedLiteralVariables.containsKey(lvValue)) {
						logMessage(Level.FINEST, 2, "==> in simplified literal variable set");
						updateSet.put(entry.getKey(), simplifiedLiteralVariables.get(lvValue));
					} else if (simplifiedLiteralVariables.containsKey(lvValueC)) {
						logMessage(Level.FINEST, 2, "==> NEGATION in simplified literal variable set");
						updateSet.put(entry.getKey(), simplifiedLiteralVariables.get(lvValueC).getComplementClone());
					}
				} else {
					logMessage(Level.FINER, 2, "==> contains NO user defined variable");
					removeSet.add(entry.getKey());
					simplifiedLiteralVariables.put(entry.getKey(), lvValue);
				}
			}

			for (java.util.Map.Entry<LiteralVariable, LiteralVariable> entry : updateSet.entrySet()) {
				ComparableEntry<LiteralVariable, Set<LiteralVariable>> lvEntry = literalVariables.get(entry.getKey());
				Set<LiteralVariable> historySet = lvEntry.getValue();
				if (historySet.contains(entry.getValue())) {
					historySet.add(entry.getKey());
					throw new LiteralVariablesEvaluatorException(ErrorMessage.LITERAL_VARIABLE_EVALUATOR_CYCLIC_VARIABLE_DEPENDENCIES,
							new Object[] { historySet.toString() });
				}

				historySet.add(entry.getValue());
				lvEntry.setKey(entry.getValue());

				literalVariables.remove(entry.getKey());
				literalVariables.put(entry.getKey(), lvEntry);
			}
			for (LiteralVariable literalVariable : removeSet) {
				literalVariables.remove(literalVariable);
			}
		} while (updateSet.size() > 0 || removeSet.size() > 0);

		if (!AppConst.isDeploy) {
			logMessage(Level.INFO, 1, "simplified variables - start");
			for (java.util.Map.Entry<LiteralVariable, LiteralVariable> entry : simplifiedLiteralVariables.entrySet()) {
				logMessage(Level.INFO, 2, "+-- " + entry.toString());
			}
			logMessage(Level.INFO, 1, "simplified variables - end");
		}
		if (literalVariables.size() > 0) {
			Object[] args = new Object[] { literalVariables.keySet() };
			throw new LiteralVariablesEvaluatorException(ErrorMessage.LITERAL_VARIABLE_EVALUATOR_THEORY_VARIABLE_UNRESOLVABLE, args);
		}
	}

	private void simplifyTheoryBooleanFunctions(Map<LiteralVariable, LiteralVariable> theoryBooleanFunctions) throws LiteralVariablesEvaluatorException {
		if (theoryBooleanFunctions.size() == 0) return;

		Set<LiteralVariable>literalVariablesToRemove=new TreeSet<LiteralVariable>();
		Map<LiteralVariable, ComparableEntry<LiteralVariable, Set<LiteralVariable>>> theoryBooleanFunctionsToEvaluate = new TreeMap<LiteralVariable, ComparableEntry<LiteralVariable, Set<LiteralVariable>>>();
		try {
			for (java.util.Map.Entry<LiteralVariable, LiteralVariable> entry : theoryBooleanFunctions.entrySet()) {
				String theoryBooleanFunctionStr = entry.getValue().getName();

				Set<LiteralVariable> userVariableSet = extractUserLiteralVariablesFromBooleanFunction(theoryBooleanFunctionStr);
				if (userVariableSet.size() == 0) {
					boolean ans = evaluateBooleanValue(theoryBooleanFunctionStr);
					literalFunctionAnswers.put(entry.getKey(), "" + ans);
					addBooleanConclusionGenerated(entry.getKey(), ans);
					literalVariablesToRemove.add(entry.getKey());
				} else {
					theoryBooleanFunctionsToEvaluate.put(entry.getKey(), new ComparableEntry<LiteralVariable, Set<LiteralVariable>>(entry.getValue(), userVariableSet));
				}
			}

			Set<LiteralVariable> updatedSet = new TreeSet<LiteralVariable>();
			do {
				updatedSet.clear();
				for (java.util.Map.Entry<LiteralVariable, ComparableEntry<LiteralVariable, Set<LiteralVariable>>> entry : theoryBooleanFunctionsToEvaluate.entrySet()) {
					Set<LiteralVariable> missingLiteralVariables = entry.getValue().getValue();
					Set<LiteralVariable> removeSet = new TreeSet<LiteralVariable>();
					for (LiteralVariable literalVariable : missingLiteralVariables) {
						if (literalFunctionAnswers.containsKey(literalVariable)) removeSet.add(literalVariable);
					}
					missingLiteralVariables.removeAll(removeSet);
					if (missingLiteralVariables.size() == 0) {
						boolean ans = evaluateBooleanValue(entry.getValue().getKey().getName());
						literalFunctionAnswers.put(entry.getKey(), "" + ans);
						addBooleanConclusionGenerated(entry.getKey(), ans);
						updatedSet.add(entry.getKey());
						literalVariablesToRemove.add(entry.getKey());
					}
				}
				for (LiteralVariable literalVariable : updatedSet) {
					theoryBooleanFunctionsToEvaluate.remove(literalVariable);
				}
			} while (updatedSet.size() > 0);			
		} catch (LiteralVariablesEvaluatorException e) {
			throw e;
		} catch (Exception e) {
			throw new LiteralVariablesEvaluatorException(e);
		}
	}

	private void addBooleanConclusionGenerated(LiteralVariable literalVariable, boolean ans) throws TheoryException {
		logMessage(Level.FINE, 1, "literal variable: ", literalVariable, ", ans=", ans);
		
		Literal headLiteral = DomUtilities.getLiteral(literalVariable);
		headLiteral.setPlaceHolder(true);
		
		literalVariablesUpdate.put(literalVariable, headLiteral);
	
		if (ans) {
			try {
				Rule newRule = DomUtilities.getRule(theory.getUniqueRuleLabel(), RuleType.FACT);
				newRule.addHeadLiteral(headLiteral);
				rulesToAdd.add(newRule);
				logMessage(Level.FINER, 2, "generate new fact:", newRule);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Set<LiteralVariable> extractUserLiteralVariablesFromBooleanFunction(final String literalVariableStr) throws LiteralVariablesEvaluatorException {
		Set<LiteralVariable> userLiteralVariables = new TreeSet<LiteralVariable>();
		if (null == literalVariableStr || "".equals(literalVariableStr.trim())) return userLiteralVariables;
		try {
			List<String> tokens = DflTheoryParser2.getTokenizeLiteralFunction(literalVariableStr, simplifiedLiteralVariables, literalFunctionAnswers);
			for (String token : tokens) {
				LiteralVariable literalVariable = isUserDefinedVariable(token);
				if (null != literalVariable) userLiteralVariables.add(literalVariable);
			}
			return userLiteralVariables;
		} catch (LiteralVariablesEvaluatorException e) {
			throw e;
		} catch (Exception e) {
			throw new LiteralVariablesEvaluatorException(e);
		}
	}

	protected LiteralVariable isUserDefinedVariable(String literalVariableStr) throws LiteralVariablesEvaluatorException {
		if (literalVariableStr.startsWith("" + DomConst.Literal.LITERAL_VARIABLE_PREFIX)
				|| literalVariableStr.startsWith("" + DomConst.Literal.LITERAL_NEGATION_SIGN + DomConst.Literal.LITERAL_VARIABLE_PREFIX)) {
			try {
				LiteralVariable literalVariable = DflTheoryParser2.extractLiteralVariable(literalVariableStr);
				return isUserDefinedVariable(literalVariable);
			} catch (ParserException e) {
				System.err.println("literalVariableStr [" + literalVariableStr + "] cannot be parsed");
				return null;
			} catch (Exception e) {
				System.err.println("literalVariableStr [" + literalVariableStr + "] " + e.getMessage());
				return null;
			}
		} else return null;
	}

	private LiteralVariable isUserDefinedVariable(LiteralVariable literalVariable) throws LiteralVariablesEvaluatorException {
		try {
			if (literalVariable.isLiteralVariable()) {
				return (appConstants.isAppConstant(literalVariable)) ? null : literalVariable;
			} else throw new LiteralVariablesEvaluatorException("literal variable: [" + literalVariable + "] is a boolean function");
		} catch (NullValueException e) {
		}
		return literalVariable;
	}

	private boolean evaluateBooleanValue(String str) throws ValueEvaluationException {
		try {
			String s = DflTheoryParser2.getLiteralFunctionEvaluationString(str, simplifiedLiteralVariables, literalFunctionAnswers);
			s = s.substring(1, s.length() - 1);
			Object res = evaluator.eval(s);
			if (res instanceof Boolean) {
				return (Boolean) res;
			} else throw new ValueEvaluationException(ErrorMessage.LITERAL_VARIABLE_EVALUATOR_IMPROPER_RESULT_TYPE, new Object[] { "Boolean",
					getResultType(res) });
		} catch (Exception e) {
			throw new ValueEvaluationException(e);
		}
	}

	private String getResultType(Object res) {
		if (res instanceof Boolean) {
			return "Boolean";
		} else if (res instanceof Long) {
			return "Long";
		} else if (res instanceof Integer) {
			return "Integer";
		} else if (res instanceof Double) {
			return "Double";
		} else if (res instanceof Float) { return "Float"; }
		return res.getClass().getName();
	}

//	@SuppressWarnings("unused")
//	private void removeLiteralVariable() {
//
//	}

	public Theory getTheory() {
		return theory;
	}

	public void addLiteralVariablesEvaluatorListener(LiteralVariablesEvaluatorListener listener) {
		addAppModuleListener(listener);
	}

	public void removeLiteralVariablesEvaluatorListener(LiteralVariablesEvaluatorListener listener) {
		removeAppModuleListener(listener);
	}

	protected void fireLiteralVariablesEvaluatorMessage(MessageType messageType, String message) {
		for (AppModuleListener listener : getAppModuleListeners()) {
			if (listener instanceof LiteralVariablesEvaluatorListener) {
				((LiteralVariablesEvaluatorListener) listener).onLiteralVariablesEvaluatorMesage(messageType, message);
			}
		}
	}
}
