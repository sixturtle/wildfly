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

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
* Base test case for testing management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = JGroupsSchema.CURRENT.format("subsystem-jgroups-%d_%d.xml");

    public OperationTestCaseBase() {
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    protected static ModelNode getSubsystemAddOperation(String defaultStack) {
        ModelNode operation = Util.createAddOperation(getSubsystemAddress());
        operation.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getDefinition().getName()).set(defaultStack);
        return operation;
    }

    protected static ModelNode getSubsystemReadOperation(Attribute attribute) {
        return Operations.createReadAttributeOperation(getSubsystemAddress(), attribute);
    }

    protected static ModelNode getSubsystemWriteOperation(Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getSubsystemAddress(), attribute, new ModelNode(value));
    }

    protected static ModelNode getSubsystemRemoveOperation() {
        return Util.createRemoveOperation(getSubsystemAddress());
    }

    protected static ModelNode getProtocolStackAddOperation(String stackName) {
        return Util.createAddOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getProtocolStackAddOperationWithParameters(String stackName) {
        ModelNode[] operations = new ModelNode[] {
                getProtocolStackAddOperation(stackName),
                getTransportAddOperation(stackName, "UDP"),
                getProtocolAddOperation(stackName, "MPING"),
                getProtocolAddOperation(stackName, "pbcast.FLUSH"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getProtocolStackRemoveOperation(String stackName) {
        return Util.createRemoveOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getTransportAddOperation(String stackName, String protocol) {
        return Util.createAddOperation(getTransportAddress(stackName, protocol));
    }

    protected static ModelNode getTransportAddOperationWithProperties(String stackName, String type) {
        ModelNode[] operations = new ModelNode[] {
                getTransportAddOperation(stackName, type),
                getProtocolPropertyAddOperation(stackName, type, "A", "a"),
                getProtocolPropertyAddOperation(stackName, type, "B", "b"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getTransportRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getTransportAddress(stackName, type));
    }

    protected static ModelNode getTransportReadOperation(String stackName, String type, Attribute attribute) {
        return Operations.createReadAttributeOperation(getTransportAddress(stackName, type), attribute);
    }

    protected static ModelNode getTransportWriteOperation(String stackName, String type, Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getTransportAddress(stackName, type), attribute, new ModelNode(value));
    }

    protected static ModelNode getTransportPropertyAddOperation(String stackName, String type, String propertyName, String propertyValue) {
        ModelNode operation = Util.createAddOperation(getTransportPropertyAddress(stackName, type, propertyName));
        operation.get(PropertyResourceDefinition.VALUE.getName()).set(propertyValue);
        return operation;
    }

    protected static ModelNode getTransportPropertyRemoveOperation(String stackName, String type, String propertyName) {
        return Util.createRemoveOperation(getTransportPropertyAddress(stackName, type, propertyName));
    }

    protected static ModelNode getTransportPropertyReadOperation(String stackName, String type, String propertyName) {
        return Operations.createReadAttributeOperation(getTransportPropertyAddress(stackName, type, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE));
    }

    protected static ModelNode getTransportPropertyWriteOperation(String stackName, String type, String propertyName, String propertyValue) {
        return Operations.createWriteAttributeOperation(getTransportPropertyAddress(stackName, type, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE), new ModelNode(propertyValue));
    }

    // Transport property map operations
    protected static ModelNode getTransportGetPropertyOperation(String stackName, String type, String propertyName) {
        return Operations.createMapGetOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getTransportPutPropertyOperation(String stackName, String type, String propertyName, String propertyValue) {
        return Operations.createMapPutOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName, propertyValue);
    }

    protected static ModelNode getTransportRemovePropertyOperation(String stackName, String type, String propertyName) {
        return Operations.createMapRemoveOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getTransportClearPropertiesOperation(String stackName, String type) {
        return Operations.createMapClearOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES);
    }

    protected static ModelNode getTransportUndefinePropertiesOperation(String stackName, String type) {
        return Operations.createUndefineAttributeOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES);
    }

    /**
     * Creates operations such as /subsystem=jgroups/stack=tcp/transport=TCP/:write-attribute(name=properties,value={a=b,c=d})".
     *
     * @return resulting :write-attribute operation
     */
    protected static ModelNode getTransportSetPropertiesOperation(String stackName, String type, ModelNode values) {
        return Operations.createWriteAttributeOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES, values);
    }

    // Protocol operations
    protected static ModelNode getProtocolAddOperation(String stackName, String type) {
        return Util.createAddOperation(getProtocolAddress(stackName, type));
    }

    protected static ModelNode getProtocolAddOperationWithProperties(String stackName, String type) {
        ModelNode[] operations = new ModelNode[] {
                getProtocolAddOperation(stackName, type),
                getProtocolPropertyAddOperation(stackName, type, "A", "a"),
                getProtocolPropertyAddOperation(stackName, type, "B", "b"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getProtocolReadOperation(String stackName, String protocolName, Attribute attribute) {
        return Operations.createReadAttributeOperation(getProtocolAddress(stackName, protocolName), attribute);
    }

    protected static ModelNode getProtocolWriteOperation(String stackName, String protocolName, Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getProtocolAddress(stackName, protocolName), attribute, new ModelNode(value));
    }

    protected static ModelNode getProtocolPropertyAddOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        ModelNode operation = Util.createAddOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName));
        operation.get(PropertyResourceDefinition.VALUE.getName()).set(propertyValue);
        return operation;
    }

    protected static ModelNode getProtocolPropertyRemoveOperation(String stackName, String protocolName, String propertyName) {
        return Util.createRemoveOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName));
    }

    protected static ModelNode getProtocolPropertyReadOperation(String stackName, String protocolName, String propertyName) {
        return Operations.createReadAttributeOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE));
    }

    protected static ModelNode getProtocolPropertyWriteOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        return Operations.createWriteAttributeOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE), new ModelNode(propertyValue));
    }

    protected static ModelNode getProtocolGetPropertyOperation(String stackName, String protocolName, String propertyName) {
        return Operations.createMapGetOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getProtocolPutPropertyOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        return Operations.createMapPutOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName, propertyValue);
    }

    protected static ModelNode getProtocolRemovePropertyOperation(String stackName, String protocolName, String propertyName) {
        return Operations.createMapRemoveOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getProtocolClearPropertiesOperation(String stackName, String protocolName) {
        return Operations.createMapClearOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES);
    }

    protected static ModelNode getProtocolUndefinePropertiesOperation(String stackName, String protocolName) {
        return Operations.createUndefineAttributeOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES);
    }

    /**
     * Creates operations such as /subsystem=jgroups/stack=tcp/protocol=MPING/:write-attribute(name=properties,value={a=b,c=d})".
     */
    protected static ModelNode getProtocolSetPropertiesOperation(String stackName, String protocolName, ModelNode values) {
        return Operations.createWriteAttributeOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES, values);
    }

    protected static ModelNode getProtocolRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getProtocolAddress(stackName, type));
    }

    protected static PathAddress getSubsystemAddress() {
        return PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
    }

    protected static PathAddress getProtocolStackAddress(String stackName) {
        return getSubsystemAddress().append(StackResourceDefinition.pathElement(stackName));
    }

    protected static PathAddress getTransportAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(TransportResourceDefinition.pathElement(type));
    }

    protected static PathAddress getTransportPropertyAddress(String stackName, String type, String propertyName) {
        return getTransportAddress(stackName, type).append(PropertyResourceDefinition.pathElement(propertyName));
    }

    protected static PathAddress getProtocolAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(ProtocolResourceDefinition.pathElement(type));
    }

    protected static PathAddress getProtocolPropertyAddress(String stackName, String type, String propertyName) {
        return getProtocolAddress(stackName, type).append(PropertyResourceDefinition.pathElement(propertyName));
    }

    protected String getSubsystemXml() throws IOException {
        return readResource(SUBSYSTEM_XML_FILE) ;
    }

    protected KernelServices buildKernelServices() throws Exception {
        return createKernelServicesBuilder(new AdditionalInitialization().require(RequiredCapability.SOCKET_BINDING, "some-binding", "jgroups-diagnostics", "jgroups-mping", "jgroups-tcp-fd", "new-socket-binding")).setSubsystemXml(this.getSubsystemXml()).build();
    }

    protected List<ModelNode> executeOpInBothControllers(KernelServices services, ModelVersion version, ModelNode operation) throws Exception {
        List<ModelNode> results = new ArrayList<>(2);
        results.add(ModelTestUtils.checkOutcome(services.executeOperation(operation.clone())));
        results.add(ModelTestUtils.checkOutcome(services.executeOperation(version, services.transformOperation(version, operation.clone()))));
        return results;
    }

    /**
     * Executes a given operation asserting that an attachment has been created. Given {@link KernelServices} must have enabled attachment grabber.
     *
     * @return {@link ModelNode} result of the transformed operation
     */
    protected ModelNode executeOpInBothControllersWithAttachments(KernelServices services, ModelVersion version, ModelNode operation) throws Exception {
        OperationTransformer.TransformedOperation op = services.executeInMainAndGetTheTransformedOperation(operation, version);
        Assert.assertFalse(op.rejectOperation(success()));
        //System.out.println(operation + "\nbecomes\n" + op.getTransformedOperation());
        if (op.getTransformedOperation() != null) {
            return ModelTestUtils.checkOutcome(services.getLegacyServices(version).executeOperation(op.getTransformedOperation()));
        }
        return null;
    }

    private static ModelNode success() {
        final ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        return result;
    }

}