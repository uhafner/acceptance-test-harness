package plugins;

import java.util.List;
import java.util.Map;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.plugins.warnings.IssuesRecorder;
import org.jenkinsci.test.acceptance.plugins.warnings.IssuesRecorder.StaticAnalysisTool;
import org.jenkinsci.test.acceptance.plugins.warnings.WarningsResultDetailsPage;
import org.jenkinsci.test.acceptance.plugins.warnings.WarningsResultDetailsPage.Tabs;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.*;

/**
 * Acceptance tests for the White Mountains release of the warnings plug-in.
 *
 * @author Manuel Hampp
 * @author Anna-Maria Hardi
 * @author Stephan Plöderl
 */
@WithPlugins("warnings")
public class AnalysisPluginsTest extends AbstractJUnitTest {
    private static final String WARNINGS_PLUGIN_PREFIX = "/warnings_plugin/white-mountains/";

    /**
     * Simple test to check that there are some duplicate code warnings.
     */
    @Test
    public void should_have_duplicate_code_warnings() {
        FreeStyleJob job = createFreeStyleJob("duplicate_code/cpd.xml", "duplicate_code/Main.java");

        IssuesRecorder recorder = job.addPublisher(IssuesRecorder.class);
        StaticAnalysisTool tool = recorder.setTool("CPD");
        tool.setNormalThreshold(1);
        tool.setHighThreshold(2);
        job.save();

        Build build = job.startBuild().waitUntilFinished();

        WarningsResultDetailsPage page = getWarningsResultDetailsPage("cpd", build);
        page.openTab(Tabs.DETAILS);
        List<Map<String, WebElement>> issuesTable = page.getIssuesTable();
        Map<String, WebElement> firstRowOfIssuesTable = issuesTable.get(0);
        assertThat(firstRowOfIssuesTable.keySet()).contains("Details");
    }

    private WarningsResultDetailsPage getWarningsResultDetailsPage(final String id, final Build build) {
        WarningsResultDetailsPage resultPage = new WarningsResultDetailsPage(build, id);
        resultPage.open();
        return resultPage;
    }

    /**
     * Simple test to check that warnings of checkstyle and pmd file are handled separately if aggregation is not
     * activated.
     */
    @Test
    public void should_log_ok_in_console_with_not_activated_aggregation() {
        FreeStyleJob job = createFreeStyleJob("aggregation/checkstyle.xml", "aggregation/pmd.xml");

        job.addPublisher(IssuesRecorder.class, recorder -> {
            recorder.setTool("CheckStyle", "**/checkstyle.xml");
            recorder.addTool("PMD", "**/pmd.xml");
            recorder.setEnabledForAggregation(false);
        });

        job.save();

        Build build = job.startBuild().waitUntilFinished();

        assertThat(build.getConsole()).contains("[CheckStyle] Created analysis result for 6 issues");
        assertThat(build.getConsole()).contains("[PMD] Created analysis result for 4 issues");
    }

    /**
     * Simple test to check that warnings of checkstyle and pmd file are summed up if aggregation is activated.
     */
    @Test
    public void should_log_ok_in_console_with_activated_aggregation() {
        FreeStyleJob job = createFreeStyleJob("aggregation/checkstyle.xml", "aggregation/pmd.xml");

        job.addPublisher(IssuesRecorder.class, recorder -> {
            recorder.setTool("CheckStyle", "**/checkstyle.xml");
            recorder.addTool("PMD", "**/pmd.xml");
            recorder.setEnabledForAggregation(true);
        });

        job.save();

        Build build = job.startBuild().waitUntilFinished();

        assertThat(build.getConsole()).contains("[Static Analysis] Created analysis result for 10 issues");
    }

    private FreeStyleJob createFreeStyleJob(final String... resourcesToCopy) {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        for (String resource : resourcesToCopy) {
            job.copyResource(resource(WARNINGS_PLUGIN_PREFIX + resource));
        }
        return job;
    }

    /**
     * Test to check that the issue filter can be configured and is applied.
     */
    @Test
    public void should_log_filter_applied_in_console() {
        FreeStyleJob job = createFreeStyleJob("issue_filter/checkstyle-result.xml");

        job.addPublisher(IssuesRecorder.class, recorder -> {
            recorder.setTool("CheckStyle");
            recorder.openAdvancedOptions();
            recorder.setEnabledForFailure(true);
            recorder.addIssueFilter("Exclude categories", "Checks");
            recorder.addIssueFilter("Include types", "JavadocMethodCheck");
        });
        
        job.save();

        Build build = job.startBuild().waitUntilFinished();

        assertThat(build.getConsole()).contains(
                "[CheckStyle] Applying 2 filters on the set of 4 issues (3 issues have been removed)");
    }

    /**
     * Test that the default pattern for checkStyle is used, if no other pattern is specified.
     */
    @Test
    public void should_use_default_pattern_xml_for_checkStyle_checked_result_on_console () {

        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        //job.copyResource("/warnings_plugin/issue_filter_test/checkstyle-result.xml");

        IssuesRecorder recorder = job.addPublisher(IssuesRecorder.class);
        recorder.setTool("CheckStyle");
        recorder.setEnabledForFailure(true);
        job.save();

        Build build = job.startBuild().waitUntilFinished();

        assertThat(build.getConsole()).contains("[CheckStyle] Using default pattern '**/checkstyle-result.xml' since user defined pattern is not set\n");
        assertThat(build.getConsole()).contains("[CheckStyle] Created analysis result for 0 issues (found 0 new issues, fixed 0 issues)\n");
    }

    /**
     * Test that the specified pattern for checkStyle is used, if one specified.
     */
    @Test
    public void should_use_specified_pattern_xml_for_checkStyle_checked_result_on_console () {

        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);

        IssuesRecorder recorder = job.addPublisher(IssuesRecorder.class);
        recorder.setTool("CheckStyle", "issue_filter/checkstyle-result.xml");
        recorder.setEnabledForFailure(true);
        job.save();

        Build build = job.startBuild().waitUntilFinished();

        assertThat(build.getConsole()).contains("[CheckStyle] [ERROR] No files found for pattern 'issue_filter/checkstyle-result.xml'. Configuration error?\n");
        assertThat(build.getConsole()).contains("[CheckStyle] Created analysis result for 0 issues (found 0 new issues, fixed 0 issues)\n");
    }

}

