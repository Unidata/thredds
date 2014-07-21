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

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
import ucar.nc2.util.log.LoggerFactory;
import ucar.unidata.util.StringUtil2;

import java.util.HashMap;
import java.util.Map;

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

      //create logger in log4j2
      Configuration config = new NullConfiguration(); // ?? LOOK
      Layout layout = PatternLayout.createLayout("%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z} %-5p - %m%n", config, null, null, null, null);

      /*
       fileName - The name of the file that is actively written to. (required).
       filePattern - The pattern of the file name to use on rollover. (required).
       append - If true, events are appended to the file. If false, the file is overwritten when opened. Defaults to "true"
       name - The name of the Appender (required).
       bufferedIO - When true, I/O will be buffered. Defaults to "true".
       immediateFlush - When true, events are immediately flushed. Defaults to "true".
       policy - The triggering policy. (required).
       strategy - The rollover strategy. Defaults to DefaultRolloverStrategy.
       layout - The layout to use (defaults to the default PatternLayout).
       filter - The Filter or null.
       ignore - If "true" (default) exceptions encountered when appending events are logged; otherwise they are propagated to the caller.
       advertise - "true" if the appender configuration should be advertised, "false" otherwise.
       advertiseURI - The advertised URI which can be used to retrieve the file contents.
       config - The Configuration.
        */
      RollingFileAppender app = RollingFileAppender.createAppender(
              fileName,
              fileNamePattern,
              "true",
              name,
              "true",
              "true",
              SizeBasedTriggeringPolicy.createPolicy(Long.toString(maxSize)),

              //   public static org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy createStrategy
              // (@org.apache.logging.log4j.core.config.plugins.PluginAttr("max") java.lang.String max,
              // @org.apache.logging.log4j.core.config.plugins.PluginAttr("min") java.lang.String min,
              // @org.apache.logging.log4j.core.config.plugins.PluginAttr("fileIndex") java.lang.String fileIndex,
              // @org.apache.logging.log4j.core.config.plugins.PluginConfiguration org.apache.logging.log4j.core.config.Configuration config) { /* compiled code */ }

              /*
              public static DefaultRolloverStrategy createStrategy(String max,
                                                                   String min,
                                                                   String fileIndex,
                                                                   String compressionLevelStr,
                                                                   Configuration config)
              Create the DefaultRolloverStrategy.
              Parameters:
              max - The maximum number of files to keep.
              min - The minimum number of files to keep.
              fileIndex - If set to "max" (the default), files with a higher index will be newer than files with a smaller index. If set to "min", file renaming and the counter will follow the Fixed Window strategy.
              compressionLevelStr - The compression level, 0 (less) through 9 (more); applies only to ZIP files.
              config - The Configuration.
              Returns:
              A DefaultRolloverStrategy.

               */
              DefaultRolloverStrategy.createStrategy(Integer.toString(maxBackups), "1", "max", null, config),
              layout,
              null,
              "true",
              "false",
              null,
              config);
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

