package mxdev.webclient.beans.workorder;

import java.rmi.RemoteException;

import psdi.util.MXException;
import psdi.webclient.system.beans.DataBean;
import psdi.webclient.system.beans.WhereClauseBean;
import psdi.webclient.system.runtime.LoggingUtils;


public class SmartSearchAppBean extends WhereClauseBean
{
	public SmartSearchAppBean()
	{
		super();
	}

	public int execute() throws MXException, RemoteException
	{
		LoggingUtils.logUIMXmessages(LoggingUtils.MXLOGGER_INFO, "Entering SmartSearchAppBean.execute()");

		DataBean bean = this.app.getDataBean("ssearch_s1");
		
		String st = bean.getString("searchtext");
		boolean sh = bean.getBoolean("searchhist");
		
		String where =
			getW("description", st) + " or " +
			"exists (select 1 from longdescription where ldownertable='WORKORDER' and ldownercol='DESCRIPTION' and ldkey=workorder.workorderid and " + getW("ldtext", st) +")" + " or " +
			getW("location", st) + " or " +
			getW("assetnum", st) + " or " +
			"exists (select 1 from worklog where recordkey=workorder.wonum and class=workorder.woclass and siteid=workorder.siteid and " + getW("description", st) +")";
		
		if(!sh)
		{
			where = "(" + where + ") and historyflag = 0";
		}
		
		
		LoggingUtils.logUIMXmessages(LoggingUtils.MXLOGGER_INFO, "SmartSearch where clause: " + where);
		
		setWhere(where);
		
		super.execute();

		return EVENT_HANDLED;
	}
	
	private String getW(String field, String word)
	{
		String ret = "upper(" + field + ") like '%" + word.toUpperCase() + "%'";
		return ret;
	}

}
