/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/**
 * Define a subclass of java.io.OutputStream
 * that dumps to a given slf4j logger on a line
 * by line basis.
 */

package ucar.nc2.util.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class LogStream extends java.io.OutputStream {

  static enum Mode {error, info, warn, debug, trace}

  // Add a PrintStream logflush function
  static public class LogPrintStream extends PrintStream {
    LogStream logger = null;

    public LogPrintStream(OutputStream os) {
      super(os);
      logger = (LogStream) os;
    }

    // Add a logflush method
    public void logflush() {
      if (logger != null) logger.logflush();
    }

  }

  static public org.slf4j.Logger log;

  static LogStream outlog;
  static LogStream errlog;
  static LogStream dbglog;

  static public LogPrintStream out;
  static public LogPrintStream err;
  static public LogPrintStream dbg;

  static Class currentlogclass;

  static {
    // Make sure that the printstreams are always defined
    setLogger(LogStream.class);
  }

  static public void setLogger(Class cl) {
    currentlogclass = cl;

    log = org.slf4j.LoggerFactory.getLogger(cl);

    if (outlog == null)
      outlog = new LogStream(log).setMode(Mode.info);
    else
      outlog.setLogger(log);

    if (errlog == null)
      errlog = new LogStream(log).setMode(Mode.error);
    else
      errlog.setLogger(log);

    if (dbglog == null)
      dbglog = new LogStream(log).setMode(Mode.debug);
    else
      dbglog.setLogger(log);

    if (out == null)
      out = new LogPrintStream(outlog);
    if (err == null)
      err = new LogPrintStream(errlog);
    if (dbg == null)
      dbg = new LogPrintStream(dbglog);
  }

  static public org.slf4j.Logger getLog() {
    return log;
  }

  //////////////////////////////////////////////////
  // Instance Code

  StringBuilder buffer = new StringBuilder();
  Mode mode = null;

  public LogStream() {
  }

  public LogStream(org.slf4j.Logger logger) {
    this();
    this.setLogger(logger);
  }

  public org.slf4j.Logger getLogger() {
    return this.log;
  }

  LogStream setLogger(org.slf4j.Logger logger) {
    this.log = logger;
    return this;
  }

  public Mode getMode() {
    return mode;
  }

  public LogStream setMode(Mode mode) {
    this.mode = mode;
    return this;
  }

  // Since log output is nevere actually closed by us,
  // Use logflush to push to the logger
  public void logflush() {
    String line = buffer.toString();
    buffer.setLength(0);
    switch (mode) {
      case error:
        log.error(line);
        break;
      case info:
        log.info(line);
        break;
      case warn:
        log.warn(line);
        break;
      case debug:
        log.debug(line);
        break;
      case trace:
        log.trace(line);
        break;
    }
  }

  public void
  write(int b) throws IOException {
    buffer.append((char) b);
    if(b == '\n') logflush();
  }


}
