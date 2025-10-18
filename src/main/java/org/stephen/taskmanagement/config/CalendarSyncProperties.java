package org.stephen.taskmanagement.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.calendar")
@Getter
@Setter
public class CalendarSyncProperties {
    private Boolean enabled = true;
    private String credentialsPath = "credentials.json";
    private String primaryCalendarId = "primary";
    private Integer syncIntervalMinutes = 5;
    private Boolean autoSyncEnabled = true;
    private String conflictResolutionStrategy = "TASK_WINS";
    private Boolean webhookEnabled = false;
    private String webhookUrl;
    private Integer maxRetries = 3;
    private Integer retryDelaySeconds = 5;
}
