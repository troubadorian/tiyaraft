/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.seller.impl;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.Constants;
import org.globus.wsrf.NoResourceHomeException;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceContextException;
import org.globus.wsrf.ResourceHome;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.security.SecurityManager;
import org.globus.wsrf.utils.AddressingUtils;
import org.globus.examples.stubs.filebuy.FilesForSaleFactory.AddFileResponse;
import org.globus.examples.stubs.filebuy.FilesForSaleFactory.AddFile;

public class FilesForSaleFactoryService implements FilesForSaleConstants {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileResource.class);

	private static final long MAX_FILE_SIZE = 32 * 1024;

	/* Implementation of AddFile operation */
	public AddFileResponse addFile(AddFile params) throws RemoteException {
		FileResourceHome home = null;
		ResourceKey key = null;

		// Retrieve parameters 
		String name = params.getName();
		String location = params.getLocation();
		float price = params.getPrice();

		logger.debug("addFile invoked with name=" + name + ",location="
				+ location + ",price=" + price);

		// First of all, we check if the file exceeds the size limit 
		File file = new File(location);
		if (file.length() > MAX_FILE_SIZE) {
			logger.error("ERROR: File size (" + file.length()
					+ ") exceeds size limit.");
			throw new RemoteException("ERROR: File size (" + file.length()
					+ ") exceeds size limit.");
		}

		
		/*
		 * CREATE A FILE RESOURCE
		 */
		
		// We create a new FileResource through the FileResourceHome
		try {
			home = (FileResourceHome) getInstanceResourceHome();
			key = home.create(name, location, price);
		} catch (Exception e) {
			logger.error("ERROR: Couldn't create new File resource");
			throw new RemoteException("ERROR: Couldn't create File EPR", e);
		}
		EndpointReferenceType epr = null;

		// We construct the EPR for the recently created WS-Resource.
		// The File resource will be accessed through an instance service.
		try {
			URL baseURL = ServiceHost.getBaseURL();
			// The instance service path is contained in the configuration
			// object
			FilesForSaleConfiguration config = FilesForSaleConfiguration
					.getConfObject();
			String instanceService = config.getInstanceServicePath();
			String instanceURI = baseURL.toString() + instanceService;
			// The endpoint reference includes the instance's URI and the
			// resource key
			epr = AddressingUtils.createEndpointReference(instanceURI, key);
		} catch (Exception e) {
			logger.error("ERROR: Couldn't create File EPR");
			throw new RemoteException("ERROR: Couldn't create File EPR", e);
		}

		logger.info("Added new file. NAME=" + name + ", LOCATION=" + location
				+ ", PRICE=" + price);
		logger.info("File has been added by user '"
				+ SecurityManager.getManager().getCaller() + "'");

		
		/*
		 * RETURN ENDPOINT REFERENCE TO NEW WS-RESOURCE
		 */
	
		AddFileResponse response = new AddFileResponse();
		response.setEndpointReference(epr);
		return response;
	}

	/* Retrieves the resource home for the instance service */
	protected ResourceHome getInstanceResourceHome()
			throws NoResourceHomeException, ResourceContextException {
		ResourceHome home;
		ResourceContext ctx;
		FilesForSaleConfiguration config;

		// The path for the instance service is contained in the configuration object
		try {
			config = FilesForSaleConfiguration.getConfObject();
		} catch (Exception e) {
			throw new NoResourceHomeException(
					"Unable to access configuration object to retrieve instance service path.");
		}
		String instanceService = config.getInstanceServicePath();

		// Lookup resource home
		String homeLoc = Constants.JNDI_SERVICES_BASE_NAME + instanceService
				+ "/home";
		try {
			Context initialContext = new InitialContext();
			home = (ResourceHome) initialContext.lookup(homeLoc);
		} catch (NameNotFoundException e) {
			throw new NoResourceHomeException();
		} catch (NamingException e) {
			throw new ResourceContextException("", e);
		}

		return home;
	}

}