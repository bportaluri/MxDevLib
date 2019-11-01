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

import java.rmi.RemoteException;
import psdi.app.system.CrontaskParamInfo;
import psdi.iface.mic.MicService;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.server.SimpleCronTask;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

/*
http://maximodev.blogspot.com/2012/04/mif-schedule-file-export.html
*/
public class RunPublishChannelCrontask extends SimpleCronTask
{
	protected static final MXLogger log = MXLoggerFactory.getLogger("maximo.mxdev");

	private MicService micSrv = null;
	private UserInfo userInfo;


	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException
	{
		CrontaskParamInfo[] params = new CrontaskParamInfo[4];
		params[0] = new CrontaskParamInfo();
		params[0].setName("PUBLISHCHANNEL");
		params[1] = new CrontaskParamInfo();
		params[1].setName("EXTERNALSYSTEM");
		params[2] = new CrontaskParamInfo();
		params[2].setName("WHERECLAUSE");
		params[3] = new CrontaskParamInfo();
		params[3].setName("MAXCOUNT");

		return params;
	}

	public void init() throws MXException
	{
		super.init();
		try
		{
			micSrv = ((MicService) MXServer.getMXServer().lookup("MIC"));
			userInfo = micSrv.getNewUserInfo();
			
			log.info("Initializing crontask:" + getName());
		}
		catch (Exception e)
		{
			log.error("Error initializing crontask:" + getName(), e);
		}
	}

	public void cronAction()
	{
		try
		{
			String publishChannel = getParamAsString("PUBLISHCHANNEL");
			String externalSys = getParamAsString("EXTERNALSYSTEM");
			String where = getParamAsString("WHERECLAUSE");
			int maxcount = getParamAsInt("MAXCOUNT");

			log.debug("Exporting publishChannel:" + publishChannel);
			log.debug("  External System:" + externalSys);
			log.debug("  Where:" + where);
			log.debug("  Maxcount:" + maxcount);

			micSrv.exportData(publishChannel, externalSys, where, userInfo, maxcount);
		}
		catch (Exception e)
		{
			log.error("Error in crontask:" + getName(), e);
		}
	}
}