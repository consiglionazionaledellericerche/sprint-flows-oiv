package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A NotificationRule.
 */
@Entity
@Table(name = "notification_rule")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class NotificationRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "process_id", nullable = false)
    private String processId;

    @NotNull
    @Column(name = "task_name", nullable = false)
    private String taskName;

    @NotNull
    @Column(name = "recipients", nullable = false)
    private String recipients;

    @NotNull
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "persona")
    private Boolean persona;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public NotificationRule processId(String processId) {
        this.processId = processId;
        return this;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getTaskName() {
        return taskName;
    }

    public NotificationRule taskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getRecipients() {
        return recipients;
    }

    public NotificationRule recipients(String recipients) {
        this.recipients = recipients;
        return this;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getEventType() {
        return eventType;
    }

    public NotificationRule eventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Boolean isPersona() {
        return persona;
    }

    public NotificationRule persona(Boolean persona) {
        this.persona = persona;
        return this;
    }

    public void setPersona(Boolean persona) {
        this.persona = persona;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationRule notificationRule = (NotificationRule) o;
        if(notificationRule.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, notificationRule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "NotificationRule{" +
            "id=" + id +
            ", processId='" + processId + "'" +
            ", taskName='" + taskName + "'" +
            ", recipients='" + recipients + "'" +
            ", eventType='" + eventType + "'" +
            ", persona='" + persona + "'" +
            '}';
    }
}
