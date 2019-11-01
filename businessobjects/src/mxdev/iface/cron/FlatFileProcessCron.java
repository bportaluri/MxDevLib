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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.rmi.RemoteException;

import psdi.app.system.CrontaskParamInfo;
import psdi.server.SimpleCronTask;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

/**
 * Maximo crontask class to schedule processing of a simple CSV file.
 * The FlatFileProcess Cron Task allows to perform few basic manipulation of CSV files to adapt
 * their format to a Maximo MIF-compliant structure.
 * It allows to:
 * - Reorder columns
 * - Skip rows from source files
 * - Insert one or to two headers to generate MIF file headers and rename column names
 * - Extract parts of the fields
 * - Parse dates and convert them to the correct format accepted by MIF
 */
public class FlatFileProcessCron extends SimpleCronTask
{
	public static final MXLogger LOGGER = MXLoggerFactory.getLogger("maximo.mxdev");

	private static final String[][] CRONPARAMETERS =
	{
		{ "INPUTDIR", null },
		{ "INPUTFILE", "*.csv" },
		{ "OUTPUTDIR", null },
		{ "BACKUPDIR", null },
		
		{ "HEADER1", null },
		{ "HEADER2", null },
		{ "SOURCECOLUMNS", null },
		
		{ "SKIPLINES", "0" },
		{ "TEXTQUALIFIER", "\"" },
		{ "DELIMITER", "," },
		{ "ESCAPEXML", "N" },
		{ "DATEFORMAT", "dd MMM yyyy" }
	};

	
	private FileFilter filter;
	private String inDir;
	private String outDir;
	private String bckDir;
	private boolean escapeXML;
	
	@Override
	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException
	{
		LOGGER.info("Entering CsvFileTransCron.getParameters()");
		
		CrontaskParamInfo[] params = new CrontaskParamInfo[CRONPARAMETERS.length];
		for (int i = 0; i < CRONPARAMETERS.length; i++)
		{
			params[i] = new CrontaskParamInfo();
			params[i].setName(CRONPARAMETERS[i][0]);
			params[i].setDefault(CRONPARAMETERS[i][1]);
		}
		
		return params;
	}


	@Override
	public void cronAction()
	{
		LOGGER.info("Entering FlatFileProcessCron.cronAction()");
		
		try
		{
			filter = new FlatFileProcessFilter(getParamAsString("INPUTFILE"));

			inDir = fixDir(getParamAsString("INPUTDIR"));
			outDir = fixDir(getParamAsString("OUTPUTDIR"));
			bckDir = fixDir(getParamAsString("BACKUPDIR"));
			
			if ("Y".equalsIgnoreCase(getParamAsString("ESCAPEXML")))
				escapeXML = true;
			else
				escapeXML = false;

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		synchronized ("FlatFileProcessCron")
		{
			if (isShutdown())
			{
				return;
			}
			
			if (!checkDirs())
			{
				return;
			}

			LOGGER.debug("Listing " + filter + " in folder " + inDir);
			
			File inputDir = new File(inDir);
			
	        File[] filesToProcess = inputDir.listFiles(filter);

	        if (filesToProcess.length==0)
			{
	        	LOGGER.debug("No files to process");
	        	return;
			}

	        
			FlatFileProcess csvTr = null;
			
			try
			{
				csvTr = new FlatFileProcess(getParamAsInt("SKIPLINES"), getParamAsString("HEADER1"), getParamAsString("HEADER2"), getParamAsString("SOURCECOLUMNS"), getParamAsString("DELIMITER"), getParamAsString("DATEFORMAT"), escapeXML);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return;
			}

	        
	        csvTr.init();
	        
	        for (int i = 0; i < filesToProcess.length; i++)
	        {
	        	
	        	try
				{
	        		File inFile = filesToProcess[i];
	        		
	        		File outFile = new File(outDir + inFile.getName());
	        		// on some systems the tempFile.renameTo(outFile) fails when using temp folder
	        		//File tempFile = File.createTempFile("FlatFileProcess", ".csv");
	        		File tempFile = new File(outDir + inFile.getName() + ".tmp");
	        		File bckFile = new File(bckDir + inFile.getName());

	        		if(outFile.exists())
	        			outFile.delete();
	        		if(bckFile.exists())
	        			bckFile.delete();
	        		
	        		csvTr.processCsvFile(inFile, tempFile);

	        		LOGGER.debug("Moving input file: " + inFile.getCanonicalPath() + " > " + bckFile.getCanonicalPath());
	    			boolean ret = inFile.renameTo(bckFile);
	    			
	    			if (!ret)
	    			{
	    				LOGGER.warn("Error moving input file: " + inFile.getCanonicalPath() + " > " + bckFile.getCanonicalPath());
	    				return;
	    			}
	    			
	    			LOGGER.debug("Moving output file: " + tempFile.getCanonicalPath() + " > " + outFile.getCanonicalPath());
	    			ret = tempFile.renameTo(outFile);
	    			
	    			if (!ret)
	    			{
	    				LOGGER.warn("Error moving output file: " + tempFile.getCanonicalPath() + " > " + outFile.getCanonicalPath());
	    				return;
	    			}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
	        }
			
		}
		
	}

		
	static private String fixDir(String dir)
	{
		if(dir.endsWith("\\") || dir.endsWith("/"))
			return dir;
		
		return dir = dir + "/";
	}
	
	private boolean checkDirs()
	{
		LOGGER.debug("Entering FlatFileProcessCron.checkDirs()");
		
		if(!(new File(inDir)).exists())
		{
			LOGGER.error("Input directory not found: " + inDir);
			return false;
		}
		if(!(new File(outDir)).exists())
		{
			LOGGER.error("Output directory not found: " + outDir);
			return false;
		}
		if(!(new File(bckDir)).exists())
		{
			LOGGER.error("Backup directory not found: " + bckDir);
			return false;
		}
		
		return true;
	}
}
