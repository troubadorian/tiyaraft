/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.broker.impl;

import org.apache.axis.components.uuid.UUIDGen;
import org.apache.axis.components.uuid.UUIDGenFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceIdentifier;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourceProperty;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.impl.ReflectionResourceProperty;
import org.globus.wsrf.impl.SimpleResourcePropertySet;
import org.apache.axis.message.addressing.EndpointReferenceType;

public class FileOrderResource implements Resource, ResourceIdentifier,
		ResourceProperties {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileOrderResource.class);

	/* Resource Property set */
	private ResourcePropertySet propSet;

	/* Resource key. This uniquely identifies this resource. */
	private Object key;

	/* UUID generator to generate unique resource key */
	private static final UUIDGen uuidGen = UUIDGenFactory.getUUIDGen();

	/* Resource properties */
	private String orderStatus;
	private String name;
	private EndpointReferenceType fileEPR;

	/* Initializes RPs and returns a unique identifier for this resource */
	public Object initialize(String name, EndpointReferenceType fileEPR)
			throws Exception {
		this.key = uuidGen.nextUUID();
		this.propSet = new SimpleResourcePropertySet(
				FileBrokerConstants.RESOURCE_PROPERTIES);

		ResourceProperty orderStatusRP = new ReflectionResourceProperty(
				FileBrokerConstants.RP_ORDERSTATUS, "OrderStatus", this);
		this.propSet.add(orderStatusRP);
		setOrderStatus("NEW");

		ResourceProperty nameRP = new ReflectionResourceProperty(
				FileBrokerConstants.RP_NAME, "Name", this);
		this.propSet.add(nameRP);
		setName(name);
		
		ResourceProperty fileEPR_RP = new ReflectionResourceProperty(
				FileBrokerConstants.RP_FILEEPR, "FileEPR", this);
		this.propSet.add(fileEPR_RP);
		setFileEPR(fileEPR);

		return key;
	}

	public EndpointReferenceType getFileEPR() {
		return fileEPR;
	}
	public synchronized void setFileEPR(EndpointReferenceType fileEPR) {
		this.fileEPR = fileEPR;
	}
	public String getName() {
		return name;
	}
	public synchronized void setName(String name) {
		this.name = name;
	}
	public String getOrderStatus() {
		return orderStatus;
	}
	public synchronized void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}
	
	/* Required by interface ResourceProperties */
	public ResourcePropertySet getResourcePropertySet() {
		return this.propSet;
	}

	/* Required by interface ResourceIdentifier */
	public Object getID() {
		return this.key;
	}


}