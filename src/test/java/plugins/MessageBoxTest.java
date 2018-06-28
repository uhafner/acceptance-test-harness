package plugins;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.plugins.warnings.IssuesRecorder;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.MessageBox;
import org.jenkinsci.test.acceptance.po.WorkflowJob;
import org.junit.Test;

import static plugins.MessageBoxAssert.*;

/**
 * UI Test for the PageObject {@link MessageBox}
 * @author Alexander Praegla, Nikolai Wohlgemuth, Arne Schöntag
 */
@WithPlugins({"warnings", "checkstyle"})
public class MessageBoxTest extends AbstractJUnitTest {
    private static final String WARNINGS_PLUGIN_PREFIX = "/warnings_plugin/white-mountains/";
    private static final String WARNINGS_XML = "checkstyle-result.xml";
    private static final String CHECKSTYLE_PLUGIN_ROOT = WARNINGS_PLUGIN_PREFIX + "message_box/";

    /**
     * Builds a FreeStyle build and that builds with a xml file
     * and checks if the results shown in the MessageBox are as expected.
     */
    @Test
    public void shouldBeOkIfContentsOfMsgBoxesAreCorrectForFreeStyleJob() {

        FreeStyleJob job = createFreeStyleJob(WARNINGS_XML);
        job.addPublisher(IssuesRecorder.class, recorder -> {
            recorder.setTool("CheckStyle", "**/checkstyle-result.xml");
        });
        job.save();
        job.startBuild().waitUntilFinished();

        MessageBox messageBox = new MessageBox(job);
        messageBox.open();

        // Check Error Panel
        messageBox.getErrorMsgContent();
        String errno1 = "Can't read file '/mnt/hudson_workspace/workspace/HTS-CheckstyleTest/ssh-slaves"
                + "/src/main/java/hudson/plugins/sshslaves/RemoteLauncher.java': java.nio.file.NoSuchFileException:"
                + " \\mnt\\hudson_workspace\\workspace\\HTS-CheckstyleTest\\ssh-slaves\\src\\main\\java\\hudson\\plugins"
                + "\\sshslaves\\RemoteLauncher.java";
        assertThat(messageBox).hasErrorMessagesSize(3);
        assertThat(messageBox).containsErrorMessage(errno1);

        // Check Info Panel
        messageBox.getInfoMsgContent();
        assertThat(messageBox).hasInfoMessagesSize(7);
        assertThat(messageBox).containsInfoMessage("found 1 file");
        assertThat(messageBox).containsInfoMessage("for 2 issues");
        assertThat(messageBox).containsInfoMessage("No quality gates have been set - skipping");
    }

    /**
     * Builds a Pipeline build and that builds with a xml file
     * and checks if the results shown in the MessageBox are as expected.
     */
    @Test
    public void shouldBeOkIfContentsOfMsgBoxesAreCorrectForPipelineJob() {

        WorkflowJob job = createPipelineWithResource(WARNINGS_XML);
        job.startBuild().waitUntilFinished();

        MessageBox messageBox = new MessageBox(job);
        messageBox.open();

        // Check Error Panel
        messageBox.getErrorMsgContent();
        String errno1 = "Can't read file '/mnt/hudson_workspace/workspace/HTS-CheckstyleTest/ssh-slaves"
                + "/src/main/java/hudson/plugins/sshslaves/RemoteLauncher.java': java.nio.file.NoSuchFileException:"
                + " \\mnt\\hudson_workspace\\workspace\\HTS-CheckstyleTest\\ssh-slaves\\src\\main\\java\\hudson\\plugins"
                + "\\sshslaves\\RemoteLauncher.java";
        assertThat(messageBox).hasErrorMessagesSize(3);
        assertThat(messageBox).containsErrorMessage(errno1);

        // Check Info Panel
        messageBox.getInfoMsgContent();
        assertThat(messageBox).hasInfoMessagesSize(7);
        assertThat(messageBox).containsInfoMessage("found 1 file");
        assertThat(messageBox).containsInfoMessage("for 2 issues");
        assertThat(messageBox).containsInfoMessage("No quality gates have been set - skipping");
    }

    /**
     * Helping method for creating a Pipeline build.
     * @param resourceFile to be loaded in the pipeline step
     * @return
     */
    private WorkflowJob createPipelineWithResource(String resourceFile) {
        WorkflowJob job = jenkins.jobs.create(WorkflowJob.class);
        String resource = job.copyResourceStep(CHECKSTYLE_PLUGIN_ROOT + resourceFile);
        job.script.set("node {\n" +
                        resource.replace("\\", "\\\\") +
                        "recordIssues enabledForFailure: true, tools: [[pattern: '', tool: [$class: 'CheckStyle']]]" +
                        "}");
        job.sandbox.check();
        job.save();
        return job;
    }

    /**
     * Helping method for creating a FreeStyle build.
     * @param resourcesToCopy to copy for the build
     * @return
     */
    private FreeStyleJob createFreeStyleJob(final String... resourcesToCopy) {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        for (String resource : resourcesToCopy) {
            job.copyResource(resource(CHECKSTYLE_PLUGIN_ROOT + resource));
        }
        return job;
    }
}
