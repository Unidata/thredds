/* Copyright */
package thredds.server;


import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.exception.RequestTooLargeException;
import thredds.server.exception.ServiceNotAllowed;
import thredds.server.ncss.exception.NcssException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Global Exception handling
 *   ServiceNotAllowed                      FORBIDDEN
 *   FileNotFoundException                  NOT_FOUND
 *   IOException                            INTERNAL_SERVER_ERROR
 *   UnsupportedOperationException          BAD_REQUEST
 *   IllegalArgumentException               BAD_REQUEST
 *   BindException                          BAD_REQUEST
 *   Throwable                              INTERNAL_SERVER_ERROR
 *
 * @author caron
 * @see "https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc"
 * @since 4/15/2015
 */
@Configuration
@ControllerAdvice
public class TdsErrorHandling implements HandlerExceptionResolver {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TdsErrorHandling.class);

  @ExceptionHandler(ServiceNotAllowed.class)
  public ResponseEntity<String> handle(ServiceNotAllowed ex) {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("Service Not Allowed: " + ex.getMessage(), responseHeaders, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(RequestTooLargeException.class)
  public ResponseEntity<String> handle(RequestTooLargeException ex) {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("Request Too Large: " + ex.getMessage(), responseHeaders, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<String> handle(FileNotFoundException ex) {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("FileNotFound: " + ex.getMessage(), responseHeaders, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<String> handle(IOException ex) {
    String eName = ex.getClass().getName(); // dont want compile time dependency on ClientAbortException
    if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
      logger.debug("ClientAbortException while sending file: {}", ex.getMessage());
      return null;
    }

    logger.error("IOException sending file ", ex);
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("IOException sending File " + ex.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handle(IllegalArgumentException ex) {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    // ex.printStackTrace();
    return new ResponseEntity<>("IllegalArgumentException: " + ex.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NcssException.class)
  public ResponseEntity<String> handle(NcssException ex) {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("Invalid Request: " + ex.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(org.springframework.web.bind.ServletRequestBindingException.class)
  public ResponseEntity<String> handle(org.springframework.web.bind.ServletRequestBindingException ex) {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("Invalid Request: " + ex.getMessage(), responseHeaders, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<String> handle(BindException ex) {
    BindingResult validationResult = ex.getBindingResult();
    List<ObjectError> errors = validationResult.getAllErrors();
    Formatter f = new Formatter();
    f.format("Validation errors: ");
    for (ObjectError err : errors) {
      f.format(" %s%n", err.getDefaultMessage());
    }
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>(f.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
  }

  // LOOK this could be a problem
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<String> handle(Throwable ex) throws Throwable {
    // If the exception is annotated with @ResponseStatus rethrow it and let
    // the framework handle it - like the OrderNotFoundException example
    // at the start of this post.
    // AnnotationUtils is a Spring Framework utility class.
    // see https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc
    if (AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class) != null)
      throw ex;

    logger.error("uncaught exception", ex);
    // ex.printStackTrace(); // temporary - remove in production

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>("Throwable exception handled : " + ex.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /////////////////////////////////////////////
  /// this catches exception from everything else, eg views

  @Override
  public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    logger.error("uncaught exception 2", ex);
    return null;
  }


  /*
  see http://www.mytechnotes.biz/2012/08/spring-mvc-with-annotations-example.html


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import java.util.ArrayList;
import java.util.List;
    @Bean
    HandlerExceptionResolverComposite getHandlerExceptionResolverComposite() {

      HandlerExceptionResolverComposite result = new HandlerExceptionResolverComposite();

      List<HandlerExceptionResolver> l = new ArrayList<>();

        l.add(new AnnotationMethodHandlerExceptionResolver());
        l.add(new ResponseStatusExceptionResolver());
        l.add(getSimpleMappingExceptionResolver());
        l.add(new DefaultHandlerExceptionResolver());

      result.setExceptionResolvers(l);

      return result;
    }      */

}