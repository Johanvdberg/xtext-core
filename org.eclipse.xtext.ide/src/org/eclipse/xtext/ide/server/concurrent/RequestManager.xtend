/*******************************************************************************
 * Copyright (c) 2016 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ide.server.concurrent

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import org.eclipse.xtext.util.CancelIndicator
import org.apache.log4j.Logger
import org.eclipse.xtext.service.OperationCanceledManager
import java.util.concurrent.CompletionException

/**
 * @author kosyakov - Initial contribution and API
 * @since 2.11
 */
@Singleton
class RequestManager {
    
    static val LOGGER = Logger.getLogger(RequestManager)

	public static val READ_EXECUTOR_SERVICE = 'org.eclipse.xtext.ide.server.concurrent.RequestManager.readExecutorService'
	public static val WRITE_EXECUTOR_SERVICE = 'org.eclipse.xtext.ide.server.concurrent.RequestManager.writeExecutorService'

	val final MAX_PERMITS = Integer.MAX_VALUE

	@Inject
	@Named(READ_EXECUTOR_SERVICE)
	ExecutorService readExecutorService

	@Inject
	@Named(WRITE_EXECUTOR_SERVICE)
	ExecutorService writeExecutorService
	
	@Inject
	OperationCanceledManager operationCanceledManager

	val cancelIndicators = new LinkedBlockingQueue<CancellableIndicator>

	val semaphore = new Semaphore(MAX_PERMITS)

	def void shutdown() {
		readExecutorService.shutdown()
		writeExecutorService.shutdown()
	}

	def CompletableFuture<Void> runWrite((CancelIndicator)=>void writeRequest) {
		return runWrite(writeRequest, new RequestCancelIndicator)
	}

	/**
	 * <p>
	 * The given <i>write request</i> will be run first when <i>all running requests</i> completed.
	 * </p>
	 * <p>
	 * Currently <i>running requests</i> will be cancelled.
	 * </p>
	 * <p>
	 * A provided cancel indicator should implement {@link org.eclipse.xtext.ide.server.concurrent.CancellableIndicator CancellableIndicator} 
	 * to let the given request to be cancelled by a write request.
	 * </p>
	 */
	def CompletableFuture<Void> runWrite((CancelIndicator)=>void writeRequest, CancelIndicator cancelIndicator) {
		cancelIndicators.forEach[cancel]

		if (cancelIndicator instanceof CancellableIndicator)
			cancelIndicators += cancelIndicator

		semaphore.acquire(MAX_PERMITS)
		return CompletableFuture.runAsync([
			try {
				writeRequest.apply(cancelIndicator)
			} finally {
				semaphore.release(MAX_PERMITS)
			}
		], writeExecutorService).whenComplete [ result, throwable |
			if (cancelIndicator instanceof CancellableIndicator)
				cancelIndicators -= cancelIndicator
            
            handleError(throwable)
		]
	}

	def <V> CompletableFuture<V> runRead((CancelIndicator)=>V readRequest) {
		return runRead(readRequest, new RequestCancelIndicator)
	}

	/**
	 * <p>
	 * The given <i>read request</i> will be run:
	 * <ul>
	 * 	<li>concurrent with <i>running read requests</i>;</li>
	 * 	<li>first when <i>running write requests</i> completed.</li>
	 * </ul>
	 * </p>
	 * <p>
	 * A provided cancel indicator should implement {@link org.eclipse.xtext.ide.server.concurrent.CancellableIndicator CancellableIndicator} 
	 * to let the given request to be cancelled by a write request.
	 * </p>
	 */
	def <V> CompletableFuture<V> runRead((CancelIndicator)=>V readRequest, CancelIndicator cancelIndicator) {
		if (cancelIndicator instanceof CancellableIndicator)
			cancelIndicators += cancelIndicator

		semaphore.acquire(1)
		return CompletableFuture.supplyAsync([
			try {
				return readRequest.apply(cancelIndicator)
			} finally {
				semaphore.release(1)
			}
		], readExecutorService).whenComplete [ result, throwable |
			if (cancelIndicator instanceof CancellableIndicator)
				cancelIndicators -= cancelIndicator
            
            handleError(throwable)
		]
	}
	
	protected def void handleError(Throwable t) {
        if(t === null) return;
        val cause = if (t instanceof CompletionException) t.cause else t
        if (operationCanceledManager.isOperationCanceledException(cause)) {
            LOGGER.trace('Request has been canceled.')
        } else {
            LOGGER.error('Request fails: ' + t.message, t)
        }
    }

}
