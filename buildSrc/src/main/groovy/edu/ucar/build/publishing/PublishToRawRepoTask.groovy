package edu.ucar.build.publishing

import groovy.io.FileType
import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import groovyx.net.http.HttpBuilder
import java.util.regex.Matcher

/**
 * Publishes artifacts to a Nexus Repository Manager 3 raw repository.
 *
 * @author cwardgar
 * @since 2017-09-29
 */
class PublishToRawRepoTask extends DefaultTask {
    /** The host to publish to, e.g. {@code https://artifacts.unidata.ucar.edu}. */
    @Input
    String host
    
    /** The name of the raw repository to publish to, e.g. {@code thredds-doc}. */
    @Input
    String repoName
    
    /**
     * The local file that will be published. If it is a directory, all descendant files within will be published.
     * The paths of those files, relative to {@code srcFile}, will be retained in the final artifact URL. For example:
     * <pre>
     *     [cwardgar@lenny ~/foo] $ tree
     *     .
     *     └── build
     *         ├── PublishingUtil.groovy
     *         └── publishing
     *             └── PublishToRawRepoTask.groovy
     * </pre>
     * If {@code srcFile == ~/foo}, then the URLs of the artifacts will be:
     * <ul>
     *     <li>{@code $host/$repoName/$destPath/build/PublishingUtil.groovy}</li>
     *     <li>{@code $host/$repoName/$destPath/build/publishing/PublishToRawRepoTask.groovy}</li>
     * </ul>
     */
    @Input
    File srcFile
    
    /**
     * The destination, relative to {@code repoName}'s root, to which artifacts will be published, e.g.
     * {@code https://artifacts.unidata.ucar.edu/$destPath/index.adoc}. Leading and trailing slashes are neither
     * required nor prohibited; their presence makes no difference.
     * <p>
     * This property is {@code "/"} by default.
     */
    @Input @Optional
    String destPath = "/"
    
    /** The name of the user we're interacting with Nexus as. */
    @Input
    String username
    
    /** The password of the user we're interacting with Nexus as. */
    @Input
    String password
    
    // No @Output. The task will never be considered up-to-date.
    
    PublishToRawRepoTask() {
        group = 'Publishing'
        description = "Publishes artifacts to a NXRM 3 raw repository."
    }
    
    @TaskAction
    def publish() {
        def srcFiles = []
    
        if (srcFile.isFile()) {
            srcFiles << srcFile
        } else if (srcFile.isDirectory()) {
            srcFile.eachFileRecurse(FileType.FILES) { File file ->
                srcFiles << file
            }
        } else {
            throw new GradleException("'$srcFile' isn't a normal file or directory. Most likely it doesn't exist.")
        }
    
        HttpBuilder http = HttpBuilder.configure {
            request.uri = host
            request.contentType = ContentTypes.BINARY.first()
    
            // Do preemptive auth. This isn't strictly necessary because the 'PUT /repository/*' endpoint DOES return
            // an authentication challenge if the client's initial request doesn't include credentials.
            // However, preemptive auth is likely faster as it saves us a round trip with the server.
            // See the comment in DeleteFromNexusTask.destroy() for more info about preemptive auth.
            request.headers['Authorization'] = 'Basic ' + "$username:$password".bytes.encodeBase64().toString()
        }
    
        srcFiles.each { File file ->
            http.put {
                request.uri.path = makeResourcePath(repoName, destPath, srcFile, file)
                request.body = file.bytes
            
                response.success { FromServer ret ->
                    logger.info "PUT successful (${ret.statusCode}): ${request.uri.toURI()}"
                }
                response.failure { FromServer ret ->
                    throw new GradleException(ret.message)
                }
                response.exception { Throwable t ->
                    throw new GradleException("There was an exception during request/response processing.", t)
                }
            }
        }
    
        logger.info "Published ${srcFiles.size()} documents to Nexus."
    }
    
    
    /**
     * Makes a resource path from the given arguments that will be appended to {@link #host} to form the full artifact
     * URL. The format of the path will be: {@code /repository/$repoName/$destPath/$relativeSrcPath}. If
     * {@code srcBase == srcFile}, the path will simply be:
     * <code>/repository/$repoName/$destPath/${srcFile.name}</code>
     *
     * @param repoName  the name of a raw repository.
     * @param destPath  the destination path. Will be excluded from resource path if it's {@code "/"} or {@code ""}.
     * @param srcBase   the base directory within which {@code srcFile} lives.
     * @param srcFile   a file within {@code srcBase}.
     * @return  the resource path that {@code srcFile} will have on {@link #host} when it's published.
     * @throws IllegalArgumentException  if {@code repoName} is null or empty.
     */
    static String makeResourcePath(String repoName, String destPath, File srcBase, File srcFile)
            throws IllegalArgumentException {
        if (!repoName) {
            throw new IllegalArgumentException("repoName must be non-null and non-empty.")
        }
        
        StringBuilder sb = new StringBuilder("/repository/")
        sb << repoName << '/'
    
        String destPathStripped = stripLeadingAndTrailingSlashes(destPath)
        if (destPathStripped) {
            sb << destPathStripped << '/'
        }
        
        sb << relativize((srcBase == srcFile ? srcBase.parentFile : srcBase), srcFile)
        return sb.toString()
    }
    
    /**
     * Strips all leading and trailing forward slashes from the given string. Internal slashes are left alone.
     *
     * @param s  a string.
     * @return   the given string with all leading and trailing forward slashes removed.
     */
    static String stripLeadingAndTrailingSlashes(String s) {
        Matcher matcher = (s =~ '^/*(.*?)/*$')  // First and third quantifiers are greedy; the second is lazy.
        assert matcher.matches() : "Pattern should match every string, but failed to match '$s'."
    
        // Match numbering starts at 0; capture group numbering starts at 1.
        // See http://mrhaki.blogspot.com/2009/09/groovy-goodness-matchers-for-regular.html
        return matcher[0][1]
    }
    
    /**
     * Returns the path of {@code child}, relative to {@code parent}.
     *
     * @param parent  a file.
     * @param child   a file that is a descendant of {@code parent}.
     * @return  the path of {@code child}, relative to {@code parent}.
     * @throws IllegalArgumentException  if {@code parent} is not a prefix of {@code child}.
     */
    static String relativize(File parent, File child) throws IllegalArgumentException {
        URI ret = parent.toURI().relativize(child.toURI())
        if (ret == child.toURI()) {
            throw new IllegalArgumentException("'$parent' is not a prefix of '$child'.")
        }
        return ret
    }
}
