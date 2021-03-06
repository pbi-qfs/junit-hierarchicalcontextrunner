package de.bechte.junit.runners.context.statements.builder;

import de.bechte.junit.runners.context.statements.builder.rules.TestRuleDefinitions;
import de.bechte.junit.runners.model.TestClassPool;
import org.junit.Rule;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.MethodRule;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.util.ArrayList;
import java.util.List;

import static de.bechte.junit.runners.util.ReflectionUtil.getEnclosingInstance;

/**
 * The {@link HierarchicalRunRulesStatementBuilder} creates {@link RunRules} statements for all {@code @Rule} annotated
 * fields and methods along the context hierarchy. If no rules exist, the builder returns the provided next
 * {@link Statement}.
 */
public class HierarchicalRunRulesStatementBuilder implements MethodStatementBuilder {
    public Statement createStatement(final TestClass testClass, final FrameworkMethod method, final Object target,
                                     final Statement next, final Description description, final RunNotifier notifier) {
        try {
            final TestRuleDefinitions testRules = new TestRuleDefinitions(hierarchyOfTestsFromLowestToHighest(target));
            for (Object instance = target; instance != null; instance = getEnclosingInstance(instance)) {
                final TestClass instanceTestClass = TestClassPool.forClass(instance.getClass());
                testRules.addTestRules(instanceTestClass.getAnnotatedMethodValues(instance, Rule.class, TestRule.class), instance);
                testRules.addTestRules(instanceTestClass.getAnnotatedFieldValues(instance, Rule.class, TestRule.class), instance);
                testRules.addMethodRules(instanceTestClass.getAnnotatedFieldValues(instance, Rule.class, MethodRule.class), instance);
            }

            Statement statement = next;
            for (Object hierarchyContext = target; hierarchyContext != null; hierarchyContext = getEnclosingInstance(hierarchyContext)) {
                for (MethodRule methodRule : testRules.getMethodRulesDefinedForThisHierarchyLevel(hierarchyContext))
                    if (!testRules.contains(methodRule)) {
                        statement = methodRule.apply(statement, method, hierarchyContext);
                    }
                if (testRules.testRulesPresent())
                    statement = new RunRules(statement, testRules.getTestRulesDefinedForThisHierarchyLevel(hierarchyContext), description);
            }
            return statement;
        } catch (final IllegalAccessException e) {
            return new Fail(e);
        }
    }

    private List<Object> hierarchyOfTestsFromLowestToHighest(Object target) throws IllegalAccessException {
        List<Object> result = new ArrayList<Object>();
        for (Object instance = target; instance != null; instance = getEnclosingInstance(instance)) {
            result.add(instance);
        }
        return result;
    }

}
