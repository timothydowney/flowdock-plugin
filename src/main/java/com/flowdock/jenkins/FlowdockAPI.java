package com.flowdock.jenkins;

import hudson.ProxyConfiguration;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URL;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;

import com.flowdock.jenkins.exception.FlowdockException;

public class FlowdockAPI {
    private static final Logger LOGGER = Logger.getLogger(FlowdockAPI.class.getName());
    private String apiUrl;
    private String flowToken;

    public FlowdockAPI(String apiUrl, String flowToken) {
        this.apiUrl = apiUrl;
        this.flowToken = trimFlowTokens(flowToken);
    }

    public void pushTeamInboxMessage(TeamInboxMessage msg) throws FlowdockException {
        try {
            doPost("/messages/team_inbox/", msg.asPostData());
        } catch (UnsupportedEncodingException ex) {
            throw new FlowdockException("Cannot encode request data: " + ex.getMessage());
        }
    }

    public void pushChatMessage(ChatMessage msg) throws FlowdockException {
        try {
            doPost("/messages/chat/", msg.asPostData());
        } catch (UnsupportedEncodingException ex) {
            throw new FlowdockException("Cannot encode request data: " + ex.getMessage());
        }
    }

    private void doPost(String path, String data) throws FlowdockException {
        URL url;
        HttpURLConnection connection = null;
        String flowdockUrl = apiUrl + path + flowToken;
        try {
            // create connection
            url = new URL(flowdockUrl);
            connection = getConnection(url);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // send the request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

            if (connection.getResponseCode() != 200) {
                StringBuffer responseContent = new StringBuffer();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        responseContent.append(responseLine);
                    }
                    in.close();
                } catch (Exception ex) {
                    // nothing we can do about this
                } finally {
                    throw new FlowdockException("Flowdock returned an error response with status "
                            + connection.getResponseCode() + " " + connection.getResponseMessage() + ", "
                            + responseContent.toString() + "\n\nURL: " + flowdockUrl);
                }
            }
        } catch (MalformedURLException ex) {
            throw new FlowdockException("Flowdock API URL is invalid: " + flowdockUrl);
        } catch (ProtocolException ex) {
            throw new FlowdockException("ProtocolException in connecting to Flowdock: " + ex.getMessage());
        } catch (IOException ex) {
            throw new FlowdockException("IOException in connecting to Flowdock: " + ex.getMessage());
        }
    }

    protected HttpURLConnection getConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection(getProxy());
    }

    /**
     * Returns the Jenkins proxy configuration. {@link Proxy#NO_PROXY} if none
     * configured.
     * 
     * @return the Jenkins proxy configuration. {@link Proxy#NO_PROXY} if none
     *         configured.
     */
    private Proxy getProxy() {
        Proxy proxy = null;
        final ProxyConfiguration proxyConf = Jenkins.getInstance().proxy;

        if (proxyConf == null) {
            proxy = Proxy.NO_PROXY;
            LOGGER.finest("No proxy found");
        } else {
            LOGGER.finest("Proxy found: " + proxyConf.name + ":" + proxyConf.port);
            System.out.println();
            SocketAddress socketAddress = new InetSocketAddress(proxyConf.name, proxyConf.port);
            proxy = new Proxy(Type.HTTP, socketAddress);

            // Considering only the presence of a username, implying there's a
            // pwd. Is it right?
            final String userName = proxyConf.getUserName();
            if (StringUtils.isNotEmpty(userName)) {
                final String passwd = proxyConf.getPassword();

                LOGGER.finest("Proxy authentication found: username=" + userName + ", password empty? "
                        + StringUtils.isEmpty(passwd));

                // Will impact the whole server instance. May not be a good idea
                // :-/.
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(userName, passwd.toCharArray());
                    }
                });
            }
        }
        return proxy;
    }

    public static String trimFlowTokens(String flowTokens) {
        return flowTokens.replaceAll("\\s", "");
    }
}
