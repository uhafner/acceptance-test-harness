package org.jenkinsci.test.acceptance.plugins.warnings.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.util.Objects;
import org.jenkinsci.test.acceptance.plugins.warnings.issues_table.AbstractNonDetailsIssuesTableRow;

/**
 * Abstract base class for {@link AbstractNonDetailsIssuesTableRow} specific assertions - Generated by
 * CustomAssertionGenerator.
 */
@javax.annotation.Generated(value = "assertj-assertions-generator")
public abstract class AbstractAbstractNonDetailsIssuesTableRowAssert<S extends AbstractAbstractNonDetailsIssuesTableRowAssert<S, A>, A extends AbstractNonDetailsIssuesTableRow>
        extends AbstractObjectAssert<S, A> {

    /**
     * Creates a new <code>{@link AbstractAbstractNonDetailsIssuesTableRowAssert}</code> to make assertions on actual
     * AbstractNonDetailsIssuesTableRow.
     *
     * @param actual
     *         the AbstractNonDetailsIssuesTableRow we want to make assertions on.
     */
    protected AbstractAbstractNonDetailsIssuesTableRowAssert(A actual, Class<S> selfType) {
        super(actual, selfType);
    }

    /**
     * Verifies that the actual AbstractNonDetailsIssuesTableRow's priority is equal to the given one.
     *
     * @param priority
     *         the given priority to compare the actual AbstractNonDetailsIssuesTableRow's priority to.
     *
     * @return this assertion object.
     * @throws AssertionError
     *         - if the actual AbstractNonDetailsIssuesTableRow's priority is not equal to the given one.
     */
    public S hasPriority(String priority) {
        // check that actual AbstractNonDetailsIssuesTableRow we want to make assertions on is not null.
        isNotNull();

        // overrides the default error message with a more explicit one
        String assertjErrorMessage = "\nExpecting priority of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        // null safe check
        String actualPriority = actual.getPriority();
        if (!Objects.areEqual(actualPriority, priority)) {
            failWithMessage(assertjErrorMessage, actual, priority, actualPriority);
        }

        // return the current assertion for method chaining
        return myself;
    }

}