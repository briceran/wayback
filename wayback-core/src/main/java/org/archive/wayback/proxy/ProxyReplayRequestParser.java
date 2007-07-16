/* ProxyReplayRequestParser
 *
 * $Id$
 *
 * Created on 3:43:24 PM Apr 26, 2007.
 *
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of wayback-core.
 *
 * wayback-core is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * wayback-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with wayback-core; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.wayback.proxy;

import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.URIException;
import org.archive.util.InetAddressUtil;
import org.archive.wayback.WaybackConstants;
import org.archive.wayback.core.Timestamp;
import org.archive.wayback.core.WaybackRequest;
import org.archive.wayback.exception.BadQueryException;
import org.archive.wayback.exception.ConfigurationException;
import org.archive.wayback.requestparser.BaseRequestParser;
import org.archive.wayback.webapp.WaybackContext;

/**
 *
 *
 * @author brad
 * @version $Date$, $Revision$
 */
public class ProxyReplayRequestParser extends BaseRequestParser {
	/**
	 * name of attribute in config Properties for specifying an additional 
	 * hostname that should be considered "local" for discriminating between 
	 * Replays and Queries
	 */
	private static final String LOCAL_HOSTNAME = "query.localhostname";

	private List<String> localhostNames = null;

	public void init(final Properties p) throws ConfigurationException {
		this.localhostNames = InetAddressUtil.getAllLocalHostNames();
		String extraLocalHostname = p.getProperty(LOCAL_HOSTNAME);
		if ((extraLocalHostname != null) && (extraLocalHostname.length() > 0)) {
			localhostNames.add(extraLocalHostname);
		}
		super.init(p);
	}
	/**
	 * @param list
	 */
	public void init(List<String> list) {
		this.localhostNames = InetAddressUtil.getAllLocalHostNames();
		if((list != null) && (list.size() > 0)) {
			localhostNames.addAll(list);
		}
	}
	/**
	 * 
	 */
	public void init() {
		List<String> empty = null;
		init(empty);
	}
	private boolean isLocalRequest(HttpServletRequest httpRequest) {
		return this.localhostNames.contains(httpRequest.getServerName());
	}

	/* (non-Javadoc)
	 * @see org.archive.wayback.requestparser.BaseRequestParser#parse(javax.servlet.http.HttpServletRequest, org.archive.wayback.webapp.WaybackContext)
	 */
	@Override
	public WaybackRequest parse(HttpServletRequest httpRequest,
			WaybackContext wbContext) throws BadQueryException {

		if (isLocalRequest(httpRequest)) {
			// local means query: let the following RequestParsers have a go 
			// at it.
			return null;
		}

		WaybackRequest wbRequest = null;
		String requestServer = httpRequest.getServerName();
		String requestPath = httpRequest.getRequestURI();
		//int port = httpRequest.getServerPort();
		String requestQuery = httpRequest.getQueryString();
		String requestScheme = httpRequest.getScheme();
		if (requestQuery != null) {
			requestPath = requestPath + "?" + requestQuery;
		}

		String requestUrl = requestScheme + "://" + requestServer + requestPath;

		wbRequest = new WaybackRequest();
		try {
			wbRequest.setRequestUrl(requestUrl);
		} catch (URIException e) {
			e.printStackTrace();
			return null;
		}
		wbRequest.put(WaybackConstants.REQUEST_TYPE,
				WaybackConstants.REQUEST_REPLAY_QUERY);

		// Get the id from the request. If no id, use the ip-address instead.
		// Then get the timestamp (or rather datestr) matching this id.
		String id = httpRequest.getHeader("Proxy-Id");
		if (id == null)
			id = httpRequest.getRemoteAddr();
		wbRequest.put(WaybackConstants.REQUEST_EXACT_DATE, Timestamp
				.getTimestampForId(httpRequest.getContextPath(), id));
		addHttpHeaderFields(wbRequest, httpRequest);

		return wbRequest;
	}

}