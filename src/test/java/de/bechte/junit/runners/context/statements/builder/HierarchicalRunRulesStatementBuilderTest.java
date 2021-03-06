package de.bechte.junit.runners.context.statements.builder;

import de.bechte.junit.stubs.statements.rules.*;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class HierarchicalRunRulesStatementBuilderTest {
    private final Statement nextStatement = mock(Statement.class);
    private final RunNotifier runNotifier = mock(RunNotifier.class);
    private final FrameworkMethod frameworkMethod = mock(FrameworkMethod.class);

    private final HierarchicalRunRulesStatementBuilder hierarchicalRunRulesStatementBuilder = new HierarchicalRunRulesStatementBuilder();

    @Test
    public void whenNoRulesArePresentInTestTheNextStatementRemainsUnwrapped() throws Exception {
        Statement statement = hierarchicalRunRulesStatementBuilder.createStatement(new TestClass(TestWithoutRule.class), mock(FrameworkMethod.class), new TestWithoutRule(), nextStatement, Description.createTestDescription(TestWithoutRule.class, "Desc"), runNotifier);

        assertThat(statement, is(nextStatement));
    }

    @Test
    public void testRulesAreAppliedForAllHierarchyLevels() throws Throwable {
        Description testDescription = Description.createTestDescription(TestWithTestRuleOnHighestLevel.class, "Test with TestRule");

        CapturingTestRuleStub capturingTestRuleStub = new CapturingTestRuleStub();

        TestWithTestRuleOnHighestLevel outer = new TestWithTestRuleOnHighestLevel(capturingTestRuleStub);
        Object target = TestWithTestRuleOnHighestLevel.Context.class.getConstructors()[0].newInstance(outer);

        Statement statement = hierarchicalRunRulesStatementBuilder.createStatement(new TestClass(TestWithTestRuleOnHighestLevel.class), mock(FrameworkMethod.class), target, nextStatement, testDescription, runNotifier);

        assertThat(capturingTestRuleStub.getNumberOfApplications(), is(2));
        assertThat(capturingTestRuleStub.getApplyMethodParameters(), everyItem(descriptionTestRuleApplyWasCalledWith(equalTo(testDescription))));
        assertThat(capturingTestRuleStub.getApplyMethodParameters(), contains(statementTestRuleApplyWasCalledWith(equalTo(nextStatement)), statementTestRuleApplyWasCalledWith(notNullValue(Statement.class))));

        statement.evaluate();

        assertThat(capturingTestRuleStub.statementReturnedByRuleApplyMethodWasEvaluated(), is(true));
    }

    @Test
    public void testRulesAreNotAppliedForHierarchiesAboveTheirPlaceOfDefinition() throws Throwable {
        Description testDescription = Description.createTestDescription(TestWithTestRuleOnLowerLevel.class, "Test with TestRule");

        CapturingTestRuleStub capturingTestRuleStub = new CapturingTestRuleStub();

        TestWithTestRuleOnLowerLevel outer = new TestWithTestRuleOnLowerLevel();
        Object target = TestWithTestRuleOnLowerLevel.Context.class.getConstructors()[0].newInstance(outer, capturingTestRuleStub);

        Statement statement = hierarchicalRunRulesStatementBuilder.createStatement(new TestClass(TestWithTestRuleOnHighestLevel.class), mock(FrameworkMethod.class), target, nextStatement, testDescription, runNotifier);

        assertThat(capturingTestRuleStub.getNumberOfApplications(), is(1));
        assertThat(capturingTestRuleStub.getApplyMethodParameters(), contains(descriptionTestRuleApplyWasCalledWith(equalTo(testDescription))));
        assertThat(capturingTestRuleStub.getApplyMethodParameters(), contains(statementTestRuleApplyWasCalledWith(equalTo(nextStatement))));

        statement.evaluate();

        assertThat(capturingTestRuleStub.statementReturnedByRuleApplyMethodWasEvaluated(), is(true));
    }

    @Test
    public void methodRuleIsPresentOnHighestLevelAndTestClassHasNoInnerContexts() throws Throwable {
        Description testDescription = Description.createTestDescription(TestWithMethodRuleOnHighestLevelWithoutInnerContexts.class, "Test with MethodRule");
        CapturingMethodRuleStub capturingMethodRuleStub = new CapturingMethodRuleStub();

        Object target = new TestWithMethodRuleOnHighestLevelWithoutInnerContexts(capturingMethodRuleStub);
        Statement statement = hierarchicalRunRulesStatementBuilder.createStatement(new TestClass(TestWithMethodRuleOnHighestLevelWithoutInnerContexts.class), frameworkMethod, target, nextStatement, testDescription, runNotifier);

        assertThat(capturingMethodRuleStub.getNumberOfApplications(), is(1));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), contains(targetCalledWith(target)));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), contains(frameworkMethodCalledWith(frameworkMethod)));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), contains(statementCalledWith(nextStatement)));

        statement.evaluate();

        assertThat(capturingMethodRuleStub.statementReturnedByRuleApplyMethodWasEvaluated(), is(true));
    }

    @Test
    public void whenRuleImplementsBothTestRuleAndMethodRule_onlyTestRuleApplyIsExecuted() throws Throwable {
        Description testDescription = Description.createTestDescription(TestWithRuleThatImplementsBothTestRuleAndMethodRule.class, "Test with rule that implements both TestRule and MethodRule");

        CapturingTestAndMethodRuleStub capturingTestAndMethodRuleStub = new CapturingTestAndMethodRuleStub();
        Object target = new TestWithRuleThatImplementsBothTestRuleAndMethodRule(capturingTestAndMethodRuleStub);
        Statement statement = hierarchicalRunRulesStatementBuilder.createStatement(new TestClass(TestWithRuleThatImplementsBothTestRuleAndMethodRule.class), frameworkMethod, target, nextStatement, testDescription, runNotifier);

        assertThat(capturingTestAndMethodRuleStub.getNumberOfApplicationsOfTestRulesApplyMethod(), is(1));
        assertThat(capturingTestAndMethodRuleStub.getStatementTestRuleApplyWasCalledWith(), is(nextStatement));
        assertThat(capturingTestAndMethodRuleStub.getDescriptionTestRuleApplyWasCalledWith(), is(testDescription));

        statement.evaluate();

        assertThat(capturingTestAndMethodRuleStub.statementReturnedByRuleApplyMethodWasEvaluated(), is(true));
    }

    @Test
    public void methodRuleIsAppliedForEachHierarchy() throws Throwable {
        CapturingMethodRuleStub capturingMethodRuleStub = new CapturingMethodRuleStub();
        TestWithMethodRuleAndTwoHierarchies outer = new TestWithMethodRuleAndTwoHierarchies(capturingMethodRuleStub);
        Object target = TestWithMethodRuleAndTwoHierarchies.Context.class.getConstructors()[0].newInstance(outer);
        Statement statement = hierarchicalRunRulesStatementBuilder.createStatement(new TestClass(TestWithMethodRuleAndTwoHierarchies.class), frameworkMethod, target, nextStatement, Description.createTestDescription(TestWithMethodRuleAndTwoHierarchies.class, "Test with MethodRule and hierarchies"), runNotifier);

        assertThat(capturingMethodRuleStub.getNumberOfApplications(), is(2));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), everyItem(frameworkMethodCalledWith(frameworkMethod)));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), contains(statementCalledWith(nextStatement), statementCalledWith(notNullValue(Statement.class))));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), contains(targetCalledWith(target), targetCalledWith(instanceOf(TestWithMethodRuleAndTwoHierarchies.class))));

        statement.evaluate();

        assertThat(capturingMethodRuleStub.statementReturnedByRuleApplyMethodWasEvaluated(), is(true));
    }

    @Test
    public void methodRulesAreNotAppliedForHierarchiesAboveTheirPlaceOfDefinition() throws Throwable {
        Description testDescription = Description.createTestDescription(TestWithMethodRuleOnLowerLevel.class, "Test with TestRule");

        CapturingMethodRuleStub capturingMethodRuleStub = new CapturingMethodRuleStub();

        TestWithMethodRuleOnLowerLevel outer = new TestWithMethodRuleOnLowerLevel();
        Object target = TestWithMethodRuleOnLowerLevel.Context.class.getConstructors()[0].newInstance(outer, capturingMethodRuleStub);

        Statement statement = hierarchicalRunRulesStatementBuilder.createStatement(new TestClass(TestWithTestRuleOnHighestLevel.class), mock(FrameworkMethod.class), target, nextStatement, testDescription, runNotifier);

        assertThat(capturingMethodRuleStub.getNumberOfApplications(), is(1));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), contains(statementCalledWith(equalTo(nextStatement))));
        assertThat(capturingMethodRuleStub.getApplyInvocationParameters(), contains(targetCalledWith(target)));

        statement.evaluate();

        assertThat(capturingMethodRuleStub.statementReturnedByRuleApplyMethodWasEvaluated(), is(true));
    }

    private Matcher<CapturingMethodRuleStub.ApplyInvocationParameter> targetCalledWith(Object target) {
        return targetCalledWith(equalTo(target));
    }

    private Matcher<CapturingMethodRuleStub.ApplyInvocationParameter> targetCalledWith(Matcher<Object> submatcher) {
        return new FeatureMatcher<CapturingMethodRuleStub.ApplyInvocationParameter, Object>(submatcher, "target", "target") {
            @Override
            protected Object featureValueOf(CapturingMethodRuleStub.ApplyInvocationParameter actual) {
                return actual.getTargetApplyWasCalledWith();
            }
        };
    }

    private Matcher<CapturingMethodRuleStub.ApplyInvocationParameter> statementCalledWith(Statement statement) {
        return statementCalledWith(equalTo(statement));
    }

    private Matcher<CapturingMethodRuleStub.ApplyInvocationParameter> statementCalledWith(Matcher<Statement> submatcher) {
        return new FeatureMatcher<CapturingMethodRuleStub.ApplyInvocationParameter, Statement>(submatcher, "statement", "statement") {
            @Override
            protected Statement featureValueOf(CapturingMethodRuleStub.ApplyInvocationParameter actual) {
                return actual.getStatementApplyWasCalledWith();
            }
        };
    }

    private Matcher<CapturingMethodRuleStub.ApplyInvocationParameter> frameworkMethodCalledWith(FrameworkMethod frameworkMethod) {
        return new FeatureMatcher<CapturingMethodRuleStub.ApplyInvocationParameter, FrameworkMethod>(equalTo(frameworkMethod), "framework method", "method") {
            @Override
            protected FrameworkMethod featureValueOf(CapturingMethodRuleStub.ApplyInvocationParameter actual) {
                return actual.getFrameworkMethodApplyWasCalledWith();
            }
        };
    }

    private Matcher<CapturingTestRuleStub.ApplyMethodParameter> statementTestRuleApplyWasCalledWith(Matcher<Statement> submatcher) {
        return new FeatureMatcher<CapturingTestRuleStub.ApplyMethodParameter, Statement>(submatcher, "statement", "statement") {
            @Override
            protected Statement featureValueOf(CapturingTestRuleStub.ApplyMethodParameter actual) {
                return actual.getStatementAppliedWasCalledWith();
            }
        };
    }

    private Matcher<CapturingTestRuleStub.ApplyMethodParameter> descriptionTestRuleApplyWasCalledWith(Matcher<Description> submatcher) {
        return new FeatureMatcher<CapturingTestRuleStub.ApplyMethodParameter, Description>(submatcher, "description", "description") {
            @Override
            protected Description featureValueOf(CapturingTestRuleStub.ApplyMethodParameter actual) {
                return actual.getDescriptionApplyWasCalledWith();
            }
        };
    }
}
