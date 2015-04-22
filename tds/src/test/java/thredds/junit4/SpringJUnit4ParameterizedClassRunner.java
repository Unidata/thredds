package thredds.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.*;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * The custom runner <code>Parameterized</code> implements parameterized tests.
 * When running a parameterized test class, instances are created for the
 * cross-product of the test methods and the test data elements.
 * </p>
 * 
 * For example, to test a Fibonacci function, write:
 * 
 * <pre>
 * &#064;RunWith(Parameterized.class)
 * public class FibonacciTest {
 * 	&#064;Parameters
 * 	public static List&lt;Object[]&gt; data() {
 * 		return Arrays.asList(new Object[][] {
 * 				Fibonacci,
 * 				{ { 0, 0 }, { 1, 1 }, { 2, 1 }, { 3, 2 }, { 4, 3 }, { 5, 5 },
 * 						{ 6, 8 } } });
 * 	}
 * 
 * 	private int fInput;
 * 
 * 	private int fExpected;
 * 
 * 	public FibonacciTest(int input, int expected) {
 * 		fInput= input;
 * 		fExpected= expected;
 * 	}
 * 
 * 	&#064;Test
 * 	public void test() {
 * 		assertEquals(fExpected, Fibonacci.compute(fInput));
 * 	}
 * }
 * </pre>
 * 
 * <p>
 * Each instance of <code>FibonacciTest</code> will be constructed using the
 * two-argument constructor and the data values in the
 * <code>&#064;Parameters</code> method.
 * </p>
 *
 * @see "https://jira.springsource.org/secure/attachment/19038/SpringJUnit4ParameterizedClassRunner.java"
 */
public class SpringJUnit4ParameterizedClassRunner extends Suite {
	/**
	 * Annotation for a method which provides parameters to be injected into the
	 * test class constructor by <code>Parameterized</code>
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Parameters {
	}

	private class TestClassRunnerForParameters extends
			BlockJUnit4ClassRunner {
		private final int fParameterSetNumber;

		private final List<Object[]> fParameterList;

		private final Logger logger = LoggerFactory.getLogger(TestClassRunnerForParameters.class);

		private final TestContextManager testContextManager;
		
		/**
		 * Constructs a new <code>SpringJUnit4ClassRunner</code> and initializes a
		 * {@link TestContextManager} to provide Spring testing functionality to
		 * standard JUnit tests.
		 * 
		 * @param type the test class to be run
		 * @see #createTestContextManager(Class)
		 */
		TestClassRunnerForParameters(Class<?> type,
				List<Object[]> parameterList, int i) throws InitializationError {
			super(type);
			if (logger.isDebugEnabled()) {
				logger.debug("SpringJUnit4ClassRunner constructor called with [" + type + "].");
			}
			fParameterList= parameterList;
			fParameterSetNumber= i;
			this.testContextManager = createTestContextManager(type);
		}
		
		/**
		 * Delegates to the parent implementation for creating the test instance and
		 * then allows the {@link #getTestContextManager() TestContextManager} to
		 * prepare the test instance before returning it.
		 * 
		 * @see TestContextManager#prepareTestInstance(Object)
		 */
		@Override
		public Object createTest() throws Exception {
			Object testInstance = getTestClass().getOnlyConstructor().newInstance(
					computeParams());
			getTestContextManager().prepareTestInstance(testInstance);
			return testInstance;
		}
		
		private Object[] computeParams() throws Exception {
			try {
				return fParameterList.get(fParameterSetNumber);
			} catch (ClassCastException e) {
				throw new Exception(String.format(
						"%s.%s() must return a Collection of arrays.",
						getTestClass().getName(), getParametersMethod(
								getTestClass()).getName()));
			}
		}

		@Override
		protected String getName() {
			return String.format("[%s]", fParameterSetNumber);
		}

		@Override
		protected String testName(final FrameworkMethod method) {
			return String.format("%s[%s]", method.getName(),
					fParameterSetNumber);
		}

		@Override
		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
		}

		@Override
		protected Statement classBlock(RunNotifier notifier) {
			return childrenInvoker(notifier);
		}
		
		
		/*SPRING*/
		
		/**
		 * Constructs a new <code>SpringJUnit4ClassRunner</code> and initializes a
		 * {@link TestContextManager} to provide Spring testing functionality to
		 * standard JUnit tests.
		 * 
		 * @param clazz the test class to be run
		 * @see #createTestContextManager(Class)
		 */
//		public TestClassRunnerForParameters(Class<?> clazz) throws InitializationError {
//			super(clazz);
//			if (logger.isDebugEnabled()) {
//				logger.debug("SpringJUnit4ClassRunner constructor called with [" + clazz + "].");
//			}
//			this.testContextManager = createTestContextManager(clazz);
//		}

		/**
		 * Creates a new {@link TestContextManager} for the supplied test class and
		 * the configured <em>default <code>ContextLoader</code> class name</em>.
		 * Can be overridden by subclasses.
		 * 
		 * @param clazz the test class to be managed
		 * @see #getDefaultContextLoaderClassName(Class)
		 */
		protected TestContextManager createTestContextManager(Class<?> clazz) {
			return new TestContextManager(clazz);
		}

		/**
		 * Get the {@link TestContextManager} associated with this runner.
		 */
		protected final TestContextManager getTestContextManager() {
			return this.testContextManager;
		}

		/**
		 * Get the name of the default <code>ContextLoader</code> class to use for
		 * the supplied test class. The named class will be used if the test class
		 * does not explicitly declare a <code>ContextLoader</code> class via the
		 * <code>&#064;ContextConfiguration</code> annotation.
		 * <p>
		 * The default implementation returns <code>null</code>, thus implying use
		 * of the <em>standard</em> default <code>ContextLoader</code> class name.
		 * Can be overridden by subclasses.
		 * </p>
		 * 
		 * @param clazz the test class
		 * @return <code>null</code>
		 */
		protected String getDefaultContextLoaderClassName(Class<?> clazz) {
			return null;
		}

		/**
		 * Returns a description suitable for an ignored test class if the test is
		 * disabled via <code>&#064;IfProfileValue</code> at the class-level, and
		 * otherwise delegates to the parent implementation.
		 * 
		 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
		 */
		@Override
		public Description getDescription() {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
				return Description.createSuiteDescription(getTestClass().getJavaClass());
			}
			return super.getDescription();
		}

		/**
		 * Check whether the test is enabled in the first place. This prevents
		 * classes with a non-matching <code>&#064;IfProfileValue</code> annotation
		 * from running altogether, even skipping the execution of
		 * <code>prepareTestInstance()</code> <code>TestExecutionListener</code>
		 * methods.
		 * 
		 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
		 * @see org.springframework.test.annotation.IfProfileValue
		 * @see org.springframework.test.context.TestExecutionListener
		 */
		@Override
		public void run(RunNotifier notifier) {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
				notifier.fireTestIgnored(getDescription());
				return;
			}
			super.run(notifier);
		}

		/**
		 * Wraps the {@link Statement} returned by the parent implementation with a
		 * {@link RunBeforeTestClassCallbacks} statement, thus preserving the
		 * default functionality but adding support for the Spring TestContext
		 * Framework.
		 * 
		 * @see RunBeforeTestClassCallbacks
		 */
		@Override
		protected Statement withBeforeClasses(Statement statement) {
			Statement junitBeforeClasses = super.withBeforeClasses(statement);
			return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
		}

		/**
		 * Wraps the {@link Statement} returned by the parent implementation with a
		 * {@link RunAfterTestClassCallbacks} statement, thus preserving the default
		 * functionality but adding support for the Spring TestContext Framework.
		 * 
		 * @see RunAfterTestClassCallbacks
		 */
		@Override
		protected Statement withAfterClasses(Statement statement) {
			Statement junitAfterClasses = super.withAfterClasses(statement);
			return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
		}

		/**
		 * Performs the same logic as
		 * {@link BlockJUnit4ClassRunner#runChild(FrameworkMethod, RunNotifier)},
		 * except that tests are determined to be <em>ignored</em> by
		 * {@link #isTestMethodIgnored(FrameworkMethod)}.
		 */
		@Override
		protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {
			EachTestNotifier eachNotifier = springMakeNotifier(frameworkMethod, notifier);
			if (isTestMethodIgnored(frameworkMethod)) {
				eachNotifier.fireTestIgnored();
				return;
			}

			eachNotifier.fireTestStarted();
			try {
				methodBlock(frameworkMethod).evaluate();
			}
			catch (AssumptionViolatedException e) {
				eachNotifier.addFailedAssumption(e);
			}
			catch (Throwable e) {
				eachNotifier.addFailure(e);
			}
			finally {
				eachNotifier.fireTestFinished();
			}
		}

		/**
		 * <code>springMakeNotifier()</code> is an exact copy of
		 * {@link BlockJUnit4ClassRunner BlockJUnit4ClassRunner's}
		 * <code>makeNotifier()</code> method, but we have decided to prefix it with
		 * "spring" and keep it <code>private</code> in order to avoid the
		 * compatibility clashes that were introduced in JUnit between versions 4.5,
		 * 4.6, and 4.7.
		 */
		private EachTestNotifier springMakeNotifier(FrameworkMethod method, RunNotifier notifier) {
			Description description = describeChild(method);
			return new EachTestNotifier(notifier, description);
		}

		/**
		 * Augments the default JUnit behavior
		 * {@link #withPotentialRepeat(FrameworkMethod, Object, Statement) with
		 * potential repeats} of the entire execution chain.
		 * <p>
		 * Furthermore, support for timeouts has been moved down the execution chain
		 * in order to include execution of {@link org.junit.Before &#064;Before}
		 * and {@link org.junit.After &#064;After} methods within the timed
		 * execution. Note that this differs from the default JUnit behavior of
		 * executing <code>&#064;Before</code> and <code>&#064;After</code> methods
		 * in the main thread while executing the actual test method in a separate
		 * thread. Thus, the end effect is that <code>&#064;Before</code> and
		 * <code>&#064;After</code> methods will be executed in the same thread as
		 * the test method. As a consequence, JUnit-specified timeouts will work
		 * fine in combination with Spring transactions. Note that JUnit-specific
		 * timeouts still differ from Spring-specific timeouts in that the former
		 * execute in a separate thread while the latter simply execute in the main
		 * thread (like regular tests).
		 * </p>
		 * 
		 * @see #possiblyExpectingExceptions(FrameworkMethod, Object, Statement)
		 * @see #withBefores(FrameworkMethod, Object, Statement)
		 * @see #withAfters(FrameworkMethod, Object, Statement)
		 * @see #withPotentialTimeout(FrameworkMethod, Object, Statement)
		 * @see #withPotentialRepeat(FrameworkMethod, Object, Statement)
		 */
		@Override
		protected Statement methodBlock(FrameworkMethod frameworkMethod) {

			Object testInstance;
			try {
				testInstance = new ReflectiveCallable() {

					@Override
					protected Object runReflectiveCall() throws Throwable {
						return createTest();
					}
				}.run();
			}
			catch (Throwable e) {
				return new Fail(e);
			}

			Statement statement = methodInvoker(frameworkMethod, testInstance);
			statement = possiblyExpectingExceptions(frameworkMethod, testInstance, statement);
			statement = withRulesReflectively(frameworkMethod, testInstance, statement);
			statement = withBefores(frameworkMethod, testInstance, statement);
			statement = withAfters(frameworkMethod, testInstance, statement);
			statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
			statement = withPotentialTimeout(frameworkMethod, testInstance, statement);

			return statement;
		}

		/**
		 * Invokes JUnit 4.7's private <code>withRules()</code> method using
		 * reflection. This is necessary for backwards compatibility with the JUnit
		 * 4.5 and 4.6 implementations of {@link BlockJUnit4ClassRunner}.
		 */
		private Statement withRulesReflectively(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
			Method withRulesMethod = ReflectionUtils.findMethod(getClass(), "withRules", FrameworkMethod.class,
				Object.class, Statement.class);
			if (withRulesMethod != null) {
				// Original JUnit 4.7 code:
				// statement = withRules(frameworkMethod, testInstance, statement);
				ReflectionUtils.makeAccessible(withRulesMethod);
				statement = (Statement) ReflectionUtils.invokeMethod(withRulesMethod, this, frameworkMethod, testInstance,
					statement);
			}
			return statement;
		}

		/**
		 * Returns <code>true</code> if {@link Ignore &#064;Ignore} is present for
		 * the supplied {@link FrameworkMethod test method} or if the test method is
		 * disabled via <code>&#064;IfProfileValue</code>.
		 * 
		 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Method, Class)
		 */
		protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
			Method method = frameworkMethod.getMethod();
			return (method.isAnnotationPresent(Ignore.class) || !ProfileValueUtils.isTestEnabledInThisEnvironment(method,
				getTestClass().getJavaClass()));
		}

		/**
		 * Performs the same logic as
		 * {@link BlockJUnit4ClassRunner#possiblyExpectingExceptions(FrameworkMethod, Object, Statement)}
		 * except that the <em>expected exception</em> is retrieved using
		 * {@link #getExpectedException(FrameworkMethod)}.
		 */
//		@Override
//		protected Statement possiblyExpectingExceptions(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
//			Class<? extends Throwable> expectedException = getExpectedException(frameworkMethod);
//			return expectedException != null ? new ExpectException(next, expectedException) : next;
//		}

		/**
		 * Get the <code>exception</code> that the supplied {@link FrameworkMethod
		 * test method} is expected to throw.
		 * <p>
		 * Supports both Spring's {@link ExpectedException @ExpectedException(...)}
		 * and JUnit's {@link Test#expected() @Test(expected=...)} annotations, but
		 * not both simultaneously.
		 * </p>
		 * 
		 * @return the expected exception, or <code>null</code> if none was
		 * specified
		 */
//		protected Class<? extends Throwable> getExpectedException(FrameworkMethod frameworkMethod) {
//			Test testAnnotation = frameworkMethod.getAnnotation(Test.class);
//			Class<? extends Throwable> junitExpectedException = testAnnotation != null
//					&& testAnnotation.expected() != Test.None.class ? testAnnotation.expected() : null;
//
//			ExpectedException expectedExAnn = frameworkMethod.getAnnotation(ExpectedException.class);
//			Class<? extends Throwable> springExpectedException = (expectedExAnn != null ? expectedExAnn.value() : null);
//
//			if (springExpectedException != null && junitExpectedException != null) {
//				String msg = "Test method [" + frameworkMethod.getMethod()
//						+ "] has been configured with Spring's @ExpectedException(" + springExpectedException.getName()
//						+ ".class) and JUnit's @Test(expected=" + junitExpectedException.getName()
//						+ ".class) annotations. "
//						+ "Only one declaration of an 'expected exception' is permitted per test method.";
//				logger.error(msg);
//				throw new IllegalStateException(msg);
//			}
//
//			return springExpectedException != null ? springExpectedException : junitExpectedException;
//		}

		/**
		 * Supports both Spring's {@link Timed &#064;Timed} and JUnit's
		 * {@link Test#timeout() &#064;Test(timeout=...)} annotations, but not both
		 * simultaneously. Returns either a {@link SpringFailOnTimeout}, a
		 * {@link FailOnTimeout}, or the unmodified, supplied {@link Statement} as
		 * appropriate.
		 * 
		 * @see #getSpringTimeout(FrameworkMethod)
		 * @see #getJUnitTimeout(FrameworkMethod)
		 */
		@Override
		protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
			Statement statement = null;
			long springTimeout = getSpringTimeout(frameworkMethod);
			long junitTimeout = getJUnitTimeout(frameworkMethod);
			if (springTimeout > 0 && junitTimeout > 0) {
				String msg = "Test method [" + frameworkMethod.getMethod()
						+ "] has been configured with Spring's @Timed(millis=" + springTimeout
						+ ") and JUnit's @Test(timeout=" + junitTimeout
						+ ") annotations. Only one declaration of a 'timeout' is permitted per test method.";
				logger.error(msg);
				throw new IllegalStateException(msg);
			}
			else if (springTimeout > 0) {
				statement = new SpringFailOnTimeout(next, springTimeout);
			}
			else if (junitTimeout > 0) {
				statement = new FailOnTimeout(next, junitTimeout);
			}
			else {
				statement = next;
			}

			return statement;
		}

		/**
		 * Retrieves the configured JUnit <code>timeout</code> from the {@link Test
		 * &#064;Test} annotation on the supplied {@link FrameworkMethod test
		 * method}.
		 * 
		 * @return the timeout, or <code>0</code> if none was specified.
		 */
		protected long getJUnitTimeout(FrameworkMethod frameworkMethod) {
			Test testAnnotation = frameworkMethod.getAnnotation(Test.class);
			return (testAnnotation != null && testAnnotation.timeout() > 0 ? testAnnotation.timeout() : 0);
		}

		/**
		 * Retrieves the configured Spring-specific <code>timeout</code> from the
		 * {@link Timed &#064;Timed} annotation on the supplied
		 * {@link FrameworkMethod test method}.
		 * 
		 * @return the timeout, or <code>0</code> if none was specified.
		 */
		protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
			Timed timedAnnotation = frameworkMethod.getAnnotation(Timed.class);
			return (timedAnnotation != null && timedAnnotation.millis() > 0 ? timedAnnotation.millis() : 0);
		}

		/**
		 * Wraps the {@link Statement} returned by the parent implementation with a
		 * {@link RunBeforeTestMethodCallbacks} statement, thus preserving the
		 * default functionality but adding support for the Spring TestContext
		 * Framework.
		 * 
		 * @see RunBeforeTestMethodCallbacks
		 */
		@Override
		protected Statement withBefores(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
			Statement junitBefores = super.withBefores(frameworkMethod, testInstance, statement);
			return new RunBeforeTestMethodCallbacks(junitBefores, testInstance, frameworkMethod.getMethod(),
				getTestContextManager());
		}

		/**
		 * Wraps the {@link Statement} returned by the parent implementation with a
		 * {@link RunAfterTestMethodCallbacks} statement, thus preserving the
		 * default functionality but adding support for the Spring TestContext
		 * Framework.
		 * 
		 * @see RunAfterTestMethodCallbacks
		 */
		@Override
		protected Statement withAfters(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
			Statement junitAfters = super.withAfters(frameworkMethod, testInstance, statement);
			return new RunAfterTestMethodCallbacks(junitAfters, testInstance, frameworkMethod.getMethod(),
				getTestContextManager());
		}

		/**
		 * Supports Spring's {@link Repeat &#064;Repeat} annotation by returning a
		 * {@link SpringRepeat} statement initialized with the configured repeat
		 * count or <code>1</code> if no repeat count is configured.
		 * 
		 * @see SpringRepeat
		 */
		protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
			Repeat repeatAnnotation = frameworkMethod.getAnnotation(Repeat.class);
			int repeat = (repeatAnnotation != null ? repeatAnnotation.value() : 1);
			return new SpringRepeat(next, frameworkMethod.getMethod(), repeat);
		}
		
	}

	private final ArrayList<Runner> runners= new ArrayList<Runner>();

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public SpringJUnit4ParameterizedClassRunner(Class<?> klass) throws Throwable {
		super(klass, Collections.<Runner>emptyList());
		List<Object[]> parametersList= getParametersList(getTestClass());
		for (int i= 0; i < parametersList.size(); i++)
			runners.add(new TestClassRunnerForParameters(getTestClass().getJavaClass(),
					parametersList, i));
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> getParametersList(TestClass klass)
			throws Throwable {
		return (List<Object[]>) getParametersMethod(klass).invokeExplosively(
				null);
	}

	private FrameworkMethod getParametersMethod(TestClass testClass)
			throws Exception {
		List<FrameworkMethod> methods= testClass
				.getAnnotatedMethods(Parameters.class);
		for (FrameworkMethod each : methods) {
			int modifiers= each.getMethod().getModifiers();
			if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
				return each;
		}

		throw new Exception("No public static parameters method on class "
				+ testClass.getName());
	}

}
