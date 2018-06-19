/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENSE file for more information.
*/

/**
Wrap the logging functionality
(essentially org.slf4j.Logger)
so we can replace it if needed.
Currently wraps org.slf4j.Logger
*/

package dap4.dap4lib;

public class DapStartLog
{
    //////////////////////////////////////////////////
    // Static variables

    static private org.slf4j.Logger log = null;

    static synchronized private void getLog()
    {
        if(log == null)
            log = org.slf4j.LoggerFactory.getLogger("serverStartup");
    }

    static synchronized public void error(String s)
    {
        if(log == null) getLog();
        log.error(s);
    }

    static synchronized public void warn(String s)
    {
        if(log == null) getLog();
        log.warn(s);
    }

    static synchronized public void info(String s)
    {
        if(log == null) getLog();
        log.info(s);
    }

    static synchronized public void debug(String s)
    {
        if(log == null) getLog();
        log.debug(s);
    }
}
