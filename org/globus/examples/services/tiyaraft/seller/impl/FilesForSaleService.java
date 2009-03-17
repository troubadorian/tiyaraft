/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.seller.impl;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceContextException;
import org.globus.wsrf.impl.security.authorization.NoAuthorization;
import org.globus.wsrf.security.Constants;
import org.globus.examples.stubs.filebuy.FileTransfer.FileTransferPortType;
import org.globus.examples.stubs.filebuy.FileTransfer.Transfer;
import org.globus.examples.stubs.filebuy.FileTransfer.service.FileTransferServiceAddressingLocator;
import org.globus.examples.stubs.filebuy.FilesForSale.FillOrderResponse;
import org.globus.examples.stubs.filebuy.FilesForSale.FillOrder;
import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FilesForSaleService {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileResource.class);
	
	/*
	 * Private method that gets a reference to the resource specified in the
	 * endpoint reference.
	 */
	private FileResource getResource() throws RemoteException {
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

		FileResource fileResource = (FileResource) resource;
		return fileResource;
	}


	/* Implementation of FillOrder operation */
    public FillOrderResponse fillOrder(FillOrder params) throws RemoteException
	{
    	// Retrieve parameters
    	EndpointReferenceType buyerEPR = params.getBuyerEPR();
    	
    	logger.debug("fillOrder invoked");
    	
    	/*
    	 * INVOKE FILE TRANSFER SERVICE
    	 */
    	
		// Create transfer service EPR
    	// (Transfer service URI is in the configuration object)
    	FilesForSaleConfiguration conf = null;
		try {
			conf = FilesForSaleConfiguration.getConfObject();
		} catch (Exception e) {
			logger
					.error("ERROR: Unable to obtain FilesForSale configuration object (JNDI).");
			throw new RemoteException(
					"ERROR: Unable to obtain FilesForSale configuration object (JNDI).",
					e);
		}
		String transferURI = conf.getTransferURI();
		EndpointReferenceType indexEPR = new EndpointReferenceType();
		try {
			indexEPR.setAddress(new Address(transferURI));
		} catch (Exception e) {
			logger.error("ERROR: Malformed transfer URI '" + transferURI + "'");
			throw new RemoteException("ERROR: Malformed transfer URI '" + transferURI
					+ "'", e);
		}
		
		// Get FileTransfer portType
		FileTransferServiceAddressingLocator transferLocator;
		transferLocator = new FileTransferServiceAddressingLocator();
		FileTransferPortType transfer = null;
		try {
			transfer = transferLocator.getFileTransferPortTypePort(indexEPR);
		} catch (ServiceException e) {
			logger.error("ERROR: Unable to obtain transfer portType.");
			throw new RemoteException(
					"ERROR: Unable to obtain transfer portType.", e);
		}

		// Setup security options
		((Stub) transfer)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) transfer)._setProperty(Constants.AUTHORIZATION, NoAuthorization
				.getInstance());
		
		// Retrieve file name and location from File resource
		FileResource file = getResource();
		String fileName = file.getName();
		String fileLocation = file.getLocation();
		
		logger.debug("Requesting transfer of file "+ fileName + " to " + buyerEPR.getAddress());
		
		/* Invoke Transfer operation */

		// Create request to Transfer operation
		Transfer transferRequest = new Transfer();
		transferRequest.setName(fileName);
		transferRequest.setLocation(fileLocation);
		transferRequest.setBuyerEPR(buyerEPR);

		// Perform invocation
		try {
			transfer.transfer(transferRequest);
		} catch (RemoteException e) {
			logger.error("ERROR: Unable to invoke Transfer operation.");
			throw new RemoteException(
					"ERROR: Unable to invoke Transfer operation.", e);

		}
    	
		logger.debug("fillOrder completed successfully");
		
    	return new FillOrderResponse();
	}
	
}