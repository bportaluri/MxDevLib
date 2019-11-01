/*
 * Copyright (c) 2017 Bruno Portaluri (MaximoDev)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mxdev.common.commlog;

import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetInfo;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValue;
import psdi.server.MXServer;
import psdi.server.MaxPropCache;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

/*strings in the email template that will be replaced by the Java class with lists of data retrieved from db */
public class FldCommLogTmpltId extends psdi.common.commlog.FldCommLogTmpltId
{
	private static MXLogger logger = MXLoggerFactory.getLogger("maximo.mxdev");
	
	public FldCommLogTmpltId(MboValue mbv) throws MXException
	{
		super(mbv);
	}

	public void action() throws MXException, RemoteException
	{
		logger.debug("Entering mxdev.common.commlog.FldCommLogTmpltId.action method");
		
		super.action();
		
		MboRemote mbo = getMboValue().getMbo();	// current CommLog object
		String message = mbo.getString("message");
		message = searchCustomField(message);
		mbo.setValue("message", message, NOACCESSCHECK);
	}

	
	// method used to search the custom text strings
	// Uses regular expression - https://regex101.com/
	
	private String searchCustomField(String message) throws RemoteException, MXException
	{
		// Retrieve system properties for special characters

		MaxPropCache properties = (MaxPropCache)MXServer.getMXServer().getFromMaximoCache("MAXPROP");
		String custChar1 = properties.getProperty("mxdev.commtmpl.texttable.tag");
		String custChar2 = properties.getProperty("mxdev.commtmpl.htmltable.tag");

		// regex used to find occurrences of the string starting with the character 1
		Pattern p = Pattern.compile(custChar1 + ":(\\w*[.\\w]*)");
		Matcher m = p.matcher(message);

		StringBuffer result = new StringBuffer();
		while (m.find())
		{
			logger.debug("Found string " + m.group());
			String valueField = searchValue(m.group(1), 1);
			m.appendReplacement(result, valueField);
		}
		m.appendTail(result);
		String parsmessage1 = result.toString();
		//logger.debug("parsmessage1=" + parsmessage1);

		// the regex searches the '%HTMLTABLE%' prefix and then fields separated by '.' char
		// %HTMLTABLE%:(\w*[.\w]*)

		p = Pattern.compile(custChar2 + ":(\\w*[.\\w]*)");
		m = p.matcher(parsmessage1);

		// Empty the buffer
		result.setLength(0);

		while (m.find())
		{
			logger.debug("Found string " + m.group());
			String valueField = searchValue(m.group(1), 2);
			
			// FIX the '$' character generates java.lang.IllegalArgumentException: Illegal group reference
			// https://stackoverflow.com/questions/11913709/why-does-replaceall-fail-with-illegal-group-reference
			m.appendReplacement(result, Matcher.quoteReplacement(valueField));
			//m.appendReplacement(result, valueField);
		}
		m.appendTail(result);
		
		String ret = result.toString();
		logger.debug("ret=" + ret);
		
		return ret;
	}

	// Search the relationship name and the table columns to build the required query
	private String searchValue(String value, int charcase) throws RemoteException, MXException
	{
		// search the number of points in the string
		String[] output = value.split("\\.");

		
		logger.debug("output: " + output.toString());
		
		String resultString = "";
		
		// charcase=1 corresponds to cust.commtmpl.char1
		// charcase=2 corresponds to cust.commtmpl.char2
		if (charcase == 1)
		{
			resultString = makeQueryString(output);
		}
		else if (charcase == 2)
		{
			resultString = makeQueryTable(output);
		}


		return resultString;
	}

	// method used to make database queries, and save the string in tabular format
	private String makeQueryTable(String[] queryData) throws RemoteException, MXException
	{
		MboRemote owner = getMboValue().getMbo().getOwner();
		String mboName = owner.getName();
		MboSetRemote relationshipMboSet = owner.getMboSet(queryData[0]);
		logger.debug(relationshipMboSet.getName() + " related to " + mboName);

		MaxPropCache properties = (MaxPropCache)MXServer.getMXServer().getFromMaximoCache("MAXPROP");
		String tbstyle = properties.getProperty("mxdev.commtmpl.htmltable.tbstyle");
		String trstyle = properties.getProperty("mxdev.commtmpl.htmltable.trstyle");
		String thstyle = properties.getProperty("mxdev.commtmpl.htmltable.thstyle");
		String tdstyle = properties.getProperty("mxdev.commtmpl.htmltable.tdstyle");

		String resultsQuery = "";

		// HTML table starts here
		resultsQuery += "<table " + tbstyle + ">\n";
		
		// table header
		resultsQuery += "<tr " + trstyle + ">\n";
		
		MboSetInfo mboSetInfo = relationshipMboSet.getMboSetInfo();
		for (int v = 1; v < queryData.length; v++)
		{
			String fieldDesc = mboSetInfo.getAttribute(queryData[v]).getTitle();
			resultsQuery += "<th " + thstyle + ">" + fieldDesc + "</th>\n";
		}
		resultsQuery += "</tr>\n";

		// table rows
		if (relationshipMboSet.isEmpty())
			logger.debug("Empty set for relationship");
		
		MboRemote relationshipMbo = relationshipMboSet.moveFirst();
		while (relationshipMbo != null)
		{
			resultsQuery += "<tr " + trstyle + ">";
			for (int v = 1; v < queryData.length; v++)
			{
				String val = relationshipMbo.getString(queryData[v]);
				// converts newlines for non-richtext long descriptions
				if (!val.endsWith("<!-- RICH TEXT -->"))
					val = val.replace("\n", "<br>");
				resultsQuery += "<td " + tdstyle + ">" + val + "</td>\n";
			}
			resultsQuery += "</tr>\n";
			relationshipMbo = relationshipMboSet.moveNext();
		}

		resultsQuery += "</table>\n";
		logger.debug(resultsQuery);

		return resultsQuery;
	}

	// method used to make database queries, and save the string in linear
	// format
	private String makeQueryString(String[] queryData) throws RemoteException, MXException
	{
		MboRemote owner = getMboValue().getMbo().getOwner();
		String mboName = owner.getName();
		MboSetRemote relationshipMboSet = owner.getMboSet(queryData[0]);
		logger.debug(relationshipMboSet.getName() + " related to " + mboName);

		String resultsQuery = "";

		MboRemote relationshipMbo = relationshipMboSet.moveFirst();
		while (relationshipMbo != null)
		{
			for (int v = 1; v < queryData.length; v++)
			{
				resultsQuery += queryData[v] + ": " + relationshipMbo.getString(queryData[v]);
				if (v != queryData.length - 1)
				{
					resultsQuery += ", ";
				}
			}
			relationshipMbo = relationshipMboSet.moveNext();
			if (relationshipMbo == null)
				resultsQuery += ".\n";
			else
				resultsQuery += ";\n";
		}
		
		logger.debug(resultsQuery);

		return resultsQuery;
	}

}
