package org.jenkinsci.test.acceptance.po;

import com.google.inject.Injector;
import hudson.util.VersionNumber;
import org.jenkinsci.test.acceptance.controller.JenkinsController;
import org.jenkinsci.test.acceptance.guice.TestScope;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Top-level object that acts as an entry point to various systems.
 *
 * This is also the only page object that can be injected since there's always one that points to THE Jenkins instance
 * under test.
 *
 * @author Kohsuke Kawaguchi
 */
@TestScope
public class Jenkins extends ContainerPageObject {
    private VersionNumber version;

    public Jenkins(Injector injector, URL url) {
        super(injector,url);
        getVersion();
    }

    @Inject
    public Jenkins(Injector injector, JenkinsController controller) {
        this(injector, startAndGetUrl(controller));
    }

    private static URL startAndGetUrl(JenkinsController controller) {
        try {
            controller.start();
            return controller.getUrl();
        } catch (IOException e) {
            throw new AssertionError("Failed to start JenkinsController",e);
        }
    }

    /**
     * Get the version of Jenkins under test.
     */
    public VersionNumber getVersion() {
        if (version!=null)      return  version;

        String prefix = "About Jenkins ";
        visit("about");
        String text = waitFor(by.xpath("//h1[starts-with(., '"+prefix+"')]")).getText();

        Matcher m = VERSION.matcher(text);
        if (m.find())
            return version = new VersionNumber(m.group(1));
        else
            throw new AssertionError("Unexpected version string: "+text);
    }

    public <T extends Job> T createJob(Class<T> type, String name) {
        String sut_type = type.getAnnotation(JobPageObject.class).value();

        visit("newJob");
        fillIn("name", name);
        find(by.xpath("//input[starts-with(@value, '"+sut_type+"')]")).click();
        clickButton("OK");

        return getJob(type, name);
    }

    public <T extends Job> T getJob(Class<T> type, String name) {
        try {
            return type.getConstructor(Injector.class,URL.class,String.class)
                    .newInstance(injector, url("job/%s/", name), name);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public FreeStyleJob createJob() {
        return createJob(FreeStyleJob.class);
    }

    public <T extends Job> T createJob(Class<T> type) {
        return createJob(type, createRandomName());
    }

    public void copyJob(Job from, String to) {
        copyJob(from.name,to);
    }

    public void copyJob(String from, String to) {
        visit("newJob");
        fillIn("name",to);
        check(find(by.radioButton("copy")));
        fillIn("from",from);
        clickButton("OK");
    }

    /**
     * Access global configuration page.
     */
    public JenkinsConfig getConfigPage() {
        return new JenkinsConfig(this);
    }

    /**
     * Access the plugin manager page object
     */
    public PluginManager getPluginManager() {
        return new PluginManager(this);
    }

    public JenkinsLogger getLogger(String name) {
        return new JenkinsLogger(this,name);
    }

    public JenkinsLogger createLogger(String name, Map<String,Level> levels) {
        return JenkinsLogger.create(this,name,levels);
    }

    public DumbSlave createDumbSlave(String name) {
        return DumbSlave.create(this,name);
    }


    private static final Pattern VERSION = Pattern.compile("^About Jenkins ([^-]*)");
}
