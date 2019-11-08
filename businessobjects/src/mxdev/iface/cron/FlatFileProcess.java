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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FlatFileProcess
{
	int linesToSkip;
	String header1;
	String header2;
	String colDef;
	String separator;
	String dateFormat;
	boolean escapeXML;

	final static SimpleDateFormat dateOutFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // 2011-12-31T00:00:00
	final static String TYPE_CONST = "CONST";
	final static String TYPE_DATE = "DATE";
	
	
	SimpleDateFormat dateInFormat;
	
	int[] colsIdx;
	int[] colsSubstrStart;
	int[] colsSubstrEnd;
	String[] colsVal;
	String[] colsType;

	public FlatFileProcess(
			int linesToSkip,
			String header1,
			String header2,
			String colDef,
			String separator,
			String dateInFormat,
			boolean escapeXML)
	{
		this.linesToSkip = linesToSkip;
		this.header1 = header1;
		this.header2 = header2;
		this.colDef = colDef;
		this.separator = separator;
		this.dateFormat = dateInFormat;
		this.escapeXML = escapeXML;
	}
	
	public void init() throws RuntimeException
	{
		dateInFormat = new SimpleDateFormat(dateFormat);
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
	public void processCsvFile(File fIn, File fOut)
	{
		FlatFileProcessCron.LOGGER.info("Processing file: " + fIn + "  >  " + fOut);

		String inputLine = null;
		BufferedWriter writer = null;
		BufferedReader in = null;
		String outLine = null;
		int lineNum = linesToSkip;

		try
		{
			writer = new BufferedWriter(new FileWriter(fOut));
			in = new BufferedReader(new FileReader(fIn));
			for (int i = 0; i < linesToSkip; i++)
				inputLine = in.readLine();

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

			String[] lineCols = null;
			while ((lineCols = splitRow(in)) != null)
			{
				// check number of columns parsed to avoid null pointer errors
				if (lineCols.length<max(colsIdx))
				{
					FlatFileProcessCron.LOGGER.error("Too few columns in row " + inputLine);
				}
				
				outLine = "";
				
				for (int i=0; i<colsIdx.length; i++)
				{
					String val = "";
					
					if(TYPE_CONST.equals(colsType[i]))
					{
						val = colsVal[i];
					}
					else
					{
						val = lineCols[colsIdx[i]-1];
						
						if(!val.isEmpty())
						{
							if(colsSubstrEnd[i]!=0)
							{
								if(colsSubstrStart[i]>val.length())
									val = "";
								else if(colsSubstrEnd[i]>val.length())
									val = val.substring(colsSubstrStart[i]-1, val.length());
								else
									val = val.substring(colsSubstrStart[i]-1, colsSubstrEnd[i]);
							}
							
							if(TYPE_DATE.equals(colsType[i]))
								val = dateOutFormat.format(dateInFormat.parse(val));
						}
					}
					
					outLine = outLine + trimField(val) + ",";
				}
				
				outLine = outLine.substring(0, outLine.length()-1);
				FlatFileProcessCron.LOGGER.debug("Output:"+outLine);

				writer.write(outLine);
				writer.newLine();
				
				lineNum++;
			}
			
			FlatFileProcessCron.LOGGER.info("File processed succcessfully");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			FlatFileProcessCron.LOGGER.error("Error processing line " + lineNum + " >>>> " + inputLine);
		}
		finally
		{
			try
			{
				in.close();
				writer.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	// read row from input file and split
	private String[] splitRow(BufferedReader fIn) throws IOException
	{
		String inputLine = fIn.readLine();
		if (inputLine==null)
			return null;
		
		inputLine = inputLine.trim();
		
		// stops when an empty line is found
		if (inputLine.equals(""))
			return null;
		
		FlatFileProcessCron.LOGGER.debug("Input: "+inputLine);
		
		List<String> tokensList = new ArrayList<String>();
		boolean inQuotes = false;
		StringBuilder b = new StringBuilder();
		for (char c : inputLine.toCharArray()) {
		    switch (c) {
		    case ',':
		        if (inQuotes) {
		            b.append(c);
		        } else {
		            tokensList.add(b.toString());
		            b = new StringBuilder();
		        }
		        break;
		    case '\"':
		        inQuotes = !inQuotes;
		    default:
		        b.append(c);
		    break;
		    }
		}
		
		tokensList.add(b.toString());
		
		return tokensList.toArray(new String[0]);
	}


	private void parseCols(String colDef) throws RuntimeException
	{
		FlatFileProcessCron.LOGGER.debug("Parsing SOURCECOLUMNS: " + colDef);
		
		String[] cols = colDef.split(",");
		
		colsIdx = new int[cols.length];
		colsSubstrStart = new int[cols.length];
		colsSubstrEnd = new int[cols.length];
		colsVal = new String[cols.length];
		colsType = new String[cols.length];
		
		for (int i = 0; i < cols.length; i++)
		{
			//FlatFileProcessCron.LOGGER.debug(cols[i]);
			
			if (cols[i].equals("[CURRDATETIME]"))
			{
				colsType[i] = TYPE_CONST;
				colsVal[i] = dateOutFormat.format(new Date());
			}
			else if (cols[i].startsWith("[") && cols[i].endsWith("]"))
			{
				colsType[i]=TYPE_CONST;
				colsVal[i]=cols[i].substring(1, cols[i].length()-1);
			}
			else
			{
				String[] flds = cols[i].split("\\.");

				colsIdx[i] = Integer.parseInt(flds[0]);
				
				if(flds.length > 1)
				{
					String[] substr = flds[1].split("-");
					if(substr.length == 2)
					{
						colsSubstrStart[i] = Integer.parseInt(substr[0]);
						colsSubstrEnd[i] = Integer.parseInt(substr[1]);
					}
				}
				
				if(flds.length > 2)
				{
					if(flds[2].equalsIgnoreCase(TYPE_DATE))
						colsType[i] = TYPE_DATE;
					else
						throw new RuntimeException("Unable to detect data type " + flds[2]);
				}
				
			}
			FlatFileProcessCron.LOGGER.debug("Column:" + colsIdx[i] + " - start=" + colsSubstrStart[i] + " end=" + colsSubstrEnd[i] + " type=" + colsType[i]);

		}
	}


	private String trimField(String str)
	{
		// quote the string if it contains a space or separator and it's not already quoted
		if ((str.contains(" ") || str.contains(",")) && !(str.startsWith("\"") && str.endsWith("\"")))
			str = "\"" + str + "\"";

		if (escapeXML)
		{
			str = str.replace("&", "&amp;");
			//str.replace("""", "&quot;");
			str = str.replace("'", "&apos;");
			str = str.replace("<", "&lt;");
			str = str.replace(">", "&gt;");
		}

		return str;
	}
	
	static private int max(int[] arr)
	{
		int max = arr[0];
		for(int i=1; i<arr.length; i++)
			if (arr[i]>max)
				max = arr[i];
		
		return max;
	}
	
}
