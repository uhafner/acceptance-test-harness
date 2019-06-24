package org.jenkinsci.test.acceptance.plugins.email_ext;

import org.jenkinsci.test.acceptance.po.*;

import org.openqa.selenium.NoSuchElementException;

/**
 * @author Kohsuke Kawaguchi
 */
@Describable("Editable Email Notification")
public class EmailExtPublisher extends AbstractStep implements PostBuildStep {
    public final Control subject = control("project_default_subject");
    private final Control recipient = control("project_recipient_list", "recipientlist_recipients");
    public final Control body = control("project_default_content");

    private boolean advancedOpened;

    public EmailExtPublisher(Job parent, String path) {
        super(parent, path);
    }

    public void setRecipient(String r) {
        recipient.set(r);

        ensureAdvancedOpened();
        // since 2.38 refactored to hetero-list, recipients were preselected
        try {
            control("project_triggers/sendToList").check();
        }
        catch (NoSuchElementException ex) {
            // some later releases do not preselect recipients
            control("project_triggers/hetero-list-add[recipientProviders]").selectDropdownMenu("Recipient List");

        }
    }

    public void addProjectTrigger(final String stageTitle) {
        ensureAdvancedOpened();

        control("hetero-list-add[project_triggers]").selectDropdownMenu(stageTitle);
    }

    public void ensureAdvancedOpened() {
        if (!advancedOpened) {
            control("advanced-button").click();
            advancedOpened = true;
        }
    }
}
