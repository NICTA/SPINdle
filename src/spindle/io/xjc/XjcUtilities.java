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
package spindle.io.xjc;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import spindle.io.xjc.dom2.Conclusions;
import spindle.io.xjc.dom2.ObjectFactory;
import spindle.io.xjc.dom2.Rules;
import spindle.io.xjc.dom2.Theory;
import spindle.sys.AppConst;

public class XjcUtilities {

	private static JAXBContext jcTheory = null;
	private static JAXBContext jcConclusions = null;
	private static JAXBContext jcRule = null;

	private static Unmarshaller theoryUnmarshaller = null;
	private static Unmarshaller conclusionsUnmarshaller = null;
	private static Unmarshaller ruleUnmarshaller = null;

	private static Marshaller theoryMarshaller = null;
	private static Marshaller conclusionsMarshaller = null;
	private static Marshaller ruleMarshaller = null;

	private static ObjectFactory objectFactory = null;

	private static JAXBContext getTheoryJaxbContext() throws JAXBException {
		if (null == jcTheory) jcTheory = JAXBContext.newInstance(Theory.class);
		return jcTheory;
	}

	private static JAXBContext getConclusionsJaxbContext() throws JAXBException {
		if (null == jcConclusions) jcConclusions = JAXBContext.newInstance(Conclusions.class);
		return jcConclusions;
	}

	private static JAXBContext getRuleJaxbContext() throws JAXBException {
		if (null == jcRule) jcRule = JAXBContext.newInstance(Rules.class);
		return jcRule;
	}

	private static Schema getSchema() throws SAXException {
		final URL schemaUrl = ClassLoader.getSystemResource(AppConst.XML_SCHEMA_FILE);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		return schemaFactory.newSchema(schemaUrl);
	}

	public static Validator getSchemaValidator() throws SAXException {
		Schema schema = getSchema();
		Validator validator = schema.newValidator();
		validator.setErrorHandler(new ErrorHandler() {

			@Override
			public void error(SAXParseException cause) throws SAXException {
				cause.printStackTrace();
			}

			@Override
			public void fatalError(SAXParseException cause) throws SAXException {
				cause.printStackTrace();
			}

			@Override
			public void warning(SAXParseException cause) throws SAXException {
				cause.printStackTrace();
			}
		});
		return validator;
	}

	public static Unmarshaller getTheoryUnmarshaller() throws JAXBException {
		if (null == theoryUnmarshaller) theoryUnmarshaller = getTheoryJaxbContext().createUnmarshaller();
		return theoryUnmarshaller;
	}

	public static Unmarshaller getConclusionsUnmarshaller() throws JAXBException {
		if (null == conclusionsUnmarshaller) conclusionsUnmarshaller = getConclusionsJaxbContext().createUnmarshaller();
		return conclusionsUnmarshaller;
	}

	public static Unmarshaller getRuleUnmarshaller() throws JAXBException {
		if (null == ruleUnmarshaller) ruleUnmarshaller = getRuleJaxbContext().createUnmarshaller();
		return ruleUnmarshaller;
	}

	public static Marshaller getTheoryMarshaller() throws JAXBException {
		if (null == theoryMarshaller) {
			theoryMarshaller = getTheoryJaxbContext().createMarshaller();
			if (!AppConst.isDeploy) theoryMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		}
		return theoryMarshaller;
	}

	public static Marshaller getConclusionsMarshaller() throws JAXBException {
		if (null == conclusionsMarshaller) {
			conclusionsMarshaller = getConclusionsJaxbContext().createMarshaller();
			if (!AppConst.isDeploy) conclusionsMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		}
		return conclusionsMarshaller;
	}

	public static Marshaller getRuleMarshaller() throws JAXBException {
		if (null == ruleMarshaller) {
			ruleMarshaller = getRuleJaxbContext().createMarshaller();
			ruleMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		}
		return ruleMarshaller;
	}

	public static ObjectFactory getObjectFactory() {
		if (null == objectFactory) objectFactory = new ObjectFactory();
		return objectFactory;
	}

}
