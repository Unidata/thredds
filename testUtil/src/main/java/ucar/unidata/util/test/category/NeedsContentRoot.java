package ucar.unidata.util.test.category;

/**
 * A marker to be used with JUnit categories to indicate that a test method or test class requires that a TDS
 * content root be defined via the "tds.content.root.path" property. That property can be specified on the command line
 * with the "-D" switch or in gradle.properties.
 *
 * @author cwardgar
 * @since 2015/04/15
 */
public interface NeedsContentRoot {
}
