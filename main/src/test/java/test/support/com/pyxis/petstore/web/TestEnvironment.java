package test.support.com.pyxis.petstore.web;

import org.testinfected.petstore.jdbc.DataSourceProperties;
import org.testinfected.petstore.WebLayout;
import test.support.com.pyxis.petstore.web.browser.BrowserControl;
import test.support.com.pyxis.petstore.web.browser.LastingBrowser;
import test.support.com.pyxis.petstore.web.browser.PassingBrowser;
import test.support.com.pyxis.petstore.web.browser.RemoteBrowser;
import test.support.com.pyxis.petstore.web.server.ExternalServer;
import test.support.com.pyxis.petstore.web.server.LastingServer;
import test.support.com.pyxis.petstore.web.server.PassingServer;
import test.support.com.pyxis.petstore.web.server.ServerLifeCycle;
import test.support.com.pyxis.petstore.web.server.ServerSettings;
import test.support.org.testinfected.petstore.jdbc.PropertyFile;
import test.support.org.testinfected.petstore.web.WebRoot;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.lang.Integer.parseInt;

public class TestEnvironment {

    public static final String SERVER_LIFECYCLE = "server.lifecycle";
    public static final String SERVER_SCHEME = "server.scheme";
    public static final String SERVER_HOST = "server.host";
    public static final String SERVER_PORT = "server.port";
    public static final String CONTEXT_PATH = "server.context.path";
    public static final String WEBAPP_PATH = "server.webapp.path";

    public static final String BROWSER_LIFECYCLE = "browser.lifecycle";
    public static final String BROWSER_REMOTE_URL = "browser.remote.url";
    public static final String BROWSER_REMOTE_CAPABILITY = "browser.remote.capability.";

    public static final String JDBC_URL = "jdbc.url";
    public static final String JDBC_USERNAME = "jdbc.username";
    public static final String JDBC_PASSWORD = "jdbc.password";

    private static final String TEST_PROPERTIES = "test.properties";

    private static TestEnvironment environment;

    public static TestEnvironment load() {
        if (environment == null) {
            environment = load(TEST_PROPERTIES);
        }
        return environment;
    }

    public static TestEnvironment load(String resource) {
        return new TestEnvironment(PropertyFile.load(resource));
    }

    private final Properties properties;
    private final ServerSettings serverSettings;
    private final ServerLifeCycle serverLifeCycle;
    private final BrowserControl browserControl;

    public TestEnvironment(Properties properties) {
        this.properties = overrideWithSystemProperties(properties);
        this.serverSettings = readServerSettings();
        this.serverLifeCycle = selectServer();
        this.browserControl = selectBrowser();
    }

    private Properties overrideWithSystemProperties(Properties properties) {
        properties.putAll(System.getProperties());
        return properties;
    }

    private ServerSettings readServerSettings() {
        return new ServerSettings(
                getString(SERVER_SCHEME),
                getString(SERVER_HOST),
                getInt(SERVER_PORT),
                getString(CONTEXT_PATH),
                getString(WEBAPP_PATH));
    }

    private String getString(final String key) {
        return properties.getProperty(key);
    }

    private int getInt(final String key) {
        return parseInt(getString(key));
    }

    private URL getUrl(String key) {
        String url = getString(key);
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(key + " is not a valid url: " + url, e);
        }
    }

    public Map<String, String> readBrowserCapabilities() {
        Map<String, String> capabilities = new HashMap<String, String>();
        for (String property : properties.stringPropertyNames()) {
            if (isCapability(property)) {
                capabilities.put(capabilityName(property), getString(property));
            }
        }
        return capabilities;
    }

    private String capabilityName(String property) {
        return property.substring(BROWSER_REMOTE_CAPABILITY.length());
    }

    private boolean isCapability(String property) {
        return property.startsWith(BROWSER_REMOTE_CAPABILITY);
    }

    private ServerLifeCycle selectServer() {
        String lifeCycle = getString(SERVER_LIFECYCLE);
        // new tests don't use a server lifecycle
        if (lifeCycle == null) return null;
        if (lifeCycle.equals("lasting")) return new LastingServer(serverSettings);
        if (lifeCycle.equals("passing")) return new PassingServer(serverSettings);
        if (lifeCycle.equals("external")) return new ExternalServer();
        throw new IllegalArgumentException(SERVER_LIFECYCLE + " should be one of lasting, passing, external: " + lifeCycle);
    }

    private BrowserControl selectBrowser() {
        String lifeCycle = getString(BROWSER_LIFECYCLE);
        if (lifeCycle.equals("lasting")) return new LastingBrowser();
        if (lifeCycle.equals("passing")) return new PassingBrowser();
        if (lifeCycle.equals("remote")) {
            RemoteBrowser remoteBrowser = new RemoteBrowser(getUrl(BROWSER_REMOTE_URL));
            remoteBrowser.addCapabilities(readBrowserCapabilities());
            return remoteBrowser;
        }
        throw new IllegalArgumentException(BROWSER_LIFECYCLE + " should be one of lasting, passing, remote: " + lifeCycle);
    }

    public Properties getProperties() {
        return properties;
    }

    @Deprecated
    public ServerLifeCycle getServerLifeCycle() {
        return serverLifeCycle;
    }

    public BrowserControl browserControl() {
        return browserControl;
    }

    public int serverPort() {
        return serverSettings.port;
    }

    public WebLayout webLayout() {
        return WebRoot.locate();
    }

    public DataSourceProperties databaseProperties() {
        return new DataSourceProperties(getString(JDBC_URL), getString(JDBC_USERNAME), getString(JDBC_PASSWORD));
    }

    @Deprecated
    public Routing getRoutes() {
        return new Routing(serverBaseUrl());
    }

    private String serverBaseUrl() {
        return String.format("%s://%s:%s%s", serverSettings.scheme, serverSettings.host, serverSettings.port, serverSettings.contextPath);
    }
}
