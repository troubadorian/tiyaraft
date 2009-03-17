/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.seller.impl;

import java.net.URL;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.mds.servicegroup.client.ServiceGroupRegistrationParameters;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceContextException;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.config.ContainerConfig;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.servicegroup.client.ServiceGroupRegistrationClient;
import org.globus.wsrf.utils.AddressingUtils;

import commonj.timers.Timer;

public class FileResourceHome extends ResourceHomeImpl {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileResource.class);

	public ResourceKey create(String name, String location, float price)
			throws Exception {
		// Create a resource and initialize it
		FileResource fileResource = (FileResource) this.createNewInstance();
		fileResource.initialize(name, location, price);
		// Get key
		ResourceKey key = new SimpleResourceKey(this.getKeyTypeName(),
				fileResource.getID());
		// Add the resource to the list of resources in this home
		this.add(key, fileResource);
		return key;
	}

	/*
	 * We override the add method to register our File resources with the
	 * FilesForSale index (the DefaultIndexService deployed in the same
	 * container as the seller components)
	 */
	protected void add(ResourceKey key, Resource resource) {
		// Call the parent "add" method
		super.add(key, resource);

		// Get registration client
		ServiceGroupRegistrationClient regClient = ServiceGroupRegistrationClient
				.getContainerClient();
		
		// Get resource context
		ResourceContext ctx;
		try {
			ctx = ResourceContext.getResourceContext();
		} catch (ResourceContextException e) {
			logger.error("Could not get ResourceContext: " + e);
			throw new RuntimeException("Could not get ResourceContext", e);
		}

		// Construct EPR of WS-Resource we want to register in the index.
		EndpointReferenceType epr;
		try {
			URL baseURL = ServiceHost.getBaseURL();
			FilesForSaleConfiguration config = FilesForSaleConfiguration
					.getConfObject();
			String instanceService = config.getInstanceServicePath();
			String instanceURI = baseURL.toString() + instanceService;
			epr = AddressingUtils.createEndpointReference(instanceURI, key);
		} catch (Exception e) {
			logger.error("Could not form EPR: " + e);
			throw new RuntimeException("Could not form EPR", e);
		}

		// Get registration options from registration.xml file
		// Remember kids: Hard-coding is bad karma
		String regPath = ContainerConfig.getGlobusLocation()
				+ "/etc/org_globus_examples_services_filebuy_seller/registration.xml";

		/*
		 * REGISTER WS-RESOURCE IN INDEX
		 */
		try {
			FileResource fileResource = (FileResource) resource;
			ServiceGroupRegistrationParameters params = ServiceGroupRegistrationClient
					.readParams(regPath);
			params.setRegistrantEPR(epr);
			Timer regTimer = regClient.register(params);
			fileResource.setRegTimer(regTimer);

		} catch (Exception e) {
			logger
					.error("ERROR: Couldn't register File resource in local index.");
			throw new RuntimeException(
					"ERROR: Couldn't register File resource in local index.", e);
		}
	}

	public void remove(ResourceKey key) throws ResourceException {
		// Call the parent "remove" method
		super.remove(key);
	}
}