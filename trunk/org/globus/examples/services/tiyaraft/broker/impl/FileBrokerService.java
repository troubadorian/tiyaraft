/*
 * This file is licensed under the terms of the Globus Toolkit Public License
 * v3, found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file, with or without
 * modification.
 */
package org.globus.examples.services.filebuy.broker.impl;

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.MessageElement;
import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.types.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.examples.stubs.filebuy.FileBroker.Find;
import org.globus.examples.stubs.filebuy.FileBroker.FindResponse;
import org.globus.examples.stubs.filebuy.FileBroker.Purchase;
import org.globus.examples.stubs.filebuy.FileBroker.PurchaseResponse;
import org.globus.examples.stubs.filebuy.FileBroker.ReportResponse;
import org.globus.examples.stubs.filebuy.FilesForSale.FilesForSalePortType;
import org.globus.examples.stubs.filebuy.FilesForSale.FillOrder;
import org.globus.examples.stubs.filebuy.FilesForSale.service.FilesForSaleServiceAddressingLocator;
import org.globus.mds.aggregator.types.AggregatorContent;
import org.globus.mds.aggregator.types.AggregatorData;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceContextException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.WSRFConstants;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.impl.security.authorization.NoAuthorization;
import org.globus.wsrf.security.Constants;
import org.globus.wsrf.security.SecurityManager;
import org.globus.wsrf.utils.AddressingUtils;
import org.oasis.wsrf.properties.QueryExpressionType;
import org.oasis.wsrf.properties.QueryResourcePropertiesResponse;
import org.oasis.wsrf.properties.QueryResourceProperties_Element;
import org.oasis.wsrf.properties.QueryResourceProperties_PortType;
import org.oasis.wsrf.properties.WSResourcePropertiesServiceAddressingLocator;
import org.oasis.wsrf.servicegroup.EntryType;

public class FileBrokerService {

	/* Added for logging */
	static final Log logger = LogFactory.getLog(FileBrokerService.class);

	/*
	 * Private method that gets a reference to the resource specified in the
	 * endpoint reference.
	 */
	private FileOrderResource getResource() throws RemoteException {
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

		FileOrderResource fileResource = (FileOrderResource) resource;
		return fileResource;
	}

	/* Implementation of Find operation */
	public FindResponse find(Find params) throws RemoteException {
		/* Retrieve parameters */
		String name = params.getName();
		float maxPrice = params.getMaxPrice();

		logger
				.debug("find invoked with name=" + name + ",maxPrice="
						+ maxPrice);

		/* Create response object */
		FindResponse response = new FindResponse();

		/*
		 * QUERY INDEX SERVICE
		 */

		// Create index service EPR
		FileBrokerConfiguration conf = null;
		try {
			conf = FileBrokerConfiguration.getConfObject();
		} catch (Exception e) {
			logger
					.error("ERROR: Unable to obtain FileBroker configuration object (JNDI).");
			throw new RemoteException(
					"ERROR: Unable to obtain FileBroker configuration object (JNDI).",
					e);
		}
		String indexURI = conf.getIndexURI();
		EndpointReferenceType indexEPR = new EndpointReferenceType();
		try {
			indexEPR.setAddress(new Address(indexURI));
		} catch (Exception e) {
			logger.error("ERROR: Malformed index URI '" + indexURI + "'");
			throw new RemoteException("ERROR: Malformed index URI '" + indexURI
					+ "'", e);
		}

		// Get QueryResourceProperties portType
		WSResourcePropertiesServiceAddressingLocator queryLocator;
		queryLocator = new WSResourcePropertiesServiceAddressingLocator();
		QueryResourceProperties_PortType query = null;
		try {
			query = queryLocator.getQueryResourcePropertiesPort(indexEPR);
		} catch (ServiceException e) {
			logger.error("ERROR: Unable to obtain query portType.");
			throw new RemoteException(
					"ERROR: Unable to obtain query portType.", e);
		}

		// Setup security options
		((Stub) query)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) query)._setProperty(Constants.AUTHORIZATION, NoAuthorization
				.getInstance());

		// The following XPath query retrieves all the files with the specified
		// name
		String xpathQuery = "//*[local-name()='Entry'][./*/*/*[local-name()='Name']/text()='"
				+ name + "']";

		// Create request to QueryResourceProperties
		QueryExpressionType queryExpr = new QueryExpressionType();
		try {
			queryExpr.setDialect(new URI(WSRFConstants.XPATH_1_DIALECT));
		} catch (Exception e) {
			logger
					.error("ERROR: Malformed URI (WSRFConstants.XPATH_1_DIALECT)");
			throw new RemoteException(
					"ERROR: Malformed URI (WSRFConstants.XPATH_1_DIALECT)", e);
		}
		queryExpr.setValue(xpathQuery);
		QueryResourceProperties_Element queryRequest = new QueryResourceProperties_Element(
				queryExpr);

		// Invoke QueryResourceProperties
		QueryResourcePropertiesResponse queryResponse = null;
		try {
			queryResponse = query.queryResourceProperties(queryRequest);
		} catch (RemoteException e) {
			logger.error("ERROR: Unable to invoke QueryRP operation.");
			throw new RemoteException(
					"ERROR: Unable to invoke QueryRP operation.", e);
		}
		// The response includes 0 or more entries from the index service.
		MessageElement[] entries = queryResponse.get_any();

		/*
		 * FIND THE CHEAPEST FILE
		 */

		// If the number of entries is 0, there are no files with that name.
		if (entries == null || entries.length == 0) {
			logger.debug("No file found with name " + name);

			// We return a null EPR to indicate that no file has
			// been found meeting the specified criteria.
			// Ideally, we should throw a custom exception.
			response.setFileOrderEPR(null);

		} else {
			// We know that there is at least one file with the specified name.
			// Now, we find the cheapest file, to then see if it meets the
			// price restriction
			float minPrice = Float.MAX_VALUE;
			EntryType cheapestEntry = null;
			for (int i = 0; i < entries.length; i++) {

				try {
					// Access information contained in the entry. First of all,
					// we need to deserialize the entry...
					EntryType entry = (EntryType) ObjectDeserializer.toObject(
							entries[i], EntryType.class);

					// ... access its content ...
					AggregatorContent content = (AggregatorContent) entry
							.getContent();

					// ... then the data ...
					AggregatorData data = content.getAggregatorData();

					/*
					 * Now, because of how the registration is set up, we know
					 * that the price is the second element.
					 * 
					 * From registration.xml:
					 * <agg:ResourcePropertyNames>ffs:Name
					 * </agg:ResourcePropertyNames>
					 * <agg:ResourcePropertyNames>ffs:Price
					 * </agg:ResourcePropertyNames>
					 */
					String price_str = data.get_any()[1].getValue();
					float price = Float.parseFloat(price_str);

					/* Is this the cheapest? */
					if (price < minPrice) {
						minPrice = price;
						cheapestEntry = entry;
					}

				} catch (Exception e) {
					logger.error("Error when accessing index service entry.");
					throw new RemoteException(
							"Error when accessing index service entry.", e);
				}
			}

			if (minPrice < maxPrice) {
				// A file matches the specified criteria!

				// Return values
				float price = minPrice;
				EndpointReferenceType fileOrderEPR = null;

				/*
				 * CREATE A FILE ORDER RESOURCE, AND ADD THE CHEAPEST FILE TO
				 * THE RESOURCE
				 */

				// Get EPR of File resource. It will be included in the
				// FileOrder resource we are going to create
				EndpointReferenceType fileEPR = cheapestEntry
						.getMemberServiceEPR();
				ResourceContext ctx = null;
				FileOrderResourceHome home = null;
				ResourceKey key = null;
				try {
					/* Create new FileOrder resource */
					ctx = ResourceContext.getResourceContext();
					home = (FileOrderResourceHome) ctx.getResourceHome();
					key = home.create(name, fileEPR);
					/* Create FileOrder EPR */
					URL serviceURL = ctx.getServiceURL();
					fileOrderEPR = AddressingUtils.createEndpointReference(
							serviceURL.toString(), key);
				} catch (Exception e) {
					logger.error("Error when creating FileOrder resource.");
					throw new RemoteException(
							"Error when creating FileOrder resource.", e);
				}

				logger.debug("Found file with price=" + price);

				logger.info("Created new file order for file NAME=" + name
						+ ", PRICE=" + price);
				logger.info("FileOrder has been created for user '"
						+ SecurityManager.getManager().getCaller() + "'");

				/* Create response object */
				response.setPrice(price);
				response.setFileOrderEPR(fileOrderEPR);

			} else {
				logger.debug("No file with name " + name + " is cheaper than "
						+ maxPrice);

				// We return a null EPR to indicate that no file has
				// been found meeting the specified criteria.
				// Ideally, we should throw a custom exception.
				response.setFileOrderEPR(null);
			}
		}

		return response;
	}

	/* Implementation of Purchase operation */
	public PurchaseResponse purchase(Purchase params) throws RemoteException {
		/* Retrieve parameters */
		EndpointReferenceType buyerEPR = params.getBuyerEPR();

		/* Get EPR of file to purchase (contained in FileOrder resource) */
		FileOrderResource fileOrder = getResource();
		EndpointReferenceType fileEPR = fileOrder.getFileEPR();

		logger.info("Going to purchase file " + fileOrder.getName());
		logger.info("Purchase requested by user '"
				+ SecurityManager.getManager().getCaller() + "'");

		/*
		 * INVOKE FILLORDER OPERATION IN THE SELLE
		 */

		// Get FIlesForSale portType
		FilesForSaleServiceAddressingLocator filesForSaleLocator;
		filesForSaleLocator = new FilesForSaleServiceAddressingLocator();
		FilesForSalePortType filesForSale = null;
		try {
			filesForSale = filesForSaleLocator
					.getFilesForSalePortTypePort(fileEPR);
		} catch (ServiceException e) {
			logger.error("ERROR: Unable to obtain FilesForSale portType.");
			throw new RemoteException(
					"ERROR: Unable to obtain FilesForSale portType.", e);
		}

		// Setup security options
		((Stub) filesForSale)._setProperty(Constants.GSI_TRANSPORT,
				Constants.SIGNATURE);
		((Stub) filesForSale)._setProperty(Constants.AUTHORIZATION,
				NoAuthorization.getInstance());

		/* Invoke FillOrder operation */

		// Create request to FillOrder operation
		FillOrder fillOrderRequest = new FillOrder();
		fillOrderRequest.setBuyerEPR(buyerEPR);

		// Perform invocation
		try {
			filesForSale.fillOrder(fillOrderRequest);
		} catch (RemoteException e) {
			logger.error("ERROR: Unable to invoke FillOrder operation.");
			throw new RemoteException(
					"ERROR: Unable to invoke FillOrder operation.", e);
		}

		return new PurchaseResponse();
	}

	public ReportResponse report(int reportCode) throws RemoteException {

		logger.info("Received report of transfer of file "
				+ getResource().getName());
		logger.info("Report code: " + reportCode);

		if (reportCode == 0) {
			logger.info("Order completed successfully.");
			ResourceContext ctx = null;
			FileOrderResourceHome home = null;
			ResourceKey key = null;
			try {
				/* Destroy FileOrder resource */
				ctx = ResourceContext.getResourceContext();
				home = (FileOrderResourceHome) ctx.getResourceHome();
				key = ctx.getResourceKey();
				home.remove(key);
			} catch (Exception e) {
				logger.error("Error when destroying FileOrder resource.");
				throw new RemoteException(
						"Error when destroying FileOrder resource.", e);
			}
		} else {
			logger.error("File was not transfered!");
		}
		return new ReportResponse();
	}

}