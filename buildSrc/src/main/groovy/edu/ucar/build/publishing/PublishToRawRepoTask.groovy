package edu.ucar.build.publishing

import groovy.io.FileType
import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.MultipartContent
import groovyx.net.http.OkHttpBuilder
import groovyx.net.http.OkHttpEncoders
import okhttp3.OkHttpClient
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.TimeUnit

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
     * {@code https://artifacts.unidata.ucar.edu/$repoName/$destPath/index.adoc}. Leading and trailing slashes are
     * neither required nor prohibited; their presence makes no difference.
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
    
    /**
     * When the sums of the sizes of the source files included in a multipart POST request exceeds this value,
     * stop adding additional files to the request. Submit it and start building the next one.
     */
    @Input @Optional
    int endRequestThreshold = 5 * 1024 * 1024  // 5 MB
    
    // No @Output. The task will never be considered up-to-date.
    
    PublishToRawRepoTask() {
        group = 'Publishing'
        description = "Publishes artifacts to a NXRM 3 raw repository."
    }
    
    @TaskAction
    def publish() {
        LinkedList<File> srcFiles = new LinkedList<>()
    
        if (srcFile.isFile()) {
            srcFiles << srcFile
        } else if (srcFile.isDirectory()) {
            srcFile.eachFileRecurse(FileType.FILES) { File file ->
                srcFiles << file
            }
        } else {
            throw new GradleException("'$srcFile' isn't a normal file or directory. Most likely it doesn't exist.")
        }
    
        HttpBuilder http = OkHttpBuilder.configure {
            request.uri = host
            request.uri.path = "/service/rest/v1/components"
            request.uri.query['repository'] = repoName
            
            request.contentType = ContentTypes.MULTIPART_FORMDATA.first()
            request.encoder ContentTypes.MULTIPART_FORMDATA.first(), OkHttpEncoders.&multipart
            request.auth.basic username, password
    
            client.clientCustomizer { OkHttpClient.Builder builder ->
                // "readTimeout" refers to how long we should wait for the server to respond to our request.
                // The I/O performance of the "data" volume on nexus-prod is wildly variable, so we have to increase
                // the read timeout. The connect, read, and write timeouts are all 10 seconds by default.
                builder.readTimeout(5, TimeUnit.MINUTES)
            }
    
            logger.info "Publishing ${srcFiles.size()} files to directory '$destPath' at '${request.uri.toURI()}'."
        }
        
        while (!srcFiles.isEmpty()) {
            int numFilesInRequest = 0
            int sumOfSizesOfFilesInRequest = 0
            
            http.post {
                request.body = MultipartContent.multipart {
                    field 'raw.directory', destPath
            
                    for (int assetNum = 1; !srcFiles.isEmpty() &&
                                           sumOfSizesOfFilesInRequest < endRequestThreshold; ++assetNum) {
                        File file = srcFiles.removeFirst()
                        String remoteFilePath = relativize(srcFile, file)
                
                        field "raw.asset${assetNum}.filename", remoteFilePath
                        part "raw.asset${assetNum}", remoteFilePath, ContentTypes.BINARY.first(), file
                        
                        ++numFilesInRequest
                        sumOfSizesOfFilesInRequest += file.size()
                    }
                }
    
                response.success { FromServer ret ->
                    logger.info String.format(
                            "POSTed multipart request including %d files totalling %.2f MB to Nexus. " +
                            "There are %d files remaining.",
                            numFilesInRequest, sumOfSizesOfFilesInRequest / 1_048_576, srcFiles.size())
                }
                response.failure { FromServer ret ->
                    throw new GradleException(ret.message)
                }
                response.exception { Throwable t ->
                    throw new GradleException("There was an exception during request/response processing.", t)
                }
            }
        }
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
