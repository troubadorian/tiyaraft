/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.buyer.impl;

import org.apache.axis.components.uuid.UUIDGen;
import org.apache.axis.components.uuid.UUIDGenFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceIdentifier;

public class FilePurchaseResource implements Resource, ResourceIdentifier {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FilePurchaseResource.class);

	/* Resource key. This uniquely identifies this resource. */
	private Object key;

	/* UUID generator to generate unique resource key */
	private static final UUIDGen uuidGen = UUIDGenFactory.getUUIDGen();

	/* Resource properties */
	private String name;

	private EndpointReferenceType fileOrderEPR;

	/* Initializes RPs and returns a unique identifier for this resource */
	public Object initialize(String name, EndpointReferenceType fileOrderEPR)
			throws Exception {
		this.key = uuidGen.nextUUID();
		setName(name);
		setFileOrderEPR(fileOrderEPR);

		return key;
	}

	/* Required by interface ResourceIdentifier */
	public Object getID() {
		return this.key;
	}

	public String getName() {
		return name;
	}

	public synchronized void setName(String name) {
		this.name = name;
	}

	public EndpointReferenceType getFileOrderEPR() {
		return fileOrderEPR;
	}

	public synchronized void setFileOrderEPR(EndpointReferenceType fileOrderEPR) {
		this.fileOrderEPR = fileOrderEPR;
	}

}