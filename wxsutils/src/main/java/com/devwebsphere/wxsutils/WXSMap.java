//
//This sample program is provided AS IS and may be used, executed, copied and
//modified without royalty payment by customer (a) for its own instruction and 
//study, (b) in order to develop applications designed to run with an IBM 
//WebSphere product, either for customer's own internal use or for redistribution 
//by customer, as part of such an application, in customer's own products. "
//
//5724-J34 (C) COPYRIGHT International Business Machines Corp. 2005
//All Rights Reserved * Licensed Materials - Property of IBM
//
package com.devwebsphere.wxsutils;

import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import com.devwebsphere.wxsutils.jmx.wxsmap.WXSMapMBeanImpl;
import com.ibm.websphere.objectgrid.BackingMap;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;

/**
 * This is a simplified interface to a WXS Map. It throws runtime exceptions and is completely
 * thread safe. It doesn't use transactions. It provides implementations for commonly used
 * Map methods like put, bulk methods and so on.
 *
 */
public class WXSMap <K,V>
{
	BackingMap bmap;
	ThreadLocalSession tls;
	WXSUtils utils;
	String mapName;
	ObjectGrid grid;
	
	WXSMap(WXSUtils utils, String mapName)
	{
		this.mapName = mapName;
		this.utils = utils;
		grid = utils.getObjectGrid();
		bmap = utils.grid.getMap(mapName);
		tls = new ThreadLocalSession(utils);
	}

	/**
	 * Clear the Map
	 */
	public void clear()
	{
		try
		{
			tls.getMap(mapName).clear();
		}
		catch(Exception e)
		{
			throw new ObjectGridRuntimeException(e);
		}
	}

	/**
	 * Fetch a value from the Map
	 * @param k
	 * @return
	 */
	public V get(K k)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		try
		{
			V rc = (V)tls.getMap(mapName).get(k);
			mbean.getGetMetrics().logTime(System.nanoTime() - start);
			return rc;
		}
		catch(Exception e)
		{
			mbean.getGetMetrics().logException(e);
			throw new ObjectGridRuntimeException(e);
		}
	}

	/**
	 * Fetch all the values for the specified keys. Null is returned if the key
	 * isn't found.
	 * @param keys
	 * @return
	 */
	public Map<K,V> getAll(Collection<K> keys)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		Map<K,V> rc =  utils.getAll(keys, bmap);
		mbean.getGetMetrics().logTime(System.nanoTime() - start);
		return rc;
	}

	/**
	 * Set the value for the key. If the entry doesn't exist then it
	 * is inserted otherwise it's updated.
	 * @param k
	 * @param v
	 */
	public void put(K k, V v)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		try
		{
			InsertAgent<K, V> a = new InsertAgent<K, V>();
			a.doGet = true;
			a.batch = new Hashtable<K, V>();
			a.batch.put(k, v);
			tls.getMap(mapName).getAgentManager().callReduceAgent(a, Collections.singletonList(k));
			mbean.getPutMetrics().logTime(System.nanoTime() - start);
		}
		catch(Exception e)
		{
			mbean.getPutMetrics().logException(e);
			throw new ObjectGridRuntimeException(e);
		}
	}

	/**
	 * This does a single entry insert. If the key already exists then an exception is thrown.
	 * @param k
	 * @param v
	 */
	public void insert(K k, V v)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		try
		{
			tls.getMap(mapName).insert(k, v);
			mbean.getInsertMetrics().logTime(System.nanoTime() - start);
		}
		catch(Exception e)
		{
			mbean.getInsertMetrics().logException(e);
			throw new ObjectGridRuntimeException(e);
		}
	}

	/**
	 * Parallel put all the entries.
	 * @param batch
	 */
	public void putAll(Map<K,V> batch)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		utils.putAll(batch, bmap);
		mbean.getPutMetrics().logTime(System.nanoTime() - start);
	}

	/**
	 * Parallel insert all the entries. This does a real insert, not a put (get/update)
	 * @param batch
	 */
	public void insertAll(Map<K,V> batch)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		utils.insertAll(batch, bmap);
		mbean.getInsertMetrics().logTime(System.nanoTime() - start);
	}
	
	/**
	 * Remove the entry from the Map
	 * @param k
	 * @return The last value otherwise null
	 */
	public V remove(K k)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		try
		{
			V rc = (V)tls.getMap(mapName).remove(k);
			mbean.getRemoveMetrics().logTime(System.nanoTime() - start);
			return rc;
		}
		catch(Exception e)
		{
			mbean.getRemoveMetrics().logException(e);
			throw new ObjectGridRuntimeException(e);
		}
	}

	/**
	 * Remove all entries with these keys
	 * @param keys
	 */
	public void removeAll(Collection<K> keys)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		utils.removeAll(keys, bmap);
		mbean.getRemoveMetrics().logTime(System.nanoTime() - start);
	}

	/**
	 * Check if the entry exists for the key
	 * @param k
	 * @return
	 */
	public boolean contains(K k)
	{
		WXSMapMBeanImpl mbean = WXSUtils.getWXSMapMBeanManager().getBean(grid, mapName);
		long start = System.nanoTime();
		try
		{
			boolean rc = tls.getMap(mapName).containsKey(k);
			mbean.getContainsMetrics().logTime(System.nanoTime() - start);
			return rc;
		}
		catch(Exception e)
		{
			mbean.getContainsMetrics().logException(e);
			throw new ObjectGridRuntimeException(e);
		}
	}
}