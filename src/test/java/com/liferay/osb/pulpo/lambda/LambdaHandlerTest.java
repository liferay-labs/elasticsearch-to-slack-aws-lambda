package com.liferay.osb.pulpo.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.liferay.osb.pulpo.lambda.handler.elasticsearch.CountRequest;
import org.junit.Assert;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Ruben Pulido
 */
public class LambdaHandlerTest {

  @Test
  public void handleRequestTestNoLogEntriesFound() {

    // Given
    Context context = _getContext();

    LambdaHandler lambdaHandler = new LambdaHandler();

    // When
    CountRequest inputCountRequest = new CountRequest();

    // To execute this test locally make sure you have started aws-es-kibana
    // as follows:
    // aws-es-kibana -p 9999 search-pulpo-elasticsearch-log-bu5rbksghqwcoha4yj4sebrx7y.us-east-1.es.amazonaws.com &;
    inputCountRequest.setHost("http://127.0.0.1:9999");
    inputCountRequest.setEnvironment("prod");
    inputCountRequest.setInterval("1s");

    Optional<String> messageOptional = lambdaHandler.handleRequest(
        inputCountRequest, context);

    // Then
    Assert.assertTrue(messageOptional.isPresent());

    String message = messageOptional.get();

    Assert.assertTrue(
        message.contains(
            "No log entries found in *prod* environment in the last *1s*"));
  }

  @Test
  public void handleRequestTestErrorEntriesFound() {

    // Given
    Context context = _getContext();

    LambdaHandler lambdaHandler = new LambdaHandler();

    // When
    CountRequest inputCountRequest = new CountRequest();

    // To execute this test locally make sure you have started aws-es-kibana
    // as follows:
    // aws-es-kibana -p 9999 search-pulpo-elasticsearch-log-bu5rbksghqwcoha4yj4sebrx7y.us-east-1.es.amazonaws.com &;
    inputCountRequest.setHost("http://127.0.0.1:9999");
    inputCountRequest.setEnvironment("prod");
    inputCountRequest.setInterval("7d");

    Optional<String> messageOptional = lambdaHandler.handleRequest(
        inputCountRequest, context);

    // Then
    Assert.assertTrue(messageOptional.isPresent());

    String message = messageOptional.get();

    Assert.assertTrue(
        message.contains(
            "errors found in *prod* environment in the last *2h*"));
  }

  private Context _getContext() {
    return new Context() {
      @Override
      public String getAwsRequestId() {
        return null;
      }

      @Override
      public String getLogGroupName() {
        return null;
      }

      @Override
      public String getLogStreamName() {
        return null;
      }

      @Override
      public String getFunctionName() {
        return null;
      }

      @Override
      public String getFunctionVersion() {
        return null;
      }

      @Override
      public String getInvokedFunctionArn() {
        return null;
      }

      @Override
      public CognitoIdentity getIdentity() {
        return null;
      }

      @Override
      public ClientContext getClientContext() {
        return null;
      }

      @Override
      public int getRemainingTimeInMillis() {
        return 0;
      }

      @Override
      public int getMemoryLimitInMB() {
        return 0;
      }

      @Override
      public LambdaLogger getLogger() {
        return System.out::println;
      }
    };
  }

}