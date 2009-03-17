/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.transfer.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.examples.services.filebuy.seller.impl.FileResource;
import org.globus.examples.stubs.filebuy.FileBuyer.FileBuyerPortType;
import org.globus.examples.stubs.filebuy.FileBuyer.service.FileBuyerServiceAddressingLocator;
import org.globus.examples.stubs.filebuy.FileTransfer.Transfer;
import org.globus.examples.stubs.filebuy.FileTransfer.TransferResponse;

public class FileTransferService {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileResource.class);

	public TransferResponse transfer(Transfer params) throws RemoteException {
		/* Retrieve parameters */
		String name = params.getName();
		String location = params.getLocation();
		EndpointReferenceType buyerEPR = params.getBuyerEPR();

		/*
		 * INVOKE TRANSFER OPERATION IN BUYER
		 */
		
		// Get FileBuyer portType
		FileBuyerServiceAddressingLocator buyerLocator;
		buyerLocator = new FileBuyerServiceAddressingLocator();
		FileBuyerPortType buyerPortType = null;
		try {
			buyerPortType = buyerLocator.getFileBuyerPortTypePort(buyerEPR);
		} catch (ServiceException e) {
			logger.error("ERROR: Unable to obtain buyer portType.");
			throw new RemoteException(
					"ERROR: Unable to obtain buyer portType.", e);
		}

		// Read file to transfer
		byte[] fileBytes = null;
		try {
			File file = new File(location);
			FileInputStream fis = new FileInputStream(file);
			fileBytes = new byte[fis.available()];
			fis.read(fileBytes);
		} catch (IOException e) {
			logger.error("ERROR: Unable to open file to transfer.");
			throw new RemoteException(
					"ERROR: Unable to open file to transfer.", e);
		}

		// Create request to Transfer operation
		org.globus.examples.stubs.filebuy.FileBuyer.Transfer transferRequest;
		transferRequest = new org.globus.examples.stubs.filebuy.FileBuyer.Transfer();
		transferRequest.setName(name);
		transferRequest.setData(fileBytes);

		// Perform invocation
		try {
			buyerPortType.transfer(transferRequest);
		} catch (RemoteException e) {
			logger.error("ERROR: Unable to invoke Transfer operation.");
			throw new RemoteException(
					"ERROR: Unable to invoke Transfer operation.", e);

		}

		return new TransferResponse();
	}
}