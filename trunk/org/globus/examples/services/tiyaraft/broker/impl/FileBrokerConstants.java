/*
 * This file is licensed under the terms of the Globus Toolkit Public License v3,
 * found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file,
 * with or without modification.
 */
package org.globus.examples.services.filebuy.broker.impl;

import javax.xml.namespace.QName;

public interface FileBrokerConstants {
	public static final String NS = "http://www.globus.org/namespaces/examples/filebuy/FileBroker";

	public static final QName RP_ORDERSTATUS = new QName(NS, "OrderStatus");

	public static final QName RP_NAME = new QName(NS, "Name");

	public static final QName RP_FILEEPR = new QName(NS, "FileEPR");

		public static final QName RESOURCE_PROPERTIES = new QName(NS,
			"FileOrderResourceProperties");

	public static final QName RESOURCE_REFERENCE = new QName(NS,
			"FileOrderResourceReference");
}