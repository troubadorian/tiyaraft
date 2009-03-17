/*
 * This file is licensed under the terms of the Globus Toolkit Public License v3,
 * found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file,
 * with or without modification.
 */
package org.globus.examples.services.filebuy.buyer.impl;

import javax.xml.namespace.QName;

public interface FileBuyerConstants {
	public static final String BROKER_NS = "http://www.globus.org/namespaces/examples/filebuy/FileBroker";

	public static final QName BROKER_RP_NAME = new QName(BROKER_NS, "Name");
}