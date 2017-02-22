package com.flowdock.jenkins;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.EnvVars;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import jenkins.model.Jenkins;

import java.io.UnsupportedEncodingException;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class TeamInboxMessage extends FlowdockMessage {

    /*
     * Default sender email addresses for displaying Gravatar icons in build
     * notification based on the build status. You can also setup your own email
     * addresses and configure custom Gravatar icons for them.
     */
    public static final String FLOWDOCK_BUILD_OK_EMAIL = "build+ok@flowdock.com";
    public static final String FLOWDOCK_BUILD_FAIL_EMAIL = "build+fail@flowdock.com";

    protected String source;
    protected String project;
    protected String subject;
    protected String link;
    protected String fromAddress;
    protected String fromName;

    public TeamInboxMessage() {
        this.source = "Jenkins";
        this.fromAddress = FLOWDOCK_BUILD_OK_EMAIL;
        this.fromName = "CI";
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String asPostData() throws UnsupportedEncodingException {
        StringBuilder postData = new StringBuilder();
        postData.append("subject=").append(urlEncode(subject));
        postData.append("&content=").append(urlEncode(content));
        postData.append("&from_address=").append(urlEncode(fromAddress));
        postData.append("&from_name=").append(urlEncode(fromName));
        postData.append("&source=").append(urlEncode(source));
        postData.append("&project=").append(urlEncode(project));
        postData.append("&link=").append(urlEncode(link));
        postData.append("&tags=").append(urlEncode(removeWhitespace(tags)));
        return postData.toString();
    }

    public static TeamInboxMessage fromBuild(Jenkins jenkins, AbstractBuild build, BuildResult buildResult,
            BuildListener listener) throws IOException, InterruptedException {
        TeamInboxMessage msg = new TeamInboxMessage();

        String projectName = "";
        String configuration = "";
        if (build.getProject().getRootProject() != build.getProject()) {
            projectName = build.getProject().getRootProject().getDisplayName();
            configuration = " on " + build.getProject().getDisplayName();
        } else {
            projectName = build.getProject().getDisplayName();
        }

        msg.setProject(projectName.replaceAll("[^a-zA-Z0-9\\-_ ]", ""));
        String buildNo = build.getDisplayName().replaceAll("#", "");
        msg.setSubject(projectName + " build " + buildNo + configuration + " " + buildResult.getHumanResult());

        String rootUrl = jenkins.getRootUrl();
        String buildLink = (rootUrl == null) ? null : rootUrl + build.getUrl();
        if (buildLink != null)
            msg.setLink(buildLink);

        if (build.getResult().isWorseThan(Result.SUCCESS))
            msg.setFromAddress(FLOWDOCK_BUILD_FAIL_EMAIL);

        StringBuilder content = new StringBuilder();
        content.append("<h3>").append(projectName).append("</h3>");
        content.append("Build: ").append(build.getDisplayName()).append("<br />");
        content.append("Result: <strong>").append(buildResult.toString()).append("</strong><br />");
        if (buildLink != null)
            content.append("URL: <a href=\"").append(buildLink).append("\">").append(build.getFullDisplayName())
                    .append("</a>").append("<br />");

        EnvVars envVars = build.getEnvironment(listener);
        String vcsInfo = versionControlVariableList(envVars);
        if (vcsInfo.length() > 0) {
            content.append("<br /><strong>Version control:</strong><br />");
            content.append(vcsInfo);
            content.append("<br/>");
        }

        List<Entry> commits = parseCommits(build);
        if (commits != null) {
            content.append("<h3>Changes</h3><div class=\"commits\"><ul class=\"commit-list clean\">");
            for (Entry commit : commits) {
                content.append("<li class=\"commit\"><span class=\"commit-details\">");
                content.append("<span class=\"author-info\">").append("<span>").append(commit.getAuthor())
                        .append("</span>").append("</span> &nbsp;");
                content.append("<span title=\"" + commitId(commit) + "\" class=\"commit-sha\">")
                        .append(commitId(commit, 7)).append("</span> &nbsp;");
                content.append("<span class=\"commit-message\">").append(escapeHtml(commit.getMsg())).append("</span>");
                content.append("</span></li>");
            }
            content.append("</ul></div>");
        }

        msg.setContent(content.toString());

        return msg;
    }

    public static List<Entry> parseCommits(AbstractBuild build) {
        final ChangeLogSet<? extends Entry> cs = build.getChangeSet();
        if (cs == null || cs.isEmptySet())
            return null;

        List<Entry> commits = new ArrayList();
        for (final Entry entry : cs) {
            // reverse order in order to have recent commits first
            commits.add(0, entry);
        }
        return commits;
    }

    private static String commitId(Entry commit) {
        String id = commit.getCommitId();
        if (id == null) {
            return "unknown";
        } else {
            return id;
        }
    }

    private static String commitId(Entry commit, int length) {
        String id = commitId(commit);
        return id.substring(0, Math.min(length, id.length()));
    }

    private static String versionControlVariableList(EnvVars envVars) {
        StringBuilder envList = new StringBuilder();

        if (envVars.get("GIT_BRANCH") != null) {
            envList.append("Git branch: ").append(envVars.get("GIT_BRANCH")).append("<br/>");
        }
        if (envVars.get("GIT_URL") != null) {
            envList.append("Git URL: ").append(envVars.get("GIT_URL")).append("<br/>");
        }
        if (envVars.get("SVN_REVISION") != null) {
            envList.append("SVN revision: ").append(envVars.get("SVN_REVISION")).append("<br/>");
        }
        if (envVars.get("SVN_URL") != null) {
            envList.append("SVN URL: ").append(envVars.get("SVN_URL")).append("<br/>");
        }

        return envList.toString();
    }
}
