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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import spindle.core.dom.DomConst;
import spindle.sys.AppConst;
import spindle.sys.AppLogger;
import spindle.sys.Conf;

import com.app.utils.LogFormatter;
import com.app.utils.Utilities;

/**
 * Dummy class for logging application information.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public class AppLoggerImpl implements AppLogger {
	private static String logFolder = Conf.getLogFolder();

	private static final Logger generateLogger(File file) throws SecurityException, IOException {
		if (null == file) file = new File(logFolder, "dummy_" + Utilities.getRandomString(DomConst.LOG_FILE_ID_LENGTH));
		File logFolderF = file.getParentFile();
		if (!logFolderF.exists()) logFolderF.mkdirs();

		Logger logger = Logger.getLogger(file.toString());
		logger.setUseParentHandlers(false);

		FileHandler fileHandler = new FileHandler(file.getCanonicalPath());
		fileHandler.setFormatter(new LogFormatter());
		logger.addHandler(fileHandler);

		return logger;
	}

	private Logger logger = null;

	public AppLoggerImpl(File file) {
		try {
			if (null == logger) logger = generateLogger(file);
		} catch (Exception e) {
			logger = null;
			e.printStackTrace();
		}
	}

	@Override
	public String getLoggerName() {
		return logger.getName();
	}

	@Override
	public void setLogLevel(Level logLevel) {
		if (null == logger) return;
		logger.setLevel(logLevel);
	}

	@Override
	public void onLogMessage(Level logLevel, int indentLevel, String message, Object... objects) {
		if (null == logger || !logger.isLoggable(logLevel)) return;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indentLevel; i++) {
			sb.append(AppConst.IDENTATOR);
		}

		String msg = "";
		if (null != message) msg = message.trim();
		sb.append(msg);

		if (null != objects) {
			sb.append("".equals(msg) ? "" : ": ");
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				if (null != object) {
					if (i > 0) sb.append(",");
					sb.append(object.toString());
				}
			}
		}
		logger.log(logLevel, sb.toString());
	}

}
