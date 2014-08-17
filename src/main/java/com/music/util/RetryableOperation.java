/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.util;

import java.util.concurrent.Callable;

/**
 * Class that provides retrying functionality. Example:
 *
 * <code>
 * Callable<String> callable = new Callable<String>() {..};
 * String result = RetryableOperation.create(callable).retry(5, IOException.class);
 * </code>
 * @author bozho
 * @param <T> the return type of the operation
 */
public class RetryableOperation<T> {

    private Callable<T> callable;
    private Runnable runnable;
    private boolean exponentialBackoff;
    private int backoffInterval = 500;
    private ExceptionHandler<? super Exception> exceptionHandler;

    /**
     * Create a retryable operation based on a Callable instance. The return
     * type of retry(..) is the type parameter of the Callable instance.
     *
     * @param callable
     * @return a new instance of RetryableOperation
     */
    public static <T> RetryableOperation<T> create(Callable<T> callable) {
        return new RetryableOperation<T>().withCallable(callable);
    }

    /**
     * Creates a retryable operation based on a Runnable instance. In this case
     * the retry(..) method always returns null.
     *
     * @param runnable
     * @return a new instance of RetryableOperation
     */
    public static RetryableOperation<?> create(Runnable runnable) {
        return new RetryableOperation<Object>().withRunnable(runnable);
    }


    /**
     * Retries the operation. Retrying happens regardless of the exception thrown.
     *
     * @param retries number of retries before the exception is thrown to the caller
     * @return the result of the operation (null if Runnable is used instead of Callable)
     * @throws Exception the exception that occurred on the last attempt
     */
    public <E extends Exception> T retry(int retries) throws Exception {
        return retry(retries, Exception.class);
    }

    /**
     * @param retries
     *            number of retries before the exception is thrown to the caller
     * @param retryForException
     *            retry only if the exception thrown by the operation is of this
     *            type. Otherwise rethrow the exception immediately, wrapped in
     *            a RuntimeException
     * @return the result of the operation (null if Runnable is used instead of
     *         Callable)
     * @throws E
     *             the exception that is expected to cause the operation to
     *             retry. It is only if the last retry attempt fails
     */
    public <E extends Exception> T retry(int retries, Class<E> retryForException) throws E {
        if (callable == null && runnable == null) {
            throw new IllegalStateException("Either runnable or callable must be set");
        }
        for (int i = 0; i < retries; i++) {
            try {
                if (exponentialBackoff && i > 0) {
                    int sleepTime = (int) ((Math.pow(2, i) - 1) / 2) * backoffInterval;
                    Thread.sleep(sleepTime);
                }
                if (callable != null) {
                    return callable.call();
                } else if (runnable != null) {
                    runnable.run();
                    return null;
                }
            } catch (Exception e) {

                // if there is a registered exception handler, perform the exception handling
                // and optionally return, if the handler indicates that
                if (exceptionHandler != null
                        && exceptionHandler.getExceptionClass().isInstance(e)
                        && !exceptionHandler.handle(exceptionHandler.getExceptionClass().cast(e))) {
                    return null;
                }
                // if the exception is the expected type, do nothing (i.e. retry the next iteration)
                // if this is the last iteration, rethrow the exception
                if (retryForException.isInstance(e)) {
                    E ex = retryForException.cast(e);
                    if (i == retries - 1) {
                        throw ex;
                    }
                } else {
                    // if the exception is not the expected one, throw a runtime exception to the caller
                    throw new RuntimeException(e);
                }
            }
        }
        // can't be reached - in case of failure on the last iteration the exception is rethrown
        return null;
    }

    private RetryableOperation<T> withCallable(Callable<T> callable) {
        this.callable = callable;
        return this;
    }

    private RetryableOperation<T> withRunnable(Runnable runnable) {
        this.runnable = runnable;
        return this;
    }

    /**
     * Sets an optional exception handler to this retryable operation. The
     * handler can choose whether to proceed with retrying or stop immediately.
     *
     * @param exceptionHandler
     * @return the RetryableOperation instance
     */
    @SuppressWarnings("unchecked")
    public RetryableOperation<T> withExceptionHandler(ExceptionHandler<? extends Exception> exceptionHandler) {
        this.exceptionHandler = (ExceptionHandler<? super Exception>) exceptionHandler;
        return this;
    }

    public RetryableOperation<T> withExponentialBackoff() {
        this.exponentialBackoff = true;
        return this;
    }
    public RetryableOperation<T> withExponentialBackoff(int backoffInterval) {
        this.exponentialBackoff = true;
        this.backoffInterval = backoffInterval;
        return this;
    }
    /**
     * Interface to be implemented whenever custom code is to be executed when an exception occurs while retrying
     * @author bozho
     *
     * @param <E> the type of the exception to be handled by this handler
     */
    public static interface ExceptionHandler<E extends Exception>  {
        /**
         *
         * @param ex
         * @return true if retrying should proceed; false if it should stop immediately
         */
        boolean handle(E ex);

        /**
         *
         * @return the exception class which this handler is capable of handling
         */
        Class<E> getExceptionClass();
    }
}