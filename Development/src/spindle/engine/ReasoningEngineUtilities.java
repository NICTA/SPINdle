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
package spindle.engine;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.app.utils.FileManager;
import com.app.utils.TextUtilities;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.ProvabilityLevel;
import spindle.core.dom.Temporal;
import spindle.core.dom.Theory;
import spindle.sys.AppConst;
import spindle.sys.Messages;
import spindle.sys.message.SystemMessage;

/**
 * Utilities class for reasoning engine
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class ReasoningEngineUtilities {
	private static final String SUMMARY_BORDER = "=================================================";
	private static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;
	private static String LINE_SEPARTOR_IDENTATOR = LINE_SEPARATOR + AppConst.IDENTATOR;

	public void printTheorySummary(final Theory theory) {
		StringBuilder sb = new StringBuilder();
		sb.append(SUMMARY_BORDER) //
				.append("\ntheory (regular form) summary") //
				.append("\n-----------------------------") //
				.append("\ntheory size=").append(theory.getFactsAndAllRules().size()) //
				.append("\nnumber of literals=").append(theory.getAllLiteralsInRules().size()) //
				.append("\nnumber of strict rule(s)=").append(theory.getStrictRulesCount()) //
				.append("\nnumber of defeasible rule(s)=").append(theory.getDefeasibleRulesCount()) //
				.append("\n").append(SUMMARY_BORDER);
		System.out.println(sb.toString());
	}

	public String generateEngineInferenceStatusMessage(
			final String callerClassName,
			Theory theory,//
			Map<Literal, Map<ConclusionType, Conclusion>> conclusions, //
			Collection<Conclusion>[] pendingConclusions, //
			Map<Conclusion, Set<String>>[] ambiguousConclusions,
			Map<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> ambiguousTemporalConclusions[],
			Map<Literal, Set<ConclusionType>> records) {
		StringBuilder sb = new StringBuilder();

		sb.append("===").append(LINE_SEPARATOR).append("=== ").append(callerClassName)
				.append(Messages.getSystemMessage(SystemMessage.APPLICATION_TEXT_START)).append(LINE_SEPARATOR).append("===");

		if (null!=theory &&!theory.isEmpty()) {
			sb.append(LINE_SEPARATOR).append("------");
			sb.append(LINE_SEPARATOR).append("theory");
			sb.append(LINE_SEPARATOR).append("------");
			sb.append(LINE_SEPARATOR).append(theory.toString());
		}

		sb.append(getConclusionsString("conclusions", conclusions));

		if (null != pendingConclusions) {
			for (int i = 0; i < pendingConclusions.length; i++) {
				if (null != pendingConclusions[i] && pendingConclusions[i].size() > 0) {
					sb.append(getConclusionString("pending conclusions (" + (i == 0 ? "definite" : "defeasible") + ")",
							pendingConclusions[i]));
				}
			}
		}

		if (null != ambiguousConclusions) {
			for (int i = 0; i < ambiguousConclusions.length; i++) {
				if (null != ambiguousConclusions[i] && ambiguousConclusions[i].size() > 0) {
					sb.append(getConclusionString("ambiguous conclusions (" + (i == 0 ? "definite" : "defeasible") + ")",
							ambiguousConclusions[i].keySet()));
				}
			}
		}

		if (null != ambiguousTemporalConclusions) {
			for (int i = 0; i < ambiguousTemporalConclusions.length; i++) {
				if (null != ambiguousTemporalConclusions[i] && ambiguousTemporalConclusions[i].size() > 0) {
					if (sb.length() > 0) sb.append(LINE_SEPARATOR);
					sb.append(getAmbiguousTemporalConclusionString("ambiguous temporal conclusions ("
							+ (i == 0 ? "definite" : "defeasible") + ")", ambiguousTemporalConclusions[i]));
				}
			}
		}

		sb.append(getRecordString("records", records));

		sb.append(LINE_SEPARATOR).append("===").append(LINE_SEPARATOR).append("=== ").append(callerClassName)
				.append(Messages.getSystemMessage(SystemMessage.APPLICATION_TEXT_END)).append(LINE_SEPARATOR).append("===");
		return sb.toString();
	}

	private String getAmbiguousTemporalConclusionString(String label,
			Map<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> conclusions) {
		if (null == conclusions || conclusions.size() == 0) return null;
		String sep = null;
		StringBuilder sb = new StringBuilder();
		if (null != label && !"".equals(label.trim())) {
			sep = TextUtilities.repeatStringPattern("-", label.trim().length());
			sb.append(sep).append(LINE_SEPARATOR).append(label.trim()).append(LINE_SEPARATOR).append(sep);
		}
		for (Entry<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> literalsEntry : conclusions.entrySet()) {
			Literal literal = literalsEntry.getKey().clone();
			for (Entry<Temporal, Map<ConclusionType, Set<String>>> temporalsEntry : literalsEntry.getValue().entrySet()) {
				literal.setTemporal(temporalsEntry.getKey());
				for (Entry<ConclusionType, Set<String>> conclusionEntry : temporalsEntry.getValue().entrySet()) {
					sb.append(LINE_SEPARTOR_IDENTATOR).append(conclusionEntry.getKey().getSymbol()).append(" ").append(literal);
				}
			}
		}
		if (null != sep) sb.append(LINE_SEPARATOR).append(sep);
		return sb.toString();
	}

	public void printAmbiguousTemporalConclusions(Map<Literal, TreeMap<Temporal, Map<ConclusionType, Set<String>>>> conclusions[]) {
		if (null == conclusions) return;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < conclusions.length; i++) {
			String str = getAmbiguousTemporalConclusionString("ambiguous temporal conclusions (" + (i == 0 ? "definite" : "defeasible")
					+ ")", conclusions[i]);
			if (null != str && !"".equals(str.trim())) {
				if (sb.length() > 0) sb.append(LINE_SEPARATOR);
				sb.append(str);
			}
		}
		if (sb.length() > 0) System.out.println(sb.toString());
	}

	private String getRecordString(String label, Map<Literal, Set<ConclusionType>> records) {
		if (null == records || records.size() == 0) return "";
		StringBuilder sb = new StringBuilder(getLabelString(label));
		for (Entry<Literal, Set<ConclusionType>> entry : records.entrySet()) {
			sb.append(LINE_SEPARTOR_IDENTATOR).append(entry.getKey()).append(":").append(entry.getValue());
		}
		return sb.toString();
	}

	private String getConclusionsString(String label, Map<Literal, Map<ConclusionType, Conclusion>> conclusions) {
		if (null == conclusions || conclusions.size() == 0) return "";
		StringBuilder sb = new StringBuilder(getLabelString(label));
		for (Entry<Literal, Map<ConclusionType, Conclusion>> entry : conclusions.entrySet()) {
			sb.append(LINE_SEPARTOR_IDENTATOR).append(entry.getKey()).append(":").append(entry.getValue().keySet());
		}
		return sb.toString();
	}

	private String getConclusionString(String label, Collection<Conclusion> conclusions) {
		if (null == conclusions || conclusions.size() == 0) return "";
		StringBuilder sb = new StringBuilder(getLabelString(label));
		for (Conclusion conclusion : conclusions) {
			sb.append(LINE_SEPARTOR_IDENTATOR).append(conclusion);
		}
		return sb.toString();
	}

	private String getLabelString(String label) {
		if (null == label || "".equals(label.trim())) return "";
		StringBuilder sb = new StringBuilder();
		String sep = TextUtilities.repeatStringPattern("-", label.length());
		sb.append(LINE_SEPARATOR).append(sep);
		sb.append(LINE_SEPARATOR).append(label);
		sb.append(LINE_SEPARATOR).append(sep);
		return sb.toString();
	}

	// private void printAmbiguousConclusions(Map<Conclusion, Set<String>> conclusions[]) {
	// for (int i = 0; i < conclusions.length; i++) {
	// if (conclusions[i].size() > 0) {
	// System.out.println(getAmbiguousConclusionString("ambiguous conclusions (" + (i == 0 ? "definite" : "defeasible")
	// + ")",
	// conclusions[i]));
	// }
	// }
	// }
	//
	// private String getAmbiguousConclusionString(String label, Map<Conclusion, Set<String>> conclusions) {
	// StringBuilder sb = new StringBuilder(getLabelString(label));
	// for (Conclusion conclusion : conclusions.keySet()) {
	// sb.append(LINE_SEPARTOR_IDENTATOR).append(conclusion);
	// }
	// return sb.toString();
	// }

	public Map<Literal, Map<ConclusionType, Conclusion>> transformConclusions(
			Map<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> conclusions) {
		if (!AppConst.isDeploy) {
			StringBuilder sb = new StringBuilder(TextUtilities.generateHighLightedMessage("transformConclusions - origConclusions - start"));
			for (Entry<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> provabilitySet : conclusions.entrySet()) {
				for (Entry<Literal, TreeMap<Temporal, ConclusionType>> literalEntry : provabilitySet.getValue().entrySet()) {
					sb.append(LINE_SEPARATOR).append(literalEntry.getKey()).append(":");
					for (Entry<Temporal, ConclusionType> temporalEntry : literalEntry.getValue().entrySet()) {
						sb.append(LINE_SEPARTOR_IDENTATOR).append(temporalEntry.getKey()).append(":")
								.append(temporalEntry.getValue().getSymbol());
					}
				}
			}
			sb.append(LINE_SEPARATOR).append(TextUtilities.generateHighLightedMessage("transformConclusions - origConclusions - end"));
			System.out.println(sb.toString());
		}

		Map<Literal, Map<ConclusionType, Conclusion>> newConclusions = new TreeMap<Literal, Map<ConclusionType, Conclusion>>();
		for (Entry<ProvabilityLevel, Map<Literal, TreeMap<Temporal, ConclusionType>>> provabilitySet : conclusions.entrySet()) {
			for (Entry<Literal, TreeMap<Temporal, ConclusionType>> literalEntry : provabilitySet.getValue().entrySet()) {
				Literal literal = literalEntry.getKey();
				for (Entry<Temporal, ConclusionType> temporalEntry : literalEntry.getValue().entrySet()) {
					Literal l = literal.clone();
					l.setTemporal(temporalEntry.getKey());
					ConclusionType ct = temporalEntry.getValue();

					Map<ConclusionType, Conclusion> conclusionSet = newConclusions.get(l);
					if (null == conclusionSet) {
						conclusionSet = new TreeMap<ConclusionType, Conclusion>();
						newConclusions.put(l, conclusionSet);
					}
					conclusionSet.put(ct, new Conclusion(ct, l));
				}
			}
		}

		if (!AppConst.isDeploy) {
			System.out.println(getConclusionsString("transformed conclusions", newConclusions));
			// StringBuilder sb = new
			// StringBuilder(TextUtilities.generateHighLightedMessage("transformConclusions - newConclusions - start"));
			// for (Entry<Literal, Map<ConclusionType, Conclusion>> literalEntry : newConclusions.entrySet()) {
			// for (Entry<ConclusionType, Conclusion> conclusionEntry : literalEntry.getValue().entrySet()) {
			// sb.append(LINE_SEPARATOR).append(conclusionEntry.getValue());
			// }
			// }
			// sb.append(LINE_SEPARATOR).append(TextUtilities.generateHighLightedMessage("transformConclusions - newConclusions - end"));
			// System.out.println(sb.toString());
		}

		return newConclusions;
	}

}
