package at.refugeescode.checkin;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class SlackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    /**
     * When this marker is used, a message will be post to Slack.
     */
    public static final Marker POST_TO_SLACK = MarkerFactory.getMarker("POST_TO_SLACK");

    private static final PatternLayout DEFAULT_LAYOUT = new PatternLayout();
    static {
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

    public String getWebhookURL() {
        return webhookURL;
    }

    public void setWebhookURL(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
        if (this.icon != null && this.icon.startsWith(":") && !this.icon.endsWith(":")) {
            this.icon += ":";
        }
    }

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public EventEvaluator<ILoggingEvent> getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(EventEvaluator<ILoggingEvent> evaluator) {
        this.evaluator = evaluator;
    }

}