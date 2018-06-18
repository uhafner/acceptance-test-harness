package org.jenkinsci.test.acceptance.plugins.warnings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.ContainerPageObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Class which represents a result details page of the warnings plugin.
 *
 * @author Stephan Ploederl
 */
public class WarningsResultDetailsPage extends ContainerPageObject {

    private final static String resultPathEnd = "Result/";

    /**
     * Creates a WarningsResultDetailsPage.
     *
     * @param parent
     *         the build of the result page.
     * @param plugin
     *         the plugin for which the result page shall be constructed (i.e. cpd, simian).
     */
    public WarningsResultDetailsPage(final Build parent, final String plugin) {
        super(parent, parent.url(plugin.toLowerCase() + resultPathEnd));
    }

    /**
     * returns the container holding the tabs.
     *
     * @return the container
     */
    private WebElement getTabsContainer() {
        return getElement(By.id("tab-details"));
    }

    /**
     * Takes a {@link WebElement}, which is representing a html-table and transforms it to a {@link List} of {@link
     * HashMap]s.
     *
     * @param element
     *         the WebElement representing a table
     *
     * @return the table as List of HashMaps.
     */
    private List<HashMap<String, WebElement>> parseTable(final WebElement element) {
        List<HashMap<String, WebElement>> parsedTable = new ArrayList<>();
        List<String> tableHeaders = element.findElements(By.xpath(".//thead/tr/th"))
                .stream()
                .map(WebElement::getText)
                .collect(
                        Collectors.toList());
        for (WebElement row : element.findElements(By.xpath(".//tbody/tr"))) {
            List<WebElement> cellsOfRow = row.findElements(By.tagName("td"));
            HashMap<String, WebElement> cellData = new HashMap<>();
            for (int i = 0; i < tableHeaders.size(); i++) {
                cellData.put(tableHeaders.get(i), cellsOfRow.get(i));
            }
            parsedTable.add(cellData);
        }
        return parsedTable;
    }

    /**
     * Opens the tab within this page having the name of the corresponding value of the {@link tabs} enum.
     *
     * @param tab
     *         the tab that shall be opened.
     */
    public void openTab(final tabs tab) {
        open();
        WebElement tabs = getTabsContainer();
        WebElement tabElement = tabs.findElement(By.xpath("//a[text()='" + tab.name() + "']"));
        tabElement.click();
    }

    /**
     * Opens the Issues tab and returns the rows of issue table as {@link List} of {@link HashMap} containing the
     * column-name as String and the {@link WebElement} as value.
     *
     * @return the issues-table.
     */
    public List<HashMap<String, WebElement>> getIssuesTable() {
        openTab(tabs.Issues);
        WebElement issuesTable = find(By.id("issues"));
        return parseTable(issuesTable);
    }

    public enum tabs {Issues, Details, Packages, Modules}

}
