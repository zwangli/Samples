//
//This sample program is provided AS IS and may be used, executed, copied and
//modified without royalty payment by customer (a) for its own instruction and
//study, (b) in order to develop applications designed to run with an IBM
//WebSphere product, either for customer's own internal use or for redistribution
//by customer, as part of such an application, in customer's own products. "
//
//5724-J34 (C) COPYRIGHT International Business Machines Corp. 2009
//All Rights Reserved * Licensed Materials - Property of IBM
//
package com.devwebsphere.wxsutils.wxsmap;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.devwebsphere.wxsutils.WXSUtils;
import com.devwebsphere.wxsutils.jmx.agent.AgentMBeanImpl;
import com.devwebsphere.wxsutils.wxsmap.BigListHead.LR;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.datagrid.MapGridAgent;

/**
 * This agent is used to pop N items at most from a specified list. If less than N items
 * are on the list then we return as many as possible.
 * @author ibm
 *
 * @param <K>
 * @param <V>
 */
public class BigListRemoveNItemsAgent<K extends Serializable, V extends Serializable> implements MapGridAgent 
{
	static Logger logger = Logger.getLogger(BigListRemoveNItemsAgent.class.getName());
	/**
	 * 
	 */
	private static final long serialVersionUID = -3820884829092397741L;

	public K dirtyKey;
	public LR isLeft;
	public int numItems;
	public boolean releaseLease;
	
	
	static public <K extends Serializable, V extends Serializable> int removeNItems(Session sess, ObjectMap map, Object key, LR isLeft, int numItems, K dirtyKey, boolean releaseLease)
	{
		AgentMBeanImpl mbean = WXSUtils.getAgentMBeanManager().getBean(sess.getObjectGrid().getName(), BigListRemoveNItemsAgent.class.getName());
		long startNS = System.nanoTime();
		try
		{
			ObjectMap dirtyMap = null;
			// lock dirtymap first to avoid dead locks
			if(dirtyKey != null)
			{
				dirtyMap = sess.getMap(BigListPushAgent.getDirtySetMapNameForListMap(map.getName()));
				dirtyMap.getForUpdate(dirtyKey);
			}
			int numRemoved = 0;
			BigListHead<V> head = (BigListHead<V>)map.getForUpdate(key);
			if(head != null)
				numRemoved = head.removeNItems(sess, map, key, isLeft, numItems, dirtyKey, releaseLease);
			mbean.getKeysMetric().logTime(System.nanoTime() - startNS);
			return numRemoved;
		}
		catch(ObjectGridException e)
		{
			logger.log(Level.SEVERE, "Exception", e);
			mbean.getKeysMetric().logException(e);
			throw new ObjectGridRuntimeException(e);
		}
	}
	/**
	 * 
	 */
	public Object process(Session sess, ObjectMap map, Object key) 
	{
		return new Integer(removeNItems(sess, map, key, isLeft, numItems, dirtyKey, releaseLease));
	}

	public Map processAllEntries(Session arg0, ObjectMap arg1) {
		// TODO Auto-generated method stub
		return null;
	}

}