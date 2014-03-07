/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import com.cohort.array.StringComparatorIgnoreCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * This class contains static procedures generate standard error messages.
 * <UL>
 * <LI> It is best to pass the programming form of the classAndMethodName,
 *   e.g., "ArrayInt.set".
 * <LI> It is best to pass the menu form of the valueName,
 *   i.e., with capital first letters and spaces between words,
 *   e.g., "How Many".
 * <LI> All of the error messages have short lines, suitable for
 *   a small dialog box (not a super wide, 1-line box).
 * <LI> All of the error messages start with the classAndMethodName.
 *   [If JITs or compiling to native code may make the stack trace
 *   unavailable, it would be possible to make my own stack trace system
 *   by putting classAndMethodName at the end.  Every routine passing
 *   on the error message could return
 *   error+"CurrentClass.method"+lineSeparator.]
 * <LI> Although it makes sense to have these routines to check for the
 *   error and then return "" or an error message, it is clumsy to implement.
 *   It is better to do the exact check there, then generate this error
 *   message if there is an error.  Also, some checks are on 0..n-1
 *   versions of the value, but messages should be 1..n.
 * <LI> Because the tests are done locally, there is no need to
 *   differentiate between different numeric data types; just use doubles
 *   here.
 * <LI> Because the tests are done locally, there is no need to
 *   distinguish between > and >=, or between < and <=; both can
 *   use the same error messages.
 * <LI> ******* Don't generate lots of MustBe messages.
 *   throwableStackTrace's throwable.printStackTrace is very slow
 *   (~50ms) so all MustBe methods are pretty slow.
 * </UL>
 */
public class MustBe {
    public static String lineSeparator = "\n"; //not String2.lineSeparator;

    /** 
     * This matches the standard DAP message (except different case) for no data found. 
     * This is NOT final so EDStatic can change it. (I'm not sure if that is a good idea.)
     */
    public static String THERE_IS_NO_DATA = "Your query produced no matching results.";

    /**
     * These are MessageFormat-style strings that are NOT final so EDStatic can change them.
     */
    public static String NotNull = "{0} must not be null."; 
    public static String NotEmpty = "{0} must not be an empty string.";
    public static String InternalError = "Internal Error";
    public static String OutOfMemoryError = "Out of Memory Error";

    /**
     * This gets the current stack trace, which can be useful for debugging.
     * This is useful for debugging when no throwable is already available.
     */
    public static String getStackTrace() {

        //generate the stackTrace and print it to a printStream 
        //this is (relatively) a very slow method: ~50ms.
        //so generating lots of stackTraces takes a lot of time!
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        (new Exception()).printStackTrace(ps);
        ps.close(); //it flushes first
        return baos.toString();   
    }

    /**
     * This gets the stack trace of the thread, which can be useful for debugging.
     * This is useful for debugging when no throwable is already available.
     */
    public static String getStackTrace(Thread thread) {
        try {
            StringBuilder sb = new StringBuilder(
                "Stack trace for thread=" + thread.getName() + ":\n");
            StackTraceElement st[] = thread.getStackTrace();
            for (int i = 0; i < st.length; i++)
                sb.append(st[i].toString() + "\n");
            return sb.toString();
        } catch (Throwable t) {        
            return "ERROR while trying to get stack trace:\n" +
                throwableToString(t);
        }
    }

    /**
     * This prints the current stack trace to System.err.
     * This is useful for debugging when no throwable is already available.
     */
    public static void printStackTrace() {
        //generate the Exception
        String2.log(throwableToString(new Exception()));
    }


    /**
     * Given a throwable, this generates a multiline string
     *   (with lineSeparator's) with the method stack trace, e.g.,
     * <pre>
         java.lang.Exception: I threw this exception!
         at com.cohort.util.TestUtil.testMustBe(TestUtil.java:3330)
         at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:1006)
     * </pre>
     * @param throwable is the Throwable object providing the information
     * @param nRemoveLines determines how many lines should be
     *   removed from the start of the stack trace.
     *   For handler routine and artificial Throwable
     *     (like MustBe.between -> stackTrace()), 3 is recommended.
     *   For any routine handling a real Thrown exception
     *     (e.g., MustBe.throwable), 0 is recommended.
     * @param removeAtJava removes all lines starting with "at java".
     * @param removeAtSun removes all lines starting with "at sun".
     * @param removeAtOrgApache removes all lines starting with "at org.apache".
     */
    public static String throwableStackTrace(Throwable throwable,
            int nRemoveLines, boolean removeAtJava, boolean removeAtSun,
            boolean removeAtOrgApache) {
        if (throwable == null) 
            return "";

        //create a stream to capture the results in an ArrayByte
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true);

        //generate the stackTrace
        //this is (relatively) a very slow method: ~50ms.
        //so generating lots of stackTraces takes a lot of time!
        throwable.printStackTrace(ps);

        //convert to byte[]
        byte ba[] = baos.toByteArray();
        int i, n = ba.length;
        //replace tabs with spaces
        for (i = 0; i < n; i++) 
            if (ba[i] == '\t') 
                ba[i] = (byte)32;

        //convert to ArrayList of Strings 
        ArrayList arrayList = String2.multiLineStringToArrayList(new String(ba));
      
        //remove the first nRemoveLines lines; trim strings; store results in ca
        if (nRemoveLines < 0)
            nRemoveLines = 0;
        if (nRemoveLines > arrayList.size())
            nRemoveLines = arrayList.size();
        for (i = 0; i < nRemoveLines; i++)
          arrayList.remove(0); //each time #0 is removed, another becomes #0
        String seBase = "com.cohort.util.";
        String seName = "SimpleException";
        StringBuilder sb = new StringBuilder();
        for (i = 0; i < arrayList.size(); i++) {
          String s = (String)arrayList.get(i);
          if (i == 0 && s.startsWith(seBase + seName))
              s = s.substring(seBase.length());
          String trims = s.trim();
          if (removeAtJava && trims.startsWith("at java")) {}
          else if (removeAtSun && trims.startsWith("at sun")) {}
          else if (removeAtOrgApache && trims.startsWith("at org.apache")) {}
          else sb.append(s + lineSeparator);
          }
        //String2.noLongLines(sb, 80, "    ");
     
        return sb.toString();
    }

    /**
     * Returns a string with t.toString but with "at java" and "at sun" lines
     * removed.
     */
    public static String throwableToString(Throwable t) {
        return throwableStackTrace(t, 0, true, true, true);
    }

    /**
     * Like throwableToString, but with first "at" line only.
     */
    public static String throwableToShortString(Throwable t) {
        String s = throwableStackTrace(t, 0, true, true, true);
        int atPo = s.indexOf("\n at ");
        if (atPo > 0) {
            int nPo = s.indexOf("\n", atPo + 6);
            if (nPo > 0)
                s = s.substring(0, nPo);
        }
        return s;

    }

    /**
     * This generates a method stack trace for the current position
     *   in the program.
     * <UL>
     * <LI> This creates an Exception object and then calls
     *   throwableStackTrace().
     * </UL>
     */
    public static String stackTrace(int nRemoveLines, boolean removeAtJava,
        boolean removeAtSun, boolean removeAtOrgApache) {
        //generate the Exception 
        Exception e = new Exception();
        return throwableStackTrace(e, nRemoveLines, removeAtJava,
            removeAtSun, removeAtOrgApache);
    }

    /**
     * This generates a method stack trace for the current position
     *   in the program.
     * <UL>
     * <LI> This creates a Exception object and then calls
     *   throwableStackTrace().
     * </UL>
     */
    public static String stackTrace() {
        return stackTrace(3, true, true, true);
    }

    /**
     * This returns an error message like: <pre>
     *   String2.toUpperCase:
     *   's' must not be null</pre>.
     * <UL>
     * <LI> E.g., MustBe.notNull("MyClass.myMethod", "Value");
     * </UL>
     */
    public static String notNull(String classAndMethodName, String valueName) {
        return classAndMethodName + ":" + lineSeparator +
            MessageFormat.format(NotNull, String2.toJson(valueName)) + lineSeparator + 
            stackTrace();
    }

    /**
     * This returns an error message like: <pre>
     *   String2.toUpperCase:
     *   's' must not be an empty string</pre>.
     * <UL>
     * <LI> E.g., MustBe.notEmpty("MyClass.myMethod", "Value");
     * </UL>
     */
    public static String notEmpty(String classAndMethodName, String valueName) {
        return classAndMethodName + ":" + lineSeparator + 
            MessageFormat.format(NotEmpty, String2.toJson(valueName)) + lineSeparator +
            stackTrace();
    }

    /**
     * This returns an error message like: <pre>
     *   Data.keepIf:
     *   Internal error: f.u.n.=-1.</pre>.
     * <UL>
     * <LI> E.g., MustBe.internalError("MyClass.myMethod", "message");
     * </UL>
     */
    public static String internalError(String classAndMethodName, String message) {
        return classAndMethodName + ":" + lineSeparator + 
            InternalError + ": " + message + lineSeparator +
            stackTrace();
    }

    /**
     * This returns an error message like: <pre>
     *   Data.keepIf:
     *   Not enough data.</pre>
     * <UL>
     * <LI> E.g., MustBe.error("MyClass.myMethod", "My message.");
     * </UL>
     */
    public static String error(String classAndMethodName, String message) {
        return classAndMethodName + ":" + lineSeparator + 
            message + lineSeparator + 
            stackTrace();
    }

    /**
     * This returns an error message like: <pre>
     *   DataRound:
     *   ERROR: NullPointerException
     *   at DataRound.run(200);
     *   at DataRound.testRound(175)
     *   </pre>.
     * <UL>
     * <LI> E.g., MustBe.throwable("MyClass.myMethod", throwable);
     * </UL>
     */
    public static String throwable(String classAndMethodName,
        Throwable throwable) {
        return throwableWithMessage(classAndMethodName, 
            String2.ERROR + ":" + lineSeparator, throwable);
    }

    /**
     * This returns an error message like: <pre>
     *   DataRound.testRound:
     *   ERROR: NullPointerException
     *   at DataRound.run(200);
     *   at DataRound.testRound(175)
     *   </pre>.
     * <UL>
     * <LI> E.g., MustBe.throwable("MyClass.myMethod", throwable);
     * <LI> OutOfMemoryError is handled as a special case that prints
     *   a more extensive error message.
     *   In general, outOfMemory possibility can't just be ignored and caught.
     *   Methods need to try to catch it so that method can recover nicely.
     * </UL>
     * 
     * @param classAndMethodName e.g., DataRound.testRound
     * @param message e.g., ERROR: Null
     * 
     */
    public static String throwableWithMessage(String classAndMethodName,
        String message, Throwable throwable) {

        if (throwable instanceof OutOfMemoryError)
            //unfortunately, there isn't (ever?) a stack trace for out of memory errors
            return classAndMethodName + ":" + lineSeparator + 
                message + lineSeparator +
                OutOfMemoryError + lineSeparator +
                throwableStackTrace(throwable, 0, true, true, true) +
                "\nMustBe.throwableWithMessage detected OutOfMemoryError at\n" +
                getStackTrace();  

        return classAndMethodName + ":" + lineSeparator + 
            message + lineSeparator + 
            throwableStackTrace(throwable, 0, true, true, true);
    }

    /**
     * This returns a String with stack traces for all active threads in this JVM.
     *
     * @param hideThisThread hides this thread
     * @param hideTomcatWaitingThreads if true, this tries to not show tomcat threads 
     *   that are waiting (not really active)
     */
    public static String allStackTraces(boolean hideThisThread, boolean hideTomcatWaitingThreads) {
        try {
            //gather info for each thread
            //long tTime = System.currentTimeMillis();
            Thread thread = Thread.currentThread();
            Object oar[] = thread.getAllStackTraces().entrySet().toArray();
            int count = 0;
            String sar[] = new String[oar.length];
            for (int i = 0; i < oar.length; i++) {
                try {
                    Map.Entry me = (Map.Entry)oar[i];
                    Thread t = (Thread)me.getKey();
                    StackTraceElement ste[] = (StackTraceElement[])me.getValue();
                    String ste0 = ste.length == 0? "" : ste[0].toString();
                    if (hideThisThread && ste0.startsWith("java.lang.Thread.dumpThreads(Native Method)"))
                        continue;
                    //linux
                    if (hideTomcatWaitingThreads &&
                        ste.length == 4 && ste0.startsWith("java.lang.Object.wait(Native Method)") &&
                        ste[2].toString().startsWith("org.apache.tomcat.util.threads.ThreadPool")) 
                        continue;
                    //Mac OS/X
                    if (hideTomcatWaitingThreads &&
                        ste.length == 5 && ste0.startsWith("java.lang.Object.wait(Native Method)") &&
                        ste[2].toString().startsWith("org.apache.tomcat.util.net.JIoEndpoint$Worker.await(JIoEndpoint.java:")) 
                        continue;
                    sar[count] = t.toString() + " " + t.getState().toString() + 
                            (t.isDaemon()? " daemon\n" : "\n") + 
                        String2.toNewlineString(ste) + "\n";
                    count++;
                } catch (Exception e) {
                    sar[count] = throwableToString(e) + "\n";
                    count++;
                }
            }

            //sort
            Arrays.sort(sar, 0, count, new StringComparatorIgnoreCase());

            //write to StringBuilder
            StringBuilder sb = new StringBuilder();
            sb.append("Number of " + (hideTomcatWaitingThreads? "non-Tomcat-waiting " : "") + 
                "threads in this JVM = " + count + "\n" +
                "(format: #threadNumber Thread[threadName,threadPriority,threadGroup] threadStatus)\n\n");
            for (int i = 0; i < count; i++) 
                sb.append("#" + (i + 1) + " " + sar[i]);
            //sb.append("gather thread info time=" + (System.currentTimeMillis() - tTime) + "\n\n");
            return sb.toString();

        } catch (Exception e) {
            return throwableToString(e) + "\n";
        }
    }


    /** This returns are really short error message geared to end users. */
    public static String getShortErrorMessage(Throwable t) {

        String tError = t.getMessage();
        if (tError == null) //does happen
            tError = "";
        if (tError.length() > 0 &&
            (t instanceof SimpleException ||
             tError.indexOf(THERE_IS_NO_DATA) >= 0 ||
             tError.toLowerCase().indexOf("error") >= 0)) {
            //it would be nice to send SC_NO_CONTENT, but it is #204, so not considered an error
            //This error may have additional info, so use getMessage()
            //DON'T send extra info, it scares users
        } else {
            //send e.toString, e.g., "java.lang.nullPointerException: ....."
            //but remove parent directories, e.g., "java.lang."
            tError = t.toString(); //includes java exception name  
            if (tError == null)  //shouldn't happen
                tError = "UnknownError"; 
            int cpo = tError.indexOf(":");
            if (cpo > 0) {
                int ppo = tError.substring(0, cpo).lastIndexOf('.');
                if (ppo > 0)
                    tError = tError.substring(ppo + 1);

                //further modify the message?
                if (tError.startsWith("OutOfMemoryError")) //the Java message
                    tError = OutOfMemoryError + ": " + Math2.memoryTooMuchData;
                else if (tError.startsWith("Exception:") ||
                    tError.startsWith("RuntimeException:")) {
                    //completely remove bland exceptions
                    cpo = tError.indexOf(":");
                    tError = tError.substring(cpo + 1).trim();  //remove possible space at beginning
                }
            }
        }
        return tError;
    }

}
