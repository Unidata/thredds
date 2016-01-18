package edu.ucar.build

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 *
 *
 * @author cwardgar
 * @since 2016-01-16
 */
class GenerateMavenBomTest extends Specification {
    def "DoIt"() {
        setup:
        ProjectBuilder builder = ProjectBuilder.builder().withName("GenerateMavenBomTest")
        Project project = builder.build()

        Task task = project.tasks.create(name: 'generateBom', type: GenerateMavenBom) {
        }

        when:
        task.generateBom()

        then:
        true
    }
}
