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
package spindle.io.outputter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.DomConst;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.Mode;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.io.OutputterException;

/**
 * Defeasible theory and conclusions outputter in DFL language.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.05.30
 */
public class DflTheoryOutputter extends AbstractTheoryOutputter {
	public static final String OUTPUTTER_TYPE = "dfl";

	private static StringBuilder THEORY_COMMENT = null;
	private static StringBuilder CONCLUSION_COMMENT = null;

	static {
		String NEW_LINE_AND_COMMENT = LINE_SEPARATOR + DflTheoryConst.COMMENT_SYMBOL;

		THEORY_COMMENT = new StringBuilder();
		THEORY_COMMENT.append("########################################").append(LINE_SEPARATOR);
		THEORY_COMMENT.append("# symbols used").append(LINE_SEPARATOR);
		THEORY_COMMENT.append("# ------------").append(LINE_SEPARATOR);
		for (RuleType ruleType : RuleType.values()) {
			THEORY_COMMENT.append(DflTheoryConst.COMMENT_SYMBOL).append(" ")//
					.append("[").append(ruleType.getSymbol()).append("] ")//
					.append(ruleType.getLabel()).append(LINE_SEPARATOR);
		}
		THEORY_COMMENT.append("#")
				//
				.append(NEW_LINE_AND_COMMENT).append(" [").append(DflTheoryConst.SYMBOL_NEGATION).append("] negation")
				//
				.append(NEW_LINE_AND_COMMENT).append(NEW_LINE_AND_COMMENT).append(" [").append(DflTheoryConst.SYMBOL_MODE_CONVERSION)
				.append("] mode conversion")
				//
				.append(NEW_LINE_AND_COMMENT).append(" [").append(DflTheoryConst.SYMBOL_MODE_CONFLICT).append("] mode conflict")//
				.append(LINE_SEPARATOR);
		THEORY_COMMENT.append("########################################").append(LINE_SEPARATOR).append(LINE_SEPARATOR);

		CONCLUSION_COMMENT = new StringBuilder();
		CONCLUSION_COMMENT.append("########################################").append(LINE_SEPARATOR);
		CONCLUSION_COMMENT.append("# symbols used").append(LINE_SEPARATOR);
		CONCLUSION_COMMENT.append("# ------------").append(LINE_SEPARATOR);
		for (ConclusionType conclusionType : ConclusionType.values()) {
			switch (conclusionType) {
			case DEFINITE_PROVABLE:
			case DEFINITE_NOT_PROVABLE:
			case DEFEASIBLY_PROVABLE:
			case DEFEASIBLY_NOT_PROVABLE:
				CONCLUSION_COMMENT.append(DflTheoryConst.COMMENT_SYMBOL).append(" ")//
						.append(conclusionType.getSymbol())//
						.append(" - ")//
						.append(conclusionType.getLabel()).append(LINE_SEPARATOR);
				break;
			default:
			}
		}
		CONCLUSION_COMMENT.append("########################################").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
	}

	public DflTheoryOutputter() {
		super(OUTPUTTER_TYPE);
	}

	protected void printHeader() throws IOException {
		writer.write("# " + getHeaderComment() + LINE_SEPARATOR);
		writer.write("# " + getGenerationTimeString() + LINE_SEPARATOR);
		writer.write(LINE_SEPARATOR);
	}

	@Override
	protected void saveToStream(OutputStream os, Theory theory) throws OutputterException {
		try {
			if (null == writer) throw new IOException("writer is null");

			printHeader();

			writer.write(THEORY_COMMENT.toString());

			saveLiteralVariables(theory.getLiteralVariables(), "Literal variables");
			saveLiteralVariables(theory.getLiteralBooleanFunctions(), "Literal boolean functions");
			saveConversionRules(theory.getAllModeConversionRules(), theory.getAllModeConflictRules(), theory.getAllModeExclusionRules());
			saveFactsAndAllRules(theory.getFactsAndAllRules().values());
			saveSuperiority(theory.getAllSuperiority());
		} catch (Exception e) {
			throw new OutputterException(e);
		}
	}

	private void saveLiteralVariables(Map<LiteralVariable, LiteralVariable> literalVariables, String label) throws IOException {
		if (literalVariables.size() > 0) {
			if (null != label && !"".equals(label)) {
				writer.write(DflTheoryConst.COMMENT_SYMBOL + " " + label.trim());
				writer.write(LINE_SEPARATOR);
			}
			for (Entry<LiteralVariable, LiteralVariable> entry : literalVariables.entrySet()) {
				writer.write(RuleType.LITERAL_VARIABLE_SET.getSymbol());
				writer.write(" ");
				writer.write(entry.getKey().getName() + DflTheoryConst.THEORY_EQUAL_SIGN + entry.getValue().getName());
				writer.write(LINE_SEPARATOR);
			}
			writer.write(LINE_SEPARATOR);
		}
	}

	private void saveConversionRules(Map<String, Set<String>> conversionRules, //
			Map<String, Set<String>> conflictRules, Map<String, Set<String>> exclusionRules) throws IOException {

		List<String> modeUsed = new Vector<String>();
		modeUsed.addAll(conversionRules.keySet());
		for (String m : conflictRules.keySet()) {
			if (!modeUsed.contains(m)) modeUsed.add(m);
		}
		for (String m : exclusionRules.keySet()) {
			if (!modeUsed.contains(m)) modeUsed.add(m);
		}

		for (String mode : modeUsed) {
			if (null != conversionRules.get(mode)) {
				writer.write(generateConversionRuleStr(mode, conversionRules.get(mode), DflTheoryConst.SYMBOL_MODE_CONVERSION));
				writer.write(LINE_SEPARATOR);
			}
			if (null != conflictRules.get(mode)) {
				writer.write(generateConversionRuleStr(mode, conflictRules.get(mode), DflTheoryConst.SYMBOL_MODE_CONFLICT));
				writer.write(LINE_SEPARATOR);
			}
			if (null != exclusionRules.get(mode)) {
				writer.write(generateConversionRuleStr(mode, exclusionRules.get(mode), DflTheoryConst.SYMBOL_MODE_EXCLUSION));
				writer.write(LINE_SEPARATOR);
			}
			writer.write(LINE_SEPARATOR);
		}
	}

	private String generateConversionRuleStr(String mode, Set<String> modeList, String typeStr) {
		StringBuilder sb = new StringBuilder();
		for (String m : modeList) {
			if (sb.length() > 0) sb.append(",");
			sb.append(m);
		}
		return mode + " " + typeStr + " " + sb.toString();
	}

	private void saveFactsAndAllRules(Collection<Rule> rules) throws IOException {
		if (null == rules || rules.size() == 0) return;
		for (Rule rule : rules) {
			StringBuilder sb = new StringBuilder();
			if (!rule.getLabel().startsWith(DEFAULT_RULE_LABEL_PREFIX)) sb.append(rule.getLabel());
			if (!"".equals(rule.getMode().getName())) {
				Mode mode = rule.getMode();
				sb.append(DflTheoryConst.MODE_START);
				sb.append(mode.toString());
				sb.append(DflTheoryConst.MODE_END);
			}
			if (sb.length() > 0) {
				sb.append(" " + DflTheoryConst.RULE_LABEL_SEPARATOR + " ");
				writer.write(sb.toString());
			}

			String ruleStr = rule.getRuleAsString();
			writer.write(ruleStr.replaceAll("" + DomConst.Literal.LITERAL_NEGATION_SIGN, DflTheoryConst.SYMBOL_NEGATION));
			writer.write(LINE_SEPARATOR);
		}
		writer.write(LINE_SEPARATOR);
	}

	private void saveSuperiority(List<Superiority> sup) throws IOException {
		if (null == sup || sup.size() == 0) return;
		for (Superiority s : sup) {
			writer.write(s.toString());
			writer.write(LINE_SEPARATOR);
		}
		writer.write(LINE_SEPARATOR);
	}

	@Override
	protected void saveToStream(OutputStream os, List<Conclusion> conclusionsAsList) throws OutputterException {
		try {
			if (null == writer) throw new IOException("writer is null");

			printHeader();
			writer.write(CONCLUSION_COMMENT.toString());

			for (Conclusion conclusion : conclusionsAsList) {
				String conclusionStr = conclusion.getLiteral().toString();
				conclusionStr = conclusionStr.replaceAll("" + DomConst.Literal.LITERAL_NEGATION_SIGN, DflTheoryConst.SYMBOL_NEGATION);

				ConclusionType conclusionType = conclusion.getConclusionType();
				writer.write(conclusionType.getSymbol() + " " + conclusionStr + LINE_SEPARATOR);
			}
		} catch (Exception e) {
			throw new OutputterException(e);
		}
	}
}
