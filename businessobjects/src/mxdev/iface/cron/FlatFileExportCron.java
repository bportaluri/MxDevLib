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
import java.rmi.RemoteException;

import psdi.app.system.CrontaskParamInfo;
import psdi.server.SimpleCronTask;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

/**
 * Maximo crontask class to schedule the data export to a CSV file
 * See this page for details
 * http://maximodev.blogspot.it/2012/04/mif-schedule-file-export.html
 */
public class FlatFileExportCron extends SimpleCronTask
{
	public static final MXLogger LOGGER = MXLoggerFactory.getLogger("maximo.mxdev");

	private static final String[][] CRONPARAMETERS =
	{
		{ "OUTPUTDIR", null },
		{ "OUTPUTFILE", null },
		
		{ "HEADER1", null },
		{ "HEADER2", null },
		{ "OBJECTNAME", null },
		{ "WHERECLAUSE", null },
		{ "SOURCECOLUMNS", null },

		{ "TEXTQUALIFIER", "\"" },
		{ "DELIMITER", "," },
		{ "DATEFORMAT", "yyyy-MM-dd'T'HH:mm:ss" }
	};

	private String outDir;

	
	@Override
	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException
	{
		LOGGER.info("Entering FlatFileExportCron.getParameters()");
		
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
		LOGGER.info("Entering FlatFileExportCron.cronAction()");
		
		try
		{
			outDir = fixDir(getParamAsString("OUTPUTDIR"));
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
			
			if (!checkDir())
			{
				return;
			}


			try
			{
				FlatFileExport csvTr = new FlatFileExport(this.getRunasUserInfo(), getParamAsString("HEADER1"), getParamAsString("HEADER2"), getParamAsString("OBJECTNAME"), getParamAsString("WHERECLAUSE"), getParamAsString("SOURCECOLUMNS"), getParamAsString("TEXTQUALIFIER"), getParamAsString("DELIMITER"), getParamAsString("DATEFORMAT"));
		        csvTr.init();
		        csvTr.processFile(new File(outDir + getParamAsString("OUTPUTFILE")));
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return;
			}

			
		}
		
	}

		
	static private String fixDir(String dir)
	{
		if(dir.endsWith("\\") || dir.endsWith("/"))
			return dir;
		
		return dir = dir + "/";
	}


	private boolean checkDir()
	{
		LOGGER.debug("Entering FlatFileProcessCron.checkDirs()");
		
		if(!(new File(outDir)).exists())
		{
			LOGGER.error("Output directory not found: " + outDir);
			return false;
		}
		
		return true;
	}
}
