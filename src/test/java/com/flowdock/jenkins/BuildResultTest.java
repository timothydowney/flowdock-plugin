package com.flowdock.jenkins;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.model.AbstractBuild;
import hudson.model.Result;

/**
 * Unit tests for {@link BuildResult} 
 *
 */
public class BuildResultTest {

	@Mock
	private AbstractBuild build;
	
	@Mock
	private AbstractBuild previousBuild;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testFromBuildSuccessNoPreviousBuild() {
		when(build.getResult()).thenReturn(Result.SUCCESS);
		when(build.getPreviousBuild()).thenReturn(null);
		
		assertEquals(BuildResult.SUCCESS, BuildResult.fromBuild(build));
	}

	@Test
	public void testFromBuildSuccessPreviousBuildFailure() {
		when(build.getResult()).thenReturn(Result.SUCCESS);
		when(build.getPreviousBuild()).thenReturn(previousBuild);
		when(previousBuild.getResult()).thenReturn(Result.FAILURE);
		
		assertEquals(BuildResult.FIXED, BuildResult.fromBuild(build));
	}

	@Test
	public void testFromBuildSuccessPreviousBuildUnstable() {
		when(build.getResult()).thenReturn(Result.SUCCESS);
		when(build.getPreviousBuild()).thenReturn(previousBuild);
		when(previousBuild.getResult()).thenReturn(Result.UNSTABLE);
		
		assertEquals(BuildResult.FIXED, BuildResult.fromBuild(build));
	}
	
	@Test
	public void testFromBuildUnstable() {
		when(build.getResult()).thenReturn(Result.UNSTABLE);
		
		assertEquals(BuildResult.UNSTABLE, BuildResult.fromBuild(build));
	}
	
	@Test
	public void testFromBuildAborted() {
		when(build.getResult()).thenReturn(Result.ABORTED);
		
		assertEquals(BuildResult.ABORTED, BuildResult.fromBuild(build));
	}

	@Test
	public void testFromBuildNotBuilt() {
		when(build.getResult()).thenReturn(Result.NOT_BUILT);
		
		assertEquals(BuildResult.NOT_BUILT, BuildResult.fromBuild(build));
	}

	@Test
	public void testFromBuildFailure() {
		when(build.getResult()).thenReturn(Result.FAILURE);
		
		assertEquals(BuildResult.FAILURE, BuildResult.fromBuild(build));
	}
	
}
