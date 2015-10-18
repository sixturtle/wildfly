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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;

/**
 * {@link ServiceExecutor} implemented via a {@link StampedLock}.
 * @author Paul Ferraro
 */
public class StampedLockServiceExecutor implements ServiceExecutor {

    private final StampedLock lock = new StampedLock();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public void execute(Runnable executeTask) {
        long stamp = this.lock.tryReadLock();
        if (stamp != 0L) {
            try {
                executeTask.run();
            } finally {
                this.lock.unlock(stamp);
            }
        }
    }

    @Override
    public void close(Runnable closeTask) {
        // Allow only one thread to close
        if (this.closed.compareAndSet(false, true)) {
            // Closing is final - we don't need the stamp
            this.lock.writeLock();
            closeTask.run();
        }
    }
}
