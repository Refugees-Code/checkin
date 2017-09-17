package at.refugeescode.checkin.config;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import lombok.Getter;
import lombok.Setter;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Getter
@Setter
public class SlackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    /**
     * When this marker is used, a message will be post to Slack.
     */
    public static final Marker POST_TO_SLACK = MarkerFactory.getMarker("POST_TO_SLACK");

    private static final PatternLayout DEFAULT_LAYOUT;
    static {
        DEFAULT_LAYOUT = new PatternLayout();
        DEFAULT_LAYOUT.setPattern("%-5level [%thread]: %message%n");
    }

    private String webhookURL;
    private String channel;
    private String username;
    private String icon;
    private Layout<ILoggingEvent> layout = DEFAULT_LAYOUT;
    private EventEvaluator<ILoggingEvent> evaluator;

    private SlackApi slackApi;

    @Override
    public void start() {
        if (evaluator == null) {
            addError("No evaluator set for the appender '" + name + "'.");
            return;
        }

        if (this.webhookURL == null) {
            addError("No webhookURL set for the appender '" + name + "'.");
            return;
        }

        slackApi = new SlackApi(webhookURL);
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            if (evaluator.evaluate(event)) {
                String text = layout.doLayout(event);
                SlackMessage message = new SlackMessage(channel, username, text).setIcon(icon);
                slackApi.call(message);
            }
        } catch (EvaluationException ex) {
            addError("Exception in appender '" + name + "'.", ex);
        }
    }

}