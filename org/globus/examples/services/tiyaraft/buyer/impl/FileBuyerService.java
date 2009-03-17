/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.buyer.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.examples.stubs.filebuy.FileBuyer.Purchase;
import org.globus.examples.stubs.filebuy.FileBuyer.PurchaseResponse;
import org.globus.examples.stubs.filebuy.FileBuyer.Transfer;
import org.globus.examples.stubs.filebuy.FileBuyer.TransferResponse;
import org.globus.examples.stubs.filebuy.FileBroker.FileBrokerPortType;
import org.globus.examples.stubs.filebuy.FileBroker.service.FileBrokerServiceAddressingLocator;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceContextException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.security.authorization.NoAuthorization;
import org.globus.wsrf.security.Constants;
import org.globus.wsrf.security.SecurityManager;
import org.globus.wsrf.utils.AddressingUtils;
import org.oasis.wsrf.properties.GetResourceProperty;
import org.oasis.wsrf.properties.GetResourcePropertyResponse;
import org.oasis.wsrf.properties.WSResourcePropertiesServiceAddressingLocator;

public class FileBuyerService {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileBuyerService.class);

	/*
	 * Private method that gets a reference to the resource specified in the
	 * endpoint reference.
	 */
	private FilePurchaseResource getResource() throws RemoteException {
		Object resource = null;
		try {
			resource = ResourceContext.getResourceContext().getResource();
		} catch (NoSuchResourceException e) {
			throw new RemoteException("Specified resource does not exist", e);
		} catch (ResourceContextException e) {
			throw new RemoteException("Error during resource lookup", e);
		} catch (Exception e) {
			throw new RemoteException("", e);
		}

		FilePurchaseResource fileResource = (FilePurchaseResource) resource;
		return fileResource;
	}

	public PurchaseResponse purchase(Purchase params) throws RemoteException {
		/* Use delegated credentials */
		SecurityManager.getManager().setServiceOwnerFromContext();
		
		/* Retrieve parameters */
		EndpointReferenceType fileOrderEPR = params.getFileOrderEPR();

		// Get name of file to be purchased
		// The name will be saved to make sure that we receive the expected file
		String name;
		try {
			name = getFileName(fileOrderEPR);
		} catch (Exception e) {
			logger.error("ERROR: Unable to obtain file's name.");
			throw new RemoteException("ERROR: Unable to obtain file's name.", e);
		}

		// Create a FilePurchase resource. This resource will allow us to keep
		// a copy of the FileOrder EPR and the name of the file, which we will
		// need when the Transfer operation is invoked by the FileTransfer
		// service.
		ResourceContext ctx = null;
		FilePurchaseResourceHome home = null;
		ResourceKey key = null;
		EndpointReferenceType buyerEPR = null;
		try {
			/* Create new FilePurchase resource */
			ctx = ResourceContext.getResourceContext();
			home = (FilePurchaseResourceHome) ctx.getResourceHome();
			key = home.create(name, fileOrderEPR);
			/* Create FilePurchase EPR */
			URL serviceURL = ctx.getServiceURL();
			buyerEPR = AddressingUtils.createEndpointReference(serviceURL
					.toString(), key);
		} catch (Exception e) {
			logger.error("Error when creating FilePurchase resource.");
			throw new RemoteException(
					"Error when creating FilePurchase resource.", e);
		}

		/*
		 * INVOKE THE PURCHASE OPERATION IN THE FILE BROKER
		 */

		// Get FileBroker portType
		FileBrokerServiceAddressingLocator brokerLocator;
		brokerLocator = new FileBrokerServiceAddressingLocator();
		FileBrokerPortType broker = null;
		try {
			broker = brokerLocator
					.getFileBrokerPortTypePort(fileOrderEPR);
		} catch (ServiceException e) {
			logger.error("ERROR: Unable to obtain broker portType.");
			throw new RemoteException(
					"ERROR: Unable to obtain broker portType.", e);
		}

		// Setup security options
		((Stub) broker)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) broker)._setProperty(Constants.AUTHORIZATION, NoAuthorization
				.getInstance());
		
		/* Invoke Purchase operation */

		// Create request to Purchase operation
		org.globus.examples.stubs.filebuy.FileBroker.Purchase purchaseRequest;
		purchaseRequest = new org.globus.examples.stubs.filebuy.FileBroker.Purchase();
		purchaseRequest.setBuyerEPR(buyerEPR);

		// Perform invocation
		PurchaseResponse findResponse = null;
		try {
			broker.purchase(purchaseRequest);
		} catch (RemoteException e) {
			logger
					.error("ERROR: Unable to invoke Purchase operation in broker.");
			throw new RemoteException(
					"ERROR: Unable to invoke Purchase operation in broker.", e);

		}

		return new PurchaseResponse();
	}

	public TransferResponse transfer(Transfer params) throws RemoteException {
		/* Retrieve parameters */
		String name = params.getName();
		byte data[] = params.getData();

		// Report code
		int reportCode;
		
		// Retrieve expected file name from FilePurchase resource
		FilePurchaseResource purchase = getResource();
		String expectedName = purchase.getName();

		// Check if this is the file we're expecting
		if (!expectedName.equals(name)) {
			logger
					.error("ERROR: Received a transfer request for an unexpected file.");
			throw new RemoteException(
					"ERROR: Received a transfer request for an unexpected file.");
		}

		// Write file to disk
		String filePath = System.getProperty("java.io.tmpdir") + File.separatorChar
				+ "File-" + name;
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			fos.write(data);
			fos.flush();
			reportCode=0; // Success
		} catch (IOException e) {
			logger.error("ERROR: Unable to write file to disk.");
			reportCode=-1; // Error
		}
		
		/*
		 * REPORT TRANSFER
		 */
		
		// Retrieve fileOrder EPR from FilePurchase resource. We will use it to
		// confirm that the transfer has been successful.
		EndpointReferenceType fileOrderEPR = purchase.getFileOrderEPR();

		// Get FileBroker portType
		FileBrokerServiceAddressingLocator brokerLocator;
		brokerLocator = new FileBrokerServiceAddressingLocator();
		FileBrokerPortType broker = null;
		try {
			broker = brokerLocator
					.getFileBrokerPortTypePort(fileOrderEPR);
		} catch (ServiceException e) {
			logger.error("ERROR: Unable to obtain broker portType.");
			throw new RemoteException(
					"ERROR: Unable to obtain broker portType.", e);
		}
		
		// Setup security options
		((Stub) broker)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) broker)._setProperty(Constants.AUTHORIZATION, NoAuthorization
				.getInstance());
		
		// Invoke Report operation
		try {
			broker.report(reportCode); 
		} catch (RemoteException e) {
			logger
					.error("ERROR: Unable to invoke Purchase operation in broker.");
			throw new RemoteException(
					"ERROR: Unable to invoke Purchase operation in broker.", e);

		}

		return new TransferResponse();
	}

	private String getFileName(EndpointReferenceType fileOrderEPR)
			throws Exception {
		WSResourcePropertiesServiceAddressingLocator locator;
		GetResourcePropertyResponse nameRP;

		locator = new WSResourcePropertiesServiceAddressingLocator();
		GetResourceProperty port = locator
				.getGetResourcePropertyPort(fileOrderEPR);

		nameRP = port.getResourceProperty(FileBuyerConstants.BROKER_RP_NAME);

		String name = nameRP.get_any()[0].getValue();
		return name;
	}
}