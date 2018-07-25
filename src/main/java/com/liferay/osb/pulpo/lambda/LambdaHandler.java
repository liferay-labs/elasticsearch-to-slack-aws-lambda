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

package com.liferay.osb.pulpo.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.liferay.osb.pulpo.lambda.handler.SendMessageToSlackRequest;
import com.liferay.osb.pulpo.lambda.handler.elasticsearch.CountRequest;
import com.liferay.osb.pulpo.lambda.handler.elasticsearch.ElasticSearchAWSUtil;
import com.liferay.osb.pulpo.lambda.handler.file.FileUtil;
import com.liferay.osb.pulpo.lambda.handler.slack.SlackAWSUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Lambda which checks periodically (by default, every hour) whether there are
 * log messages in Elasticsearch.
 *
 * If there are no log messages in the last interval (by default, in the last
 * hour) it sends a message to a Slack channel.
 *
 * If there are log messages, it checks whether there are any errors in the last
 * interval.
 *
 * If there are errors it sends a message to a Slack channel.
 *
 * If there are log entries and there are no errors, no message is sent to
 * Slack.
 *
 * @author Ruben Pulido
 */
public class LambdaHandler
	implements RequestHandler<CountRequest, Optional<String>> {

	@Override
	public Optional<String> handleRequest(
		CountRequest inputCountRequest, Context context) {

		LambdaLogger logger = context.getLogger();

		CountRequest countRequest = new CountRequest();

		Optional<CountRequest> optionalInputCountRequest =
			Optional.of(inputCountRequest);

		Optional<String> hostOptional = Optional.empty();

		Optional<String> environmentOptional = Optional.empty();

		Optional<String> intervalOptional = Optional.empty();

		if (optionalInputCountRequest.isPresent()) {

			hostOptional = Optional.ofNullable(
				inputCountRequest.getHost());

			environmentOptional = Optional.ofNullable(
				inputCountRequest.getEnvironment());

			intervalOptional = Optional.ofNullable(
				inputCountRequest.getInterval());
		}

		String interval = intervalOptional.orElse(_DEFAULT_INTERVAL);

		String environment = environmentOptional.orElse(_DEFAULT_ENVIRONMENT);

		String queryTemplateFileName = "queryTemplate.json";

		String query = _getQuery(
			logger, queryTemplateFileName, environment, interval);

		long logEntriesCount = ElasticSearchAWSUtil.getCount(
			hostOptional.orElse(_DEFAULT_ES_HOST), query, logger);

		logger.log("countLogEntriesResponse: \n" + logEntriesCount);

		Optional<String> messageOptional;

		if (logEntriesCount == 0) {
			String kibanaUrl = String.format(
				_KIBANA_URL_TEMPLATE, interval, environment, environment,
				environment
			);

			String message = String.format(
				"No log entries found in *%s* environment in the last *%s*",
				environment, interval);

			SendMessageToSlackRequest sendMessageToSlackRequest =
				_getSendMessageToSlackRequest(message, kibanaUrl, logger);

			logger.log(
				"Sending slack message: " + sendMessageToSlackRequest + "\n");

			SlackAWSUtil.sendMessageToSlack(
				sendMessageToSlackRequest, logger);

			return Optional.of(message);
		}
		else {
			String errorsQueryTemplateFileName = "errorsQueryTemplate.json";

			String errorsQuery = _getQuery(
				logger, errorsQueryTemplateFileName, environment, interval);

			long errorsCount = ElasticSearchAWSUtil.getCount(
				hostOptional.orElse(_DEFAULT_ES_HOST), errorsQuery, logger);

			logger.log("countErrorsResponse: \n" + errorsCount);

			if (errorsCount >= 0) {
				String kibanaErrorsUrl = String.format(
					_KIBANA_ERRORS_URL_TEMPLATE, interval, environment,
					environment, environment
				);

				String message = String.format(
					"Errors found in *%s* environment in the last *%s*: *%s*",
					environment, interval, errorsCount);

				SendMessageToSlackRequest sendMessageToSlackRequest =
					_getSendMessageToSlackRequest(
						message, kibanaErrorsUrl, logger);

				logger.log(
					"Sending slack message: " + sendMessageToSlackRequest
						+ "\n");

				SlackAWSUtil.sendMessageToSlack(
					sendMessageToSlackRequest, logger);

				return Optional.of(message);
			}
			else {
				logger.log("NOT sending any message to slack\n");

				Optional<String> emptyMessage = Optional.empty();

				return emptyMessage;
			}
		}

	}

	private String _getQuery(
		LambdaLogger logger, String queryTemplateFileName, String environment,
		String interval) {

		FileUtil fileUtil = new FileUtil();

		String query = null;

		try {
			String queryTemplate = fileUtil.fileInClasspathToString(
				queryTemplateFileName);

			logger.log("Query template: \n" + queryTemplate + "\n");

			logger.log("Environment: \n" + environment + "\n");

			logger.log("interval: \n" + interval + "\n");

			query = String.format(queryTemplate, environment, interval);
		}
		catch (URISyntaxException | IOException e) {
			logger.log(
				"Could not read from classpath file: " + queryTemplateFileName +
					"Exception: " + e.getMessage() + "\n");

			throw new RuntimeException(e);
		}

		logger.log("ElasticSearch Query: \n" + query + "\n");

		return query;
	}

	private SendMessageToSlackRequest _getSendMessageToSlackRequest(
		String message, String buttonUrl, LambdaLogger logger) {

		SendMessageToSlackRequest sendMessageToSlackRequest =
			new SendMessageToSlackRequest();

		sendMessageToSlackRequest.setMessage(message);

		String webHookUrl = System.getenv("WEB_HOOK_URL");

		logger.log("WEB_HOOK_URL env variable: " + webHookUrl + "\n");

		sendMessageToSlackRequest.setWebHookUrl(webHookUrl);

		String channel = System.getenv("CHANNEL");

		logger.log("CHANNEL env variable: " + channel + "\n");

		sendMessageToSlackRequest.setChannel(channel);

		sendMessageToSlackRequest.setButtonUrl(buttonUrl);

		return sendMessageToSlackRequest;
	}

	private static final String _DEFAULT_ENVIRONMENT = "prod";

	private static final String _DEFAULT_ES_HOST =
		"http://search-pulpo-elasticsearch-log-bu5rbksghqwcoha4yj4sebrx7y." +
			"us-east-1.es.amazonaws.com";

	private static final String _DEFAULT_INTERVAL = "1h";

	private static final String _KIBANA_URL_TEMPLATE =
		"https://search-pulpo-elasticsearch-log-bu5rbksghqwcoha4yj4sebrx7y." +
			"us-east-1.es.amazonaws.com/_plugin/kibana/app/kibana#/discover/" +
			"0b263210-1e11-11e8-a571-77b54284e9b7?" +
			"_g=(refreshInterval:(display:Off,pause:!f,value:0)," +
			"time:(from:now-%s,mode:quick,to:now))" +
			"&_a=(columns:!(traceId,level,message),filters:!(('$state':(" +
			"store:appState),meta:(alias:!n,disabled:!f,index:c708e7c0" +
			"-8e69-11e8-8cdd-5fdfb14faa84,key:'@log_group',negate:!f," +
			"params:(query:%s,type:phrase),type:phrase,value:%s),query:" +
			"(match:('@log_group':(query:%s,type:phrase))))),index:" +
			"c708e7c0-8e69-11e8-8cdd-5fdfb14faa84,interval:auto,query:(" +
			"language:lucene,query:''),sort:!('@timestamp',desc))";

	private static final String _KIBANA_ERRORS_URL_TEMPLATE =
		"https://search-pulpo-elasticsearch-log-bu5rbksghqwcoha4yj4sebrx7y." +
			"us-east-1.es.amazonaws.com/_plugin/kibana/app/kibana#/discover/" +
			"e43be5b0-7869-11e8-be96-c92de1459781?" +
			"_g=(refreshInterval:(display:Off,pause:!f,value:0),time:" +
			"(from:now-%s,mode:quick,to:now))" +
			"&_a=(columns:!(level,message),filters:!(('$state':" +
			"(store:appState),meta:(alias:!n,disabled:!f,index:" +
			"'2949d340-6fe8-11e8-a747-6f78e5e9a0b8',key:level,negate:!f," +
			"params:(query:ERROR,type:phrase),type:phrase,value:ERROR)" +
			",query:(match:(level:(query:ERROR,type:phrase))))," +
			"('$state':(store:appState),meta:(alias:!n,disabled:!" +
			"f,index:'2949d340-6fe8-11e8-a747-6f78e5e9a0b8'," +
			"key:logger_name,negate:!t,params:(query:com.github.vanroy." +
			"springdata.jest.mapper.DefaultErrorMapper,type:phrase)," +
			"type:phrase,value:com.github.vanroy.springdata.jest.mapper." +
			"DefaultErrorMapper),query:(match:(logger_name:(query:com." +
			"github.vanroy.springdata.jest.mapper.DefaultErrorMapper," +
			"type:phrase)))),('$state':(store:appState),meta:(alias:!n," +
			"disabled:!f,index:c708e7c0-8e69-11e8-8cdd-5fdfb14faa84,key:'" +
			"@log_group',negate:!f,params:(query:osb-pulpo-engine-" +
			"contacts-%s,type:phrase),type:phrase,value:osb-pulpo-engine" +
			"-contacts-%s),query:(match:('@log_group':(query:osb-pulpo-" +
			"engine-contacts-%s,type:phrase))))),index:c708e7c0-8e69-" +
			"11e8-8cdd-5fdfb14faa84,interval:auto,query:(language:lucene," +
			"query:''),sort:!('@timestamp',desc))";

}