/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.service.concurrent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;

/**
 * @author Paul Ferraro
 */
public class StampedLockServiceExecutorTestCase {

    @Test
    public void test() {
        ServiceExecutor executor = new StampedLockServiceExecutor();

        Runnable executeTask = mock(Runnable.class);

        executor.execute(executeTask);

        // Task should run
        verify(executeTask).run();
        reset(executeTask);

        Runnable closeTask = mock(Runnable.class);

        executor.close(closeTask);

        verify(closeTask).run();
        reset(closeTask);

        executor.close(closeTask);

        // Close task should only run once
        verify(closeTask, never()).run();

        executor.execute(executeTask);

        // Task should no longer run since service is closed
        verify(executeTask, never()).run();
    }

    @Test
    public void concurrent() throws InterruptedException, ExecutionException {
        ServiceExecutor executor = new StampedLockServiceExecutor();

        ExecutorService service = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch executeLatch = new CountDownLatch(1);
            CountDownLatch stopLatch = new CountDownLatch(1);
            Runnable executeTask = () -> {
                try {
                    executeLatch.countDown();
                    stopLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            Future<?> executeFuture = service.submit(() -> executor.execute(executeTask));

            executeLatch.await();

            Runnable closeTask = mock(Runnable.class);

            Future<?> closeFuture = service.submit(() -> executor.close(closeTask));

            Thread.yield();

            // Verify that stop is blocked
            verify(closeTask, never()).run();

            stopLatch.countDown();

            executeFuture.get();
            closeFuture.get();

            // Verify close task was invoked, now that execute task is complete
            verify(closeTask).run();
        } finally {
            service.shutdownNow();
        }
    }
}
