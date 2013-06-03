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
package spindle.sys;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.app.utils.DateTime;
import com.app.utils.FileManager;
import com.app.utils.Properties;
import com.app.utils.ResourcesUtils;
import com.app.utils.Utilities;

import spindle.ReasonerMain;
import spindle.core.ReasonerUtilities;
import spindle.io.IOManager;
import spindle.sys.message.SystemMessage;

/**
 * System configuration class.
 * <p>
 * Used to configure the system environment and the type of reasoning parameters.
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class Conf {
	private static final Map<String, String> DEFAULT_ARGUMENT_VALUE_CHANGE = new TreeMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put(ConfTag.LOG_LEVEL, "INFO");

			put(ConfTag.IS_SHOW_PROGRESS, "true");
			put(ConfTag.IS_SHOW_RESULT, "true");
			put(ConfTag.IS_SAVE_RESULT, "true");
			put(ConfTag.IS_CONSOLE_MODE, "true");

			put(ConfTag.IS_GARBAGE_COLLECTION, "true");
			put(ConfTag.IS_MULTI_THREAD_MODE, "true");
			put(ConfTag.IS_SEARCH_IO_CLASSES, "true");
			put(ConfTag.IS_SHOW_STATISTICS, "true");
			put(ConfTag.IS_LOG_INFERENCE_PROCESS, "true");

			put(ConfTag.REASONER_AMBIGUOUS_PROPAGATION, "true");
			put(ConfTag.REASONER_WELL_FOUNDED_SEMANTICS, "true");
		}
	};

	private static boolean isInitialized = false;

	// system properties
	protected static Properties props = null;

	protected static Map<String, WeakReference<AppLogger>> loggers = null;
	protected static Level logLevel = Level.INFO;

	private static ScriptEngine scriptEvaluationEngine = null;

	public static synchronized void initializeApplicationContext(Map<String, String> _args) {
		if (isInitialized) return;
		isInitialized = true;

		String libPath = System.getProperty("java.library.path") + ":.";
		System.setProperty("java.library.path", libPath);

		if (null == props) {
			if (null == System.getProperty(AppConst.SYSTEM_CONFIG_SHUTDOWN_HOOK)) {
				Runtime.getRuntime().addShutdownHook(new AppShutdownIntercepter());
			}
			System.setProperty(AppConst.SYSTEM_CONFIG_SHUTDOWN_HOOK, AppConst.SHUTDOWN_HOOK_ADDED);
			System.out.println(AppConst.APP_START_MESSAGE);
			System.out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_CONTEXT_INITIALIZE_START));

			try {
				Messages.getLocale();
				_loadConf(AppConst.CONF_FILE, _args);
				IOManager.initialize();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}

			// initialize the loggers
			loggers = new Hashtable<String, WeakReference<AppLogger>>();

			// do something
			System.out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_CONTEXT_INITIALIZE_END));
		}
	}

	public static void terminateApplicationContext() {
		ReasonerUtilities.printPerformanceStatistics(ReasonerMain.performanceStatistics);

		System.out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_CONTEXT_TERMINATE_START));

		if (null != props) props.clear();
		props = null;

		if (loggers != null) loggers.clear();
		loggers = null;

		isInitialized = false;

		System.out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_CONTEXT_TERMINATE_END));
	}

	public static Properties getSystemProperties() {
		return props;
	}

	public static String getLicense() throws IOException {
		return ResourcesUtils.loadResourceFileAsString(AppConst.APP_LICENSE_FILE);
	}

	public static boolean isInitialized() {
		return isInitialized;
	}

	public static ScriptEngine getScriptEngine() {
		if (!isInitialized) initializeApplicationContext(null);
		if (null == scriptEvaluationEngine) {
			ScriptEngineManager evaluationEngineManager = new ScriptEngineManager();
			String engineName = props.getProperty(ConfTag.THEORY_VARIABLE_BOOLEAN_EVALUATOR_ENGINE_NAME);
			scriptEvaluationEngine = evaluationEngineManager.getEngineByName(engineName);
		}
		return scriptEvaluationEngine;
	}

	public static String getConclusionExt() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getProperty(ConfTag.REASONER_CONCLUSION_DEFAULT_EXTENSION);
	}

	public static void setConclusionExt(final String defaultExtension) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.REASONER_CONCLUSION_DEFAULT_EXTENSION, defaultExtension);
	}

	public static boolean isShowProgress() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_SHOW_PROGRESS);
	}

	public static void setShowProgress(final boolean showProgress) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_SHOW_PROGRESS, Boolean.toString(showProgress));
	}

	public static boolean isShowStatistics() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_SHOW_STATISTICS);
	}

	public static void setShowStatistics(final boolean showStatistics) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_SHOW_STATISTICS, Boolean.toString(showStatistics));
	}

	public static boolean isShowResult() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_SHOW_RESULT);
	}

	public static void setShowResult(final boolean showResult) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_SHOW_RESULT, Boolean.toString(showResult));
	}

	public static boolean isSaveResult() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_SAVE_RESULT);
	}

	public static void setSaveResult(final boolean saveResult) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_SAVE_RESULT, Boolean.toString(saveResult));
	}

	public static boolean isConsoleMode() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_CONSOLE_MODE);
	}

	public static void setConsoleMode(final boolean consoleMode) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_CONSOLE_MODE, Boolean.toString(consoleMode));
	}

	public static long getMemoryMonitorTimeInterval() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsLong(ConfTag.APP_MEMORY_MONITOR_TIME_INTERVAL);
	}

	public static void setMemoryMonitorTimeInterval(final long memoryMonitorTimeInterval) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.APP_MEMORY_MONITOR_TIME_INTERVAL, Long.toString(memoryMonitorTimeInterval));
	}

	public static long getShowProgressTimeInterval() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsLong(ConfTag.APP_PROGRESS_TIME_INTERVAL);
	}

	public static void setShowProgressTimeInterval(final long showProgressTimeInterval) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.APP_PROGRESS_TIME_INTERVAL, Long.toString(showProgressTimeInterval));
	}

	public static boolean isReasoningWithAmbiguityPropagation() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.REASONER_AMBIGUOUS_PROPAGATION);
	}

	public static void setReasoningWithAmbiguityPropagation(boolean reasoningWithAmbiguityPropagation) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.REASONER_AMBIGUOUS_PROPAGATION, Boolean.toString(reasoningWithAmbiguityPropagation));
	}

	public static boolean isLogInferenceProcess() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_LOG_INFERENCE_PROCESS);
	}

	public static void setLogInferenceProcess(boolean logInferenceProcess) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_LOG_INFERENCE_PROCESS, Boolean.toString(logInferenceProcess));
	}

	public static boolean isReasoningWithWellFoundedSemantics() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.REASONER_WELL_FOUNDED_SEMANTICS);
	}

	public static void setReasoningWithWellFoundedSemantics(final boolean reasoningWithWellFoundedSemantics) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.REASONER_WELL_FOUNDED_SEMANTICS, Boolean.toString(reasoningWithWellFoundedSemantics));
	}

	public static String getTheoryAnalyser_stronglyConnectedComponentClassName() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getProperty(ConfTag.THEORY_ANALYSER_STRONGLY_COMPONENT_IMPL);
	}

	public static int getReasonerVersion() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsInteger(ConfTag.REASONER_VERSION);
	}

	public static void setReasonerVersion(int reasonerVersion) throws ConfigurationException {
		if (reasonerVersion < 0 || reasonerVersion > 2) throw new ConfigurationException("incorrect reasoner version");
		props.setProperty(ConfTag.REASONER_VERSION, "" + reasonerVersion);
	}

	public static void setReasonerVersion(final String reasonerVersion) throws ConfigurationException {
		int loc = reasonerVersion.indexOf(".");
		String ver = ((loc >= 0) ? reasonerVersion.substring(0, loc) : reasonerVersion).trim();
		if ("".equals(ver)) ver = "2";
		try {
			setReasonerVersion(Integer.parseInt(ver));
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	public static long getReasoningFileSleeptime() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsLong(ConfTag.APP_REASONING_SLEEPTIME);
	}

	/**
	 * check for if SPINdle is running in multi-thread mode.
	 * 
	 * @return True if SPINdle is running in multi-thread mode; false otherwise
	 */
	public static boolean isMultiThreadMode() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_MULTI_THREAD_MODE);
	}

	/**
	 * The SPINdle ReasoningEngineFactory will generate a new reasoning engine for every request if this is set to true.
	 * 
	 * @param isMultiThreadMode
	 * @see spindle.engine.ReasoningEngineFactory
	 */
	public static void setMultiThreadMode(boolean isMultiThreadMode) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_MULTI_THREAD_MODE, Boolean.toString(isMultiThreadMode));
	}

	// public static int getNoOfParallelReasoningThreads() {
	// if (!isInitialized) initializeApplicationContext(null);
	// return props.getPropertyAsInteger(ConfTag.REASONER_PARALLEL_REASONING_THREADS);
	// }
	//
	// public static void setNoOfParallelReasoningThreads(int noOfParallelReasoningThreads) {
	// if (!isInitialized) initializeApplicationContext(null);
	// props.setProperty(ConfTag.REASONER_PARALLEL_REASONING_THREADS, "" + noOfParallelReasoningThreads);
	// }

	public static boolean isReasonerGarbageCollection() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_GARBAGE_COLLECTION);
	}

	public static void setReasonerGarbageCollection(boolean isGarbageCollection) {
		if (!isInitialized) initializeApplicationContext(null);
		props.setProperty(ConfTag.IS_GARBAGE_COLLECTION, Boolean.toString(isGarbageCollection));
	}

	public static long getGarbageCollectionTimeInterval() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsLong(ConfTag.REASONER_GARBAGE_COLLECTION_TIME_INTERVAL);
	}

	public static boolean isSearchIOclasses() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getPropertyAsBoolean(ConfTag.IS_SEARCH_IO_CLASSES);
	}

	public static void setLogLevel(final Level newLogLevel) {
		if (!isInitialized) initializeApplicationContext(null);
		logLevel = newLogLevel;
	}

	public static Level getLogLevel() {
		if (!isInitialized) initializeApplicationContext(null);
		return logLevel;
	}

	public static String getLogFolder() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getProperty(ConfTag.LOG_FOLDER);
	}

	public static String getLogFilePrefix() {
		if (!isInitialized) initializeApplicationContext(null);
		return props.getProperty(ConfTag.REASONER_LOG_FILE_PREFIX) + "_";
	}

	public static String getLogFileExtension() {
		if (!isInitialized) initializeApplicationContext(null);
		String ext = props.getProperty(ConfTag.LOG_FILE_EXTENSION);
		return (ext.startsWith(".") ? ext : "." + ext);
	}

	public static AppLogger getLogger(String filename) {
		if (!isInitialized) initializeApplicationContext(null);

		if (null == filename || "".equals(filename.trim())) filename = "dummy.dfl";

		File f = new File(filename);
		String logFilename = f.getName();
		String fileExtension = FileManager.getFileExtension(logFilename);
		if (!"".equals(fileExtension)) logFilename = logFilename.substring(0,
				logFilename.length() - fileExtension.length() - 1);

		String modifiedLogFilename = getLogFilePrefix() + "[" + logFilename + "]"
				+ DateTime.getCurrentTimeAsFileTimestamp() + getLogFileExtension();
		File logFile = new File(getLogFolder(), modifiedLogFilename);

		AppLogger logger = new AppLoggerImpl(logFile);
		logger.setLogLevel(getLogLevel());
		loggers.put(logger.getLoggerName(), new WeakReference<AppLogger>(logger));

		return logger;
	}

	public static String getResultFolder() {
		if (!isInitialized) initializeApplicationContext(null);
		String folder = props.getProperty(ConfTag.APP_RESULT_FOLDER);
		return (folder.endsWith(FileManager.separator) ? folder : folder + FileManager.separator);
	}

	public static void addNewProperty(String propertyName, String propertyValue) throws ConfigurationException {
		if (null == propertyName || "".equals(propertyName.trim())) throw new ConfigurationException(
				"property name is null");
		String value = (null == propertyValue) ? "" : propertyValue;
		props.setProperty(propertyName.trim(), value);
	}

	private static <E> Map<String, String> updateProperties(Set<Entry<E, E>> updatedProperties) {
		Map<String, String> modifiedProperties = new TreeMap<String, String>();
		if (null == updatedProperties || updatedProperties.size() == 0) return modifiedProperties;
		for (Entry<E, E> entry : updatedProperties) {
			String key = entry.getKey().toString().trim();			
			if (!props.containsKey(key)) continue;
			
			String value = null == entry.getValue() ? "" : entry.getValue().toString().trim();
			if ("".equals(value) && DEFAULT_ARGUMENT_VALUE_CHANGE.containsKey(key)) {
				value = DEFAULT_ARGUMENT_VALUE_CHANGE.get(key);
			}
			props.setProperty(key, value);
			modifiedProperties.put(key, value);
		}
		return modifiedProperties;
	}

	private static void _loadConf(String confFilename, Map<String, String> args) throws ConfigurationException {
		System.out.println(AppConst.IDENTATOR + "load application configuration - start");
		Map<String, String> modifiedProperties = new TreeMap<String, String>();
		try {
			props = new Properties(ResourcesUtils.loadPropertiesFile(confFilename));

			// update the properties values using system properties and user arguments
			modifiedProperties.putAll(updateProperties(System.getProperties().entrySet()));
			if (null != args) modifiedProperties.putAll(updateProperties(args.entrySet()));
			if (modifiedProperties.size() > 0) {
				for (Entry<String, String> entry : modifiedProperties.entrySet()) {
					System.out.println(AppConst.IDENTATOR + AppConst.IDENTATOR + entry.getKey() + "="
							+ entry.getValue());
				}
			}
			

			setReasonerVersion(props.getProperty(ConfTag.REASONER_VERSION));

			String logLevelStr = props.getProperty(ConfTag.LOG_LEVEL);
			if (logLevelStr == null) {
				logLevelStr = props.getProperty(ConfTag.LOG_LEVEL, "INFO");
			}
			Level l = Utilities.getLogLevel(logLevelStr);
			if (l != null) logLevel = l;

			System.out.println(AppConst.IDENTATOR + "load application configuration - end");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConfigurationException("exception throw while configurating application context", e);
		}
	}
}
