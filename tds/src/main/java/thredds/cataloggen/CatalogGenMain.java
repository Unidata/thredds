/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
// $Id: CatalogGenMain.java 56 2006-07-12 20:20:03Z edavis $
package thredds.cataloggen;

import org.apache.log4j.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;

import thredds.cataloggen.config.DatasetSource;
import thredds.cataloggen.config.CatalogRefInfo;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogImpl;

/**
 * _more_
 *
 * @author edavis
 * @since Jan 20, 2006 12:04:08 PM
 */
public class CatalogGenMain
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( CatalogGen.class );

  public static void main( String[] args )
  {
    Logger catGenLogger = null;
    Level logLevel = null;
    Appender appender = null;
    PatternLayout layout = new PatternLayout( "%d{yyyy-MM-dd HH:mm:ss.SSS} [%10r] %-5p - %c - %m%n" );

    boolean genCatalogRefs = false;

    String configFileName = null;
    File configFile = null;
    URL configDocURL = null;

    String outFileName = null;


    StringBuilder msgLog = new StringBuilder();

    {
      // Deal with command line arguments.
      if ( args.length == 0 )
      {
        usage( System.out );
        System.exit( 0 );
      }
      List arguments = new ArrayList();
      for ( int i = 0; i < args.length; i++ )
      {
        arguments.add( args[i] );
      }
      int argIndex = arguments.indexOf( "-help" );
      if ( argIndex != -1 )
      {
        usage( System.out );
        System.exit( 0 );
      }

      // Handle -log option
      String logFileName = null;
      File logFile = null;
      String logLevelName = null;

      argIndex = arguments.indexOf( "-log" );
      if ( argIndex != -1 )
      {
        try
        {
          arguments.remove( argIndex );
          logFileName = (String) arguments.get( argIndex );
          arguments.remove( argIndex );
          logLevelName = (String) arguments.get( argIndex );
          arguments.remove( argIndex );
        }
        catch ( IndexOutOfBoundsException e )
        {
          System.err.println( "CatalogGen: not enough information for -log option." );
          usage( System.err );
          System.exit( 1 );
        }

        logLevel = Level.toLevel( logLevelName );
        logFile = new File( logFileName );
        if ( ! logFile.canWrite() )
        {
          System.err.println( "CatalogGen: cannot write log file." );
          usage( System.err );
          System.exit( 1 );
        }
        try
        {
          appender = new RollingFileAppender( layout, logFile.toString(), false );
        }
        catch ( IOException e )
        {
          System.err.println( "CatalogGen: Error - log file creation got IOException (" + logFile.toString() + ")" );
          System.err.println( "    " + e.getMessage() );
          System.exit( 1 );
        }
      }
      argIndex = arguments.indexOf( "-genCatalogRefs" );
      if ( argIndex != -1 )
      {
        genCatalogRefs = true;
        arguments.remove( argIndex );
      }

      // Get the name of the configuration file from the command-line.
      if ( arguments.size() > 0 )
      {
        configFileName = (String) arguments.get( 0 );
        arguments.remove( 0 );
        // Read the name of the output file from the command-line (optional).
        if ( arguments.size() > 0 )
        {
          outFileName = (String) arguments.get( 0 );
          arguments.remove( 0 );
        }
        // If any extra command-line tokens, print usage and exit.
        if ( arguments.size() > 0 )
        {
          System.err.println( "CatalogGen: Extra info on command-line <\"" + arguments.toString() + "\">." );
          usage( System.err );
          System.exit( 1 );
        }
      }
      // No configuration file name, print usage and exit.
      else
      {
        System.err.println( "CatalogGen: No configuration file name given." );
        usage( System.err );
        System.exit( 1 );
      }
    }

    // Setup logging and log level.
    if ( appender == null )
    {
      appender = new ConsoleAppender( layout );
      logLevel = Level.toLevel( "OFF" );
    }
    catGenLogger = Logger.getLogger( "thredds.cataloggen" );
    catGenLogger.setLevel( logLevel );
    catGenLogger.addAppender( appender );

    // Create a URL for the configuration document.
    try
    {
      URI tmpURI;
      if ( ! configFileName.startsWith( "http:" ) && ! configFileName.startsWith( "file:" ) )
      {
        configFile = new File( configFileName );
        if ( ! configFile.canRead() )
        {
          log.error( "CatalogGen: config file is not readable (" + configFile.toString() + ")" );
          System.exit( 1 );
        }
        tmpURI = configFile.toURI();
      }
      else
      {
        tmpURI = new URI( configFileName );
      }
      configDocURL = tmpURI.toURL();
    }
    catch ( MalformedURLException e )
    {
      log.error( "CatalogGen: The config doc path is not a valid URL (" + e.getMessage() + ")." );
      usage( System.err );
      System.exit( 1 );
    }
    catch ( URISyntaxException e )
    {
      log.error( "CatalogGen: The config doc path is not a valid URI (" + e.getMessage() + ")." );
      System.err.println( "CatalogGen: The config doc path is not a valid URI." );
      usage( System.err );
      System.exit( 1 );
    }

    //
    CatalogGen catGen = new CatalogGen( configDocURL );
    if ( catGen.isValid( msgLog ) )
    {
      catGen.expand();
      try
      {
        catGen.writeCatalog( outFileName );
      }
      catch (IOException e )
      {
        log.error( "CatalogGen: could not write catalog: " + e.getMessage() );
        System.exit( 1 );

      }
      log.debug( "CatalogGen: wrote catalog <" + outFileName + ">." );

      if ( genCatalogRefs )
      {
        List allCatRefInfos = catGen.getCatalogRefInfoList();
        // Generate all referenced catalogs.
        while ( allCatRefInfos.size() > 0 )
        {
          // Get the first CatalogRefInfo item and remove from list.
          CatalogRefInfo curCatRefInfo = (CatalogRefInfo) allCatRefInfos.remove( 0 );

          // Get the DatasetSource for the current catalogRef.
          DatasetSource curDss = curCatRefInfo.getDatasetSource();

          // Expand the DatasetSource and get the catalog
          InvCatalog catalog = null;
          try
          {
            catalog = curDss.fullExpand();
          }
          catch ( IOException e )
          {
            String tmpMsg = "CatalogGen: failed to expand the catalogRef catalog <" + curCatRefInfo.getAccessPointDataset() + ">: " + e.getMessage();
            log.error( tmpMsg );
          }
          // Write the resulting catalog.
          try
          {
            catGen.catFactory.writeXML( (InvCatalogImpl) catalog, curCatRefInfo.getFileName() );
          }
          catch ( IOException e )
          {
            String tmpMsg = "CatalogGen: IOException, failed to write catalogRef catalog <" + curCatRefInfo.getFileName() + ">: " + e.getMessage();
            log.error( tmpMsg );
          }
          String tmpMsg = "CatalogGen: wrote catalogRef catalog <" + curCatRefInfo.getFileName() + ">.";
          log.debug( tmpMsg );

          // Add CatRefInfos from just expanded source to the list.
          ( (ArrayList) allCatRefInfos ).ensureCapacity( allCatRefInfos.size() + curDss.getCatalogRefInfoList().size() );
          allCatRefInfos.addAll( curDss.getCatalogRefInfoList() );
        }
      }
    }
    else
    {
      log.error( "CatalogGen: Invalid config file (" + configFileName + "):\n" +
                 msgLog.toString() );
      System.exit( 1 );
    }
  }

  private static void usage( PrintStream ps )
  {
    ps.println( "Usage:" );
    ps.println( "  CatalogGen [options] <configDocName> [<outFileName>]\n" +
                "    Given a CatalogGenConfig 0.5 document, produce a completed\n" +
                "    InvCatalog 0.6 document. NOTE: the configuration document may\n" +
                "    be specified as a local file name or as a URL." );
    ps.println( "Options:\n" +
                "  -help\n" +
                "      Print this usage message.\n" +
                "  -log <logFileName> <logLevel>\n" +
                "      Write a log file at the given log level (OFF, FATAL, WARN, INFO, DEBUG, ALL).\n" +
                "  -genCatalogRefs\n" +
                "      Use this option if your config document generates catalogRefs and\n" +
                "      you want to generate the catalogs those catalogRefs reference." );
  }
}
