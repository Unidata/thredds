
/**
 * Define a subclass of java.io.OutputStream
 * that dumps to a given slf4j logger on a line
 * by line basis.
 */

package opendap.servlet;

import java.io.IOException;

public class LogStream extends java.io.OutputStream
{

    static enum Mode {error, info, warn, debug, trace};

    org.slf4j.Logger logger = null;
    Mode mode = Mode.error;

    StringBuilder buffer = new StringBuilder();

    public LogStream() {}

    public LogStream(org.slf4j.Logger logger)
    {
	this();
	setLogger(logger);
    }

    public org.slf4j.Logger getLogger() {return this.logger;}
    public LogStream setLogger(org.slf4j.Logger logger)
	{this.logger = logger; return this;}

    public Mode getMode() {return mode;}
    public LogStream setMode(Mode mode) {this.mode = mode; return this;}

    public void
    write(int b) throws IOException
    {
	buffer.append((char)b);
	if(b != '\n') return;
	String line = buffer.toString();
	buffer.setLength(0);
	switch (mode) {	
	case error: logger.error(line); break;
	case info: logger.info(line); break;
	case warn: logger.warn(line); break;
	case debug: logger.debug(line); break;
	case trace: logger.trace(line); break;
	}	
    }
}
