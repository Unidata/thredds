package thredds.servlet.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;
import thredds.server.config.CorsConfig;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by rmay on 11/24/14.
 * Taken from here: https://gist.github.com/kdonald/2232095
 * in lieu of support in Spring itself (for now).
 */

public class RequestCORSFilter extends OncePerRequestFilter {

    @Autowired
    private CorsConfig corsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (corsConfig.isEnabled()) {
            // Add header to allow any origin
            response.addHeader("Access-Control-Allow-Origin",
                    corsConfig.getAllowedOrigin());

            // Check for CORS "pre-flight" request
            if (request.getHeader("Access-Control-Request-Method") != null &&
                    "OPTIONS".equals(request.getMethod())) {

                response.addHeader("Access-Control-Allow-Methods",
                        corsConfig.getAllowedMethods());

                if (!corsConfig.getAllowedHeaders().isEmpty())
                    response.addHeader("Access-Control-Allow-Headers",
                            corsConfig.getAllowedHeaders());

                response.addHeader("Access-Control-Max-Age",
                        Integer.toString(corsConfig.getMaxAge()));
            }
        }
        filterChain.doFilter(request, response);
    }
}