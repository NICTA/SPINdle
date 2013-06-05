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
package spindle.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.io.parser.DflTheoryParser2;
import spindle.io.parser.XmlTheoryParser2;
import spindle.sys.AppConst;
import spindle.sys.AppLogger;
import spindle.sys.Conf;
import spindle.sys.ConfigurationException;
import spindle.sys.message.ErrorMessage;

import com.app.utils.ClassList;
import com.app.utils.FileManager;
import com.app.utils.ResourcesUtils;
import com.app.utils.Utilities;
import com.app.utils.Utilities.ProcessStatus;

/**
 * SPINdle I/O Manager.
 * <p>
 * Provides I/O (theories and conclusions) support (and coordination) to SPINdle reasoning engines.
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2011.07.27
 * @since version 1.0.0
 */
public final class IOManager {

	private static ClassList classList = new ClassList();
	private static Set<String> interfaceFilter = new HashSet<String>();
	private static long configurationTimeUsed = 0;

	private static Map<String, TheoryParser> parsers = null;
	private static Map<String, TheoryOutputter> outputters = null;

	static {
		try {
			interfaceFilter.add(TheoryParser.class.getName());
			interfaceFilter.add(TheoryOutputter.class.getName());
			initialize();
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public synchronized static void printMessage(PrintStream out, int level, String message, boolean withLineBreak) {
		if (null == message || "".equals(message.trim())) return;
		String identator = "";
		for (int i = 0; i < level; i++) {
			identator += AppConst.IDENTATOR;
		}
		if (withLineBreak) out.println(identator + message.trim());
		else out.print(identator + message.trim());
		out.flush();
	}

	public static void printMessage(String message) {
		printMessage(System.out, 0, message, true);
	}

	public static void printMessage(int level, String message) {
		printMessage(System.out, level, message, true);
	}

	public static void printMessage(int level, String message, boolean withLineBreak) {
		printMessage(System.out, level, message, withLineBreak);
	}

	/**
	 * search for I/O classes with TheoryParser and TheoryOutputter interface
	 * 
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private synchronized static Map<String, Set<String>> searchIOclasses() throws IOException, ClassNotFoundException {
		return classList.findClass(interfaceFilter, null, null);
	}

	private synchronized static Map<String, Set<String>> getIOclassesFromConfigFile() throws ClassNotFoundException, IOException,
			ParserConfigurationException, SAXException {
		InputStream ins = null;
		try {
			ins = ResourcesUtils.getResourceAsStream(AppConst.IO_CONF_FILE);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
			Document doc = documentBuilder.parse(ins);
			doc.getDocumentElement().normalize();

			Set<String> classNames = new HashSet<String>();

			NodeList nodeList = doc.getElementsByTagName("io");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element ioClassEle = (Element) node;
					String ioClassname = ioClassEle.getAttribute("classname");
					if (null != ioClassname) classNames.add(ioClassname);
				}
			}

			return classList.identifyInterface(interfaceFilter, classNames);
		} finally {
			if (null != ins) ins.close();
			ins = null;
		}
	}

	public synchronized static void initialize() throws ConfigurationException {
		if (null != parsers && null != outputters) return;

		// search all the I/O classes in the package
		// and create the associated parser/outputter class
		long timeStart = System.currentTimeMillis();
		try {
			Map<String, Set<String>> classes = Conf.isSearchIOclasses() ? searchIOclasses() : getIOclassesFromConfigFile();

			parsers = new HashMap<String, TheoryParser>();
			outputters = new HashMap<String, TheoryOutputter>();
			printMessage(1, "configurating I/O classes - start");
			for (Entry<String, Set<String>> entry : classes.entrySet()) {
				if (TheoryParser.class.getName().equals(entry.getKey())) {
					for (String parserName : entry.getValue()) {
						try {
							printMessage(2, "generating parser [" + parserName + "]", false);
							TheoryParser parser = Utilities.getInstance(parserName, TheoryParser.class);
							// Class<?> clazz = Class.forName(parserName);
							// TheoryParser parser = clazz.asSubclass(TheoryParser.class).newInstance();
							printMessage("...success, type=[" + parser.getParserType() + "]");
							parsers.put(parser.getParserType(), parser);
						} catch (Exception e) {
							if (!AppConst.isDeploy) printMessage("...failed");
							throw new ConfigurationException(ErrorMessage.IO_PARSER_INITITATION_ERROR, new String[] { parserName }, e);
						}
					}
				} else if (TheoryOutputter.class.getName().equals(entry.getKey())) {
					for (String outputterName : entry.getValue()) {
						try {
							printMessage(2, "generating outputter [" + outputterName + "]", false);
							TheoryOutputter outputter = Utilities.getInstance(outputterName, TheoryOutputter.class);
							// Class<?> clazz = Class.forName(outputterName);
							// TheoryOutputter outputter = clazz.asSubclass(TheoryOutputter.class).newInstance();
							printMessage("...success, type=[" + outputter.getOutputterType() + "]");
							outputters.put(outputter.getOutputterType(), outputter);
						} catch (Exception e) {
							if (!AppConst.isDeploy) printMessage("...failed");
							throw new ConfigurationException(ErrorMessage.IO_OUTPUTTER_INITIATION_ERROR, new String[] { outputterName }, e);
						}
					}
				}
			}
			configurationTimeUsed += (System.currentTimeMillis() - timeStart);
			printMessage(1, "configurating I/O classes - end");
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	public static long getConfigurationTimeUsed() {
		return configurationTimeUsed;
	}

	public static final Theory getTheory(final URI uri, final AppLogger logger) throws ParserException, MalformedURLException {
		try {
			return getTheory(uri.toURL(), logger);
		} catch (IOException e) {
			throw new ParserException(e);
		}
	}

	public static final Theory getTheory(final URL url, final AppLogger logger) throws ParserException, IOException {
		String filename = url.getFile();
		try {
			URLConnection urlc = url.openConnection();

			// get the content type
			String parserType = FileManager.getFileExtension(filename);
			if ("".equals(parserType)) {
				String contentType = urlc.getContentType();
				if (null != contentType) {
					contentType = contentType.toLowerCase();
					if (contentType.indexOf(XmlTheoryParser2.PARSER_TYPE) >= 0) parserType = XmlTheoryParser2.PARSER_TYPE;
					else if (contentType.indexOf(DflTheoryParser2.PARSER_TYPE) >= 0) parserType = DflTheoryParser2.PARSER_TYPE;
				}
				// set the default parserType to xml if no theory type info can
				// be found
				if ("".equals(parserType)) parserType = DflTheoryParser2.PARSER_TYPE;
			}
			return getTheory(urlc.getInputStream(), getParser(parserType), logger);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static final Theory getTheory(final File filename, final AppLogger logger) throws ParserException, IOException {
		String parserType = FileManager.getFileExtension(filename);
		try {
			return getTheory(new FileInputStream(filename), getParser(parserType), logger);
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static final Theory getTheory(final InputStream ins, String parserType, //
			final AppLogger logger) throws ParserException, IOException {
		if (null == parserType) throw new ParserException(ErrorMessage.IO_PARSER_TYPE_UNKNOWN);
		try {
			return getTheory(ins, getParser(parserType), logger);
		} catch (ConfigurationException e) {
			throw new ParserException(e);
		}
	}

	public static final Theory getTheory(final InputStream ins, TheoryParser parser, //
			final AppLogger logger) throws ParserException, IOException {
		if (null == parser) throw new ParserException(ErrorMessage.IO_PARSER_TYPE_UNKNOWN);
		try {
			if (null != logger) parser.setAppLogger(logger);
			return parser.getTheory(ins);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		} finally {
			parser.resetAppLogger();
			if (null != ins) ins.close();
		}
	}

	public static final Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(final URL url, final AppLogger logger)
			throws ParserException, IOException {
		String filename = url.getFile();
		try {
			URLConnection urlc = url.openConnection();

			// get the content type
			String parserType = FileManager.getFileExtension(filename);
			if ("".equals(parserType)) {
				String contentType = urlc.getContentType();
				if (null != contentType) {
					contentType = contentType.toLowerCase();
					if (contentType.indexOf(XmlTheoryParser2.PARSER_TYPE) >= 0) parserType = XmlTheoryParser2.PARSER_TYPE;
					else if (contentType.indexOf(DflTheoryParser2.PARSER_TYPE) >= 0) parserType = DflTheoryParser2.PARSER_TYPE;
				}
				// set the default parserType to xml if no theory type info can
				// be found
				if ("".equals(parserType)) parserType = XmlTheoryParser2.PARSER_TYPE;
			}
			return getConclusions(urlc.getInputStream(), getParser(parserType), logger);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static final Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(File filename,//
			AppLogger logger) throws ParserException, IOException {
		String parserType = FileManager.getFileExtension(filename);
		try {
			return getConclusions(new FileInputStream(filename), getParser(parserType), logger);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static final Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(InputStream ins, String parserType,//
			AppLogger logger) throws ParserException, IOException {
		if (null == parserType) throw new ParserException("parser type is null");
		try {
			return getConclusions(ins, getParser(parserType), logger);
		} catch (ParserException e) {
			throw e;
		} catch (ConfigurationException e) {
			throw new ParserException(e);
		}
	}

	public static final Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(InputStream ins, TheoryParser parser, //
			AppLogger logger) throws ParserException, IOException {
		if (null == parser) throw new ParserException("parser is null");
		try {
			if (null != logger) parser.setAppLogger(logger);
			return parser.getConclusions(ins);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		} finally {
			parser.resetAppLogger();
			if (null != ins) ins.close();
		}
	}

	/**
	 * Return the set of parser types supported.
	 * 
	 * @return <code>Set<String></code> Set of parser types.
	 */
	public static final Set<String> getParserTypes() {
		return parsers.keySet();
	}

	public static final TheoryParser getParser(final String parserType) throws ParserException, ConfigurationException {
		if (null == parsers) initialize();

		String type = parserType.toLowerCase();
		if (parsers.containsKey(type)) {
			TheoryParser parser = parsers.get(type);
			parser.resetAppLogger();
			return parser;
		} else throw new ParserException(ErrorMessage.IO_PARSER_TYPE_UNKNOWN, new Object[] { type });
	}

	/**
	 * Return the set of outputter types supported.
	 * 
	 * @return <code>Set<String></code> Set of outputter types.
	 */
	public static final Set<String> getOutputterTypes() {
		return outputters.keySet();
	}

	/**
	 * @param outputterType
	 *            the outputter type
	 * @return the associated FileOutputter of the specified file
	 * @throws OutputterException
	 * @throws IOException
	 */
	public static final TheoryOutputter getOutputter(final String outputterType) throws ConfigurationException {
		if (null == outputters) initialize();

		String type = outputterType.toLowerCase();
		if (outputters.containsKey(type)) {
			TheoryOutputter outputter = outputters.get(type);
			outputter.resetAppLogger();
			return outputter;
		} else throw new ConfigurationException(ErrorMessage.IO_OUTPUTTER_TYPE_UNKNOWN, new Object[] { type });
	}

	/**
	 * @param filename
	 * @param theory
	 * @return ProcessStatus.SUCCESS if success, exception throw otherwise
	 * @throws TheoryException
	 */
	public static final ProcessStatus save(File filename, Theory theory, AppLogger logger) throws OutputterException {
		File path = filename.getParentFile();
		if (null != path) path.mkdirs();
		String outputterType = FileManager.getFileExtension(filename);
		try {
			return save(new FileOutputStream(filename), getOutputter(outputterType), theory, logger);
		} catch (Exception e) {
			throw new OutputterException(e);
		}
	}

	public static final ProcessStatus save(OutputStream outs, String outputterType,//
			Theory theory, AppLogger logger) throws OutputterException, IOException {
		try {
			return save(outs, getOutputter(outputterType), theory, logger);
		} catch (ConfigurationException e) {
			throw new OutputterException(e);
		}
	}

	public static final ProcessStatus save(OutputStream outs, TheoryOutputter outputter,//
			Theory theory, AppLogger logger) throws OutputterException {
		try {
			if (null != logger) outputter.setAppLogger(logger);
			outputter.save(outs, theory);
			return ProcessStatus.SUCCESS;
		} catch (Exception e) {
			throw new OutputterException("Exception throw while saving theory", e);
		} finally {
			outputter.resetAppLogger();
			if (null != outs) {
				try {
					outs.flush();
					outs.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * @param filename
	 * @param conclusionsAsList
	 * @return ProcessStatus.SUCCESS if success, exception throw otherwise
	 * @throws TheoryException
	 */
	public static final ProcessStatus save(File filename, List<Conclusion> conclusionsAsList,//
			AppLogger logger) throws OutputterException {
		File path = filename.getParentFile();
		if (null != path) path.mkdirs();
		String outputterType = FileManager.getFileExtension(filename);
		try {
			return save(new FileOutputStream(filename), getOutputter(outputterType), conclusionsAsList, logger);
		} catch (Exception e) {
			throw new OutputterException(e);
		}
	}

	public static final ProcessStatus save(OutputStream outs, String outputterType,//
			List<Conclusion> conclusionsAsList, AppLogger logger) throws OutputterException, IOException {
		try {
			return save(outs, getOutputter(outputterType), conclusionsAsList, logger);
		} catch (ConfigurationException e) {
			throw new OutputterException(e);
		}
	}

	public static final ProcessStatus save(OutputStream outs, TheoryOutputter outputter,//
			List<Conclusion> conclusionsAsList, AppLogger logger) throws OutputterException, IOException {
		try {
			if (null != logger) outputter.setAppLogger(logger);
			outputter.save(outs, conclusionsAsList);
			return ProcessStatus.SUCCESS;
		} catch (Exception e) {
			throw new OutputterException("Exception throw while saving theory", e);
		} finally {
			outputter.resetAppLogger();
			if (null != outs) {
				outs.flush();
				outs.close();
			}
		}
	}
}
