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

import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.ExecutionContext;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringUtils;
import com.liferay.osb.pulpo.lambda.handler.http.SimpleHttpErrorResponseHandler;
import com.liferay.osb.pulpo.lambda.handler.http.StringResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

/**
 * @author Ruben Pulido
 */
public class ElasticSearchAWSUtil {

	/**
	 * Executes a count query and returns the result.
	 *
	 * @param host the host
	 * @param query the query
	 * @param lambdaLogger lambda logger
	 * @return long the result of the request
	 */
	public static long getCount(
		String host, String query, LambdaLogger lambdaLogger) {

		Request<Void> awsRequest = _createAwsRequest(
			host, _COUNT_REQUEST_PATH, null, HttpMethodName.GET, query);

		lambdaLogger.log(
			"Executing AWS Request: " + awsRequest + "\n for query: " + query +
				"\n");

		Response<AmazonWebServiceResponse<String>> response =
			_executeAwsRequest(awsRequest);

		AmazonWebServiceResponse<String> awsResponse =
			response.getAwsResponse();

		String result = awsResponse.getResult();

		lambdaLogger.log(
			"Amazon Web Service Response result: \n" + result + "\n");

		long count = _getCountFromResult(result);

		lambdaLogger.log("Number of results: " + count + "\n");

		return count;
	}

	/**
	 * Executes a search query and returns the the number of hits per message
	 * prefix.
	 *
	 * @param host the host
	 * @param query the query
	 * @param lambdaLogger lambda logger
	 * @return long the result of the request
	 */
	public static Map<String, Long>  getErrorsCountByMessagePrefix(
		String host, String query, 	int maxMessagePrefixLength,
		LambdaLogger lambdaLogger) {

		Request<Void> awsRequest = _createAwsRequest(
			host, _SEARCH_REQUEST_PATH, null, HttpMethodName.GET, query);

		lambdaLogger.log(
			"Executing AWS Request: " + awsRequest + "\n for query: " + query +
				"\n");

		Response<AmazonWebServiceResponse<String>> response =
			_executeAwsRequest(awsRequest);

		AmazonWebServiceResponse<String> awsResponse =
			response.getAwsResponse();

		String result = awsResponse.getResult();

		lambdaLogger.log(
			"Amazon Web Service Response result: \n" + result + "\n");

		Map<String, Long> errorsCountByMessagePrefix =
			_getErrorsCountByMessagePrefix(result, maxMessagePrefixLength);

		lambdaLogger.log(
			"errorsCountByMessagePrefix: " + errorsCountByMessagePrefix + "\n");

		return errorsCountByMessagePrefix;
	}

	private static long _getCountFromResult(String result) {

		StringReader stringReader = new StringReader(result);

		JsonReader jsonReader = Json.createReader(stringReader);

		JsonObject responseJsonObject = jsonReader.readObject();

		jsonReader.close();

		JsonNumber jsonNumber = responseJsonObject.getJsonNumber("count");

		return jsonNumber.longValue();
	}

	private static Map<String, Long> _getErrorsCountByMessagePrefix(
		String result, int maxMessagePrefixLength) {

		StringReader stringReader = new StringReader(result);

		JsonReader jsonReader = Json.createReader(stringReader);

		JsonObject responseJsonObject = jsonReader.readObject();

		jsonReader.close();

		JsonObject hitsJsonObject = responseJsonObject.getJsonObject("hits");

		JsonArray hitsJsonArray = hitsJsonObject.getJsonArray("hits");

		Stream<JsonValue> hitsStream = hitsJsonArray.stream();

		Map<String, Long> errorsCountByMessagePrefix = hitsStream.map(hit -> {
			JsonObject hitJsonObject = (JsonObject) hit;

			JsonObject sourceJsonObject =
				hitJsonObject.getJsonObject("_source");

			String message = sourceJsonObject.getString("message");

			if (message.length() > maxMessagePrefixLength) {
				message =
					message.substring(0, maxMessagePrefixLength) + " (...)";
			}

			return message;
		}).collect(
			Collectors.groupingBy(Function.identity(), Collectors.counting())
		);

		return errorsCountByMessagePrefix;
	}

	private static Request<Void> _createAwsRequest(
		String host, String path, Map<String, List<String>> params,
		HttpMethodName httpMethodName, String content) {

		Request<Void> request = new DefaultRequest<>("es");

		request.setHttpMethod(httpMethodName);

		Map<String, String> headers = new HashMap<>();

		headers.put("Content-Type", "application/json");

		request.setHeaders(headers);

		request.setEndpoint(URI.create(host));

		request.setResourcePath(path);

		if ((params != null) && (!params.isEmpty())) {
			request.setParameters(params);
		}

		if (content != null) {
			InputStream contentInputStream = new ByteArrayInputStream(
				content.getBytes());

			request.setContent(contentInputStream);
		}

		AWS4Signer signer = new AWS4Signer();

		signer.setServiceName(request.getServiceName());
		signer.setRegionName(_REGION);

		DefaultAWSCredentialsProviderChain defaultAWSCredentialsProviderChain =
			new DefaultAWSCredentialsProviderChain();

		signer.sign(
			request, defaultAWSCredentialsProviderChain.getCredentials());

		return request;
	}

	private static Response<AmazonWebServiceResponse<String>>
		_executeAwsRequest(Request<Void> request) {

		ClientConfiguration config = new ClientConfiguration();

		AmazonHttpClient amazonHttpClient = new AmazonHttpClient(config);

		AmazonHttpClient.RequestExecutionBuilder builder =
			amazonHttpClient.requestExecutionBuilder();

		return builder.executionContext(
			new ExecutionContext(true)
		).request(
			request
		).errorResponseHandler(
			new SimpleHttpErrorResponseHandler()
		).execute(
			new StringResponseHandler()
		);
	}

	private static void _validateInputRequest(
		CountRequest countRequest) {

		if (countRequest == null) {
			throw new IllegalArgumentException(
				"CountRequest must not be null");
		}

		if (StringUtils.isNullOrEmpty(countRequest.getHost())) {
			throw new IllegalArgumentException("Host must not be empty");
		}

		if (StringUtils.isNullOrEmpty(countRequest.getEnvironment())) {
			throw new IllegalArgumentException(
				"Repository name must not be empty");
		}
	}

	private static final String _REGION = System.getenv(
		SDKGlobalConfiguration.AWS_REGION_ENV_VAR);

	private static final String _COUNT_REQUEST_PATH = "_count";

	private static final String _SEARCH_REQUEST_PATH = "_search";

}