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
package spindle.engine;

import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;

import com.app.utils.FileManager;
import com.app.utils.TextUtilities;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
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
	private static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	public String printEngineStatus(final String callerClassName, Theory theory,//
			Map<Literal, Map<ConclusionType, Conclusion>> conclusions, //
			Deque<Conclusion>[] pendingConclusions, Map<Conclusion, Set<String>>[] ambiguousConclusions,//
			Map<Literal, Map<ConclusionType, Conclusion>> records) {
		StringBuilder sb = new StringBuilder();

		sb.append("===").append(LINE_SEPARATOR).append("=== ").append(callerClassName)
				.append(Messages.getSystemMessage(SystemMessage.APPLICATION_TEXT_START)).append(LINE_SEPARATOR).append("===");

		if (!theory.isEmpty()) {
			sb.append(LINE_SEPARATOR).append("------");
			sb.append(LINE_SEPARATOR).append("theory");
			sb.append(LINE_SEPARATOR).append("------");
			sb.append(LINE_SEPARATOR).append(theory.toString());
		}
		sb.append(getConclusionString("conclusions", conclusions));
		for (int i = 0; i < pendingConclusions.length; i++) {
			if (null != pendingConclusions[i] && pendingConclusions[i].size() > 0) {
				sb.append(getConclusionString("pending conclusions (" + (i == 0 ? "definite" : "defeasible") + ")",
						new TreeSet<Conclusion>(pendingConclusions[i])));
			}
		}
		for (int i = 0; i < pendingConclusions.length; i++) {
			sb.append(getConclusionString("ambiguous conclusions (" + (i == 0 ? "definite" : "defeasible") + ")",
					ambiguousConclusions[i].keySet()));
		}
		sb.append(getConclusionString("records", records));

		sb.append(LINE_SEPARATOR).append("===").append(LINE_SEPARATOR).append("=== ").append(callerClassName)
				.append(Messages.getSystemMessage(SystemMessage.APPLICATION_TEXT_END)).append(LINE_SEPARATOR)
				.append("===");
		return sb.toString();
	}

	private String getConclusionString(String label, Map<Literal, Map<ConclusionType, Conclusion>> conclusions) {
		if (null == conclusions || conclusions.size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		String sep = TextUtilities.repeatStringPattern("-", label.length());
		sb.append(LINE_SEPARATOR).append(sep);
		sb.append(LINE_SEPARATOR).append(label);
		sb.append(LINE_SEPARATOR).append(sep);
		for (Entry<Literal, Map<ConclusionType, Conclusion>> entry : conclusions.entrySet()) {
			sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append(entry.getKey()).append(":")
					.append(entry.getValue().keySet());
		}
		return sb.toString();
	}

	private String getConclusionString(String label, Set<Conclusion> conclusions) {
		if (null == conclusions || conclusions.size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		String sep = TextUtilities.repeatStringPattern("-", label.length());
		sb.append(LINE_SEPARATOR).append(sep);
		sb.append(LINE_SEPARATOR).append(label);
		sb.append(LINE_SEPARATOR).append(sep);
		for (Conclusion conclusion : conclusions) {
			sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append(conclusion);
		}
		return sb.toString();
	}

}
