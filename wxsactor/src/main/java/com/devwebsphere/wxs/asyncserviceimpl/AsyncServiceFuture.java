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
package com.devwebsphere.wxs.asyncserviceimpl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.devwebsphere.wxs.asyncservice.SerializedFuture;
import com.devwebsphere.wxsutils.WXSUtils;
import com.devwebsphere.wxsutils.wxsmap.SessionPool;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;

/**
 * This is the implementation of our Future for blocking until a service message has been executed.
 * 
 * @author bnewport
 * 
 * @param <T>
 */
public class AsyncServiceFuture<T> implements java.util.concurrent.Future<T>, SerializedFuture<T> {
	private static final long serialVersionUID = 2578499599088815692L;
	static Logger logger = Logger.getLogger(AsyncServiceFuture.class.getName());
	transient ObjectGrid grid;

	// The message id
	RoutableKey id;
	// sanity check for inflation
	String objectgridName; // this is needed if we're serialized as grid is transient

	public AsyncServiceFuture(ObjectGrid g, RoutableKey id) {
		grid = g;
		objectgridName = g.getName();
		this.id = id;
	}

	public AsyncServiceFuture() {
	}

	/**
	 * This does a check to see if the message has been executed. It connects to the partition owning the message which
	 * is identified from the id (hash). It then checks the history/result table for the message result. If this is
	 * found then it returns the result otherwise null.
	 * 
	 * @return
	 * @throws ObjectGridException
	 */
	public T peekResult() throws ObjectGridException {
		boolean isSendable = true;
		Session sess = SessionPool.getPooledSession(grid);
		try {
			ObjectMap history = sess.getMap(MapNames.HISTORY_MAP);
			while (isSendable) {
				try {
					sess.begin();
					T rc = (T) history.get(id);
					return rc;
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Exception during peek result for " + id, e);
					isSendable = WXSUtils.isRetryable(e);
					if (!isSendable)
						throw new ObjectGridRuntimeException("Cannot send message ", e);
				} finally {
					try {
						if (sess.isTransactionActive())
							sess.rollback();
					} catch (ObjectGridException e2) {
						throw new ObjectGridRuntimeException(e2);
					}
				}
			}
		} finally {
			SessionPool.returnSession(sess);
		}

		return null;
	}

	public String toString() {
		return id.toString();
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	/**
	 * This blocks until the associated message is processed. It's pretty aggressive timing wise so it will burn a
	 * little CPU as a trade off for latency.
	 */
	public T get() throws InterruptedException, ExecutionException {
		if (logger.isLoggable(Level.FINER)) {
			logger.entering(this.toString(), "get");
		}

		try {
			int tries = 0;
			int maxRetries = 0;
			boolean messageReceived = false;
			while (!messageReceived) {
				T rc = peekResult();
				if (rc != null) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine("Receiving " + id);
						logger.fine("rc " + rc);
						logger.fine("rc className " + rc.getClass().getName());
					}
					return rc;
				}

				Thread.sleep(10);
				if (tries++ == 200) {
					tries = 0;
					logger.warning("Retrying more than 200 times for msg#" + id);
					maxRetries++;
				}

				if (maxRetries > 5) {
					logger.warning("Failed to receive " + id + " throwing exception");
					throw new ExecutionException("re-tries limit exceeed", null);
				}
			}

			return null;
		} catch (ObjectGridException e) {
			logger.log(Level.SEVERE, "Exception in get", e);
			throw new ExecutionException(e);
		} finally {
			if (logger.isLoggable(Level.FINER)) {
				logger.exiting(this.toString(), "get");
			}
		}
	}

	/**
	 * Ignores timeout for now.
	 */
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return get();
	}

	/**
	 * Cancel not supported for now.
	 */
	public boolean isCancelled() {
		return false;
	}

	/**
	 * Does a quick check to see if the message was processed. The history map has a value if its processed already.
	 */
	public boolean isDone() {
		try {
			boolean isSendable = true;
			Session sess = SessionPool.getPooledSession(grid);
			try {
				ObjectMap history = sess.getMap("IdHistory");
				while (isSendable) {
					try {
						T rc = (T) history.get(id);
						return (rc != null);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Exception during isDone for " + id, e);
						isSendable = WXSUtils.isRetryable(e);
						if (!isSendable)
							throw new ObjectGridRuntimeException("Cannot send message ", e);
					} finally {
						try {
							if (sess.isTransactionActive())
								sess.rollback();
						} catch (ObjectGridException e2) {
							throw new ObjectGridRuntimeException(e2);
						}
					}
				}
				return false;
			} finally {
				SessionPool.returnSession(sess);
			}
		} catch (ObjectGridException e) {
			logger.log(Level.SEVERE, "Exception during isDone for " + id, e);
			throw new ObjectGridRuntimeException(e);
		}
	}

	public ObjectGrid getGrid() {
		return grid;
	}

	public void setGrid(ObjectGrid grid) {
		this.grid = grid;
	}

	public String getObjectgridName() {
		return objectgridName;
	}

	public Future<T> inflate(ObjectGrid grid) {
		if (!grid.getName().equals(objectgridName))
			throw new IllegalArgumentException("Grid has different name than source");
		setGrid(grid);
		return this;
	}

	public RoutableKey getId() {
		return id;
	}
}
