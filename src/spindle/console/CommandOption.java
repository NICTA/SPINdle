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
package spindle.console;

import com.app.utils.FileManager;
import com.app.utils.TextUtilities;

import spindle.sys.AppConst;

public class CommandOption implements Comparable<Object> {
	protected static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	private String name = null;
	private String args = null;
	private String description = null;

	public CommandOption(final String name, final String args, final String description) {
		setName(name);
		setArgs(args);
		setDescription(description);
	}

	public void setArgs(final String args) {
		this.args = (null == args) ? "" : args.trim();
	}

	public String getArgs() {
		return args;
	}

	public String getName() {
		return name;
	}

	protected void setName(final String name) {
		this.name = (null == name) ? "" : name.trim();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = (null == description) ? "" : description.trim();
	}

	public String getOptionWithArgs() {
		return name + ("".equals(name) && !"".equals(args) ? "" : " ") + args;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(name);
		if (!"".equals(description)) {
			String[] desc = TextUtilities.trimTextWithWidth(description, 80 - AppConst.IDENTATOR.length()).split(LINE_SEPARATOR);
			for (String d : desc) {
				sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append(d);
			}
		}
		return sb.toString();
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (o instanceof CommandOption) {
			CommandOption opt = (CommandOption) o;
			int c = name.compareTo(opt.name);
			if (c != 0) return c;
			return args.compareTo(opt.args);
		}
		return toString().compareTo(o.toString());
	}

}
