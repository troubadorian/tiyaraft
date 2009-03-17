/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.clients.filebuy.FileBroker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.axis.util.Util;
import org.globus.examples.services.filebuy.broker.impl.FileBrokerConstants;
import org.globus.examples.stubs.filebuy.FileBroker.Find;
import org.globus.examples.stubs.filebuy.FileBroker.FindResponse;
import org.globus.examples.stubs.filebuy.FileBroker.FileBrokerPortType;
import org.globus.examples.stubs.filebuy.FileBroker.service.FileBrokerServiceAddressingLocator;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.globus.wsrf.impl.security.authorization.NoAuthorization;
import org.globus.wsrf.security.Constants;

public class FindFileClient {

	static {
		Util.registerTransport();
	}

	private static void printHelp() {
		System.out
				.println("Usage: FindFile <serviceURI> <filename> <maxPrice> [-debug]");
		System.out.println();
		System.out.println("serviceURI: URI of the FileBroker service.");
		System.out.println("filename: Name of the file you want to find");
		System.out.println("maxPrice: Maximum price you're willing to pay.");
		System.out
				.println("-debug: Print more complete error messages (including stack traces).");
	}

	public static void main(String[] args) {

		// Validate number of parameters
		if (!(args.length == 3 || args.length == 4)) {
			System.err.println("ERROR: Incorrect number of parameter");
			printHelp();
			System.exit(1);
		}

		String serviceURI = args[0];
		String filename = args[1];
		String maxPrice_str = args[2];

		float maxPrice = 0;
		boolean debug = false;

		/* Validate parameters */

		// Is the price a number?
		try {
			maxPrice = Float.parseFloat(maxPrice_str);
		} catch (NumberFormatException e) {
			System.err.println("ERROR: " + maxPrice_str + " is not a number.");
			printHelp();
			System.exit(1);
		}

		// Debugging?
		if (args.length == 4) {
			if (args[3].equals("-debug"))
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
		FileBrokerPortType broker = null;

		// Create EPR
		EndpointReferenceType brokerEPR = new EndpointReferenceType();
		try {
			brokerEPR.setAddress(new Address(serviceURI));
		} catch (Exception e) {
			System.err.println("ERROR: Malformed URI '" + serviceURI + "'");
			printHelp();
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Get portType
		FileBrokerServiceAddressingLocator brokerLocator;
		brokerLocator = new FileBrokerServiceAddressingLocator();
		try {
			broker = brokerLocator.getFileBrokerPortTypePort(brokerEPR);
		} catch (ServiceException e) {
			System.err.println("ERROR: Unable to obtain portType.");
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Setup security options
		((Stub) broker)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) broker)._setProperty(Constants.AUTHORIZATION, NoAuthorization
				.getInstance());
		
		/* Invoke Find operation */

		// Create request to Find operation
		Find findRequest = new Find();
		findRequest.setName(filename);
		findRequest.setMaxPrice(maxPrice);

		// Perform invocation
		FindResponse findResponse = null;
		try {
			findResponse = broker.find(findRequest);
		} catch (RemoteException e) {
			System.err.println("ERROR: Unable to invoke find operation.");
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Somewhat arbitrarily, we interpret a null EPR as meaning
		// "no file within the given price constraint has been found"
		// This is better done sending a custom exception (other than
		// RemoteException)
		if (findResponse.getFileOrderEPR()==null) {
			System.out
					.println("No file with the given price constraint has been found.");
		} else {
			float price = findResponse.getPrice();
			System.out.println("A file has been found! (price=" + price + ")");
			// Collect EPR of FileOrder WS-Resource
			EndpointReferenceType fileOrderEPR = findResponse.getFileOrderEPR();

			// Write EPR to file
			try {
				String endpointString = ObjectSerializer.toString(fileOrderEPR,
						FileBrokerConstants.RESOURCE_REFERENCE);
				FileWriter fileWriter = new FileWriter("FileOrder-" + filename + ".epr");
				BufferedWriter bfWriter = new BufferedWriter(fileWriter);
				bfWriter.write(endpointString);
				bfWriter.close();
				System.out.println("EPR of your order has been written to FileOrder-"
						+ filename + ".epr");
				System.out
						.println("Remember to use this EPR to complete the purchase.");
			} catch (Exception e) {
				System.err.println("ERROR: Unable to write EPR to file.");
				if (debug)
					e.printStackTrace();
				System.exit(1);
			}
		}
	}

}