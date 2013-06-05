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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import spindle.sys.message.SystemMessage;

/**
 * Internationalization messages handler.
 * 
 * <pre>
 * using the system locale as the default appliation locale when applicatin start
 * </pre>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2012.07.27
 * @since version 1.0.0
 */
public class Messages {
	private static Locale currentLocale = null;

	private static ResourceBundle systemMessages = null;
	private static ResourceBundle errorMessages = null;

	static {
		try {
			setup();
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * set up the locale environment
	 * 
	 * @throws ConfigurationException
	 */
	public static void setup() throws ConfigurationException {
		currentLocale = Locale.getDefault();
		try {
			systemMessages = ResourceBundle.getBundle(AppConst.MESSAGE_FILE_SYSTEM, currentLocale);
			errorMessages = ResourceBundle.getBundle(AppConst.MESSAGE_FILE_ERROR, currentLocale);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	/**
	 * return the current locale as string
	 * 
	 * @return current locale as string
	 * @throws ConfigurationException
	 */
	public static String getLocale() throws ConfigurationException {
		if (null == systemMessages) setup();
		return systemMessages.getString(SystemMessage.SYSTEM_LOCALE);
	}

	/**
	 * return the system message
	 * 
	 * @param messageTag message tag
	 * @param args arguments
	 * @return formatted system message
	 */
	public static String getSystemMessage(final String messageTag, Object... args) {
		return getMessage(systemMessages, messageTag, args);
	}

	/**
	 * return the error message
	 * 
	 * @param errorTag message tag
	 * @param args arguments
	 * @return formatted error message
	 */
	public static String getErrorMessage(final String errorTag, Object... args) {
		return getMessage(errorMessages, errorTag, args);
	}

	
	/**
	 * Primitive method for {@link Messages#getSystemMessage(String, Object...)} and {@link Messages#getErrorMessage(String, Object...)}.
	 * @param resource Resource bundle.
	 * @param messageTag Message Tag.
	 * @param args Arguments.
	 * @return Message retrieved from resource bundle.
	 */
	private static String getMessage(ResourceBundle resource, String messageTag, Object... args) {
		if (!resource.containsKey(messageTag)) return messageTag;
		String message = resource.getString(messageTag);

		MessageFormat format = new MessageFormat("");
		format.setLocale(currentLocale);
		format.applyPattern(message);
		if (null == args || args.length == 0) return format.toPattern();

		String[] arguments = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			arguments[i] = null == args[i] ? "" : args[i].toString();
		}
		return format.format(arguments);
	}
}
