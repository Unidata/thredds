/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import org.apache.commons.logging.Log;

/**
 * This class redirects (currently) all messages to Apache's 
 * logging system to String2.log.
 * This class responds to and attribute called "level" 
 * and an Integer with one of the xxx_LEVEL values.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-08-25
 */
public class String2Log implements Log {

    // ----------------------------------------------------- Logging Properties

    //org.apache.commons.logging.Log defines the severity levels in a relative way.
    //Here are specific ints assigned to them.
    public final static int TRACE_LEVEL = 0;
    public final static int DEBUG_LEVEL = 1;
    public final static int INFO_LEVEL = 2;
    public final static int WARN_LEVEL = 3;
    public final static int ERROR_LEVEL = 4;
    public final static int FATAL_LEVEL = 5;
    private int level; //messages of this level and higher are sent to String2.log.


    /**
     * The constructor.
     * @param level messages of this level and higher are sent to String2.log.
     */ 
    public String2Log(int level) {
        this.level = level;          
        debug("String2Log level=" + level);        
    }


    /**
     * <p> Is debug logging currently enabled? </p>
     *
     * <p> Call this method to prevent having to perform expensive operations
     * (for example, <code>String</code> concatenation)
     * when the log level is more than debug. </p>
     */
    public boolean isDebugEnabled() {return level <= DEBUG_LEVEL;}


    /**
     * <p> Is error logging currently enabled? </p>
     *
     * <p> Call this method to prevent having to perform expensive operations
     * (for example, <code>String</code> concatenation)
     * when the log level is more than error. </p>
     */
    public boolean isErrorEnabled() {return level <= ERROR_LEVEL;}


    /**
     * <p> Is fatal logging currently enabled? </p>
     *
     * <p> Call this method to prevent having to perform expensive operations
     * (for example, <code>String</code> concatenation)
     * when the log level is more than fatal. </p>
     */
    public boolean isFatalEnabled() {return level <= FATAL_LEVEL;}


    /**
     * <p> Is info logging currently enabled? </p>
     *
     * <p> Call this method to prevent having to perform expensive operations
     * (for example, <code>String</code> concatenation)
     * when the log level is more than info. </p>
     */
    public boolean isInfoEnabled() {return level <= INFO_LEVEL;}


    /**
     * <p> Is trace logging currently enabled? </p>
     *
     * <p> Call this method to prevent having to perform expensive operations
     * (for example, <code>String</code> concatenation)
     * when the log level is more than trace. </p>
     */
    public boolean isTraceEnabled() {return level <= TRACE_LEVEL;}


    /**
     * <p> Is warn logging currently enabled? </p>
     *
     * <p> Call this method to prevent having to perform expensive operations
     * (for example, <code>String</code> concatenation)
     * when the log level is more than warn. </p>
     */
    public boolean isWarnEnabled() {return level <= WARN_LEVEL;}


    // -------------------------------------------------------- Logging Methods


    /**
     * <p> Log a message with trace log level. </p>
     *
     * @param message log this message
     */
    public void trace(Object message) {
        if (level <= TRACE_LEVEL)
            String2.log("[TRACE] " + message);
    }


    /**
     * <p> Log an error with trace log level. </p>
     *
     * @param message log this message
     * @param t log this cause
     */
    public void trace(Object message, Throwable t) {
        if (level <= TRACE_LEVEL)
            String2.log("[TRACE] " + message + "\n" + MustBe.throwableToString(t));
    }



    /**
     * <p> Log a message with debug log level. </p>
     *
     * @param message log this message
     */
    public void debug(Object message) {
        if (level <= DEBUG_LEVEL)
            String2.log("[DEBUG] " + message);
    }



    /**
     * <p> Log an error with debug log level. </p>
     *
     * @param message log this message
     * @param t log this cause
     */
    public void debug(Object message, Throwable t) {
        if (level <= DEBUG_LEVEL)
            String2.log("[DEBUG] " + message + "\n" + MustBe.throwableToString(t));
    }



    /**
     * <p> Log a message with info log level. </p>
     *
     * @param message log this message
     */
    public void info(Object message) {
        if (level <= INFO_LEVEL)
            String2.log("[INFO] " + message);
    }



    /**
     * <p> Log an error with info log level. </p>
     *
     * @param message log this message
     * @param t log this cause
     */
    public void info(Object message, Throwable t) {
        if (level <= INFO_LEVEL)
            String2.log("[INFO] " + message + "\n" + MustBe.throwableToString(t));
    }



    /**
     * <p> Log a message with warn log level. </p>
     *
     * @param message log this message
     */
    public void warn(Object message) {
        if (level <= WARN_LEVEL)
            String2.log("[WARN] " + message);
    }



    /**
     * <p> Log an error with warn log level. </p>
     *
     * @param message log this message
     * @param t log this cause
     */
    public void warn(Object message, Throwable t) {
        if (level <= WARN_LEVEL)
            String2.log("[WARN] " + message + "\n" + MustBe.throwableToString(t));
    }



    /**
     * <p> Log a message with error log level. </p>
     *
     * @param message log this message
     */
    public void error(Object message) {
        if (level <= ERROR_LEVEL)
            String2.log("[ERROR] " + message);
    }



    /**
     * <p> Log an error with error log level. </p>
     *
     * @param message log this message
     * @param t log this cause
     */
    public void error(Object message, Throwable t) {
        if (level <= ERROR_LEVEL)
            String2.log("[ERROR] " + message + "\n" + MustBe.throwableToString(t));
    }



    /**
     * <p> Log a message with fatal log level. </p>
     *
     * @param message log this message
     */
    public void fatal(Object message) {
        if (level <= FATAL_LEVEL)
            String2.log("[FATAL] " + message);
    }



    /**
     * <p> Log an error with fatal log level. </p>
     *
     * @param message log this message
     * @param t log this cause
     */
    public void fatal(Object message, Throwable t) {
        if (level <= FATAL_LEVEL)
            String2.log("[FATAL] " + message + "\n" + MustBe.throwableToString(t));
    }

}
