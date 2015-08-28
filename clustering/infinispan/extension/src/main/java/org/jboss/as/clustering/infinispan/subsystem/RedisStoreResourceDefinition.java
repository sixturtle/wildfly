package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
//import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class RedisStoreResourceDefinition extends StoreResourceDefinition {
    static final PathElement LEGACY_PATH = PathElement.pathElement("redis-store", "REDIS_STORE");
    static final PathElement PATH = pathElement("redis");

    private enum Capability implements org.jboss.as.clustering.controller.Capability {
        OUTBOUND_SOCKET_BINDING("org.wildfly.clustering.infinispan.cache-container.cache.store.redis.outbound-socket-binding", OutboundSocketBinding.class),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> serviceType) {
            this.definition = RuntimeCapability.Builder.of(name, true).setServiceType(serviceType).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<Void> getRuntimeCapability(PathAddress address) {
            PathAddress cacheAddress = address.getParent();
            PathAddress containerAddress = cacheAddress.getParent();
            return this.definition.fromBaseCapability(containerAddress.getLastElement().getValue() + "." + cacheAddress.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SOCKET_TIMEOUT("socket-timeout", ModelType.INT, new ModelNode(60000)),
        SOCKET_BINDINGS("redis-servers")
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                .build();
        }

        Attribute(String name) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                .setAttributeParser(AttributeParsers.COLLECTION)
                .setCapabilityReference(new CapabilityReference(RequiredCapability.OUTBOUND_SOCKET_BINDING, Capability.OUTBOUND_SOCKET_BINDING))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMinSize(1)
                .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

        StoreResourceDefinition.buildTransformation(version, builder);
    }

    RedisStoreResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(PATH, WILDCARD_PATH), allowRuntimeOnlyRegistration);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class).addAttributes(StoreResourceDefinition.Attribute.class).addCapabilities(Capability.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(new RedisStoreBuilderFactory());
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerSubModel(this);

//        registration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration.registerSubModel(this)));
    }
}
