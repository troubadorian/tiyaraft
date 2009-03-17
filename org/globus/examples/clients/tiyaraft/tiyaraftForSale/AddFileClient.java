/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.clients.filebuy.FilesForSale;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.axis.util.Util;
import org.globus.examples.services.filebuy.seller.impl.FilesForSaleConstants;
import org.globus.examples.stubs.filebuy.FilesForSaleFactory.AddFile;
import org.globus.examples.stubs.filebuy.FilesForSaleFactory.AddFileResponse;
import org.globus.examples.stubs.filebuy.FilesForSaleFactory.FilesForSaleFactoryPortType;
import org.globus.examples.stubs.filebuy.FilesForSaleFactory.service.FilesForSaleFactoryServiceAddressingLocator;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.globus.wsrf.impl.security.authorization.NoAuthorization;
import org.globus.wsrf.security.Constants;

public class AddFileClient {

	static {
		Util.registerTransport();
	}

	
	private static void printHelp() {
		System.out
				.println("Usage: AddFile <serviceURI> <filename> <location> <price> [-debug]");
		System.out.println();
		System.out
				.println("serviceURI: URI of the FilesForSaleFactory service.");
		System.out.println("filename: Universally recognized name for the file"
				+ "(Doesn't need to match the name of the file on disk)");
		System.out.println("location: Location of the file on disk.");
		System.out.println("price: Price at which the file will be offered.");
		System.out
				.println("-debug: Print more complete error messages (including stack traces).");
	}

	public static void main(String[] args) {

		// Validate number of parameters
		if (!(args.length == 4 || args.length == 5)) {
			System.err.println("ERROR: Incorrect number of parameter");
			printHelp();
			System.exit(1);
		}

		String serviceURI = args[0];
		String filename = args[1];
		String location = args[2];
		String price_str = args[3];

		float price = 0;
		boolean debug = false;

		/* Validate parameters */

		// Does the file exists?
		File file = new File(location);
		if (!file.exists()) {
			System.err.println("ERROR: " + location + " is not a file.");
			printHelp();
			System.exit(1);
		}

		// Is the price a number?
		try {
			price = Float.parseFloat(price_str);
		} catch (NumberFormatException e) {
			System.err.println("ERROR: " + price_str + " is not a number.");
			printHelp();
			System.exit(1);
		}

		// Debugging?
		if (args.length == 5) {
			if (args[4].equals("-debug"))
				debug = true;
			else {
				System.err.println("ERROR: " + args[4]
						+ " is not a recognized option.");
				printHelp();
				System.exit(1);
			}
		} else
			debug = false;

		/* Get FilesForSaleFactory portType */

		EndpointReferenceType factoryEPR, fileEPR;
		FilesForSaleFactoryPortType factory = null;

		// Create EPR
		factoryEPR = new EndpointReferenceType();
		try {
			factoryEPR.setAddress(new Address(serviceURI));
		} catch (Exception e) {
			System.err.println("ERROR: Malformed URI '" + serviceURI + "'");
			printHelp();
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Get portType
		FilesForSaleFactoryServiceAddressingLocator factoryLocator;
		factoryLocator = new FilesForSaleFactoryServiceAddressingLocator();
		try {
			factory = factoryLocator
					.getFilesForSaleFactoryPortTypePort(factoryEPR);
		} catch (ServiceException e) {
			System.err.println("ERROR: Unable to obtain portType.");
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Setup security options
		((Stub) factory)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) factory)._setProperty(Constants.AUTHORIZATION, NoAuthorization
				.getInstance());
		
		/* Invoke AddFile operation */

		// Create request to AddFile
		AddFile addFileRequest = new AddFile();
		addFileRequest.setName(filename);
		addFileRequest.setLocation(location);
		addFileRequest.setPrice(price);

		// Perform invocation
		AddFileResponse addFileResponse = null;
		try {
			addFileResponse = factory.addFile(addFileRequest);
		} catch (RemoteException e) {
			System.err.println("ERROR: Unable to invoke AddFile operation.");
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}

		// Collect EPR of File WS-Resource
		fileEPR = addFileResponse.getEndpointReference();

		// Write EPR to file
		try {
			String endpointString = ObjectSerializer.toString(fileEPR,
					FilesForSaleConstants.RESOURCE_REFERENCE);
			FileWriter fileWriter = new FileWriter("File-"+filename + ".epr");
			BufferedWriter bfWriter = new BufferedWriter(fileWriter);
			bfWriter.write(endpointString);
			bfWriter.close();
			System.out.println("Endpoint reference written to file File-" + filename
					+ ".epr");
		} catch (Exception e) {
			System.err.println("ERROR: Unable to write EPR to file.");
			if (debug)
				e.printStackTrace();
			System.exit(1);
		}
	}

}