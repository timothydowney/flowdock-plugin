package com.flowdock.jenkins;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.flowdock.jenkins.exception.FlowdockException;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

public class FlowdockNotifier extends Notifier {

    private final String flowToken;

    private String notificationTags;
    private boolean chatNotification;

    private final Map<BuildResult, Boolean> notifyMap;

    private String subject;
    private String content;
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public FlowdockNotifier(String flowToken) {
        this.flowToken = flowToken;

        this.chatNotification = true;

        // set notification map with defaults of true
        this.notifyMap = new HashMap<BuildResult, Boolean>();
        this.notifyMap.put(BuildResult.SUCCESS, true);
        this.notifyMap.put(BuildResult.FAILURE, true);
        this.notifyMap.put(BuildResult.FIXED, true);
        this.notifyMap.put(BuildResult.UNSTABLE, true);
        this.notifyMap.put(BuildResult.ABORTED, true);
        this.notifyMap.put(BuildResult.NOT_BUILT, true);
    }

    public String getFlowToken() {
        return flowToken;
    }

    public String getNotificationTags() {
        return notificationTags;
    }

    public boolean getChatNotification() {
        return chatNotification;
    }

    public boolean getNotifySuccess() {
        return notifyMap.get(BuildResult.SUCCESS);
    }
    public boolean getNotifyFailure() {
        return notifyMap.get(BuildResult.FAILURE);
    }
    public boolean getNotifyFixed() {
        return notifyMap.get(BuildResult.FIXED);
    }
    public boolean getNotifyUnstable() {
        return notifyMap.get(BuildResult.UNSTABLE);
    }
    public boolean getNotifyAborted() {
        return notifyMap.get(BuildResult.ABORTED);
    }
    public boolean getNotifyNotBuilt() {
        return notifyMap.get(BuildResult.NOT_BUILT);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        BuildResult buildResult = BuildResult.fromBuild(build);
        if(shouldNotify(buildResult)) {
            notifyFlowdock(build, buildResult, listener);
        } else {
            listener.getLogger().println("No Flowdock notification configured for build status: " + buildResult.toString());
        }
        return true;
    }

    public boolean shouldNotify(BuildResult buildResult) {
        return notifyMap.get(buildResult);
    }

    /**
     * Returns true if the value is non-null and at least one character long.
     * 
     * @param value A value.
     * @return True if the value is non-null and has at least one non-blank character.
     */
    private boolean isValueSet(String value) {
    	return value != null && value.trim().length() > 0;
    }
    
    protected void notifyFlowdock(AbstractBuild build, BuildResult buildResult, BuildListener listener) {
        PrintStream logger = listener.getLogger();
        try {
            FlowdockAPI api = new FlowdockAPI(getDescriptor().apiUrl(), flowToken);
            TeamInboxMessage msg = TeamInboxMessage.fromBuild(build, buildResult, listener);
            
            EnvVars vars = build.getEnvironment(listener);
            
            // Check for overrides for both content and subject
            if (isValueSet(content)) {
            	msg.setContent(vars.expand(content));
            }
            
            if (isValueSet(subject)) {
            	msg.setSubject(vars.expand(subject));
            }
            
            msg.setTags(vars.expand(notificationTags));
            api.pushTeamInboxMessage(msg);
            listener.getLogger().println("Flowdock: Team Inbox notification sent successfully");

            if((build.getResult() != Result.SUCCESS || buildResult == BuildResult.FIXED) && chatNotification) {
                ChatMessage chatMsg = ChatMessage.fromBuild(build, buildResult, listener);
                
                if (isValueSet(content)) {
                	chatMsg.setContent(vars.expand(content));
                }
                
                chatMsg.setTags(vars.expand(notificationTags));
                api.pushChatMessage(chatMsg);
                logger.println("Flowdock: Chat notification sent successfully");
            }
        }

        catch(IOException ex) {
            logger.println("Flowdock: failed to get variables from build");
            logger.println("Flowdock: " + ex.getMessage());
        }

        catch(InterruptedException ex) {
            logger.println("Flowdock: failed to get variables from build");
            logger.println("Flowdock: " + ex.getMessage());
        }

        catch(FlowdockException ex) {
            logger.println("Flowdock: failed to send notification");
            logger.println("Flowdock: " + ex.getMessage());
        }


    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String apiUrl = "https://api.flowdock.com";

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Flowdock notification";
        }

        public FormValidation doTestConnection(@QueryParameter("flowToken") final String flowToken,
            @QueryParameter("notificationTags") final String notificationTags) {
            try {
                FlowdockAPI api = new FlowdockAPI(apiUrl(), flowToken);
                ChatMessage testMsg = new ChatMessage();
                testMsg.setTags(notificationTags);
                testMsg.setContent("Your plugin is ready!");
                api.pushChatMessage(testMsg);
                return FormValidation.ok("Success! Flowdock plugin can send notifications to your flow.");
            } catch(FlowdockException ex) {
                return FormValidation.error(ex.getMessage());
            }
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            apiUrl = formData.getString("apiUrl");
            save();
            return super.configure(req, formData);
        }

        public String apiUrl() {
            return apiUrl;
        }
    }

	public String getSubject() {
		return subject;
	}

	public String getContent() {
		return content;
	}

	@DataBoundSetter
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@DataBoundSetter
	public void setContent(String content) {
		this.content = content;
	}

	@DataBoundSetter
	public void setNotifySuccess(boolean notifySuccess) {
		notifyMap.put(BuildResult.SUCCESS, notifySuccess);
	}

	@DataBoundSetter
	public void setNotifyFailure(boolean notifyFailure) {
		notifyMap.put(BuildResult.FAILURE, notifyFailure);
	}

	@DataBoundSetter
	public void setNotifyFixed(boolean notifyFixed) {
		notifyMap.put(BuildResult.FIXED, notifyFixed);
	}

	@DataBoundSetter
	public void setNotifyUnstable(boolean notifyUnstable) {
		notifyMap.put(BuildResult.UNSTABLE, notifyUnstable);
	}

	@DataBoundSetter
	public void setNotifyAborted(boolean notifyAborted) {
		notifyMap.put(BuildResult.ABORTED, notifyAborted);
	}

	@DataBoundSetter
	public void setNotifyNotBuilt(boolean notifyNotBuilt) {
		notifyMap.put(BuildResult.NOT_BUILT, notifyNotBuilt);
	}

	@DataBoundSetter
	public void setNotificationTags(String notificationTags) {
		this.notificationTags = notificationTags;
	}

	@DataBoundSetter
	public void setChatNotification(boolean chatNotification) {
		this.chatNotification = chatNotification;
	}
}
