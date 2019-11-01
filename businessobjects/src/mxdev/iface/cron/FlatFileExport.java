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

package mxdev.iface.cron;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.security.UserInfo;
import psdi.server.MXServer;

public class FlatFileExport
{
	UserInfo ui;
	String header1;
	String header2;
	String objectName;
	String whereClause;
	String colDef;
	String textQualifier;
	String separator;
	SimpleDateFormat dateFormat;


	final static String TYPE_CONST = "CONST";
	final static String TYPE_DATE = "DATE";
	
	
	SimpleDateFormat dateInFormat;
	
	String[] colsName;
	String[] colsType;
	String[] colsVal;
	
	public FlatFileExport(
			UserInfo ui,
			String header1,
			String header2,
			String objectName,
			String whereClause,
			String colDef,
			String textQualifier,
			String separator,
			String dateInFormat)
	{
		this.ui = ui;
		this.header1 = header1;
		this.header2 = header2;
		this.objectName = objectName;
		this.whereClause = whereClause;
		this.colDef = colDef;
		this.textQualifier = textQualifier;
		this.separator = separator;
		this.dateFormat = new SimpleDateFormat(dateInFormat);
	}
	
	
	public void init() throws RuntimeException
	{
		try
		{
			parseCols(colDef);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error parsing SOURCECOLUMNS", e);
		}
	}
	
	
	/**
	 * @throws IOException
	 */
	public void processFile(File fOut) throws Exception
	{
		FlatFileExportCron.LOGGER.info("Exporting data to file: " + fOut);

		BufferedWriter writer = new BufferedWriter(new FileWriter(fOut));
		
		try
		{
			MXServer mx = MXServer.getMXServer();
			
			MboSetRemote mboSet = mx.getMboSet(objectName, ui);
			
			if (whereClause != null && whereClause.length()>0)
				mboSet.setWhere(whereClause);
			
			if (header1!=null && header1.length()>0)
			{
				writer.write(header1);
				writer.newLine();
			}
	
			if (header2!=null && header2.length()>0)
			{
				writer.write(header2);
				writer.newLine();				
			}
			
			
			for (MboRemote mbo=mboSet.moveFirst(); mbo!=null; mbo=mboSet.moveNext())
			{
				String outLine = "";
				
				for (int i=0; i<colsName.length; i++)
				{
					FlatFileExportCron.LOGGER.debug("i=" + i);
					
					String val = "";
					
					if(TYPE_CONST.equals(colsType[i]))
					{
						val = colsVal[i];
					}
					else
					{
						if(TYPE_DATE.equals(colsType[i]))
						{
							val = dateFormat.format(mbo.getDate(colsName[i]));
						}
						else
						{
							val = mbo.getString(colsName[i]);
							
							if (val.contains(" ") || val.contains(separator))
								val = textQualifier + val + textQualifier;
						}

					}
					
					outLine = outLine + val + separator;

				}
				
				// remove last ','
				outLine = outLine.substring(0, outLine.length()-1);
				
				writer.write(outLine);
				writer.newLine();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Error reading file ", e);
		}
		finally
		{
			try
			{
				writer.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
	}


	private void parseCols(String colDef) throws RuntimeException
	{
		FlatFileExportCron.LOGGER.debug("Parsing SOURCECOLUMNS: " + colDef);
		
		String[] cols = colDef.split(",");
		
		colsName = new String[cols.length];
		colsVal = new String[cols.length];
		colsType = new String[cols.length];
		
		for (int i = 0; i < cols.length; i++)
		{

			FlatFileExportCron.LOGGER.debug(cols[i]);
			
			if (cols[i].equals("[CURRDATETIME]"))
			{
				colsType[i] = TYPE_CONST;
				colsVal[i] = dateFormat.format(new Date());
				
				FlatFileExportCron.LOGGER.debug("'" + colsVal[i] + "' - " + colsType[i]);
			}
			else if (cols[i].startsWith("[") && cols[i].endsWith("]"))
			{
				colsType[i] = TYPE_CONST;
				colsVal[i] = cols[i].substring(1, cols[i].length()-1);
				
				FlatFileExportCron.LOGGER.debug("'" + colsVal[i] + "' - " + colsType[i]);
			}
			else
			{
				String[] flds = cols[i].split("\\.");

				colsName[i] = flds[0];
				
				if(flds.length > 1)
				{
					if(flds[1].equalsIgnoreCase(TYPE_DATE))
						colsType[i] = TYPE_DATE;
					else
						throw new RuntimeException("Unable to detect data type " + flds[2]);
				}
				
				FlatFileExportCron.LOGGER.debug(colsName[i] + " - " + colsType[i]);
			}

		}
	}

}
