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
package spindle.sys;

/**
 * Constants used in the application.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.08.01
 */
public interface AppConst {
	boolean isDeploy = true;
	
	int INITIAL_PENDING_CONCLUSIONS_QUEUE_CAPACITY=10;

	String IDENTATOR = "    ";
	String LIST_SYMBOL = AppConst.IDENTATOR + "+-- ";

	String ARGUMENT_PREFIX = "--";

	String APP_TITLE = "SPINdle";
	String APP_VERSION = "2.2.2";
	String APP_COPYRIGHT_MESSAGE = "Copyright (C) 2009-2013 NICTA Ltd.\n" //
			+ "This software and its documentation is distributed under the terms of the\n"//
			+ "FSF Lesser GNU Public License (LGPL).\n"//
			+ "\n"//
			+ "This program comes with ABSOLUTELY NO WARRANTY; This is a free software\n"//
			+ "and you are welcome to redistribute it under certain conditions; for\n"//
			+ "details type:\n" //
			+ "        java -jar spindle-<version>.jar --app.license";//

	String APP_USAGE = "Usage: java -jar spindle.jar [--options] [file1|dir1|url1] [file2|dir2|url2]...\n\n" //
			+ "where options include:\n" //
			+ ARGUMENT_PREFIX + ConfTag.APP_VERSION + "\t\t\tshow software version\n"//
			+ ARGUMENT_PREFIX + ConfTag.APP_LICENSE + "\t\t\tshow software license\n"//
			+ "\n" //
			+ ARGUMENT_PREFIX + ConfTag.USE_CONSOLE + "\t\t\trun in console mode\n" //
			+ "\n" //
			+ ARGUMENT_PREFIX + ConfTag.LOG_LEVEL + "\t\t\tlog level (ALL,INFO,FINE,FINEST,etc)\n" //
			+ ARGUMENT_PREFIX + ConfTag.IS_SHOW_PROGRESS + "\t\tshow progress time interval\n"//
			+ ARGUMENT_PREFIX + ConfTag.IS_SHOW_RESULT + "\t\tshow result on screen\n"//
			+ ARGUMENT_PREFIX + ConfTag.IS_SHOW_STATISTICS + "\t\tshow computational statistics\n"//
			+ ARGUMENT_PREFIX + ConfTag.APP_PROGRESS_TIME_INTERVAL + "\tshow progress time interval\n"//
			+ ARGUMENT_PREFIX + ConfTag.IS_SEARCH_IO_CLASSES + "\t\tsearch for I/O classes\n"//
			+ ARGUMENT_PREFIX + ConfTag.IS_SAVE_RESULT + "\t\tsave the conclusions derived\n"//
			+ ARGUMENT_PREFIX + ConfTag.APP_RESULT_FOLDER + "\t\tfolder for storing conclusions\n"//
			+ "\n" //
			+ ARGUMENT_PREFIX + ConfTag.REASONER_VERSION + "\t\treasoner version to be used (1 or 2)\n" //
			+ ARGUMENT_PREFIX + ConfTag.IS_LOG_INFERENCE_PROCESS + "\t\ttrue for log rule inference status while reasoning\n"//
			+ "\n" //
			+ ARGUMENT_PREFIX + ConfTag.REASONER_AMBIGUOUS_PROPAGATION + "\ttrue for ambiguit propagation support\n"//
			+ ARGUMENT_PREFIX + ConfTag.REASONER_WELL_FOUNDED_SEMANTICS + "\ttrue for well-founded semantics support\n"//
			// "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
	;

	String APP_START_MESSAGE = "=========================\n== application start!! ==\n=========================";

	String APP_END_MESSAGE = "=======================================\n=== Application shutdown completed! ===\n=======================================";

	int LOG_FILE_ID_LENGTH = 10;

	String CONF_FILE = "/spindle/resources/conf.properties";

	String IO_CONF_FILE = "/spindle/resources/io_conf.xml";

	String XML_SCHEMA_FILE="spindleDefeasibleTheory2.xsd";
	
	String MESSAGE_FILE_SYSTEM = "spindle.resources.SystemMessages";
	String MESSAGE_FILE_ERROR = "spindle.resources.ErrorMessages";

	String APP_LICENSE_FILE = "/LICENSE";

	String SYSTEM_CONFIG_SHUTDOWN_HOOK = "spindle.setShutdownHook";
	String SHUTDOWN_HOOK_ADDED = SYSTEM_CONFIG_SHUTDOWN_HOOK;
}
