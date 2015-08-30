package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.controller.PathAddress;

/**
 * @author Simon Paulger
 */
public class RedisStoreBuilderFactory implements ResourceServiceBuilderFactory<PersistenceConfiguration> {
    @Override
    public ResourceServiceBuilder<PersistenceConfiguration> createBuilder(PathAddress address) {
        PathAddress cacheAddress = address.getParent();
        String containerName = cacheAddress.getParent().getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        return new RedisStoreBuilder(containerName, cacheName);
    }
}
