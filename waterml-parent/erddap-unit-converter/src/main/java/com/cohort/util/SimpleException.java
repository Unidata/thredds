/* 
 * SimpleException Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package com.cohort.util;


/**
 * This is used when the user doesn't need to see the stack trace 
 * (e.g., for a syntax error in a request).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-20
 */
public class SimpleException extends RuntimeException {

    /** A constructor */
    public SimpleException(String message) {
        super(message);
    }

    /** A constructor */
    public SimpleException(String message, Throwable t) {
        super(message + "\n(Cause: " + t.toString() + ")", t);
    }
}
