/*
 * This file is licensed under the terms of the Globus Toolkit Public License v3,
 * found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file,
 * with or without modification.
 */
package org.globus.examples.services.filebuy.seller.impl;

import javax.xml.namespace.QName;

public interface FilesForSaleConstants {
	public static final String NS = "http://www.globus.org/namespaces/examples/filebuy/FilesForSale";

	public static final QName RP_NAME = new QName(NS, "Name");

	public static final QName RP_LOCATION = new QName(NS, "Location");

	public static final QName RP_PRICE = new QName(NS, "Price");

	public static final QName RESOURCE_PROPERTIES = new QName(NS,
			"FileResourceProperties");

	public static final QName RESOURCE_REFERENCE = new QName(NS,
			"FileResourceReference");
}