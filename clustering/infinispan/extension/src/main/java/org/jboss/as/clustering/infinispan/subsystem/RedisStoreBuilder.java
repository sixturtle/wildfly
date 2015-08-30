package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.persistence.redis.configuration.RedisStoreConfigurationBuilder;
import org.jboss.as.clustering.controller.CapabilityDependency;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ValueDependency;

import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.as.clustering.infinispan.subsystem.RedisStoreResourceDefinition.Attribute.*;

/**
 * @author Simon Paulger
 */
public class RedisStoreBuilder extends StoreBuilder {
    private final List<ValueDependency<OutboundSocketBinding>> bindings = new LinkedList<>();
    private volatile RedisStoreConfigurationBuilder storeBuilder;

    public RedisStoreBuilder(String containerName, String cacheName) {
        super(containerName, cacheName);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        ServiceBuilder<PersistenceConfiguration> builder = super.build(target);
        for (Dependency dependency : this.bindings) {
            dependency.register(builder);
        }
        return builder;
    }

    @Override
    public PersistenceConfiguration getValue() {
        for (Value<OutboundSocketBinding> value : this.bindings) {
            OutboundSocketBinding binding = value.getValue();
            try {
                this.storeBuilder.addServer().host(binding.getResolvedDestinationAddress().getHostAddress()).port(binding.getDestinationPort());
            } catch (UnknownHostException e) {
                throw InfinispanLogger.ROOT_LOGGER.failedToInjectSocketBinding(e, binding);
            }
        }
        return super.getValue();
    }

    @Override
    StoreConfigurationBuilder<?, ?> createStore(OperationContext context, ModelNode model) throws OperationFailedException {
        this.storeBuilder = new ConfigurationBuilder().persistence().addStore(RedisStoreConfigurationBuilder.class)
            .socketTimeout(SOCKET_TIMEOUT.getDefinition().resolveModelAttribute(context, model).asInt())
        ;
        for (String binding : StringListAttributeDefinition.unwrapValue(context, SOCKET_BINDINGS.getDefinition().resolveModelAttribute(context, model))) {
            this.bindings.add(new CapabilityDependency<>(context, RequiredCapability.OUTBOUND_SOCKET_BINDING, binding, OutboundSocketBinding.class));
        }
        return this.storeBuilder;
    }
}
