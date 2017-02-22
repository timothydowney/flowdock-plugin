package com.flowdock.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM.EntryImpl;
import org.jvnet.hudson.test.FakeChangeLogSCM.FakeChangeLogSet;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.scm.ChangeLogSet.Entry;
import jenkins.model.Jenkins;

/**
 * Unit tests for {@link TeamInboxMessage}
 * 
 *
 */
public class TeamInboxMessageTest {

	private TeamInboxMessage message;
	
	@Mock
	private AbstractProject parent;
	
	@Mock
	private AbstractBuild build;

	private ChatMessage chatMessage;
	
	@Mock
	private BuildListener buildListener;

	@Mock
	private AbstractProject project;
	
	@Mock
	private Jenkins jenkins;
	
	@Mock
	private EnvVars envVars;

	@Mock
	private SCM scm;
	
	@Mock
	private ChangeLogSet<? extends Entry> changeLogSet;
	
	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		message = new TeamInboxMessage();
		message.setContent("message-content");
		message.setFromAddress("jenkins@email.address");
		message.setFromName("Jenkins Build");
		message.setLink("http://localhost/thelink");
		message.setProject("Project Foo");
		message.setSource("the-source");
		message.setSubject("Subject Line");
		message.setTags("tag-a, tag-b");
		
		when(build.getProject()).thenReturn(project);
		when(project.getRootProject()).thenReturn(project);
		when(project.getDisplayName()).thenReturn("Project Name");
		when(jenkins.getRootUrl()).thenReturn("http://localhost:8080");
		when(build.getDisplayName()).thenReturn("Build Name");
		when(build.getUrl()).thenReturn("/the-build-url");
		when(build.getEnvironment(buildListener)).thenReturn(envVars);
	}
	
	@Test
	public void testAsPostData() throws UnsupportedEncodingException {
		String postData = message.asPostData();
		
		assertEquals("subject=Subject+Line&content=message-content&from_address=jenkins%40email.address&from_name=Jenkins+Build&source=the-source&project=Project+Foo&link=http%3A%2F%2Flocalhost%2Fthelink&tags=tag-a%2Ctag-b",
				postData);
	}

	@Test
	public void testFromBuildSuccessWithGit() throws Exception {
		when(envVars.get("GIT_BRANCH")).thenReturn("master");
		when(envVars.get("GIT_URL")).thenReturn("git://git.url");
		
		when(build.getResult()).thenReturn(Result.SUCCESS);
		
		TeamInboxMessage inboxMessage = message.fromBuild(jenkins, 
				build, BuildResult.SUCCESS, buildListener);
		
		assertNotNull(inboxMessage);
		assertEquals("subject=Project+Name+build+Build+Name+was+successful&content=%3Ch3%3EProject+Name%3C%2Fh3%3EBuild%3A+Build+Name%3Cbr+%2F%3EResult%3A+%3Cstrong%3ESUCCESS%3C%2Fstrong%3E%3Cbr+%2F%3EURL%3A+%3Ca+href%3D%22http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%22%3Enull%3C%2Fa%3E%3Cbr+%2F%3E%3Cbr+%2F%3E%3Cstrong%3EVersion+control%3A%3C%2Fstrong%3E%3Cbr+%2F%3EGit+branch%3A+master%3Cbr%2F%3EGit+URL%3A+git%3A%2F%2Fgit.url%3Cbr%2F%3E%3Cbr%2F%3E&from_address=build%2Bok%40flowdock.com&from_name=CI&source=Jenkins&project=Project+Name&link=http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url&tags=",
				inboxMessage.asPostData());
	}

	@Test
	public void testFromBuildSuccess() throws Exception {
		when(build.getResult()).thenReturn(Result.SUCCESS);
		
		TeamInboxMessage inboxMessage = message.fromBuild(jenkins, 
				build, BuildResult.SUCCESS, buildListener);
		
		assertNotNull(inboxMessage);
		assertEquals("subject=Project+Name+build+Build+Name+was+successful&content=%3Ch3%3EProject+Name%3C%2Fh3%3EBuild%3A+Build+Name%3Cbr+%2F%3EResult%3A+%3Cstrong%3ESUCCESS%3C%2Fstrong%3E%3Cbr+%2F%3EURL%3A+%3Ca+href%3D%22http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%22%3Enull%3C%2Fa%3E%3Cbr+%2F%3E&from_address=build%2Bok%40flowdock.com&from_name=CI&source=Jenkins&project=Project+Name&link=http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url&tags=",
				inboxMessage.asPostData());
	}

	@Test
	public void testFromBuildSuccessWithChangeSets() throws Exception {
		/*
		 * TODO:  This whole behavior is pretty gnarly stuff.  The act of managing
		 * changesets is terribly complicated and needs some refactoring.
		 */
		changeLogSet = setupChangeLogSet(build);

		when(build.getResult()).thenReturn(Result.SUCCESS);
		when(build.getChangeSet()).thenReturn(changeLogSet);
		
		TeamInboxMessage inboxMessage = message.fromBuild(jenkins, 
				build, BuildResult.SUCCESS, buildListener);
		
		assertNotNull(inboxMessage);
		assertEquals("subject=Project+Name+build+Build+Name+was+successful&content=%3Ch3%3EProject+Name%3C%2Fh3%3EBuild%3A+Build+Name%3Cbr+%2F%3EResult%3A+%3Cstrong%3ESUCCESS%3C%2Fstrong%3E%3Cbr+%2F%3EURL%3A+%3Ca+href%3D%22http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%22%3Enull%3C%2Fa%3E%3Cbr+%2F%3E%3Ch3%3EChanges%3C%2Fh3%3E%3Cdiv+class%3D%22commits%22%3E%3Cul+class%3D%22commit-list+clean%22%3E%3Cli+class%3D%22commit%22%3E%3Cspan+class%3D%22commit-details%22%3E%3Cspan+class%3D%22author-info%22%3E%3Cspan%3Enull%3C%2Fspan%3E%3C%2Fspan%3E+%26nbsp%3B%3Cspan+title%3D%22unknown%22+class%3D%22commit-sha%22%3Eunknown%3C%2Fspan%3E+%26nbsp%3B%3Cspan+class%3D%22commit-message%22%3Echange%3C%2Fspan%3E%3C%2Fspan%3E%3C%2Fli%3E%3C%2Ful%3E%3C%2Fdiv%3E&from_address=build%2Bok%40flowdock.com&from_name=CI&source=Jenkins&project=Project+Name&link=http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url&tags=",
				inboxMessage.asPostData());
	}

	private ChangeLogSet<? extends Entry> setupChangeLogSet(AbstractBuild build) {
		Set<MyChangeLogEntry> changes = new HashSet<TeamInboxMessageTest.MyChangeLogEntry>();
		changes.add(new MyChangeLogEntry("change", "path"));
		
		return new MyChangeLogSet(build, null, changes);
	}
	
	private EntryImpl setupEntry(String author) {
		EntryImpl entry = new EntryImpl().withAuthor(author);
		return entry;
	}

	@Test
	public void testFromBuildFailWithGit() throws Exception {
		when(envVars.get("GIT_BRANCH")).thenReturn("master");
		when(envVars.get("GIT_URL")).thenReturn("git://git.url");
		
		when(build.getResult()).thenReturn(Result.FAILURE);
		
		TeamInboxMessage inboxMessage = message.fromBuild(jenkins, 
				build, BuildResult.FAILURE, buildListener);
		
		assertNotNull(inboxMessage);
		assertEquals("subject=Project+Name+build+Build+Name+failed&content=%3Ch3%3EProject+Name%3C%2Fh3%3EBuild%3A+Build+Name%3Cbr+%2F%3EResult%3A+%3Cstrong%3EFAILURE%3C%2Fstrong%3E%3Cbr+%2F%3EURL%3A+%3Ca+href%3D%22http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%22%3Enull%3C%2Fa%3E%3Cbr+%2F%3E%3Cbr+%2F%3E%3Cstrong%3EVersion+control%3A%3C%2Fstrong%3E%3Cbr+%2F%3EGit+branch%3A+master%3Cbr%2F%3EGit+URL%3A+git%3A%2F%2Fgit.url%3Cbr%2F%3E%3Cbr%2F%3E&from_address=build%2Bfail%40flowdock.com&from_name=CI&source=Jenkins&project=Project+Name&link=http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url&tags=",
				inboxMessage.asPostData());
	}
	
	private static final class MyChangeLogSet extends ChangeLogSet<MyChangeLogEntry> {

		private Set<MyChangeLogEntry> changes;
		
		public MyChangeLogSet(Run<?, ?> run, RepositoryBrowser<?> browser, Set<MyChangeLogEntry> changes) {
			super(run, browser);
			this.changes = changes;
		}

		@Override
		public Iterator<MyChangeLogEntry> iterator() {
			return changes.iterator();
		}

		@Override
		public boolean isEmptySet() {
			return changes.isEmpty();
		}
		
	}
	
	private static final class MyChangeLogEntry extends Entry {

		private String message;
		private String path;
		
		public MyChangeLogEntry(String message, String path) {
			this.message = message;
			this.path = path;
		}
		
		@Override
		public String getMsg() {
			return message;
		}

		@Override
		public User getAuthor() {
			return null;
		}

		@Override
		public Collection<String> getAffectedPaths() {
			return Arrays.asList(path);
		}
		
	}
}
