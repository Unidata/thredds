
/**
 * Define a subclass of java.io.OutputStream
 * that dumps to a given slf4j logger on a line
 * by line basis.
 */

package opendap.log;

import java.io.IOException;
import java.io.PrintStream;

public class LogStream extends java.io.OutputStream
{

    static enum Mode {error, info, warn, debug, trace};

    static public org.slf4j.Logger log = null;

    static LogStream outlog = null;
    static LogStream errlog = null;
    static LogStream dbglog = null;

    static public PrintStream out = null;
    static public PrintStream err = null;
    static public PrintStream dbg = null;

    static public void setLogger(Class cl)
    {
        log = org.slf4j.LoggerFactory.getLogger(cl);

	if(outlog == null)
	    outlog = new LogStream(log).setMode(Mode.info);
	else
	    outlog.setLogger(log);

	if(errlog == null)
	    errlog = new LogStream(log).setMode(Mode.error);
	else
	    errlog.setLogger(log);

	if(dbglog == null)
	    dbglog = new LogStream(log).setMode(Mode.debug);
	else
	    dbglog.setLogger(log);

        if(out == null)
	    out = new PrintStream(outlog);
	if(err == null)
	    err = new PrintStream(errlog);
	if(dbg == null)
	    dbg = new PrintStream(dbglog);
    }

    static public org.slf4j.Logger getLog() {return log;}

    //////////////////////////////////////////////////
    // Instance Code

    StringBuilder buffer = new StringBuilder();
    Mode mode = null;

    public LogStream()
    {
    }

    public LogStream(org.slf4j.Logger logger)
    {
        this();
	this.setLogger(logger);
    }

    public org.slf4j.Logger getLogger() {return this.log;}

    public LogStream setLogger(org.slf4j.Logger logger)
    {
	this.log = logger;
	return this;
    }

    public Mode getMode() {return mode;}
    public LogStream setMode(Mode mode) {this.mode = mode; return this;}

    // Use flush to push to the logger
    public void flush()
    {
	String line = buffer.toString();
        buffer.setLength(0);
	switch (mode) {	
	case error: log.error(line); break;
	case info: log.info(line); break;
	case warn: log.warn(line); break;
	case debug: log.debug(line); break;
	case trace: log.trace(line); break;
	}	
    }

    public void
    write(int b) throws IOException
    {
	buffer.append((char)b);
    }


}
