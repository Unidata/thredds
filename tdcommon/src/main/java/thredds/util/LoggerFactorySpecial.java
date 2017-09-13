/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import ucar.nc2.util.log.LoggerFactory;
import ucar.unidata.util.StringUtil2;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A LoggerFactory that uses log4j to create and configure a special RollingFileAppender
 * specific to this name.
 * used by InvDatasetFeatureCollection to create a log for each feature collection.
 * all wrong see http://logging.apache.org/log4j/2.x/manual/extending.html
 *
 * @author caron
 * @since 3/27/13
 */
public class LoggerFactorySpecial implements LoggerFactory {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");
  static private Map<String, org.slf4j.Logger> map = new HashMap<>();

  private String dir = "./";
  private long maxSize;
  private int maxBackups;
  private Level level = Level.INFO;

  public LoggerFactorySpecial(long maxSize, int maxBackups, String levels) {
    String p = System.getProperty("tds.log.dir");
    if (p != null) dir = p;

    this.maxSize =  maxSize;
    this.maxBackups =  maxBackups;
    try {
      Level tlevel = Level.toLevel(levels);
      if (tlevel != null) level = tlevel;
    } catch (Exception e) {
      startupLog.error("Illegal Logger level="+levels);
    }
  }

  /* @Override
  public org.slf4j.Logger getLogger(String name) {
    name = StringUtil2.replace(name.trim(), ' ', "_");
    org.slf4j.Logger result = map.get(name);
    if (result != null) return result;

    try {
      org.apache.logging.log4j.core.Logger log4j = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(name, new MyMessageFactory());
      log4j.info( new MyMapMessage(name));
      startupLog.info("LoggerFactorySpecial add logger= {}", name);

      result = org.slf4j.LoggerFactory.getLogger(name); // get wrapper in slf4j
      map.put(name, result);
      return result;

    } catch (Throwable ioe) {
      startupLog.error("LoggerFactorySpecial failed on " + name, ioe);

      // standard slf4j - rely on external configuration
      return org.slf4j.LoggerFactory.getLogger(name);
    }
  }

  private class MyMessageFactory implements MessageFactory {

    @Override
    public Message newMessage(Object o) {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Message newMessage(String s) {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Message newMessage(String s, Object... objects) {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }

  /* <RollingFile name="fc" fileName="${tds.log.dir}/fc.${map:collectionName}.log" filePattern="${tds.log.dir}/fc.${map:collectionName}.%i.log">
      <PatternLayout pattern="%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z} %-5p - %m%n"/>
      <Policies>
        <SizeBasedTriggeringPolicy size="1 MB"/>
      </Policies>
      <DefaultRolloverStrategy max="10"/>
    </RollingFile>
   *
  private class MyMapMessage extends MapMessage {
    MyMapMessage(String name) {
      super();
      put("collectionName", name);
    }
  }  */

  public org.slf4j.Logger getLogger(String name) {
    name = StringUtil2.replace(name.trim(), ' ', "_");
    org.slf4j.Logger result = map.get(name);
    if (result != null)
      return result;

    try {
      String fileName = dir + "/" + name + ".log";
      String fileNamePattern = dir + "/" + name + "%i.log";

      // create logger in log4j2
      // TODO: There are Builders that make this logger creation less awkward.
      Configuration config = new NullConfiguration(); // LOOK: Why are we using this? Why not DefaultConfiguration?
      PatternLayout layout = PatternLayout.createLayout(
              "%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z} %-5p - %m%n",// final String pattern,
              null,   // ?? PatternSelector patternSelector,
              config, // Configuration config,
              null,   // RegexReplacement replace,
              null,   // Charset charset,
              true,   // boolean alwaysWriteExceptions,
              false,  // boolean noConsoleNoAnsi,
              null,   // String headerPattern,
              null    // String footerPattern
      );

      DefaultRolloverStrategy.createStrategy(
              Integer.toString(maxBackups),// String max,
              "1",  // String min,
              "max", // String fileIndex,
              null,  // String compressionLevelStr,
              null,  // ?? Action[] customActions,
              true,  // boolean stopCustomActionsOnError,
              config //Configuration config
      );

      RollingFileAppender app = RollingFileAppender.createAppender(
              fileName,                                                         // String fileName
              fileNamePattern,                                                  // String filePattern
              "true",                                                           // String append
              name,                                                             // String name
              "true",                                                           // String bufferedIO
              null,                                                             // String bufferSizeStr
              "true",                                                           // String immediateFlush
              SizeBasedTriggeringPolicy.createPolicy(Long.toString(maxSize)),   // TriggeringPolicy policy
              DefaultRolloverStrategy.createStrategy(                           // RolloverStrategy strategy
                      Integer.toString(maxBackups),// String max,
                      "1",  // String min,
                      "max", // String fileIndex,
                      null,  // String compressionLevelStr,
                      null,  // ?? Action[] customActions,
                      true,  // boolean stopCustomActionsOnError,
                      config //Configuration config
              ),
              layout,                                                           // Layout<? extends Serializable> layout
              null,                                                             // Filter filter
              "true",                                                           // String ignore
              "false",                                                          // String advertise
              null,                                                             // String advertiseURI
              config);                                                          // Configuration config

      app.start();

      /*LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
      Configuration conf = ctx.getConfiguration();
      LoggerConfig lconf = conf.getLoggerConfig(name);
      lconf.setAdditive(false); // otherwise, it also gets sent to root logger (threddsServlet.log)
      lconf.setLevel(level);
      lconf.addAppender(app, level, null);
      ctx.updateLoggers(conf);  */

      org.apache.logging.log4j.core.Logger log4j = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(name);
      log4j.addAppender(app);
      log4j.setLevel(level);
      log4j.setAdditive(false); // otherwise, it also gets sent to root logger (threddsServlet.log)

      startupLog.info("LoggerFactorySpecial add logger= {} file= {}", name, fileName);

      result = org.slf4j.LoggerFactory.getLogger(name); // get wrapper in slf4j
      map.put(name, result);
      return result;

    } catch (Throwable ioe) {
      startupLog.error("LoggerFactorySpecial failed on " + name, ioe);

      // standard slf4j - rely on external configuration
      return org.slf4j.LoggerFactory.getLogger(name);
    }
  }

}

