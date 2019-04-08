package edu.ucar.build.publishing

import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.OkHttpBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Deletes components from Nexus Repository Manager 3 repositories.
 *
 * @author cwardgar
 * @since 2017-10-05
 */
class DeleteFromNexusTask extends DefaultTask {
    /** The Nexus host, e.g. {@code https://artifacts.unidata.ucar.edu}. */
    @Input
    String host
    
    /** The name of the user we're interacting with Nexus as. */
    @Input
    String username
    
    /** The password of the user we're interacting with Nexus as. */
    @Input
    String password
    
    /**
     * If {@code true}, print the components found by the query, but don't actually delete them.
     * For safety, the default value if {@code true}.
     */
    @Input
    boolean dryRun = true
    
    /**
     * Set {@link #dryRun}. It is intended to be used from the command-line. An example invocation would be:
     *     {@code ./gradlew :docs:deleteFromNexus --dryRun=false}.
     *
     * @param value  the value to assign.
     * @throws IllegalArgumentException  if the value isn't a Boolean or String.
     */
    // See http://mrhaki.blogspot.com/2016/09/gradle-goodness-use-command-line.html
    @Option(option = "dryRun",
            description = "If true, print the components found by the query, but don't actually delete them.")
    // We use an Object parameter here so that we can do "--dryRun=true" and "--dryRun=false". If the parameter was a
    // boolean, we could only do "--dryRun"; there'd be no way to set 'dryRun' to 'false'.
    void setDryRun(Object value) {
        if (!(value instanceof Boolean) && !(value instanceof String)) {
            throw new IllegalArgumentException("value ('$value') is not a Boolean or String.")
        }
        dryRun = Boolean.valueOf(value)
    }
    
    /**
     * Query parameters for the Nexus REST API 'search' endpoint. For acceptable parameter names, see:
     * https://artifacts.unidata.ucar.edu/swagger-ui/#!/search/search
     */
    @Input
    Map<String, String> searchQueryParameters = new LinkedHashMap<>()
    
    // No @Output. The task will never be considered up-to-date.
    
    DeleteFromNexusTask() {
        group = 'Publishing'
        description = "Deletes artifacts from NXRM 3 repositories."
    }
    
    @TaskAction
    def searchAndDestroy() {
        List<Component> components = search()
        
        if (dryRun) {
            logger.quiet "--- Components matching the query ---"
            components.each { Component component ->
                logger.quiet "    ${component.name}"
            }
        } else {
            destroy(components)
        }
    }
    
    List<Component> search() {
        HttpBuilder http = HttpBuilder.configure {
            request.uri = host
            request.uri.path = "/service/rest/v1/search"
            request.contentType = ContentTypes.JSON.first()
        }
        
        List<Component> components = []
        
        while (true) {  // Infinite loop.
            http.get {
                searchQueryParameters.each { paramKey, paramValue ->
                    if (paramValue) {
                        // Add parameter key/value to 'query' Map object.
                        request.uri.query."$paramKey" = paramValue
                    }
                }
                
                response.success { FromServer ret, Map<String, ?> jsonResponse ->
                    logger.info "GET successful (${ret.statusCode}): ${request.uri.toURI()}"
                    searchQueryParameters.continuationToken = jsonResponse.continuationToken
                    
                    jsonResponse.items.each { Map<String, ?> jsonComponent ->
                        components << new Component(id: jsonComponent.id, name: jsonComponent.name)
                    }
                }
                response.failure { FromServer ret ->
                    throw new GradleException(ret.message)
                }
                response.exception { Throwable t ->
                    throw new GradleException("There was an exception during request/response processing.", t)
                }
            }
            
            // This looping logic would be simpler to express with a do-while loop, but Groovy lacks that construct.
            if (!searchQueryParameters.continuationToken) {
                break
            }
        }
        
        logger.info "Found ${components.size()} components matching the query."
        components
    }
    
    def destroy(List<Component> components) {
        HttpBuilder http = OkHttpBuilder.configure {
            request.uri = host
            request.auth.basic username, password
        }
        
        components.each { Component component ->
            http.delete {
                request.uri.path = "/service/rest/v1/components/${component.id}"
                
                response.success { FromServer ret, Map<String, ?> jsonResponse ->
                    logger.info "DELETE successful (${ret.statusCode}): ${request.uri.toURI()}"
                }
                response.failure { FromServer ret ->
                    throw new GradleException(ret.message)
                }
                response.exception { Throwable t ->
                    throw new GradleException("There was an exception during request/response processing.", t)
                }
            }
        }
    
        logger.info "Deleted ${components.size()} components from Nexus."
    }
    
    static class Component {
        def id
        def name
    }
}
