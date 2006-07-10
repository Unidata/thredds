// $Id: Log.java,v 1.3 2005/01/07 02:08:45 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import org.apache.log4j.*;
import thredds.servlet.Debug;
import java.io.*;

/**
 * Logging utilility; cover for log4j
 * Has static, globally-accessible logger, plus individual ones.
 * @deprecated - use commons-logging
 */

public class Log {
  static private Log globalLog = new Log("globalLog", true);
  static private String logPath = "./";

  static public void setGlobalLogName(String name) { globalLog = new Log(name.replace('/','-'), true); }
  static public void printG( String s) { globalLog.print( s); }
  static public void printlnG( String s) { globalLog.println( s); }
  static public void printIfSetG(String flagName, String s) { globalLog.printIfSet( flagName, s); }
  static public void errorG( String s) { globalLog.error( s); }
  static public void errorG( String s, Throwable e) { globalLog.error( s, e); }

  /**
   * Set the logPath directory to use for all instances of Log.
   * @param logPathS : create if not exists
   */
  static public void setLogPath( String logPathS) {
    logPath = logPathS;

    // make sure that path exists
    File logPathFile = new File(logPath);
    if (!logPathFile.exists()) {
      if (!logPathFile.mkdirs())
        throw new RuntimeException("Log.setLogPath: cant create directory "+logPath);
    }

    // recreate the global log in new spot
    setGlobalLogName("globalLog");
    printG("Log.setLogPath = "+logPathS);
    System.out.println("Log.setLogPath = "+logPathS);
  }

  //////////////////////////////////////////////////////////////////////////////
  private String loggerName;
  private boolean isLogging = false;
  private org.apache.log4j.Logger logger = null;

  /**
   * Logger is created using filename = logPath + "/" + loggerName + ".log";
   * @param loggerName: name of logger
   */
  public Log(String loggerName) {
    this.loggerName = loggerName.replace('/','-');
  }

  public Log(String loggerName, boolean isLogging) {
    this(loggerName);
    setLogging( isLogging);
  }

  public boolean isLogging() { return isLogging; }
  public void setLogging(boolean isLogging) { this.isLogging = isLogging; }

  private void init() {
    String filename = logPath + "/" + loggerName + ".log";

    logger = Logger.getLogger(loggerName);
    logger.setLevel( Level.DEBUG);

    PatternLayout layout = new PatternLayout("%p: %m (%d{yy-MM-dd HH:mm:ss} )%n");
    try {
      FileAppender fa = new FileAppender( layout, filename, false);
      logger.addAppender( fa);
    } catch (IOException ioe) {
      throw new RuntimeException("Log creation got IOException", ioe);
    }

    ConsoleAppender ca = new ConsoleAppender( layout);
    logger.addAppender( ca);
  }

  public void println( String s) {
    if (!isLogging()) return;
    if (logger == null) init();
    if (sb.length() == 0)
      logger.debug(s);
    else {
      logger.debug(sb.toString()+s);
      //System.out.println(loggerName+": "+sb.toString()+s);
      sb.setLength(0);
    }
  }

  private StringBuffer sb = new StringBuffer();
  public void print( String s) {
    if (!isLogging()) return;
    sb.append(s);
  }

  public void printIfSet(String flagName, String s) {
    if (!Debug.isSet(flagName)) return;
    println(flagName +": "+ s);
  }

  public void error( String s) {
    if (!isLogging()) return;
    if (logger == null) init();
    logger.error(s);
  }

  public void error( String s, Throwable e) {
    if (!isLogging()) return;
    if (logger == null) init();
    logger.error(s, e);
  }

}

/**
 * $Log: Log.java,v $
 * Revision 1.3  2005/01/07 02:08:45  caron
 * use nj22, commons logging, clean up javadoc
 *
 * Revision 1.2  2004/11/30 22:41:25  edavis
 * Make changes for package change of Debug, Log, and StringUtil classes.
 *
 * Revision 1.1  2004/09/24 03:26:33  caron
 * merge nj22
 *
 * Revision 1.5  2004/06/18 21:54:27  caron
 * update dqc 0.3
 *
 * Revision 1.4  2004/02/20 05:02:54  caron
 * release 1.3
 *
 * Revision 1.3  2003/12/04 22:27:48  caron
 * *** empty log message ***
 *
 * Revision 1.2  2003/01/18 19:53:45  john
 * url authenticator, better logging
 *
 * Revision 1.1.1.1  2002/11/23 17:49:48  caron
 * thredds reorg
 *
 * Revision 1.1  2002/10/18 18:21:11  caron
 * thredds server
 *
 */
