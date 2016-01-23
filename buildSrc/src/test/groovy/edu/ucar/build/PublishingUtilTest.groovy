package edu.ucar.build

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Test PublishingUtil with some dummy Projects.
 *
 * @author cwardgar
 * @since 2016-01-16
 */
// Adapted from Griffon's "GenerateBomTaskTest": https://goo.gl/IB54cK
class PublishingUtilTest extends Specification {
    Project rootProject

    def setup() {
        // Create root project 'root' with 3 subprojects: 'foo', 'bar', and 'baz'. They all start off empty.
        rootProject = new ProjectBuilder().withName('root').build()
        new ProjectBuilder().withName('foo').withParent(rootProject).build()
        new ProjectBuilder().withName('bar').withParent(rootProject).build()
        new ProjectBuilder().withName('baz').withParent(rootProject).build()

        rootProject.with {
            allprojects { Project project ->
                group = 'edu.ucar'
                version = '1.0'

                apply plugin: 'java'
                PublishingUtil.addMavenPublicationsForSoftwareComponents(project)
            }

            dependencies {
                // All projects have a compilation dependency on slf4j (naturally).
                compile "org.slf4j:slf4j-api:1.7.7"

                // These dependencies only occur in this project. The first two will appear in
                // <dependencyManagment> because they belong to the 'default' configuration. The third will not.
                compile "org.objenesis:objenesis:2.2"
                runtime "org.hamcrest:hamcrest-core:1.3"
                testRuntime "org.codehaus.groovy:groovy-all:2.4.5"
            }

            // Subprojects

            project(':foo') {
                dependencies {
                    // All projects have a compilation dependency on slf4j (naturally).
                    compile "org.slf4j:slf4j-api:1.7.7"
                    // Both children have a runtime dependency on the root project.
                    runtime project(':')

                    // These dependencies only occur in this project. The first two will appear in
                    // <dependencyManagment> because they belong to the 'default' configuration. The third will not.
                    compile "org.akhikhl.gretty:gretty:1.2.0"
                    runtime "junit:junit:4.12"
                    testRuntime "org.bounce:bounce:0.14"
                }
            }

            project(':bar') {
                dependencies {
                    // All projects have a compilation dependency on slf4j (naturally).
                    compile "org.slf4j:slf4j-api:1.7.7"
                    // Both children have a runtime dependency on the root project.
                    runtime project(':')

                    // These dependencies only occur in this project. The first two will appear in
                    // <dependencyManagment> because they belong to the 'default' configuration. The third will not.
                    compile "com.sleepycat:je:4.0.92"
                    runtime "org.tukaani:xz:1.0"
                    testRuntime "commons-io:commons-io:2.4"
                }
            }

            // Make sure PublishingUtil can handle an empty WAR project.
            project(':baz') {
                apply plugin: 'war'
            }
        }
    }

    def expectedXml = '''
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.sleepycat</groupId>
      <artifactId>je</artifactId>
      <version>4.0.92</version>
    </dependency>
    <dependency>
      <groupId>edu.ucar</groupId>
      <artifactId>bar</artifactId>
      <version>1.0</version>
    </dependency>
    <dependency>
      <groupId>edu.ucar</groupId>
      <artifactId>baz</artifactId>
      <version>1.0</version>
      <classifier>classes</classifier>
    </dependency>
    <dependency>
      <groupId>edu.ucar</groupId>
      <artifactId>baz</artifactId>
      <version>1.0</version>
      <type>war</type>
    </dependency>
    <dependency>
      <groupId>edu.ucar</groupId>
      <artifactId>foo</artifactId>
      <version>1.0</version>
    </dependency>
    <dependency>
      <groupId>edu.ucar</groupId>
      <artifactId>root</artifactId>
      <version>1.0</version>
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.12</version>
    </dependency>
    <dependency>
      <groupId>org.akhikhl.gretty</groupId>
      <artifactId>gretty</artifactId>
      <version>1.2.0</version>
    </dependency>
    <dependency>
      <groupId>org.hamcrest</groupId>
      <artifactId>hamcrest-core</artifactId>
      <version>1.3</version>
    </dependency>
    <dependency>
      <groupId>org.objenesis</groupId>
      <artifactId>objenesis</artifactId>
      <version>2.2</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>1.7.7</version>
    </dependency>
    <dependency>
      <groupId>org.tukaani</groupId>
      <artifactId>xz</artifactId>
      <version>1.0</version>
    </dependency>
  </dependencies>
</dependencyManagement>
'''

    def "generate dependencyManagement"() {
        setup:
        List<MavenPublication> pubs = rootProject.allprojects.publishing.publications.flatten()

        when:
        String actualXml = asString PublishingUtil.createDependencyManagement(pubs)

        then:
        expectedXml.trim() == actualXml.trim()
    }

    private String asString(Node node) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        XmlNodePrinter nodePrinter = new XmlNodePrinter(pw);
        nodePrinter.setPreserveWhitespace(true);
        nodePrinter.print(node);
        return sw.toString();
    }
}
