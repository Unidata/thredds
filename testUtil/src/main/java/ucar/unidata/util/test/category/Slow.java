package ucar.unidata.util.test.category;

/**
 * A marker to be used with JUnit categories to indicate that a test method or test class takes a long time to run.
 * We'll want to avoid running such tests after every commit; instead just run them once-a-night or so.
 *
 * @author cwardgar
 * @since 2015/09/22
 */
public interface Slow {
}
