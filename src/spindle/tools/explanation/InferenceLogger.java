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
package spindle.tools.explanation;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.RuleType;

/**
 * Provides inference logging services to the reasoning engines.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2012.07.26
 * @since version 2.1.2
 */
public class InferenceLogger {

	public static InferenceLogger getInstance() {
		return new InferenceLogger();
	}

	private Map<String, InferenceLogItem> inferenceLogItems;

	public InferenceLogger() {
		inferenceLogItems = new TreeMap<String, InferenceLogItem>();
	}

	public void updateRuleInferenceStatus(Set<String> ruleLabels, RuleType ruleType, //
			ConclusionType conclusionType, Literal literal, RuleInferenceStatus ruleInferenceStatus) {
		for (String ruleLabel:ruleLabels){
			updateRuleInferenceStatus(ruleLabel,ruleType,conclusionType,literal,ruleInferenceStatus);
		}
	}
	public void updateRuleInferenceStatus(String ruleLabel, RuleType ruleType, //
			ConclusionType conclusionType, Literal literal, RuleInferenceStatus ruleInferenceStatus) {
		InferenceLogItem item = inferenceLogItems.get(ruleLabel);
		if (null == item) {
			item = new InferenceLogItem(ruleLabel);
			inferenceLogItems.put(ruleLabel, item);
		}
		item.addRuleInferenceItem(ruleType, conclusionType, literal, ruleInferenceStatus);
	}

	public void updateRuleInferenceStatus(Set<String> ruleLabels, Conclusion conclusion,
			RuleInferenceStatus ruleInferenceStatus) {
		for (String ruleLabel:ruleLabels){
		updateRuleInferenceStatus(ruleLabel,conclusion,ruleInferenceStatus);	
		}		
	}
	public void updateRuleInferenceStatus(String ruleLabel, Conclusion conclusion,
			RuleInferenceStatus ruleInferenceStatus) {
		InferenceLogItem item = inferenceLogItems.get(ruleLabel);
		if (null == item) {
			item = new InferenceLogItem(ruleLabel);
			inferenceLogItems.put(ruleLabel, item);
		}
		item.addRuleInfernceItem(conclusion, ruleInferenceStatus);
	}

	
	public Map<String, InferenceLogItem> getInferenceLogItems(){
		return inferenceLogItems;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(
				"========================\n=== Inference Logger ===\n========================");
		if (inferenceLogItems.size() == 0) {
			sb.append("\n *** NO items found!");
		} else {
			for (InferenceLogItem item : inferenceLogItems.values()) {
				sb.append("\n").append(item.toString());
			}
		}
		return sb.toString();
	}

}
