# AWS Lambda for periodically checking log messages in Elasticsearch and sending a message to slack if there are no messages or there are errors or truncated messages:

Lambda which checks periodically (by default, every hour) whether there
are log messages in Elasticsearch.

If there are no log messages in the last interval (by default, in the
last hour) it sends a message to a Slack channel.

If there are log messages, it checks whether there are any errors in the
 last interval.

If there are errors it sends a message to a Slack channel.

If there are log messages, it checks whether there are any truncated
messages in the last interval.

If there are truncated messages it sends a message to a Slack channel.

A parameter of type
`com.liferay.osb.pulpo.lambda.handler.elasticsearch.CountRequest` can be
 provided to the Lambda to override the default values.

Example:

```
{
  	"environment" : "prod",
  	"host" : "http://127.0.0.1:999",
  	"interval" : "1s"
}
```

The default interval is set to `1h`.

The following environment variables are expected:

- *CHANNEL*: The name of the slack channel where the message should be
sent.
e.g: `#pulpo-dynatrace-prod

- *WEB_HOOK_URL*: The slack web hook url that should be used to send the
 message.
 e.g: `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX`
