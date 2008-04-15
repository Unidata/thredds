package thredds.server.cataloggen;

import org.jdom.input.SAXBuilder;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenConfigParser
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenConfigParser.class );

  private String rootElemName = "preferences";
  private String extXmlVerAttName = "EXTERNAL_XML_VERSION";
  private String extXmlVerAttVal = "1.0";

  private String rootUserElemName = "root";
  private String rootUserAttName = "type";
  private String rootUserAttVal = "user";

  private String mapElemName = "map";

  private String beanCollElemName = "beanCollection";
  private String beanCollKeyAttName = "key";
  private String beanCollKeyAttVal = "config";
  private String beanCollClassAttName = "class";
  private String beanCollClassAttVal = "thredds.cataloggen.servlet.CatGenTimerTask";

  private String beanElemName = "bean";
  private String beanNameAttName = "name";
  private String beanConfigDocNameAttName = "configDocName";
  private String beanResultFileNameAttName = "resultFileName";
  private String beanDelayAttName = "delayInMinutes";
  private String beanPeriodAttName = "periodInMinutes";

  public CatGenConfigParser() {}

  /**
   * Parse the given config file for CatGenServlet.
   *
   * @param inFile the config file.
   * @return a CatGenConfig object representing the config file.
   * @throws java.io.IOException if could not read File.
   */
  CatGenConfig parseXML( File inFile )
          throws IOException
  {
    if ( inFile == null )
      throw new IllegalArgumentException( "File must not be null." );

    FileInputStream inStream = null;
    CatGenConfig config = null;
    try
    {
      inStream = new FileInputStream( inFile );
      config = parseXML( inStream, inFile.getPath() );
    }
    catch ( IOException e )
    {
      log.error( "parseXML(): Could not read config file: " + e.getMessage() );
      return new CatGenConfig( "Could not read config file." );
    }
    finally
    {
      inStream.close();
    }

    return config;
  }

  /**
   * Parse the given config document for CatGenServlet.
   *
   * @param inStream an InputStream of the config document.
   * @param docId a String
   * @return a CatGenConfig representing the document being parsed.
   * @throws IOException if could not read InputStream.
   */
  CatGenConfig parseXML( InputStream inStream, String docId )
          throws IOException
  {
    if ( inStream == null )
      throw new IllegalArgumentException( "Input stream must not be null.");

    SAXBuilder builder = new SAXBuilder();
    Document doc;
    log.debug( "parseXML(): Parsing latest config doc \"" + docId + "\"." );
    try
    {
      doc = builder.build( inStream );
    }
    catch ( JDOMException e )
    {
      log.error( "parseXML(): Bad config doc <" + docId + ">: " + e.getMessage() );
      return new CatGenConfig( "Could not parse config document.");
    }

    CatGenConfig config = readConfig( doc.getRootElement() );

    if ( config.getTaskInfoList().isEmpty() )
    {
      log.warn( "parseXML(): Empty config file <" + docId + ">." );
    }

    return config;
  }

  /**
   * Read the contents of the config document and return a CatGenConfig object.
   *
   * @param rootElem the root element in the config document.
   * @return a CatGenConfig that represents the config document.
   */
  private CatGenConfig readConfig( Element rootElem )
  {
    if ( !rootElem.getName().equals( rootElemName ) )
    {
      log.error( "readConfig(): Root element <" + rootElem.getName() + "> not as expected <" + rootElemName + ">." );
      return new CatGenConfig( "Invalid config document.");
    }

    Element rootUserElem = rootElem.getChild( rootUserElemName );
    Element mapElem = rootUserElem.getChild( mapElemName );
    Element beanColElem = mapElem.getChild( beanCollElemName );
    // Catch empty "map" elements that were possible with the
    // former ucar.util.prefs implementation.
    if ( beanColElem == null )
    {
      log.info( "readConfig(): empty \"map\" element." );
      return new CatGenConfig();
    }
    String keyAttVal = beanColElem.getAttributeValue( beanCollKeyAttName );
    String classAttVal = beanColElem.getAttributeValue( beanCollClassAttName );
    if ( !keyAttVal.equals( beanCollKeyAttVal ) )
    {
      log.error( "readConfig(): bean collection element key attribute <" + keyAttVal + "> not as expected <" + beanCollKeyAttVal + ">." );
      return new CatGenConfig( "Invalid config document." );
    }
    if ( !classAttVal.equals( beanCollClassAttVal ) )
    {
      log.error( "readConfig(): bean collection element class attribute <" + classAttVal + "> not as expected <" + beanCollClassAttVal + ">." );
      return new CatGenConfig( "Invalid config document." );
    }

    List<CatGenTaskConfig> configList = new ArrayList<CatGenTaskConfig>();
    for ( Iterator it = beanColElem.getChildren( beanElemName ).iterator(); it.hasNext(); )
    {
      Element curBeanElem = (Element) it.next();
      String name = curBeanElem.getAttributeValue( beanNameAttName );
      String configDocName = curBeanElem.getAttributeValue( beanConfigDocNameAttName );
      String resultFileName = curBeanElem.getAttributeValue( beanResultFileNameAttName );
      int delayInMinutes;
      int periodInMinutes;
      Attribute delayInMinutesAtt = curBeanElem.getAttribute( beanDelayAttName );
      Attribute periodInMinutesAtt = curBeanElem.getAttribute( beanPeriodAttName );
      try
      {
        delayInMinutes = delayInMinutesAtt.getIntValue();
        periodInMinutes = periodInMinutesAtt.getIntValue();
        configList.add( new CatGenTaskConfig( name, configDocName, resultFileName, periodInMinutes, delayInMinutes ) );
      }
      catch ( DataConversionException e )
      {
        log.error( "readConfig(): bean element delay or period attribute not an integer value: " + e.getMessage() );
        configList.add( new CatGenTaskConfig( name + " - ***Invalid Task*** - Delay <" + delayInMinutesAtt.getValue() + "> or period <" + periodInMinutesAtt.getValue() + "> not integer value.",
                                              configDocName, resultFileName, 0, 0));
//        continue;
      }
    }

    return new CatGenConfig( configList);
  }

  void writeXML( File outFile, CatGenConfig config )
          throws IOException
  {
    FileOutputStream outStream = new FileOutputStream( outFile );
    writeXML( outStream, config );
    outStream.close();
  }

  void writeXML( OutputStream outStream, CatGenConfig config )
          throws IOException
  {
    Element rootElem = new Element( rootElemName );
    rootElem.setAttribute( extXmlVerAttName, extXmlVerAttVal );
    Document doc = new Document( rootElem );

    Element rootUserElem = new Element( rootUserElemName );
    rootUserElem.setAttribute( rootUserAttName, rootUserAttVal );
    rootElem.addContent( rootUserElem );

    Element mapElem = new Element( mapElemName );
    rootUserElem.addContent( mapElem );

    Element beanCollElem = new Element( beanCollElemName );
    beanCollElem.setAttribute( beanCollKeyAttName, beanCollKeyAttVal );
    beanCollElem.setAttribute( beanCollClassAttName, beanCollClassAttVal );
    mapElem.addContent( beanCollElem );

    for ( CatGenTaskConfig curTask : config.getTaskInfoList() )
    {
      Element curItemElem = new Element( beanElemName );
      curItemElem.setAttribute( beanNameAttName, curTask.getName() );
      curItemElem.setAttribute( beanConfigDocNameAttName, curTask.getConfigDocName() );
      curItemElem.setAttribute( beanResultFileNameAttName, curTask.getResultFileName() );
      curItemElem.setAttribute( beanDelayAttName, Integer.toString( curTask.getDelayInMinutes() ) );
      curItemElem.setAttribute( beanPeriodAttName, Integer.toString( curTask.getPeriodInMinutes() ) );

      beanCollElem.addContent( curItemElem );
    }

    XMLOutputter outputter = new XMLOutputter( Format.getPrettyFormat() );
    outputter.output( doc, outStream );
  }
}
