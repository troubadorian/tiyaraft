/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.broker.impl;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.servicegroup.client.ServiceGroupRegistrationClient;

public class FileOrderResourceHome extends ResourceHomeImpl {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileOrderResource.class);
	
	/* Used to register in the local index*/
	ServiceGroupRegistrationClient sgClient;
	
	public ResourceKey create(String name, EndpointReferenceType fileEPR) throws Exception {
		// Create a resource and initialize it
		FileOrderResource fileResource = (FileOrderResource) this.createNewInstance();
		fileResource.initialize(name, fileEPR);
		// Get key
		ResourceKey key = new SimpleResourceKey(this.getKeyTypeName(),
				fileResource.getID());
		// Add the resource to the list of resources in this home
		this.add(key, fileResource);
		return key;
	}

}