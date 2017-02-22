package com.flowdock.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import jenkins.model.Jenkins;

/**
 * Unit tests for {@link ChatMessage}
 * 
 */
public class ChatMessageTest {

	@Mock
	private AbstractBuild build;

	private ChatMessage chatMessage;
	
	@Mock
	private BuildListener buildListener;

	@Mock
	private AbstractProject project;
	
	@Mock
	private Jenkins jenkins;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		
		// We want to make sure that things are url encoded and safe
		chatMessage = new ChatMessage();
		chatMessage.setContent("the&content<with>junk");
		chatMessage.setTags(" tags<with>junk ");
		
		when(build.getProject()).thenReturn(project);
		when(project.getRootProject()).thenReturn(project);
		when(project.getDisplayName()).thenReturn("Project Name");
		when(jenkins.getRootUrl()).thenReturn("http://localhost:8080");
		when(build.getDisplayName()).thenReturn("Build Name");
		when(build.getUrl()).thenReturn("/the-build-url");
	}
	
	@Test
	public void testAsPostData() throws UnsupportedEncodingException {
		String postData = chatMessage.asPostData();
		
		assertNotNull(postData);
		
		// This is the url encoded expectation from above
		assertEquals("content=the%26content%3Cwith%3Ejunk&external_user_name=Jenkins&tags=tags%3Cwith%3Ejunk", postData);
	}

	@Test
	public void testAsPostDataChangeExternalName() throws UnsupportedEncodingException {
		chatMessage.setExternalUserName("My Name");
		String postData = chatMessage.asPostData();
		
		assertNotNull(postData);
		assertEquals("content=the%26content%3Cwith%3Ejunk&external_user_name=My+Name&tags=tags%3Cwith%3Ejunk", postData);
	}
	
	@Test
	public void testAsPostDataWithTags() throws UnsupportedEncodingException {
		chatMessage.setTags("a-tag");
		String postData = chatMessage.asPostData();
		
		assertNotNull(postData);
		
		// This is the url encoded expectation from above
		assertEquals("content=the%26content%3Cwith%3Ejunk&external_user_name=Jenkins&tags=a-tag", postData);
	}
	
	@Test
	public void testFromBuildSuccess() throws UnsupportedEncodingException {
		when(build.getResult()).thenReturn(Result.SUCCESS);
		
		ChatMessage message = chatMessage.fromBuild(jenkins, build, BuildResult.SUCCESS, buildListener);
		
		assertNotNull(message);
		
		String postData = message.asPostData();
		
		assertNotNull(postData);
		assertEquals("content=%3Awhite_check_mark%3A%5BProject+Name+build+Build+Name+**was+successful**%5D%28http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%29&external_user_name=Jenkins&tags=",
				postData);
	}

	@Test
	public void testFromBuildUnstable() throws UnsupportedEncodingException {
		when(build.getResult()).thenReturn(Result.UNSTABLE);
		
		ChatMessage message = chatMessage.fromBuild(jenkins, build, BuildResult.UNSTABLE, buildListener);
		
		assertNotNull(message);
		
		String postData = message.asPostData();
		
		assertNotNull(postData);
		assertEquals("content=%3Aheavy_exclamation_mark%3A%5BProject+Name+build+Build+Name+**was+unstable**%5D%28http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%29&external_user_name=Jenkins&tags=",
				postData);
	}
	
	@Test
	public void testFromBuildFailure() throws UnsupportedEncodingException {
		when(build.getResult()).thenReturn(Result.FAILURE);
		
		ChatMessage message = chatMessage.fromBuild(jenkins, build, BuildResult.FAILURE, buildListener);
		
		assertNotNull(message);
		
		String postData = message.asPostData();
		
		assertNotNull(postData);
		assertEquals("content=%3Ax%3A%5BProject+Name+build+Build+Name+**failed**%5D%28http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%29&external_user_name=Jenkins&tags=",
				postData);
	}
	
	@Test
	public void testFromBuildAborted() throws UnsupportedEncodingException {
		when(build.getResult()).thenReturn(Result.ABORTED);
		
		ChatMessage message = chatMessage.fromBuild(jenkins, build, BuildResult.ABORTED, buildListener);
		
		assertNotNull(message);
		
		String postData = message.asPostData();
		
		assertNotNull(postData);
		assertEquals("content=%3Ano_entry_sign%3A%5BProject+Name+build+Build+Name+**was+aborted**%5D%28http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%29&external_user_name=Jenkins&tags=",
				postData);
	}

	@Test
	public void testFromBuildNotBuilt() throws UnsupportedEncodingException {
		when(build.getResult()).thenReturn(Result.NOT_BUILT);
		
		ChatMessage message = chatMessage.fromBuild(jenkins, build, BuildResult.NOT_BUILT, buildListener);
		
		assertNotNull(message);
		
		String postData = message.asPostData();
		
		assertNotNull(postData);
		assertEquals("content=%3Ao%3A%5BProject+Name+build+Build+Name+**was+not+built**%5D%28http%3A%2F%2Flocalhost%3A8080%2Fthe-build-url%29&external_user_name=Jenkins&tags=",
				postData);
	}
	
}
