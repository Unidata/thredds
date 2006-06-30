// $Id: LogUtil.java,v 1.3 2005/08/17 18:54:30 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package thredds.servlet;



/**
 * A collection of utilities for doing logging and user messaging
 *
 * @author Jeff mcwhirted, modified by john
 **/



import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import javax.swing.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.apache.log4j.Category;



public class LogUtil {

    static Category log_ = Category.getInstance (LogUtil.class.getName ());

    private static boolean testMode = false;
    private static ArrayList exceptions = new ArrayList ();
    private static ArrayList msgs       = new ArrayList ();


    public static StringBuffer buff = new  StringBuffer  ();
    private static final Object mutex = new Object ();

    public static void append (String s) {
        synchronized (mutex) {
            buff.append (s);
            buff.append ("\n");
        }
    }

    public static boolean displayMsg =
        false;
    //	    true;

    private static Hashtable ticks  = new Hashtable ();
    private  static Hashtable mems  = new Hashtable ();
    private     static Hashtable tabs = new Hashtable ();
    private     static Hashtable traceMsgs = new Hashtable ();



    private static String lastThreadName = "";
    private static long      initMemory = 0;
    public static long lastMemory = 0;
    public static long lastTime = 0;




    static StringBuffer getBuffer () {
        Thread t =  Thread.currentThread ();
        StringBuffer sb = (StringBuffer) traceMsgs.get (t);
        if (sb == null) {
            sb = new StringBuffer ();
            traceMsgs.put (t, sb);
        }
        return sb;

    }

    static Integer getTab () {
        Thread t =  Thread.currentThread ();
        Integer tab = (Integer) tabs.get (t);
        if (tab == null) {
            tab = new Integer (0);
            tabs.put (t, tab);
        }
        return tab;
    }


    static int getCurrentTab () {
        return getTab ().intValue ();
    }

    public static void startTrace () {
        displayMsg = true;
        initMemory = (Runtime.getRuntime ().totalMemory ()-Runtime.getRuntime ().freeMemory ());
    }

    public static void stopTrace () {
        displayMsg = false;
    }

    public static void deltaCurrentTab (int delta) {
        if (!displayMsg) return;
        int v = getCurrentTab ();
        tabs.put (Thread.currentThread (), new Integer (v+delta));
    }

    public static void tabPlus () {
        deltaCurrentTab (1);
    }

    public static void tabMinus () {
        deltaCurrentTab (-1);
    }


    public static void call1 (String m) {
        call1 (m, "", true);
    }
    public static void call1 (String m, boolean print) {
        call1 (m, "", print);
    }
    public static void call1 (String m, String extra) {
        call1 (m, extra, true);
    }
    public static void call1 (String m, String extra, boolean print) {
        if (!displayMsg) return;
        synchronized (mutex) {
            if (print) {
                writeTrace (">"+m + extra);
            }
            deltaCurrentTab (1);
            ticks.put (m, new Long (System.currentTimeMillis ()));
            mems.put (m, new Long (usedMemory ()));
        }
    }


    public static void call2 (String m) {
        call2 (m, "");
    }

    public static void call2 (String m, String extra) {
        if (!displayMsg) return;
        synchronized (mutex) {
            deltaCurrentTab (-1);
            long now = System.currentTimeMillis ();
            Long lastTime = (Long)ticks.get (m);
            Long lastMemory = (Long) mems.get (m);
            if (lastTime != null && lastMemory != null) {
                long memDiff = usedMemory ()-lastMemory.longValue ();
                long then = lastTime.longValue ();
                writeTrace ("<"+m + " ms: " +(now-then) +extra);
                ticks.remove (m);
                mems.remove (m);
            } else {
                writeTrace (m + " NO LAST TIME");
            }
        }
    }

    public static long usedMemory () {
        return  (Runtime.getRuntime ().totalMemory ()-Runtime.getRuntime ().freeMemory ());
    }


    public static void clearMsgs () {
        tabs = new Hashtable ();
        traceMsgs = new Hashtable ();
    }

    public static void printMsgs () {
        for  (java.util.Enumeration keys = traceMsgs.keys (); keys.hasMoreElements ();) {
            Object key = keys.nextElement ();
            System.out.println (key);
            System.out.println (traceMsgs.get (key));
        }
        clearMsgs ();
    }


    public static void before (String m) {
        if (!displayMsg) return;
        synchronized (mutex) {
            writeTrace  (m);
            deltaCurrentTab (1);
        }
    }

    public static void after (String m) {
        if (!displayMsg) return;
        synchronized (mutex) {
            deltaCurrentTab (-1);
            writeTrace  (m);
        }
    }


    public static void msg (String m) {
        if (!displayMsg) return;
        synchronized (mutex) {
            writeTrace (m);
        }
    }

    private static void writeTrace  (String msg) {
        Thread t =  Thread.currentThread ();
        String crntThreadName =  t.getName ();
        if (!crntThreadName.equals (lastThreadName)) {
            System.out.println ("Thread:" + crntThreadName);
            lastThreadName = crntThreadName;
        }
        StringBuffer sb = getBuffer ();
        printTabs (sb);
        System.out.print (msg+"\n");
        sb.append (msg+"\n");
    }



    private static void printTabs (StringBuffer sb) {
        if (!displayMsg) return;
        int tabs = getCurrentTab ();
        long usedMemory2 =  (Runtime.getRuntime ().totalMemory ()-Runtime.getRuntime ().freeMemory ());
        if (initMemory == 0) {
            initMemory = usedMemory2;
        }

        long currentTime = System.currentTimeMillis ();

        String ts = ""+(currentTime-lastTime);
        while (ts.length ()<3) {
            ts = ts +" ";
        }
        String prefix = "";

        String ms = "" + (int)((usedMemory2-lastMemory)/1000.0);
        while (ms.length ()<5) {
            ms = ms +" ";
        }

        String tms = "" + (int)((usedMemory2-initMemory)/1000.0);
        while (tms.length ()<5) {
            tms = tms +" ";
        }


        if (lastTime == 0)
            prefix = "S   D     T";
        else
            //	    prefix=ts+" "+ms+" "+tms+" ";
            prefix=ts+" "+ms+" ";

        while (prefix.length ()<10) {
            prefix = prefix +" ";
        }

        System.out.print (prefix);
        sb.append  (prefix);

        lastMemory = usedMemory2;
        for (int i=0;i<tabs;i++) {
            sb.append  ("  ");
            System.out.print ("  ");
        }
        lastTime = currentTime;
    }






    public static Hashtable counters = new Hashtable ();
    public static List      counterList = new ArrayList ();
    public static void count (String name) {
        Integer i = (Integer) counters.get (name);
        if (i==null) {
            i = new Integer (0);
            counters.put (name, i);
            counterList.add (name);
        }
        i = new Integer (i.intValue()+1);
        counters.put (name, i);
    }
    public static void printAndClearCount () {
        for (int i=0;i<counterList.size ();i++) {
            String name = (String) counterList.get (i);
            Integer theCount = (Integer) counters.get (name);
            System.out.println ("Count:" + name + "="+theCount);
        }
        counterList = new ArrayList ();
        counters = new Hashtable ();
    }

    /**
    public static Hashtable current = new Hashtable ();
    public static Hashtable totals = new Hashtable ();
    public static List      tickList = new ArrayList ();
    public static void tickStart (String name) {
        Long i = new Long (System.currentTimeMillis ());
        current.put (name, i);
    }
    public static void tickEnd (String name) {
        Long i = (Long) current.get (name);
        long now = System.currentTimeMillis ();
        current.put (name, i);
        i = new Integer (i.intValue()+1);
        current.put (name, i);
    }
    public static void printAndClearTicks () {
        for (int i=0;i<tickList.size ();i++) {
            String name = (String) tickList.get (i);
            Long total = (Local) totals.get (name);
            System.err.println ("Tick:" + name + "="+total);
        }
        tickList = new ArrayList ();
        current = new Hashtable ();
        totals = new Hashtable ();
    }
    **/



    public static boolean anyErrors () {
        return (exceptions.size ()>0);
    }
    public static List getExceptions () {
        return exceptions;
    }
    public static List getMessages () {
        return msgs;
    }

    public static void  setTestMode (boolean v) {
        testMode  = v;
    }



   /* public static void configure () {
        Properties [] props = Misc.getProperties ("log4j.properties", LogUtil.class);
        for (int i=0; i<props.length;i++) {
            PropertyConfigurator.configure (props[i]);
        }
    } */


    public static void logException (String xmsg, Throwable exc) {
        //printException  (log_, xmsg, exc);
    }


    private static  void printExceptionsNoGui (List errorMessages, List exceptions) {
        if (exceptions == null) {
            return;
        }
        for (int i=0;i< exceptions.size (); i++) {
            Exception exc = (Exception) exceptions.get (i);
            String message = (String) errorMessages.get (i);
            logException (message, exc);
        }
    }


    /* public static  void printExceptions (List errorMessages, List exceptions) {
        if (exceptions == null) {
            return;
        }
        if (exceptions.size () == 1) {
            logException ((String) errorMessages.get (0), (Throwable) exceptions .get (0));
            return;
        }
        if (testMode) {
            printExceptionsNoGui (errorMessages,  exceptions);
            return;
        }
        final JTextArea tv = new JTextArea (30, 60);
        StringBuffer sb = new StringBuffer ();
        ArrayList comps = new ArrayList ();
        for (int i=0;i< exceptions.size (); i++) {
            Exception exc = (Exception) exceptions.get (i);
            String message = (String) errorMessages.get (i);
            comps.add (new JLabel (message+ "  "));
            JButton jb = new JButton ("Details");
            jb.addActionListener (new ObjectListener (new ObjectArray (message, exc)) {
                    public void actionPerformed (ActionEvent ae) {
                        ObjectArray oa = (ObjectArray) theObject;
                        Exception theException = (Exception) oa.getObject2 ();
                        theException.printStackTrace ();
                        StringBuffer stackMessage = new StringBuffer ();
                        stackMessage.append (theException +"\n");
                        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
                        theException.printStackTrace (new PrintStream (baos));
                        stackMessage.append (baos.toString ());
                        tv.setText (stackMessage.toString ());
                    }
                });
            comps.add (jb);
        }
        JPanel jp = GuiUtils.doLayout (comps, 2);

        GuiUtils.showDialog ("Errors", GuiUtils.topCenter (jp, new JScrollPane (tv)));
        exceptions = null;
        errorMessages = null;
    } */

    public static String getStackTrace () {
        return getStackTrace (new IllegalArgumentException (""));
    }


    public static String getStackTrace (Throwable exc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        exc.printStackTrace (new PrintStream (baos));
        return baos.toString ();
    }

    private static JFrame consoleWindow;
    private static JTextArea consoleText;


    /**
       Create (if needed) the Console window and show it
     **

    public static void showConsole  () {
        if (consoleWindow == null) {
            checkConsole ();
            JButton clearBtn =  new JButton ("Clear");
            clearBtn.addActionListener (new ActionListener () {
                    public void actionPerformed (ActionEvent ae) {
                        consoleText.setText ("");
                    }
                });
            JScrollPane sp =  new JScrollPane (consoleText,
                                               ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                               ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            JViewport   vp = sp.getViewport ();
            vp.setViewSize (new Dimension (300, 400));
            JPanel contents = GuiUtils.centerBottom (sp,  GuiUtils.wrap (clearBtn));
            consoleWindow = GuiUtils.makeWindow  ("Console", contents );
        }
        consoleWindow.show ();
    } */


    /**
       Create the consoleText JTextArea if needed
     **/
    private static void checkConsole () {
        if (consoleText == null) {
            consoleText = new JTextArea (10, 30);
            consoleText.setEditable (false);
        }
    }

    /**
       Append the given msg to the console text area. This will not show the console.
       To do that call showConsole.
     **/
    public static void consoleMessage (String msg) {
        checkConsole ();
        consoleText.append (msg);
    }

    /* public static void printException (Category log_, String xmsg, Throwable exc) {
        if (exc instanceof java.lang.reflect.InvocationTargetException) {
            exc = ((java.lang.reflect.InvocationTargetException)exc).getTargetException ();
        }
        if (exc instanceof WrapperException) {
            exc = ((WrapperException)exc).getException ();
        }

        userErrorMessage (log_, "An exception has occurred\n" + xmsg +"\n" + exc.getMessage ());
        consoleMessage (getStackTrace (exc));
        exc.printStackTrace  (System.err);

        if (testMode) {
            exceptions.add (exc);
            msgs.add (xmsg);
        }
    } */



    public static void printExceptionNoGui (Category log_, String xmsg, Throwable exc) {
        if (exc instanceof java.lang.reflect.InvocationTargetException) {
            exc = ((java.lang.reflect.InvocationTargetException)exc).getTargetException ();
        }
        exc.printStackTrace  (System.err);
        if (testMode) {
            exceptions.add (exc);
            msgs.add (xmsg);
        }
    }

    public static void userMessage (String msg) {
        userMessage (null, msg, false);
    }

    public static void userMessage (Category log_, String msg) {
        userMessage (log_, msg, false);
    }

    public static void userMessage (Category log_, String msg, String consoleMsg) {
        userMessage (log_, msg);
        log_.error (consoleMsg);
    }

    public static void userMessage (Category log_,String msg, boolean andLog) {
        if (andLog && log_ != null) {
            log_.error (msg);
        }
        if (!testMode) {
            consoleMessage (msg);
            javax.swing.JOptionPane.showMessageDialog (null,  msg);
        }
    }


    public static void userErrorMessage (Category log_, String msg) {
        if (log_ != null) {
            log_.error (msg);
        }
        if (!testMode) {
            consoleMessage (msg);
            javax.swing.JOptionPane.showMessageDialog (null,  msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void userErrorMessage (String msg) {
        userErrorMessage (null, msg);
    }


    private static ArrayList messageLogs = new ArrayList ();

    public static void  addMessageLogger (JTextArea t) {
        messageLogs.add (t);
    }
    public static void  addMessageLogger (JLabel t) {
        messageLogs.add (t);
    }
    public static void  removeMessageLogger (Object t) {
        messageLogs.remove (t);
    }

    private static String lastMessageString = "";



    public static void  clearMessage (String message) {
        if (lastMessageString != null && message.equals (lastMessageString)) {
            message ("");
        }
    }


    public static void  message (String msg) {
        lastMessageString = msg;
        if (msg.trim ().length () == 0) return;
        for (int i=0;i<messageLogs.size (); i++) {
            Object logger =  messageLogs.get (i);
            if (logger instanceof JTextArea) {
                ((JTextArea)logger).append (msg+"\n");
                ((JTextArea)logger).repaint ();
            }
            else if (logger instanceof JLabel) {
                ((JLabel)logger).setText (msg);
                ((JLabel)logger).repaint ();
            }
        }
    }

    public static int printlncnt = 0;
    public static void tracePrintlns () {
        System.setErr (new java.io.PrintStream (new ByteArrayOutputStream ()) {
                public void println (String s) {
                    doit (s);
                }
                public void  println(Object x)  {
                    doit (""+x);
                }
                private void doit (String s) {
                    if (printlncnt>0) {
                        System.out.println ("Recurse:" + s);
                        super.println (s);
                        return;
                    }
                    printlncnt++;
                    System.out.println ("PRINT:" + s);
                    Exception exc = new IllegalArgumentException ("");
                    exc.printStackTrace ();
                    printlncnt--;
                }
            });
    }


}

