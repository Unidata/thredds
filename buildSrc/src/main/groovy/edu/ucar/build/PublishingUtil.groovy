package edu.ucar.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.dependencies.MavenDependencyInternal
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.api.publish.maven.internal.publisher.MavenProjectIdentity

/**
 * Generates the {@code <dependencyManagement>} element of a Maven BOM from a Gradle Project.
 *
 * Utility methods for creating Maven publications. Used in publishing.gradle. Tested in PublishingUtilTest.
 *
 * @author cwardgar
 * @since 2016-01-16
 */
// Adapted from Griffon's "GenerateBomTask" task: https://goo.gl/I2abaC
abstract class PublishingUtil {
    /**
     * Adds a {@link MavenPublication} for each {@link SoftwareComponent} in {@code project}.
     * <p>
     * Applying the 'jar' plugin to a project will add a {@link org.gradle.api.internal.java.JavaLibrary} component to
     * the project. We name the publication generated from that component <code>"${project.name}Java"</code>.
     * <p>
     * Applying the 'war' plugin to a project will add a {@link org.gradle.api.internal.java.WebApplication} component
     * to the project. We name the publication generated from that component <code>"${project.name}Web"</code>.
     * Note that the 'war' plugin implicitly applies the 'java' plugin.
     * <p>
     * If {@code project} has both Java and Web components - which happens by default when the 'war' plugin is applied -
     * both will be published. In that case, the Java publication will be assigned the Maven coordinate
     * {@code classifier='classes'} to disambiguate it from the Web publication.
     *
     * @param project  a Project with software components to publish.
     */
    static void addMavenPublicationsForSoftwareComponents(Project project) {
        project.with {
            apply plugin: 'maven-publish'

            // The PublishingExtension is a DeferredConfigurable model element, meaning that it will be configured as
            // late as possible in the build. So any 'publishing' configuration blocks are not evaluated until either:
            //   1. The project is about to execute, or
            //   2. the publishing extension is referenced as an instance, as opposed to via a configuration closure.
            // This is why we can reference 'web' software components, even though the subprojects that apply the war
            // plugin haven't been evaluated yet.
            publishing {
                publications {
                    SoftwareComponent webComponent = components.findByName('web')
                    if (webComponent) {
                        // Creates a Maven publication with the given name. It will also generate several tasks:
                        //   generatePomFileFor${project.name}WebPublication
                        //   publish${project.name}WebPublicationToMavenLocal
                        //   publishToMavenLocal                          (depends on all instances of the above task)
                        //   publish${project.name}WebPublicationTo${repoName}Repository
                        //   publish                                      (depends on all instances of the above task)
                        "${project.name}Web"(MavenPublication) {
                            from webComponent
                        }
                    }

                    SoftwareComponent javaComponent = components.findByName('java')
                    if (javaComponent) {
                        "${project.name}Java"(MavenPublication) {
                            from javaComponent

                            // The project produces both a WAR and a JAR. Maven only permits one artifact in a project
                            // to have an empty classifier, and we've given that to the WAR (e.g. tds-4.6.4.war).
                            // Following the example of the maven-war-plugin when the "attachClasses" value is "true",
                            // we're going to give the JAR a 'classes' classifier (e.g. tds-4.6.4-classes.jar).
                            if (webComponent) {
                                MavenArtifact primaryArtifact = artifacts.first()
                                primaryArtifact.classifier = 'classes'

                                // When the primary artifact has a classifier, maven-publish erroneously assigns it the
                                // packaging 'pom' (see DefaultMavenPublication.determinePackagingFromArtifacts()).
                                // So, we explicitly set it to 'jar'.
                                pom.packaging = 'jar'
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Ensures that the project's compile-time dependencies will have a scope of 'compile' in the Maven POMs that are
     * generated, instead of 'runtime'. This addresses a known bug in the maven-publish plugin:
     * https://discuss.gradle.org/t/maven-publish-plugin-generated-pom-making-dependency-scope-runtime/
     *
     * @param project  the project to adjust POM scopes for.
     */
    static void adjustMavenPublicationPomScopes(Project project) {
        project.with {
            Configuration projCompileConfig = configurations.findByName('compile')
            if (!projCompileConfig) {
                // Project has no 'compile' configuration, meaning that the 'java' plugin hasn't been applied.
                // Notably, this'll trigger on rootProject, which is fine. The rootProject pubs don't need adjustment.
                logger.debug "'$project.name' has no 'compile' configuration. Skipping."
                return
            }

            apply plugin: 'maven-publish'

            // Adapted from code in the above-mentioned Gradle Forums thread but expanded for clarity.
            publishing {
                publications.all { MavenPublication pub ->
                    pub.pom.withXml {
                        Node pomProjectNode = asNode()
                        assert pomProjectNode.name().localPart == 'project'

                        // The '*' is an alias for Node.breadthFirst(). See http://goo.gl/Bp8s0k
                        List<Node> pomDependencyNodes = pomProjectNode.dependencies.'*'
                        if (!pomDependencyNodes) {
                            // Likely a War or FatJar publication. Or just a regular Java pub with no deps.
                            logger.debug "'$pub.artifactId' has no dependencies. Skipping."
                            return
                        }
                        
                        assert pomDependencyNodes*.name()*.localPart.toUnique() == ['dependency']

                        // The compile-scoped dependencies of the project. The provided-scoped dependencies are
                        // already being handled by gradle-extra-configurations-plugin: https://goo.gl/xzRuLu
                        DependencySet projCompileDeps = projCompileConfig.dependencies

                        List<Node> depNodesToFix = pomDependencyNodes.findAll { Node pomDependencyNode ->
                            boolean nodeShouldHaveCompileScope = projCompileDeps.find { Dependency projCompileDep ->
                                projCompileDep.name == pomDependencyNode.artifactId.text()
                            }

                            nodeShouldHaveCompileScope && pomDependencyNode.scope.text() != 'compile'
                        }

                        depNodesToFix.each { Node depNode ->
                            depNode.scope*.value = 'compile'
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the {@code <dependencyManagement>} element of a Maven BOM from the given publications.
     * A {@code <dependency>} will be created for each publication. Additionally, a {@code <dependency>} will be
     * created for each direct dependency of each publication. Dependency elements will be sorted and their will be no
     * duplicates.
     *
     * @param pubs  the publications to include in the BOM.
     * @return  a dependencyManagement element.
     */
    static Node createDependencyManagement(Iterable<MavenPublication> pubs) {
        SortedSet<MavenCoordinates> deps = new TreeSet<>();

        pubs.each { MavenPublication pub ->
            MavenPomInternal pom = pub.pom
            MavenProjectIdentity projId = pom.projectIdentity

            MavenCoordinates projMavenCoords = new MavenCoordinates(
                    groupId: projId.groupId, artifactId: projId.artifactId, version: projId.version,
                    packaging: pom.packaging)

            String classifier
            MavenArtifact primaryArtifact = pub.artifacts.empty ? null : pub.artifacts.first()
            if (primaryArtifact) {
                classifier = primaryArtifact.classifier
            }

            projMavenCoords.classifier = classifier ?: ''
            deps.add projMavenCoords

            // Add deps for the project's dependencies. They all have default packaging and classifier.
            pom.runtimeDependencies.each { MavenDependencyInternal dep ->
                deps.add new MavenCoordinates(
                        groupId: dep.groupId, artifactId: dep.artifactId, version: dep.version)
            }
        }

        Node depMgmtNode = new Node(null, "dependencyManagement")
        Node depsNode = depMgmtNode.appendNode("dependencies")

        deps.each {
            Node depNode = depsNode.appendNode("dependency")
            depNode.appendNode("groupId", it.groupId)
            depNode.appendNode("artifactId", it.artifactId)
            depNode.appendNode("version", it.version)

            if (it.packaging != 'jar') {
                // 'jar' is the default packaging. Only need to write non-default values.
                depNode.appendNode("type", it.packaging)
            }
            if (!it.classifier.empty) {
                // Only write classifier if it is non-empty
                depNode.appendNode('classifier', it.classifier)
            }
        }

        return depMgmtNode
    }

    // See https://maven.apache.org/pom.html#Maven_Coordinates
    private static class MavenCoordinates implements Comparable<MavenCoordinates> {
        String groupId
        String artifactId
        String packaging  = 'jar'   // Default value
        String classifier = ''      // Default value
        String version

        // Create a comparator that sorts by groupId.
        // If there's a tie, sort by artifactId.
        // If there's still a tie, sort by packaging.
        // If there's still a tie, sort by classifier.
        // If there's still a tie, sort by version.
        private def comparator = new OrderBy<>(
                [{it.groupId}, {it.artifactId}, {it.packaging}, {it.classifier}, {it.version}])

        @Override
        int compareTo(MavenCoordinates that) {
            return comparator.compare(this, that)
        }

        @Override
        String toString() {
            return "$groupId:$artifactId:$packaging:$classifier:$version"
        }
    }

    private PublishingUtil() {}
}
