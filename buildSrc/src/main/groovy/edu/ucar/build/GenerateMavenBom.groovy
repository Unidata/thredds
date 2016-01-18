package edu.ucar.build

import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.TaskAction

/**
 *
 *
 * @author cwardgar
 * @since 2016-01-16
 */
class GenerateMavenBom extends GenerateMavenPom {
    @TaskAction
    def generateBom() {
        println "YAY!"
        println "Project: $project"
    }
}
