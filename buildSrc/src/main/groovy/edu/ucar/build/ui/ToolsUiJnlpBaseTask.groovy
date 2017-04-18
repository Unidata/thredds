package edu.ucar.build.ui

import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Nullable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Writes the base webstart (JNLP) file for ToolsUI. No version or dependency information is included in
 * the output file; as a result, it doesn't need to be updated for new releases. So, a user could download the file to
 * their local machine and it should work forever.
 * <p>
 * The generated output file imports an auxiliary file – named {@code extensionJnlpFileName} – which is written by
 * {@link ToolsUiJnlpExtensionTask}. The auxiliary file is where the version and dependency information is defined.
 *
 * @author cwardgar
 * @since 2017-04-04
 */
class ToolsUiJnlpBaseTask extends DefaultTask {
    /**
     * The value for {@code /jnlp/@codebase}.
     * This property is optional; if no value is specified, the attribute won't be written to the JNLP file.
     */
    @Input @Optional @Nullable
    String codebase
    
    /**
     * The value for {@code /jnlp/@version}.
     * Will be assigned a default of {@code 'project.version'}.
     */
    @Input
    String applicationVersion = project.version
    
    /**
     * The value for {@code /jnlp/resources/j2se/@version}.
     * Will be assigned a default of {@code project.targetCompatibility}
     * (see <a href="https://goo.gl/MFQRTL">targetCompatibility docs</a>).
     * That value will only be available if the Java plugin is applied to the project.
     */
    @Input
    JavaVersion targetCompatibility = project.findProperty('targetCompatibility')
    
    /**
     * The value for {@code /jnlp/resources/extension/@href}.
     * Will be assigned a default of {@code 'netCDFtoolsExtraJars.jnlp'}.
     */
    @Input
    String extensionJnlpFileName = 'netCDFtoolsExtraJars.jnlp'
    
    /**
     * The value for {@code /jnlp/application-desc/argument}.
     * This property is optional; if no value is specified, the element won't be written to the JNLP file.
     */
    @Input @Optional @Nullable
    String applicationArgument
    
    /** The file to write the JNLP to. */
    @OutputFile
    File outputFile
    
    ToolsUiJnlpBaseTask() {
        group = 'Release'
        description = "Writes the base webstart (JNLP) file for ToolsUI."
    }
    
    @TaskAction
    def write() {
        ToolsUiJnlpBaseTask.Writer writer = new ToolsUiJnlpBaseTask.Writer(
                codebase: codebase,
                applicationVersion: applicationVersion,
                targetCompatibility: targetCompatibility,
                extensionJnlpFileName: extensionJnlpFileName,
                applicationArgument: applicationArgument,
                outputFile: outputFile
        )
        writer.write()
    }
    
    // Separate implementation to enable easier unit testing.
    static class Writer {
        @Nullable String codebase
        String applicationVersion
        JavaVersion targetCompatibility
        String extensionJnlpFileName
        @Nullable String applicationArgument
        File outputFile
    
        def write() {
            outputFile.parentFile.mkdirs()
            
            new BufferedWriter(new FileWriter(outputFile)).withCloseable {
                MarkupBuilder xml = new MarkupBuilder(it)
                // codebase variable may be null, in which case we want the corresponding attribute to be omitted.
                xml.omitNullAttributes = true
        
                xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        
                xml.jnlp(spec: '7.0', codebase: codebase, version: applicationVersion) {
                    information() {
                        title('NetCDF ToolsUI')
                        vendor('Unidata')
                        homepage(href: 'https://www.unidata.ucar.edu/' +
                                       'software/thredds/current/netcdf-java/documentation.htm')
                        description(kind: 'short', 'Graphical interface to netCDF-Java / Common Data Model')
                        icon(href: 'nc.gif')
                        'offline-allowed'()
                    }
            
                    security() {
                        'all-permissions'()
                    }
            
                    update(check: 'background', policy: 'prompt-update')
            
                    resources() {
                        java(version: "$targetCompatibility+", 'max-heap-size': '1500m')
                        extension(name: 'netcdfUI Extra', href: extensionJnlpFileName)
                    }
            
                    'application-desc'('main-class': 'ucar.nc2.ui.ToolsUI') {
                        if (applicationArgument) {
                            argument(applicationArgument)
                        }
                    }
                }
            }
        }
    }
}
