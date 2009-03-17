/*
 * This file is licensed under the terms of the Globus Toolkit Public License v3,
 * found at http://www.globus.org/toolkit/legal/4.0/license-v3.html.
 * 
 * This notice must appear in redistributions of this file,
 * with or without modification.
 */
package org.globus.examples.services.filebuy.broker.impl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.globus.wsrf.Constants;
import org.globus.wsrf.ResourceContext;

public class FileBrokerConfiguration {
	private String indexURI;
	private String transferURI;

	public String getIndexURI() {
		return indexURI;
	}
	
	public void setIndexURI(String indexURI) {
		this.indexURI = indexURI;
	}

	
	static final FileBrokerConfiguration getConfObject() throws Exception {
		FileBrokerConfiguration conf;
		ResourceContext ctx;

		ctx = ResourceContext.getResourceContext();
		String confLoc = Constants.JNDI_SERVICES_BASE_NAME + ctx.getService()
				+ "/configuration";
		try {
			Context initialContext = new InitialContext();
			conf = (FileBrokerConfiguration) initialContext.lookup(confLoc);
		} catch (NameNotFoundException e) {
			throw new Exception("Unable to find configuration object", e);
		} catch (NamingException e) {
			throw new Exception(e);
		}

		return conf;
	}
}
