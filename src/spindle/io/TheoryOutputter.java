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

import java.io.OutputStream;
import java.util.List;

import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.sys.AppModule;

/**
 * Interface for theory outputters.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public interface TheoryOutputter extends AppModule {
	/**
	 * return the outputter type
	 * 
	 * @return outputter type
	 */
	String getOutputterType();

	/**
	 * @param os <code>java.io.OutputStream</code>
	 * @param theory <code>Theory</code> theory to be save
	 * @throws OutputterException
	 */
	void save(OutputStream os, Theory theory) throws OutputterException;

	/**
	 * @param os <code>java.io.OutputStream</code>
	 * @param conclusionsAsList
	 * @throws OutputterException
	 */
	void save(OutputStream os, List<Conclusion> conclusionsAsList) throws OutputterException;

}
