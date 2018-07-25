/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.osb.pulpo.lambda.handler.elasticsearch;

/**
 * The type count request.
 *
 * @author Ruben Pulido
 */
public class CountRequest {

	/**
	 * Gets the environment name.
	 *
	 * @return environment name
	 */
	public String getEnvironment() {
		return _environment;
	}

	/**
	 * Gets host.
	 *
	 * @return host. host
	 */
	public String getHost() {
		return _host;
	}

	/**
	 * Sets the environment name.
	 *
	 * @param environment a string with the environment name
	 */
	public void setEnvironment(String environment) {
		_environment = environment;
	}

	/**
	 * Sets host.
	 *
	 * @param host a string with the host
	 */
	public void setHost(String host) {
		_host = host;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(8);

		sb.append("CountRequest{");
		sb.append("_environment='");
		sb.append(_environment);
		sb.append("', _host='");
		sb.append(_host);
		sb.append("', _interval='");
		sb.append(_interval);
		sb.append("}");

		return sb.toString();
	}

	/**
	 * Gets interval.
	 *
	 * @return the interval
	 */
	public String getInterval() {
		return _interval;
	}

	/**
	 * Sets interval.
	 *
	 * @param interval the interval
	 */
	public void setInterval(String interval) {
		_interval = interval;
	}

	private String _environment;
	private String _host;
	private String _interval;


}