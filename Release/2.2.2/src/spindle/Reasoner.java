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
package spindle;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;

import com.app.utils.Utilities;
import com.app.utils.Utilities.ProcessStatus;

import spindle.core.MessageType;
import spindle.core.ReasonerBase;
import spindle.core.ReasonerException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Theory;
import spindle.engine.TheoryNormalizerException;
import spindle.io.IOManager;
import spindle.io.ParserException;
import spindle.io.parser.DflTheoryParser2;
import spindle.io.parser.XmlTheoryParser2;
import spindle.sys.Conf;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * SPINdle - the SPIN Defeasible Logic reasoner
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class Reasoner extends ReasonerBase {
	private URL url = null;

	private Theory normalizedTheory = null;

	public Reasoner() {
		super();
	}

	/**
	 * load theory from a file
	 * 
	 * @param file
	 *            filename
	 * @return Process status
	 * @throws ReasonerException
	 */
	public ProcessStatus loadTheory(final File file) throws ReasonerException {
		clear();

		if (file == null || "".equals(file.getName())) throw new ReasonerException(ErrorMessage.IO_EMPTY_FILENAME);

		try {
			String filename = file.getCanonicalPath();
			return loadTheory(new URL("file", "", filename.replaceAll("\\\\", "/")));
		} catch (Exception e) {
			onLogMessage(Level.SEVERE, "ParserException: ", e);
			throw new ReasonerException(e);
		}
	}

	/**
	 * load theory from a url
	 * 
	 * @param url
	 *            <code>java.net.URL</code> the url of defeasible theory
	 * @return Process status
	 * @throws ReasonerException
	 */
	public ProcessStatus loadTheory(URL url) throws ReasonerException {
		clear();

		fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.IO_LOAD_THEORY_FROM_URL, new Object[] { url }));
		if (null == url || "".equals(url.getFile())) throw new ReasonerException(Messages.getErrorMessage(ErrorMessage.IO_EMPTY_FILENAME));

		this.url = url;
		setAppLogger(Conf.getLogger(url.getFile()));

		try {
			loadTheory(IOManager.getTheory(url, this));
		} catch (Exception e) {
			onLogMessage(Level.SEVERE, "ParserException: ", e);
			throw new ReasonerException("Parser Exception throw while parsing theory file", e);
		}

		return ProcessStatus.SUCCESS;
	}

	/**
	 * load theory from an xml string
	 * 
	 * @param xmlString
	 *            xml data string
	 * @return Process Status
	 * @link ProcessStatus
	 * @throws ReasonerException
	 */
	public ProcessStatus loadTheory(final String xmlString) throws ReasonerException {
		fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.IO_LOAD_THEORY_FROM_XML_STRING));
		clear();

		if (xmlString == null || "".equals(xmlString.trim())) {
			String msg = Messages.getErrorMessage(ErrorMessage.IO_XML_STRING_IS_EMPTY);
			fireOnReasonerMessage(MessageType.ERROR, "Reasoner Exception: " + msg);
			throw new ReasonerException("Reasoner Exception: " + msg);
		}

		setAppLogger(Conf.getLogger("XML_STRING_" + Utilities.getRandomString(10)));

		try {
			Theory theory = XmlTheoryParser2.getTheory(xmlString, this);
			loadTheory(theory);
		} catch (Exception e) {
			onLogMessage(Level.SEVERE, "ParserException: ", e);
			throw new ReasonerException("Parser Exception throw while parsing theory file", e);
		}

		return ProcessStatus.SUCCESS;
	}

	/**
	 * load theory from an array of rules written in dfl format (please note the difference between this method and
	 * the one that load theory as an xml string)
	 * 
	 * @param rules
	 * @return Process status
	 * @throws ReasonerException
	 */
	public ProcessStatus loadTheory(final String[] rules) throws ReasonerException {
		fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.IO_LOAD_THEORY_FROM_DFL_STRING));
		clear();

		StringBuilder sb = new StringBuilder();
		for (String rule : rules) {
			String r = rule.replaceAll("\\s", "");
			if (!"".equals(r)) sb.append(r).append(LINE_SEPARATOR);
		}

		setAppLogger(Conf.getLogger("DFL_STRING_" + Utilities.getRandomString(10)));

		try {
			Theory theory = DflTheoryParser2.getTheory(sb.toString(), this);
			loadTheory(theory);
		} catch (ParserException e) {
			onLogMessage(Level.SEVERE, "ParserException: ", e);
			throw new ReasonerException("Parser Exception throw while parsing theory file", e);
		}

		return ProcessStatus.SUCCESS;
	}

	@Override
	protected ProcessStatus doTransformTheoryToRegularForm() throws ReasonerException {
		if (workingTheory.getStrictRulesCount() > 0 || workingTheory.getDefeasibleRulesCount() > 0) {
			fireOnReasonerMessage(MessageType.INFO, "transform theory to regular form");
			try {
				getTheoryNormalizer().transformTheoryToRegularForm();
				workingTheory = getTheoryNormalizer().getTheory();
				if (Conf.isShowProgress()) System.out.println(workingTheory.toString());
			} catch (TheoryNormalizerException e) {
				throw new ReasonerException("Theory transform exception", e);
			}
			onLogMessage(Level.INFO, "=== transform theory to regular form:", getTheoryNormalizer().getTheory());
		} else {
			fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.THEORY_CONTAINS_NO_FACT_OR_STRICT_RULES));
		}

		return ProcessStatus.SUCCESS;
	}

	@Override
	public ProcessStatus removeDefeater() throws ReasonerException {
		if (workingTheory.getDefeatersCount() > 0) {
			fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_REMOVE_DEFEATER_FROM_THEORY));
			try {
				getTheoryNormalizer().removeDefeater();
				workingTheory = getTheoryNormalizer().getTheory();
				if (Conf.isShowProgress()) System.out.println(workingTheory.toString());
			} catch (TheoryNormalizerException e) {
				throw new ReasonerException("Defeater removal exception", e);
			}
			onLogMessage(Level.INFO, "=== Remove defeater:", getTheoryNormalizer().getTheory());
		} else {
			fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.THEORY_CONTAINS_NO_DEFEATER));
		}

		return ProcessStatus.SUCCESS;
	}

	@Override
	public ProcessStatus removeSuperiority() throws ReasonerException {
		if (workingTheory.getSuperiorityCount() > 0) {
			fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_REMOVE_SUPERIORITY_FROM_THEORY));
			try {
				getTheoryNormalizer().removeSuperiority();
				workingTheory = getTheoryNormalizer().getTheory();
				if (Conf.isShowProgress()) System.out.println(workingTheory.toString());
			} catch (TheoryNormalizerException e) {
				throw new ReasonerException("Superiority removal exception", e);
			}
			onLogMessage(Level.INFO, "=== Remove superiority:", getTheoryNormalizer().getTheory());
		} else {
			fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.THEORY_CONTAINS_NO_SUPERIORITY));
		}

		return ProcessStatus.SUCCESS;
	}

	/**
	 * get the normalized theory from theory normalizer and generate the conclusion from the reasoning engine
	 * 
	 * @return Set of conclusions
	 * @throws ReasonerException
	 */
	@Override
	public Map<Literal, Map<ConclusionType, Conclusion>> getConclusions() throws ReasonerException {
		fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.REASONER_GENERATE_CONCLUSIONS));
		if (conclusions == null) {
			workingTheory = getTheoryNormalizer().getTheory();
			normalizedTheory = (workingTheory == null) ? origTheory : workingTheory;

			try{
			Map<Literal, Map<ConclusionType, Conclusion>> tempConclusions = getReasoningEngine().getConclusions(normalizedTheory);
			setConclusions(tempConclusions);
			}catch (Exception e){
				throw new ReasonerException(e);
			}
		}
		if (Conf.isShowResult() || Conf.isShowProgress()) printConclusions();
		return conclusions;
	}

	public URL getUrl() {
		return url;
	}

	public Theory getNormalizedTheory() {
		try {
			return getTheoryNormalizer().getTheory();
		} catch (ReasonerException e) {
			return null;
		}
	}

	public ProcessStatus clear() {
		super.clear();

		url = null;
		normalizedTheory = null;

		return ProcessStatus.SUCCESS;
	}


}
