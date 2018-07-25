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

package com.liferay.osb.pulpo.lambda.handler.slack;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringUtils;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;
import com.liferay.osb.pulpo.lambda.handler.SendMessageToSlackRequest;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

/**
 * Util class to send a message to a Slack channel
 *
 * @author Ruben Pulido
 */
public class SlackAWSUtil {

	/**
	 * Send a message to Slack.
	 *
	 * @param sendMessageToSlackRequest input request
	 * @param lambdaLogger lambda logger
	 */
	public static void sendMessageToSlack(
		SendMessageToSlackRequest sendMessageToSlackRequest,
		LambdaLogger lambdaLogger) {

		_validateInputRequest(sendMessageToSlackRequest);

		String bodyJsonString = _getBodyJsonString(
			sendMessageToSlackRequest.getChannel(),
			sendMessageToSlackRequest.getMessage(),
			sendMessageToSlackRequest.getButtonUrl());

		String url =
			sendMessageToSlackRequest.getWebHookUrl();

		RestAssured.baseURI = url;

		lambdaLogger.log(String.format(
			"Executing HTTP Request. URL: \n%s. Body: \n%s", url, bodyJsonString));

		Response response = RestAssured.given()
			.contentType("application/json")
			.body(bodyJsonString)
			.when()
			.post("");

		ResponseBody responseBody = response.getBody();

		String responseBodyString = responseBody.asString();

		lambdaLogger.log("HTTP Response body: \n" + responseBodyString + "\n");

	}

	private static String _getBodyJsonString(
		String channel, String text, String kibanaUrl) {

		JsonObject actionJsonObject = Json.createObjectBuilder()
			.add("type", "button")
			.add("text", "Go to Kibana")
			.add("url", kibanaUrl)
			.build();

		JsonArray actionsJsonArray = Json.createArrayBuilder()
			.add(actionJsonObject)
			.build();

		JsonObject attachmentJsonObject = Json.createObjectBuilder()
			.add("fallback", "Kibana URL: " + kibanaUrl)
			.add("actions", actionsJsonArray)
			.add("color", "#F35A00")
			.build();

		JsonArray attachmentsJsonArray = Json.createArrayBuilder()
			.add(attachmentJsonObject)
			.build();

		return Json.createObjectBuilder()
			.add("channel", channel)
			.add("icon_emoji", _ICON_EMOJI)
			.add("text", text)
			.add("username", _USER_NAME)
			.add("attachments", attachmentsJsonArray)
			.build()
			.toString();
	}

	private static void _validateInputRequest(
		SendMessageToSlackRequest sendMessageToSlackRequest) {

		if (sendMessageToSlackRequest == null) {
			throw new IllegalArgumentException(
				"SendMessageToSlackRequest must not be null");
		}

		if (StringUtils.isNullOrEmpty(sendMessageToSlackRequest.getChannel())) {
			throw new IllegalArgumentException("Channel must not be empty");
		}

		if (StringUtils.isNullOrEmpty(sendMessageToSlackRequest.getMessage())) {
			throw new IllegalArgumentException("Message must not be empty");
		}

		if (StringUtils.isNullOrEmpty(
			sendMessageToSlackRequest.getWebHookUrl())) {
			throw new IllegalArgumentException(
				"Web Hook URL must not be empty");
		}
	}

	private static final String _ICON_EMOJI = ":octopus:";

	private static final String _USER_NAME = "pulpo-aws-to-slack";

}