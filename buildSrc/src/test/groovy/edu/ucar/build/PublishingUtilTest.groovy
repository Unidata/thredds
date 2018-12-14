package edu.ucar.build

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Issue
import spock.lang.Specification

/**
 * Test PublishingUtil with ProjectBuilder and GradleRunner.
 *
 * @author cwardgar
 * @since 2016-01-16
 */
class PublishingUtilTest extends Specification {
    def "createDependencyManagement() on multi-module project"() {
        setup: "Build a test Project using ProjectBuilder"
        Project rootProject = setupTestProject()
        List<MavenPublication> pubs = rootProject.allprojects.publishing.publications.flatten()

        when: "Create a dependencyManagement section from the project's publications."
        String actualDepMgmtXml = asString PublishingUtil.createDependencyManagement(pubs)

        then: "It matches the expected value."
        expectedDepMgmtXml.trim() == actualDepMgmtXml.trim()
    }

    // Adapted from Griffon's "GenerateBomTaskTest": https://goo.gl/IB54cK
    Project setupTestProject() {
        // Create root project 'root' with 3 subprojects: 'foo', 'bar', and 'baz'. They all start off empty.
        Project rootProject = new ProjectBuilder().withName('root').build()
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

        rootProject
    }

    String asString(Node node) {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        XmlNodePrinter nodePrinter = new XmlNodePrinter(pw)
        nodePrinter.setPreserveWhitespace(true)
        nodePrinter.print(node)
        return sw.toString()
    }

    def expectedDepMgmtXml = '''
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

    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    
    // This reads from a file generated by the java-gradle-plugin.
    // It is intended for use with GradleRunner.withPluginClasspath(), but that doesn't quite work for us because
    // we're not testing a plugin here; only the code in :buildSrc. So instead, we're going to feed those files
    // into the test build's buildscript classpath.
    List<File> buildSrcClasspath = PluginUnderTestMetadataReading.readImplementationClasspath()
    
    // The GradleRunner tests below used to fail on Windows when I just had:
    //     String buildSrcClasspathAsCsvString = '${buildSrcClasspath.join(',')}'
    // because the backslashes in the Windows paths were being interpreted as escape sequences.
    // This test asserts that adding
    //     .replace('\\', '/')
    // to the expression fixes the problem. Originally discovered by Dennis.
    def "buildSrcClasspathAsCsvString with Windows paths"() {
        setup: "windows paths"
        buildSrcClasspath = [ new File("C:\\Users\\cwardgar\\Desktop"), new File("D:\\git\\gh958") ]
        
        and: "create buildSrcClasspathAsCsvString"
        // This line appears in each of our build scripts below.
        String buildSrcClasspathAsCsvString = "${buildSrcClasspath.join(',').replace('\\', '/')}"
        
        expect: "backslashes have been replaced by forward slashes"
        buildSrcClasspathAsCsvString == "C:/Users/cwardgar/Desktop,D:/git/gh958"
    }
    
    def "adjustMavenPublicationPomScopes() on Java pub with various deps"() {
        setup: "settings file"
        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << "rootProject.name = 'test'"

        and: "build file"
        File buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            group = 'edu.ucar'
            version = '1.0'
            
            apply plugin: 'java'
            
            buildscript {
                dependencies {
                    // Need this in order to resolve PublishingUtil.
                    String buildSrcClasspathAsCsvString = '${buildSrcClasspath.join(',').replace('\\', '/')}'
                    classpath files(buildSrcClasspathAsCsvString.split(','))
                }
            }
            
            import edu.ucar.build.PublishingUtil
            PublishingUtil.addMavenPublicationsForSoftwareComponents(project)
            PublishingUtil.adjustMavenPublicationPomScopes(project)   // Testing this.
            
            dependencies {
                compile "org.slf4j:slf4j-api:1.7.7"
                runtime "org.hamcrest:hamcrest-core:1.3"
                testCompile "junit:junit:4.12"
                testRuntime "org.codehaus.groovy:groovy-all:2.4.5"
                compile "org.objenesis:objenesis:2.2"
            }
        """
        
        and: "Setup GradleRunner and execute it to get build result."
        BuildResult buildResult = GradleRunner.create()
                                              .withProjectDir(testProjectDir.root)
                                              .withArguments(':generatePomFileForTestPublication')
                                              .build()
        
        expect: "Task succeeded."
        buildResult.task(':generatePomFileForTestPublication')?.outcome == TaskOutcome.SUCCESS

        and: "It created a POM file."
        File pomFile = new File("${testProjectDir.root}/build/publications/test/pom-default.xml")
        pomFile.exists()

        and: "POM has 3 dependencies. junit and groovy-all were not included because they're test deps."
        Node projectNode = new XmlParser().parse(pomFile)
        List<Node> depNodes = projectNode.dependencies.dependency
        depNodes.size() == 3

        and: "One is hamcrest-core, with runtime scope."
        Node hamcrestDepNode = depNodes.find { it.artifactId.text() == 'hamcrest-core' }
        hamcrestDepNode?.scope.text() == 'runtime'

        and: "One is slf4j-api, with compile scope. Corrected by adjustMavenPublicationPomScopes()."
        Node slf4jDepNode = depNodes.find { it.artifactId.text() == 'slf4j-api' }
        slf4jDepNode?.scope.text() == 'compile'

        and: "One is objenesis, with compile scope. Corrected by adjustMavenPublicationPomScopes()."
        Node objenesisDepNode = depNodes.find { it.artifactId.text() == 'objenesis' }
        objenesisDepNode?.scope.text() == 'compile'
    }
    
    @Issue("gh-596")
    def "adjustMavenPublicationPomScopes() on Web pub"() {
        setup: "settings file"
        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << "rootProject.name = 'test'"
    
        and: "build file"
        File buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            group = 'edu.ucar'
            version = '1.0'
            
            buildscript {
                dependencies {
                    // Need this in order to resolve PublishingUtil.
                    String buildSrcClasspathAsCsvString = '${buildSrcClasspath.join(',').replace('\\', '/')}'
                    classpath files(buildSrcClasspathAsCsvString.split(','))
                }
            }
            
            apply plugin: 'war'
            
            import edu.ucar.build.PublishingUtil
            PublishingUtil.addMavenPublicationsForSoftwareComponents(project)
            PublishingUtil.adjustMavenPublicationPomScopes(project)   // Testing this.
        """
        
        and: "Setup GradleRunner and execute it to get build result."
        BuildResult buildResult = GradleRunner.create()
                                              .withProjectDir(testProjectDir.root)
                                              .withArguments(':generatePomFileForTestPublication')
                                              .build()
        
        expect: "Task succeeded."
        buildResult.task(':generatePomFileForTestPublication')?.outcome == TaskOutcome.SUCCESS
    
        /*
        Previously, build was failing with:
            Execution failed for task ':generatePomFileForTestPublication'.
            > assert pomDependencyNodes*.name()*.localPart.toUnique() == ['dependency']
                     |                   |       |         |          |
                     []                  []      []        []         false
         */
    }
    
    @Issue("gh-596")
    def "adjustMavenPublicationPomScopes() on artifact pub"() {  // Specifically our fat jars.
        setup: "settings file"
        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << "rootProject.name = 'test'"
    
        and: "build file"
        File buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            group = 'edu.ucar'
            version = '1.0'
            
            buildscript {
                dependencies {
                    // Need this in order to resolve PublishingUtil.
                    String buildSrcClasspathAsCsvString = '${buildSrcClasspath.join(',').replace('\\', '/')}'
                    classpath files(buildSrcClasspathAsCsvString.split(','))
                }
            }
            
            def buildTestArtifactTask = tasks.create(name: 'testArtifact', type: Zip) {
                baseName = 'test'
            }
            
            apply plugin: 'maven-publish'
            
            publishing {
                publications {
                    "test"(MavenPublication) {
                        artifactId buildTestArtifactTask.baseName
                        artifact buildTestArtifactTask
                    }
                }
            }
            
            import edu.ucar.build.PublishingUtil
            PublishingUtil.adjustMavenPublicationPomScopes(project)   // Testing this.
        """
        
        and: "Setup GradleRunner and execute it to get build result."
        BuildResult buildResult = GradleRunner.create()
                                              .withProjectDir(testProjectDir.root)
                                              .withArguments(':generatePomFileForTestPublication')
                                              .build()
        
        expect: "Task succeeded."
        buildResult.task(':generatePomFileForTestPublication')?.outcome == TaskOutcome.SUCCESS
    
        /*
        Previously, build failed with:
            Execution failed for task ':generatePomFileForTestPublication'.
            > assert pomDependencyNodes*.name()*.localPart.toUnique() == ['dependency']
                     |                   |       |         |          |
                     []                  []      []        []         false
         */
    }
}
