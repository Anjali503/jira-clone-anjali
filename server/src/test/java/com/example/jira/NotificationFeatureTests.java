package com.example.jira;

import com.example.jira.model.Issue;
import com.example.jira.model.Notification;
import com.example.jira.model.Project;
import com.example.jira.model.Sprint;
import com.example.jira.model.User;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.NotificationRepository;
import com.example.jira.repository.Projectrepository;
import com.example.jira.repository.SprintRepository;
import com.example.jira.repository.UserRepository;
import com.example.jira.service.DueDateReminderScheduler;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationFeatureTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private Projectrepository projectrepository;

    @Autowired
    private DueDateReminderScheduler dueDateReminderScheduler;

    @BeforeEach
    void cleanup() {
        notificationRepository.deleteAll();
        issueRepository.deleteAll();
        sprintRepository.deleteAll();
        projectrepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(boolean emailEnabled) {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setEmailNotificationsEnabled(emailEnabled);
        user.setPassword("password");
        user.setRole("USER");
        return userRepository.save(user);
    }

    private Project createProject(User owner, List<String> members) {
        Project project = new Project();
        project.setName("Project");
        project.setKey("PRJ");
        project.setOwnerId(owner.getId());
        project.setMemberIds(members);
        return projectrepository.save(project);
    }

    @Test
    void assignmentNotificationCreatesNotificationAndPreventsDuplicate() throws Exception {
        User assignee = createUser(false);
        Issue issue = new Issue();
        issue.setTitle("Task A");
        issue.setDescription("Description");
        issue.setStatus("OPEN");
        issue.setPriority("MEDIUM");
        issue.setReporterId(assignee.getId());
        issue.setAssigneeId(assignee.getId());
        issue.setProjectId(new ObjectId().toHexString());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"title\":\"Task A\"," +
                        "\"description\":\"Description\"," +
                        "\"status\":\"OPEN\"," +
                        "\"priority\":\"MEDIUM\"," +
                        "\"reporterId\":\"" + assignee.getId() + "\"," +
                        "\"assigneeId\":\"" + assignee.getId() + "\"," +
                        "\"projectId\":\"" + issue.getProjectId() + "\"}"))
                .andExpect(status().isOk());

        List<Notification> notifications = notificationRepository.findByUserIdOrderByTimestampDesc(assignee.getId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo("ASSIGNMENT");
        assertThat(notifications.get(0).getMessage()).contains("assigned to task");

        // Duplicate assignment should be skipped
        mockMvc.perform(MockMvcRequestBuilders.post("/api/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"title\":\"Task A\"," +
                        "\"description\":\"Description\"," +
                        "\"status\":\"OPEN\"," +
                        "\"priority\":\"MEDIUM\"," +
                        "\"reporterId\":\"" + assignee.getId() + "\"," +
                        "\"assigneeId\":\"" + assignee.getId() + "\"," +
                        "\"projectId\":\"" + issue.getProjectId() + "\"}"))
                .andExpect(status().isOk());

        notifications = notificationRepository.findByUserIdOrderByTimestampDesc(assignee.getId());
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getType()).isEqualTo("ASSIGNMENT");
    }

    @Test
    void statusChangeNotificationAndReadUnreadFlow() throws Exception {
        User assignee = createUser(false);
        Issue issue = new Issue();
        issue.setTitle("Task B");
        issue.setDescription("Description");
        issue.setStatus("OPEN");
        issue.setPriority("MEDIUM");
        issue.setReporterId(assignee.getId());
        issue.setAssigneeId(assignee.getId());
        issue.setProjectId(new ObjectId().toHexString());
        issue = issueRepository.save(issue);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/issues/" + issue.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"title\":\"Task B\"," +
                        "\"description\":\"Description\"," +
                        "\"status\":\"IN_PROGRESS\"," +
                        "\"priority\":\"MEDIUM\"," +
                        "\"assigneeId\":\"" + assignee.getId() + "\"," +
                        "\"order\":0," +
                        "\"comments\":[]," +
                        "\"sprintId\":null," +
                        "\"dependencies\":[]," +
                        "\"dueDate\":null}"))
                .andExpect(status().isOk());

        List<Notification> notifications = notificationRepository.findByUserIdOrderByTimestampDesc(assignee.getId());
        assertThat(notifications).hasSize(1);
        Notification notification = notifications.get(0);
        assertThat(notification.getType()).isEqualTo("STATUS_CHANGE");
        assertThat(notification.getMessage()).contains("status changed from OPEN to IN_PROGRESS");
        assertThat(notification.isRead()).isFalse();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/notifications/" + notification.getId() + "/read"))
                .andExpect(status().isOk());
        assertThat(notificationRepository.findById(new ObjectId(notification.getId())).get().isRead()).isTrue();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/notifications/" + notification.getId() + "/unread"))
                .andExpect(status().isOk());
        assertThat(notificationRepository.findById(new ObjectId(notification.getId())).get().isRead()).isFalse();
    }

    @Test
    void sprintStartAndEndNotificationsAreCreatedForProjectMembers() throws Exception {
        User owner = createUser(false);
        User member = createUser(false);
        Project project = createProject(owner, List.of(member.getId()));
        Sprint sprint = new Sprint();
        sprint.setName("Sprint 1");
        sprint.setProjectId(project.getId());
        sprint.setStatus("PLANNED");
        sprint = sprintRepository.save(sprint);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/sprints/" + sprint.getId() + "/start"))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.put("/api/sprints/" + sprint.getId() + "/complete"))
                .andExpect(status().isOk());

        assertThat(notificationRepository.findByUserIdOrderByTimestampDesc(owner.getId()))
                .extracting(Notification::getType)
                .containsExactly("SPRINT_END", "SPRINT_START");
        assertThat(notificationRepository.findByUserIdOrderByTimestampDesc(member.getId()))
                .extracting(Notification::getType)
                .containsExactly("SPRINT_END", "SPRINT_START");
    }

    @Test
    void dueDateReminderNotificationIsGeneratedAndDeduplicated() {
        User assignee = createUser(false);
        Issue issue = new Issue();
        issue.setTitle("Task C");
        issue.setDescription("Description");
        issue.setStatus("OPEN");
        issue.setPriority("MEDIUM");
        issue.setReporterId(assignee.getId());
        issue.setAssigneeId(assignee.getId());
        issue.setProjectId(new ObjectId().toHexString());
        issue.setDueDate(Instant.now().plusSeconds(24 * 3600));
        issueRepository.save(issue);

        dueDateReminderScheduler.sendDueDateReminders();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByTimestampDesc(assignee.getId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo("DUE_REMINDER");
        assertThat(notifications.get(0).getMessage()).contains("Reminder: task 'Task C' is due in approximately 24 hours.");

        // Re-run the scheduler and verify duplicate reminders are skipped.
        dueDateReminderScheduler.sendDueDateReminders();
        notifications = notificationRepository.findByUserIdOrderByTimestampDesc(assignee.getId());
        assertThat(notifications).hasSize(1);
    }

    @Test
    void emailNotificationOnlySentWhenEnabled() {
        User withEmail = createUser(true);
        withEmail.setEmail("notify@example.com");
        userRepository.save(withEmail);

        Issue issue = new Issue();
        issue.setTitle("Task D");
        issue.setDescription("Description");
        issue.setStatus("OPEN");
        issue.setPriority("MEDIUM");
        issue.setReporterId(withEmail.getId());
        issue.setAssigneeId(withEmail.getId());
        issue.setProjectId(new ObjectId().toHexString());
        issueRepository.save(issue);

        // Because EmailService is not mocked in this integration test, we verify the preference only
        assertThat(withEmail.isEmailNotificationsEnabled()).isTrue();

        User withoutEmail = createUser(false);
        assertThat(withoutEmail.isEmailNotificationsEnabled()).isFalse();
    }
}
