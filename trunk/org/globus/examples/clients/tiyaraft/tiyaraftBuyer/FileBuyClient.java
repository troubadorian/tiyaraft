/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.clients.filebuy.FileBuyer;

import java.io.FileInputStream;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.axis.gsi.GSIConstants;
import org.globus.axis.util.Util;
import org.globus.examples.stubs.filebuy.FileBuyer.Purchase;
import org.globus.examples.stubs.filebuy.FileBuyer.PurchaseResponse;
import org.globus.examples.stubs.filebuy.FileBuyer.FileBuyerPortType;
import org.globus.examples.stubs.filebuy.FileBuyer.service.FileBuyerServiceAddressingLocator;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.impl.security.authorization.HostAuthorization;
import org.globus.wsrf.security.Constants;
import org.xml.sax.InputSource;

public class FileBuyClient {

	static {
		Util.registerTransport();
	}

	private static void printHelp() {
		System.out
				.println("Usage: FindFile <service URI> <FileOrder EPR> [-debug]");
		System.out.println();
		System.out.println("serviceURI: URI of the FileBroker service.");
		System.out
				.println("-debug: Print more complete error messages (including stack traces).");
	}

	public static void main(String[] args) {

		// Validate number of parameters
		if (!(args.length == 2 || args.length == 3)) {
			System.err.println("ERROR: Incorrect number of parameter");
			printHelp();
			System.exit(1);
		}

		String serviceURI = args[0];
		String fileOrderEPR_file = args[1];
		EndpointReferenceType fileOrderEPR = null;

		boolean debug = false;

		/* Validate parameters */

//		 Get endpoint reference of WS-Resource from file
		try {
			FileInputStream fis = new FileInputStream(fileOrderEPR_file);
			fileOrderEPR = (EndpointReferenceType) ObjectDeserializer
					.deserialize(new InputSource(fis),
							EndpointReferenceType.class);
			fis.close();
		} catch (Exception e) {
			System.err.println("ERROR: Couldn't read EPR file "
					+ fileOrderEPR_file);
			printHelp();
			System.exit(1);
		}

		// Debugging?
		if (args.length == 3) {
			if (args[2].equals("-debug"))
				debug = true;
			else {
				System.err.println("ERROR: " + args[4]
						+ " is not a recognized option.");
				printHelp();
				System.exit(1);
			}
		} else
			debug = false;

		/* Get FileBroker portType */
		FileBuyerPortType buyer = null;

		// Create EPR
		EndpointReferenceType buyerEPR = new EndpointReferenceType();
		try {
			buyerEPR.setAddress(new Address(serviceURI));
		} catch (Exception e) {
			System.err.println("ERROR: Malformed URI '" + serviceURI + "'");
			printHelp();
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Get portType
		FileBuyerServiceAddressingLocator buyerLocator;
		buyerLocator = new FileBuyerServiceAddressingLocator();
		try {
			buyer = buyerLocator.getFileBuyerPortTypePort(buyerEPR);
		} catch (ServiceException e) {
			System.err.println("ERROR: Unable to obtain portType.");
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Setup security options
		((Stub) buyer)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) buyer)._setProperty(Constants.GSI_SEC_CONV,
				Constants.SIGNATURE);
		((Stub) buyer)._setProperty(Constants.AUTHORIZATION, HostAuthorization
				.getInstance());
		((Stub) buyer)._setProperty(GSIConstants.GSI_MODE,
				GSIConstants.GSI_MODE_FULL_DELEG);
		
		/* Invoke Purchase operation */

		// Create request to Purchase operation
		Purchase purchaseRequest = new Purchase();
		purchaseRequest.setFileOrderEPR(fileOrderEPR);

		// Perform invocation
		PurchaseResponse findResponse = null;
		try {
			buyer.purchase(purchaseRequest);
		} catch (RemoteException e) {
			System.err.println("ERROR: Unable to invoke Purchase operation.");
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		System.out.println("A request to purchase the file has been sent.");
		System.out.println("You should receive the file shortly.");
	}

}