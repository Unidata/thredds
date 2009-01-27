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
// $Id: LatestConfigFactory.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.latest;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.*;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * A description
 *
 * @author edavis
 * @since Sep 20, 2005T3:20:34 PM
 */
public class LatestConfigFactory
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( LatestConfigFactory.class );

  private static String rootElemName = "latestConfig";
  private static String itemElemName = "latestItem";
  private static String itemIdAttName = "id";
  private static String itemRefIdAttName = "refid";
  private static String itemNameAttName = "name";
  private static String itemDirLocationAttName = "dirLocation";
  private static String itemDatasetNameMatchPatternAttName = "datasetNameMatchPattern";
  private static String itemDatasetTimeSubstitutionPatternAttName = "datasetTimeSubstitutionPattern";
  private static String itemServiceBaseURLAttName = "serviceBaseURL";
  private static String itemInvCatSpecVersionAttName = "invCatSpecVersion";
  private static String itemDqcSpecVersionAttName = "dqcSpecVersion";


  /**
   * Parse the given config file for LatestDqcHandler.
   *
   * @param inFile the config file.
   * @return a LatestConfig (which may be empty), or null if config file is malformed.
   * @throws IOException if could not read File.
   */
  public static LatestConfig parseXML( File inFile )
          throws IOException
  {
    FileInputStream inStream = new FileInputStream( inFile );
    LatestConfig config = parseXML( inStream, inFile.getPath() );
    inStream.close();
    return config;
  }

  /**
   * Parse the given config document for LatestDqcHandler.
   *
   * @param inStream an InputStream of the config document.
   * @return a LatestConfig (which may be empty), or null if config doc is malformed.
   * @throws IOException if could not read InputStream.
   */
  public static LatestConfig parseXML( InputStream inStream, String docId )
          throws IOException
  {
    SAXBuilder builder = new SAXBuilder();
    Document doc;
    log.debug( "parseXML(): Parsing latest config doc \"" + docId + "\".");
    try
    {
      doc = builder.build( inStream );
    }
    catch ( JDOMException e )
    {
      log.error( "parseXML(): Bad config doc <" + docId + ">: " + e.getMessage());
      return null;
    }
    LatestConfig config = readConfig( doc.getRootElement() );

    if ( config == null )
    {
      log.warn( "parseXML(): Config doc <" + docId + "> not in new format; trying old format." );
      config = readOldConfig( doc.getRootElement() );
      if ( config == null )
      {
        log.error( "parseXML(): Config doc <" + docId + "> not in new or old format." );
        return null;
      }
    }
    checkConfigRefConsitency( config );

    if ( config.isEmpty() )
    {
      log.warn( "parseXML(): Empty config file <" + docId + ">." );
    }

    return config;
  }

  public static void writeXML( File outFile, LatestConfig config )
          throws IOException
  {
    Element rootElem = new Element( rootElemName );
    addConfigItemElements( rootElem, config );

    Document doc = new Document();
    doc.setRootElement( rootElem );

    XMLOutputter outputter = new XMLOutputter( Format.getPrettyFormat() );
    FileOutputStream outStream = new FileOutputStream( outFile );
    
    outputter.output( doc, outStream );
    outStream.close();
  }

  /**
   * Read the contents of the config document and return a LatestConfig.
   *
   * @param rootElem the root element in the config document.
   * @return a LatestConfig (which may be empty), or null if the config document is malformed.
   */
  private static LatestConfig readConfig( Element rootElem )
  {
    if ( ! rootElem.getName().equals( rootElemName ) )
    {
      log.error( "readConfig(): Root element <" + rootElem.getName() + "> not as expected <" + rootElemName + ">." );
      return null;
    }

    LatestConfig config = new LatestConfig();
    java.util.List list = rootElem.getChildren( itemElemName );
    for ( Iterator it = list.iterator(); it.hasNext(); )
    {
      readConfigItem( (Element) it.next(), config );
    }

    return config;
  }

  /**
   * Read a config item and add to config.
   *
   * @param curElem the current "latestItem" element.
   * @param config the LatestConfig that is being built.
   */
  private static void readConfigItem( Element curElem, LatestConfig config )
  {
    int numAtts = curElem.getAttributes().size();
    String id = curElem.getAttributeValue( "id" );
    if ( numAtts == 2 )
    {
      String refId = curElem.getAttributeValue( "refid" );
      if ( id == null || refId == null )
      {
        log.warn( "readConfigItem(): Null value for latestItem@id or latestItem@refid - continue with next latestItem.");
        return;
      }
      if ( ! config.addItem( id, new LatestConfig.Item( id, refId ) ) )
      {
        log.warn( "readConfigItem(): Config already contains item for this id <" + id + ">." );
        return;
      }

    }
    else if ( numAtts == 8 )
    {

      String name = curElem.getAttributeValue( itemNameAttName );
      String dirLocation = curElem.getAttributeValue( itemDirLocationAttName );
      String datasetNameMatchPattern = curElem.getAttributeValue( itemDatasetNameMatchPatternAttName );
      String datasetTimeSubstitutionPattern = curElem.getAttributeValue( itemDatasetTimeSubstitutionPatternAttName );
      String serviceBaseURL = curElem.getAttributeValue( itemServiceBaseURLAttName );
      if ( serviceBaseURL.endsWith( "/"))
        serviceBaseURL = serviceBaseURL.substring( 0, serviceBaseURL.length() );
      String invCatSpecVersion = curElem.getAttributeValue( itemInvCatSpecVersionAttName );
      String dqcSpecVersion = curElem.getAttributeValue( itemDqcSpecVersionAttName );

      if ( id == null || name == null || dirLocation == null ||
           datasetNameMatchPattern == null ||
           datasetTimeSubstitutionPattern == null ||
           serviceBaseURL == null ||
           invCatSpecVersion == null ||
           dqcSpecVersion == null )
      {
        log.warn( "readConfigItem(): Null value for at least one attribute of latestItem - continue with next latestItem." );
        return;
      }
      if ( ! config.addItem( id, new LatestConfig.Item( id, name, dirLocation,
                                                        datasetNameMatchPattern, datasetTimeSubstitutionPattern,
                                                        serviceBaseURL, invCatSpecVersion, dqcSpecVersion ) ) )
      {
        log.warn( "readConfigItem(): Config already contains item for this id <" + id + ">.");
        return;
      }
    }
    else
    {
      log.warn( "readConfigItem(): Wrong number of attributes for latestItem <" + numAtts + "> - continue with next latestItem." );
      return;
    }
    return;
  }

  /**
   * Read the contents of the old style config document and return a LatestConfig.
   *
   * @param rootElem the root element in the config document.
   * @return a LatestConfig (which may be empty), or null if the config document is malformed.
   */
  private static LatestConfig readOldConfig( Element rootElem )
  {
    String oldRootElemName = "preferences";
    String old2ndElemName = "root";
    String old3rdElemName = "map";
    String groupElemName = "bean";
    String groupCollectionElemName = "beanCollection";

    if ( ! rootElem.getName().equals( oldRootElemName ) )
    {
      log.error( "readOldConfig(): Root element <" + rootElem.getName() + "> not as expected <" + oldRootElemName + ">." );
      return null;
    }
    Element old2ndElem = rootElem.getChild( old2ndElemName );
    if ( old2ndElem == null )
    {
      log.error( "readOldConfig(): No second level element with expected name <" + old2ndElemName + ">.");
      return null;
    }
    Element old3rdElem = old2ndElem.getChild( old3rdElemName );
    if ( old3rdElem == null )
    {
      log.error( "readOldConfig(): No third level element with expected name <" + old3rdElemName + ">.");
      return null;
    }
    Element groupElem = old3rdElem.getChild( groupElemName );
    if ( groupElem == null )
    {
      log.error( "readOldConfig(): No group element with expected name <" + groupElemName + ">.");
      return null;
    }
    String dirName = groupElem.getAttributeValue( "dirName" );
    String dirNameRoot = groupElem.getAttributeValue( "dirNameRoot" );
    if ( dirName == null || dirNameRoot == null )
    {
      log.error( "readOldConfig(): Null value for dirName or dirNameRoot." );
      return null;
    }
    if ( ! dirName.startsWith( dirNameRoot) )
    {
      log.error( "readOldConfig(): dirName <" + dirName + "> must start with dirNameRoot <" + dirNameRoot + ">.");
      return null;
    }

    String dirLocation = dirName.endsWith( "/") ? dirName.substring( 0, dirName.length() ) : dirName;
    String datasetNameMatchPattern = groupElem.getAttributeValue( "matchPattern" );
    String datasetTimeSubstitutionPattern = groupElem.getAttributeValue( "substitutePattern" );
    String serviceBaseURL = groupElem.getAttributeValue( "serviceBaseURL" );
    serviceBaseURL = serviceBaseURL + (serviceBaseURL.endsWith( "/") ? "" : "/") + dirName.substring( dirNameRoot.length() );

    String invCatSpecVersion = groupElem.getAttributeValue( "invCatSpecVersion" );
    String dqcSpecVersion = groupElem.getAttributeValue( "dqcSpecVersion" );

    Element groupCollectionElem = old3rdElem.getChild( groupCollectionElemName );
    if ( groupCollectionElem == null )
    {
      log.error( "readOldConfig(): No group collection element with expected name <" + groupCollectionElemName + ">." );
      return null;
    }
    LatestConfig config = new LatestConfig();
    readOldConfigGroup( groupCollectionElem, config,
                        dirLocation, datasetNameMatchPattern, datasetTimeSubstitutionPattern,
                        serviceBaseURL, invCatSpecVersion, dqcSpecVersion );

    return config;
  }

  private static void readOldConfigGroup( Element groupCollectionElem, LatestConfig config, String dirLocation,
                                          String datasetNameMatchPattern,
                                          String datasetTimeSubstitutionPattern,
                                          String serviceBaseURL,
                                          String invCatSpecVersion, String dqcSpecVersion )
  {


    for ( Iterator it = groupCollectionElem.getChildren( "bean" ).iterator(); it.hasNext(); )
    {
      Element curElem = (Element) it.next();
      String id = curElem.getAttributeValue( "name" );
      String name = curElem.getAttributeValue( "value" );

      if ( id == null || name == null || dirLocation == null ||
           datasetNameMatchPattern == null ||
           datasetTimeSubstitutionPattern == null ||
           serviceBaseURL == null ||
           invCatSpecVersion == null ||
           dqcSpecVersion == null )
      {
        log.warn( "readOldConfigGroup(): Null value for at least one attribute of latestItem - continue with next latestItem." );
        return;
      }
      if ( ! config.addItem( id, new LatestConfig.Item( id, name, dirLocation,
                                                        datasetNameMatchPattern.replaceAll( "@model@", id),
                                                        datasetTimeSubstitutionPattern,
                                                        serviceBaseURL, invCatSpecVersion, dqcSpecVersion ) ) )
      {
        log.warn( "readOldConfigGroup(): Config already contains item for this id <" + id + ">." );
        return;
      }
    }
  }

  /**
   * Make sure references between the Items in the given LatestConfig
   * are consistent. I.e., all refered to Items exist and any chain of
   * refering Items end with an Item that is not a refering Item.
   *
   * @param config the LatestConfig to check for consistency
   */
  private static void checkConfigRefConsitency( LatestConfig config )
  {
    List itemsToRemove = new ArrayList();
    for ( Iterator it = config.getIds().iterator(); it.hasNext(); )
    {
      String curId = (String) it.next();
      LatestConfig.Item curItem = config.getItem( curId );

      String curRefId = curItem.getRefId();
      if ( curRefId == null ) continue; // Current Item is not a refering Item.

      if ( curRefId != null )
      {
        // Current Item refers to another. So, follow the chain of refering
        // items, add invalid Items to list to be removed, and set all attributes
        // of the current Item, except ID, to the values of the referenced Item.
        LatestConfig.Item referencedItem = isEndOfRefChainValid( curItem, config, itemsToRemove );
        if ( referencedItem != null )
          curItem.setReferencedItem( referencedItem );
      }
    }

    // Remove items on invalid chain.
    for ( Iterator it = itemsToRemove.iterator(); it.hasNext(); )
    {
      String curRemoveId = (String) it.next();
      config.removeItem( curRemoveId );
    }
  }

  /**
   * Return the non-refering Item in the given LatestConfig to which the given
   * Item (after following all references) refers. Or null if the last Item on
   * the chain of references refers to a non-existent Item.
   *
   * When a chain of refering Items does not, in the end, refer to a
   * non-refering Item, add all Items in the chain to the listOfItemsToRemove.
   *
   * @param curItem the refering Item
   * @param config the LatestConfig containing the items.
   * @param listOfItemsToRemove the list of Items to remove from the LatestConfig.
   * @return the Item the given Item references.
   */
  private static LatestConfig.Item isEndOfRefChainValid( LatestConfig.Item curItem,
                                                         LatestConfig config,
                                                         List listOfItemsToRemove)
  {
    // If no refId, current Item is the end of chain and is a valid (non-refering) item.
    if ( curItem.getRefId() == null ) return curItem;

    // Find next Item in chain, i.e., the Item the current one references.
    LatestConfig.Item nextItem = config.getItem( curItem.getRefId() );

    // If there is no next Item, the current Item is end of chain and is not valid.
    if ( nextItem == null )
    {
      if ( ! listOfItemsToRemove.contains( curItem.getId() ) )
      {
        listOfItemsToRemove.add( curItem.getId() );
      }
      log.warn( "Item <id=" + curItem.getId() + "> refers to non-existent Item <id=" + curItem.getRefId() + ">; it will be removed from config." );
      return null;
    }

    // There is more to the chain, follow.
    LatestConfig.Item referencedItem = isEndOfRefChainValid( nextItem, config, listOfItemsToRemove );
    if ( referencedItem == null )
    {
      if ( ! listOfItemsToRemove.contains( curItem.getId() ) )
      {
        listOfItemsToRemove.add( curItem.getId() );
      }
      log.warn( "Item <id=" + curItem.getId() + "> on chain that refers to non-existent Item; it will be removed from config." );
      return null;
    }
    curItem.setReferencedItem( referencedItem );
    return referencedItem;
  }

  private static void addConfigItemElements( Element rootElem, LatestConfig config )
  {
    // Loop through the Items in the LatestConfig.
    for ( Iterator it = config.getIds().iterator(); it.hasNext(); )
    {
      LatestConfig.Item curItem = config.getItem( (String) it.next() );

      // Build an Element for the current LatestConfig.Item.
      Element curItemElement = new Element( itemElemName );
      curItemElement.setAttribute( itemIdAttName, curItem.getId() );
      if ( curItem.getRefId() != null )
      {
        // An Item that refers to another.
        curItemElement.setAttribute( itemRefIdAttName, curItem.getRefId());
      }
      else
      {
        // A non-refering Item.
        curItemElement.setAttribute( itemNameAttName, curItem.getName() );
        curItemElement.setAttribute( itemDirLocationAttName, curItem.getDirLocation() );
        curItemElement.setAttribute( itemDatasetNameMatchPatternAttName, curItem.getDatasetNameMatchPattern() );
        curItemElement.setAttribute( itemDatasetTimeSubstitutionPatternAttName, curItem.getDatasetTimeSubstitutionPattern() );
        curItemElement.setAttribute( itemServiceBaseURLAttName, curItem.getServiceBaseURL() );
        curItemElement.setAttribute( itemInvCatSpecVersionAttName, curItem.getInvCatSpecVersion() );
        curItemElement.setAttribute( itemDqcSpecVersionAttName, curItem.getDqcSpecVersion() );
      }
      rootElem.addContent( curItemElement );
    }
  }
}
/*
 * $Log: LatestConfigFactory.java,v $
 * Revision 1.2  2006/01/20 20:42:04  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.1  2005/09/30 21:51:37  edavis
 * Improve "Latest" DqcHandler so it can deal with new IDD naming conventions:
 * new configuration file format; add LatestDqcHandler which handles new and old
 * config file formats; use LatestDqcHandler as a proxy for LatestModel.
 *
 */