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
package spindle.core;

import java.io.OutputStream;
import java.io.PrintStream;


/**
 * This class provides default implementations for the SPINdle reasoner message listener.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.1.2
 * @version Last modified 2012.08.06
 */
public class AbstractReasonerMessageListener implements ReasonerMessageListener {
	
	private PrintStream out;

	public AbstractReasonerMessageListener(OutputStream out) {
		this.out = null == out ? System.out : new PrintStream(out, true);
	}

	@Override
	public void onReasonerMessage(MessageType messageType, String message, Object... objects) {

		StringBuilder sb = new StringBuilder();

		switch (messageType) {
		case WARNING:
			sb.append("=== System warning: ");
			break;
		case INFO:
			sb.append("=== System info: ");
			break;
		case ERROR:
			sb.append("=== System error: ");
			break;
		}

		if (null != message && !"".equals(message.trim())) sb.append(message);

		if (null != objects && objects.length > 0) {
			for (int i = 0; i < objects.length; i++) {
				Object object = objects[i];
				if (null != object) {
					if (i > 0) sb.append("\n");
					sb.append(object.toString());
				}
			}
		}

		out.println(sb.toString());
	}
}
