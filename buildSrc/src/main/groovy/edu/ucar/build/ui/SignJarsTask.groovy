package edu.ucar.build.ui

import org.apache.tools.ant.BuildException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Digitally signs JAR files to make them suitable for use with Webstart.
 * <p>
 * Digital signatures are generated using the jarsigner tool (see
 * https://docs.oracle.com/javase/8/docs/technotes/tools/windows/jarsigner.html). To generate an entity's signature for
 * a file, the entity must first have a public/private key pair associated with it and one or more certificates that
 * authenticate its public key. Those items are typically located in a keystore, identified by an alias, and protected
 * by a password.
 * <p>
 * The input JARs to this task are not actually modified. Rather, copies of them are made in {@code outputDir}, and
 * then the signature files (typically named <code>META-INF/${keystoreAlias.toUpperCase()}.SF</code> and
 * <code>META-INF/${keystoreAlias.toUpperCase()}.RSA</code>) are inserted into the copies.
 * <p>
 * jarsigner will fail to sign a JAR if it already contains digital signatures or if there are duplicate entries.
 * If that happens, this task will remove the troublesome files and attempt to sign the JAR again.
 *
 * @author cwardgar
 * @since 2018-04-23
 */
class SignJarsTask extends DefaultTask {
    /** Path to the keystore containing the certificate that we sign Webstart JARs with. */
    @Input
    String keystorePath
    
    /** Password to access the keystore. */
    @Input
    String keystorePassword
    
    /** Name of the entry within the keystore associated with the certificate. */
    @Input
    String keystoreAlias
    
    /** The JARs to sign. */
    @InputFiles
    FileCollection sourceJars
    
    /** The directory where signed JARs will be created. */
    @OutputDirectory
    File outputDir
    
    SignJarsTask() {
        group = 'Webstart'
        description = "Digitally signs JAR files to make them suitable for use with Webstart."
    }
    
    @TaskAction
    def execute(IncrementalTaskInputs inputs) {
        if (!inputs.incremental) {
            project.delete(outputDir.listFiles())
        }
    
        inputs.outOfDate { InputFileDetails outOfDate ->
            deleteOutputFileCorrespondingTo outOfDate.file
            sign outOfDate.file
        }
    
        inputs.removed { InputFileDetails removed ->
            deleteOutputFileCorrespondingTo removed.file
        }
    }
    
    void deleteOutputFileCorrespondingTo(File inputFile) {
        def outputFile = project.file("$outputDir/${inputFile.name}")
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                throw new GradleException("Couldn't delete '$file'.")
            }
        }
    }
    
    void sign(File jarFile) {
        try {
            ant.signjar(jar: jarFile, keystore: keystorePath, storepass: keystorePassword, alias: keystoreAlias,
                        destDir: outputDir, preservelastmodified: true, verbose: false)
        } catch (BuildException e1) {
            // The BuildException that we get when an Ant task fails isn't very helpful. In the case of signjar, it just
            // has "e.getMessage() == 'jarsigner returned: 1'". "e.getCause()" and "e.getSuppressed()" are both null.
            // So, we can't find out exactly why the failure happened. We just have to assume it was caused by dupes.
            logger.info "jarsigner failed to sign '${jarFile.name}', likely because it contains duplicate entries. " +
                    "Removing dupes and trying again."
            jarFile = rejar(jarFile, new File(temporaryDir, jarFile.name))
    
            try {
                ant.signjar(jar: jarFile, keystore: keystorePath, storepass: keystorePassword, alias: keystoreAlias,
                            destDir: outputDir, preservelastmodified: true, verbose: false)
            } catch (BuildException e2) {
                // jarsigner STILL failed to sign it, meaning that the failure is likely unrelated to duplicate entries.
                throw new GradleException("jarsigner failed to sign '${jarFile.name}', even after rejarring it.", e)
            }
        }
    }
    
    /**
     * Creates a new JAR file at {@code destJarFile} containing the contents of {@code origJarFile}. Any duplicate
     * entries in {@code origJarFile} will be excluded from {@code destJarFile}. Also, any old signature files
     * (i.e. META-INF/*.DSA, META-INF/*.SF, META-INF/*.RSA) will be excluded.
     *
     * @param origJarFile the original JAR file.
     * @param destJarFile the destination JAR file.
     * @return {@code destJarFile}.
     */
    File rejar(File origJarFile, File destJarFile) {
        JarFile origJar = new JarFile(origJarFile)
        
        origJar.withCloseable {
            JarOutputStream jarOutStream = new JarOutputStream(
                    new BufferedOutputStream(new FileOutputStream(destJarFile)))
            
            jarOutStream.withCloseable {
                java.util.regex.Pattern signatureFilePattern = ~/META-INF\/.+\.(DSA|SF|RSA)/
                Set<String> entriesWritten = new HashSet<>()
                
                for (JarEntry origJarEntry : origJar.entries()) {
                    if (origJarEntry.name =~ signatureFilePattern) {
                        logger.debug "Excluding signature file: $origJarEntry.name"
                    } else if (!entriesWritten.add(origJarEntry.getName())) {
                        logger.debug "Skipping duplicate entry: $origJarEntry.name"
                    } else {
                        copyEntry origJar, origJarEntry, jarOutStream
                    }
                }
            }
        }
        
        return destJarFile
    }
    
    /**
     * Copies a file entry from a source JAR to the output stream of a destination JAR.
     *
     * @param srcJar        the source JAR.
     * @param jarEntry      a file entry within {@code srcJar}.
     * @param jarOutStream  and output stream connected to a destination JAR.
     */
    void copyEntry(JarFile srcJar, JarEntry jarEntry, JarOutputStream jarOutStream) {
        try {
            jarOutStream.putNextEntry(jarEntry)
            InputStream jarEntryInputStream = srcJar.getInputStream(jarEntry)
            
            jarEntryInputStream.withCloseable {
                byte[] buffer = new byte[8192]
                int bytesRead
                
                while ((bytesRead = jarEntryInputStream.read(buffer)) != -1) {
                    jarOutStream.write(buffer, 0, bytesRead)
                }
            }
        } finally {
            jarOutStream.closeEntry()
        }
    }
}
