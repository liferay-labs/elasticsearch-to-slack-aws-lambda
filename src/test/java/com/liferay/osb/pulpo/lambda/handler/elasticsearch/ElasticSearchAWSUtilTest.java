package com.liferay.osb.pulpo.lambda.handler.elasticsearch;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.liferay.osb.pulpo.lambda.handler.file.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author Ruben Pulido
 */
public class ElasticSearchAWSUtilTest {

	@Test
	public void testCountLogEntriesInLastSecond()
		throws IOException, URISyntaxException {

		FileUtil fileUtil = new FileUtil();

		String queryTemplate = fileUtil.fileInClasspathToString(
			"queryTemplate.json");

		String query = String.format(queryTemplate, "prod", "1s");

		ElasticSearchAWSUtil elasticSearchAWSUtil = new ElasticSearchAWSUtil();

		// To execute this test locally make sure you have started aws-es-kibana
		// as follows:
		// aws-es-kibana -p 9999 search-pulpo-elasticsearch-log-bu5rbksghqwcoha4yj4sebrx7y.us-east-1.es.amazonaws.com &;

		String host = "http://127.0.0.1:9999";

		LambdaLogger lambdaLogger = _getLambdaLogger();

		long count = elasticSearchAWSUtil.getCount(
			host, query, lambdaLogger);

		Assert.assertTrue(count == 0);
	}

	@Test
	public void testGetErrorsCountByMessagePrefix()
		throws IOException, URISyntaxException {

		FileUtil fileUtil = new FileUtil();

		String queryTemplate = fileUtil.fileInClasspathToString(
			"searchErrorsQueryTemplate.json");

		String query = String.format(queryTemplate, "prod", "10h");

		ElasticSearchAWSUtil elasticSearchAWSUtil = new ElasticSearchAWSUtil();

		// To execute this test locally make sure you have started aws-es-kibana
		// as follows:
		// aws-es-kibana -p 9999 search-pulpo-elasticsearch-log-bu5rbksghqwcoha4yj4sebrx7y.us-east-1.es.amazonaws.com &;

		String host = "http://127.0.0.1:9999";

		LambdaLogger lambdaLogger = _getLambdaLogger();

		Map<String, Long> errorsCountByMessagePrefix =
			elasticSearchAWSUtil.getErrorsCountByMessagePrefix(
				host, query, 200, lambdaLogger);

		Assert.assertNotNull(errorsCountByMessagePrefix);
		Assert.assertFalse(errorsCountByMessagePrefix.isEmpty());
	}

	private LambdaLogger _getLambdaLogger() {
		return new LambdaLogger() {
			@Override
			public void log(String string) {
				System.out.println(string);
			}
		};
	}
}
