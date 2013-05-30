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
package spindle.sys;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.app.utils.FileManager;

/**
 * Base class for application module.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public abstract class AppModuleBase implements AppModule {

	protected AppLogger logger = null;
	protected static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	public AppModuleBase() {
		super();
	}

	@Override
	public void setAppLogger(final AppLogger logger) {
		this.logger = logger;
	}

	@Override
	public String getLoggerName() {
		return logger.getLoggerName();
	}

	@Override
	public void resetAppLogger() {
		logger = null;
	}

	@Override
	public void setLogLevel(Level logLevel) {
		if (null == logger) return;
		logger.setLogLevel(logLevel);
	}

	protected void logMessage(Level logLevel, final int indentLevel, final String message, final Object... objects) {
		if (null == logger) return;
		logger.onLogMessage(logLevel, indentLevel, message, objects);
	}

	// ===================================
	// Application module listener - start
	// ===================================
	private List<AppModuleListener> listeners = new ArrayList<AppModuleListener>();

	protected void addAppModuleListener(AppModuleListener listener) {
		if (null == listener) return;
		listeners.add(listener);
	}

	protected void removeAppModuleListener(AppModuleListener listener) {
		if (null == listener) return;
		listeners.remove(listener);
	}

	protected List<AppModuleListener> getAppModuleListeners() {
		return listeners;
	}

	protected boolean hasAppModuleListeners() {
		return listeners.size() > 0;
	}

	// =================================
	// Application module listener - end
	// =================================
}
