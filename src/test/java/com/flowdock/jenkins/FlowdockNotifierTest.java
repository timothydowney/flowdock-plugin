package com.flowdock.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.flowdock.jenkins.FlowdockNotifier.DescriptorImpl;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;

/**
 * Unit tests for {@link FlowdockNotifier}
 * 
 */
public class FlowdockNotifierTest {

    private static final String TOKEN = "the-token";

    private FlowdockNotifier notifier;

    @Mock
    private AbstractBuild build;

    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener listener;

    private PrintStream printStream;

    private DescriptorImpl descriptor;

    @Mock
    private ChatMessage chatMessage;

    @Mock
    private TeamInboxMessage teamInboxMessage;

    @Mock
    private EnvVars envVars;

    @Mock
    private FlowdockAPI flowdockAPI;

    @Before
    public void setup() throws IOException, InterruptedException {
        MockitoAnnotations.initMocks(this);

        descriptor = new DescriptorImpl();

        notifier = new FlowdockNotifier(TOKEN) {
            @Override
            public DescriptorImpl getDescriptor() {
                return descriptor;
            }

            @Override
            protected ChatMessage chatMessageFromBuild(AbstractBuild build, BuildResult buildResult,
                    BuildListener listener) {
                return chatMessage;
            }

            @Override
            protected TeamInboxMessage teamInboxMessageFromBuild(AbstractBuild build, BuildResult buildResult,
                    BuildListener listener) throws IOException, InterruptedException {
                return teamInboxMessage;
            }

            @Override
            protected FlowdockAPI getFlowdockAPI() {
                return flowdockAPI;
            }
        };

        printStream = new PrintStream(new ByteArrayOutputStream());
        when(listener.getLogger()).thenReturn(printStream);

        when(build.getEnvironment(listener)).thenReturn(envVars);
    }

    @Test
    public void testNeedsToRunAfterFinalized() {
        assertTrue(notifier.needsToRunAfterFinalized());
    }

    @Test
    public void testDefaultBehaviors() {
        assertEquals(TOKEN, notifier.getFlowToken());
        assertNull(notifier.getContent());
        assertNull(notifier.getSubject());
        assertNull(notifier.getNotificationTags());
        assertTrue(notifier.getChatNotification());
        assertTrue(notifier.getNotifyAborted());
        assertTrue(notifier.getNotifyFailure());
        assertTrue(notifier.getNotifyFixed());
        assertTrue(notifier.getNotifyNotBuilt());
        assertTrue(notifier.getNotifySuccess());
        assertTrue(notifier.getNotifyUnstable());
    }

    @Test
    public void testLegacyConstructor() {
        String chatNotification = "true";
        String notifySuccess = "false";
        String notifyFailure = "false";
        String notifyFixed = "";
        String notifyUnstable = null;
        String notifyAborted = "not-a-value";
        String notifyNotBuilt = "true";

        notifier = new FlowdockNotifier(TOKEN, "tags", chatNotification, notifySuccess, notifyFailure, notifyFixed,
                notifyUnstable, notifyAborted, notifyNotBuilt);

        assertTrue(notifier.getChatNotification());
        assertFalse(notifier.getNotifySuccess());
        assertFalse(notifier.getNotifyFailure());
        assertFalse(notifier.getNotifyFixed());
        assertFalse(notifier.getNotifyUnstable());
        assertFalse(notifier.getNotifyAborted());
        assertTrue(notifier.getNotifyNotBuilt());

        assertEquals("tags", notifier.getNotificationTags());
        assertEquals(TOKEN, notifier.getFlowToken());
    }

    @Test
    public void testSubjectAndContent() {
        notifier.setSubject("the-subject");
        notifier.setContent("the-content");

        assertEquals("the-subject", notifier.getSubject());
        assertEquals("the-content", notifier.getContent());
    }

    @Test
    public void testGetRequiredMonitorService() {
        assertSame(BuildStepMonitor.NONE, notifier.getRequiredMonitorService());
    }

    @Test
    public void testPerformSuccessTraditionalBehavior() throws Exception {
        notifier.setNotificationTags("unexpanded-tags");
        when(envVars.expand("unexpanded-tags")).thenReturn("expanded-tags");

        when(build.getResult()).thenReturn(Result.SUCCESS);

        assertTrue(notifier.perform(build, launcher, listener));

        ArgumentCaptor<TeamInboxMessage> captor = ArgumentCaptor.forClass(TeamInboxMessage.class);
        verify(flowdockAPI).pushTeamInboxMessage(captor.capture());
        assertSame(teamInboxMessage, captor.getValue());

        verify(teamInboxMessage).setTags("expanded-tags");
    }

    @Test
    public void testPerformSuccessWithContentAndSubject() throws Exception {
        notifier.setContent("unexpanded-content");
        notifier.setSubject("unexpanded-subject");

        when(envVars.expand("unexpanded-content")).thenReturn("expanded-content");
        when(envVars.expand("unexpanded-subject")).thenReturn("expanded-subject");

        when(build.getResult()).thenReturn(Result.SUCCESS);

        assertTrue(notifier.perform(build, launcher, listener));

        ArgumentCaptor<TeamInboxMessage> captor = ArgumentCaptor.forClass(TeamInboxMessage.class);
        verify(flowdockAPI).pushTeamInboxMessage(captor.capture());
        assertSame(teamInboxMessage, captor.getValue());

        verify(teamInboxMessage).setContent("expanded-content");
        verify(teamInboxMessage).setSubject("expanded-subject");
    }

    @Test
    public void testPerformFailure() throws Exception {
        when(build.getResult()).thenReturn(Result.FAILURE);

        assertTrue(notifier.perform(build, launcher, listener));

        verify(flowdockAPI).pushTeamInboxMessage(any(TeamInboxMessage.class));
        verify(flowdockAPI).pushChatMessage(any(ChatMessage.class));

    }

    @Test
    public void testPerformFailureWithContent() throws Exception {
        notifier.setContent("unexpanded-content");
        notifier.setSubject("unexpanded-subject");

        when(envVars.expand("unexpanded-content")).thenReturn("expanded-content");
        when(envVars.expand("unexpanded-subject")).thenReturn("expanded-subject");

        when(build.getResult()).thenReturn(Result.FAILURE);

        assertTrue(notifier.perform(build, launcher, listener));

        verify(flowdockAPI).pushTeamInboxMessage(any(TeamInboxMessage.class));
        verify(flowdockAPI).pushChatMessage(any(ChatMessage.class));

        verify(chatMessage).setContent("expanded-content");
    }

}
