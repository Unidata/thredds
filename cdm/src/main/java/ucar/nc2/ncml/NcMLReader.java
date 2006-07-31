// $Id: NcMLReader.java 63 2006-07-12 21:50:51Z edavis $
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
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.NetworkUtils;
import ucar.unidata.util.StringUtil;

import thredds.catalog.XMLEntityResolver;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Read NcML and create NetcdfDataset.
 * Note that this is thread-safe.
 *
 * @see ucar.nc2.NetcdfFile
 * @author caron
 * @version $Revision: 63 $ $Date: 2006-07-12 15:50:51 -0600 (Wed, 12 Jul 2006) $
 */

public class NcMLReader {
  public static final Namespace ncNS = Namespace.getNamespace("nc", XMLEntityResolver.NJ22_NAMESPACE);
  private static boolean debugURL = false, debugXML = false, showParsedXML = false;
  private static boolean debugOpen = false, debugConstruct = false, debugCmd = false;
  private static boolean debugAggDetail = false;

  static public void setDebugFlags( ucar.nc2.util.DebugFlags debugFlag) {
    debugURL =  debugFlag.isSet("NcML/debugURL");
    debugXML =  debugFlag.isSet("NcML/debugXML");
    showParsedXML =  debugFlag.isSet("NcML/showParsedXML");
    debugCmd =  debugFlag.isSet("NcML/debugCmd");
    debugOpen =  debugFlag.isSet("NcML/debugOpen");
    debugConstruct =  debugFlag.isSet("NcML/debugConstruct");
    debugAggDetail =  debugFlag.isSet("NcML/debugAggDetail");
  }

  static private boolean validate = false;

  /**
   * Use NCML to modify a dataset, getting NcML as a resource stream
   *
   * @param ncDataset modify this dataset
   * @param ncmlResourceLocation resource location of NcML
   * @param cancelTask allow user to cancel task; may be null
   * @throws IOException
   */
  static public void wrapNcMLresource(NetcdfDataset ncDataset, String ncmlResourceLocation, CancelTask cancelTask) throws IOException {
    ClassLoader cl = ncDataset.getClass().getClassLoader();
    InputStream is = cl.getResourceAsStream(ncmlResourceLocation);
    if (is == null)
      throw new FileNotFoundException(ncmlResourceLocation);

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      if (debugURL) System.out.println(" NetcdfDataset URL = <"+ncmlResourceLocation+">");
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** NetcdfDataset/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
    }

    Element netcdfElem = doc.getRootElement();

    NcMLReader reader = new NcMLReader();
    reader.readNetcdf( ncDataset.getLocation(), ncDataset, ncDataset, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.wrapNcML result= \n"+ncDataset);
  }


  /**
   * Use NCML to modify the dataset, getting NcML from a URL
   *
   * @param ncDataset modify this dataset
   * @param ncmlLocation URL location of NcML
   * @param cancelTask allow user to cancel task; may be null
   * @throws IOException
   */
  static public void wrapNcML(NetcdfDataset ncDataset, String ncmlLocation, CancelTask cancelTask) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      if (debugURL) System.out.println(" NetcdfDataset URL = <"+ncmlLocation+">");
      doc = builder.build(ncmlLocation);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** NetcdfDataset/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
    }

    Element netcdfElem = doc.getRootElement();

    NcMLReader reader = new NcMLReader();
    reader.readNetcdf( ncmlLocation, ncDataset, ncDataset, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.wrapNcML result= \n"+ncDataset);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read an NcML file from a URL location, and construct a NetcdfDataset.
   * @param ncmlLocation the URL location string of the NcML document
   * @param cancelTask allow user to cancel the task; may be null
   */
  static public NetcdfDataset readNcML(String ncmlLocation, CancelTask cancelTask) throws IOException, java.net.MalformedURLException {
    return readNcML( ncmlLocation, null, cancelTask);
  }

  /**
   * Read an NcML file from a URL location, and construct a NetcdfDataset.
   * @param ncmlLocation the URL location string of the NcML document
   * @param referencedDatasetUri if null (usual case) get this from NcML, otherwise use this as the location.
   * @param cancelTask allow user to cancel the task; may be null
   */
  static public NetcdfDataset readNcML(String ncmlLocation, String referencedDatasetUri, CancelTask cancelTask) throws IOException, java.net.MalformedURLException {
    URL url =  new URL( ncmlLocation);

    if (debugURL) {
      System.out.println(" NcMLReader open "+ncmlLocation);
      System.out.println("   URL = "+url.toString());
      System.out.println("   external form = "+url.toExternalForm());
      System.out.println("   protocol = "+url.getProtocol());
      System.out.println("   host = "+url.getHost());
      System.out.println("   path = "+url.getPath());
      System.out.println("  file = "+url.getFile());
    }

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      if (debugURL) System.out.println(" NetcdfDataset URL = <"+url+">");
      doc = builder.build(url);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** NetcdfDataset/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
    }

    Element netcdfElem = doc.getRootElement();

    if (referencedDatasetUri == null) {
    // the ncml probably refers to another dataset, but doesnt have to
      referencedDatasetUri = netcdfElem.getAttributeValue("location");
      if (referencedDatasetUri == null)
        referencedDatasetUri = netcdfElem.getAttributeValue("uri");
    }

    NcMLReader reader = new NcMLReader();
    NetcdfDataset ncd = reader.readNcML( ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.readNcML result= \n"+ncd);
    return ncd;
  }

  /**
    * Read NcML doc from an InputStream, and construct a NetcdfDataset.
    * @param ins the InputStream containing the NcML document
    * @param cancelTask allow user to cancel the task; may be null
    */
   static public NetcdfDataset readNcML(InputStream ins, CancelTask cancelTask) throws IOException, java.net.MalformedURLException {

     org.jdom.Document doc;
     try {
       SAXBuilder builder = new SAXBuilder();
       doc = builder.build(ins);
     } catch (JDOMException e) {
       throw new IOException(e.getMessage());
     }
     if (debugXML) System.out.println(" SAXBuilder done");

     if (showParsedXML) {
       XMLOutputter xmlOut = new XMLOutputter();
       System.out.println ("*** NetcdfDataset/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
     }

     Element netcdfElem = doc.getRootElement();

     // the ncml probably refers to another dataset, but doesnt have to
     String referencedDatasetUri = netcdfElem.getAttributeValue("location");
     if (referencedDatasetUri == null)
       referencedDatasetUri = netcdfElem.getAttributeValue("uri");

     NcMLReader reader = new NcMLReader();
     NetcdfDataset ncd = reader.readNcML( null, referencedDatasetUri, netcdfElem, cancelTask);
     if (debugOpen) System.out.println("***NcMLReader.readNcML (stream) result= \n"+ncd);
     return ncd;
   }

  /**
   * Read an Aggregation element from a filename and return the root element.
   * used for persistence
   * @param filename the file location
   */
  static public Element readAggregation(String filename) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(filename);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** NetcdfDataset/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
    }

    return doc.getRootElement();
  }

  //////////////////////////////////////////////////////////////////////////////////////
  private boolean enhance = false;

  /**
   * common part of readNcML.
   *
   * @param ncmlLocation the URL location string of the NcML document
   * @param referencedDatasetUri refers to this dataset (may be null)
   * @param netcdfElem JDOM netcdf element
   * @param cancelTask allow user to cancel the task; may be null
   *
   * @return NetcdfDataset the constructed dataset
   * @throws IOException
   */
  private NetcdfDataset readNcML( String ncmlLocation, String referencedDatasetUri,
                            Element netcdfElem, CancelTask cancelTask) throws IOException {

    referencedDatasetUri = NetworkUtils.resolve(ncmlLocation,  referencedDatasetUri);

    // common error causing infinite regression
    if ((referencedDatasetUri != null) && referencedDatasetUri.equals(ncmlLocation))
      throw new IllegalStateException("NcML location attribute refers to the NcML document itself"+referencedDatasetUri);

    // enhance means do scale/offset and add CoordSystems
    String enhanceS = netcdfElem.getAttributeValue("enhance");
    enhance = (enhanceS != null) && enhanceS.equalsIgnoreCase("true");

    // open the referenced dataset
    NetcdfDataset refds = null;
    if (referencedDatasetUri != null) {
      refds = NetcdfDataset.openDataset( referencedDatasetUri, false, cancelTask);
    }

    Element elemE = netcdfElem.getChild("explicit", ncNS);
    boolean isExplicit = (elemE != null);

    // LOOK this may be a bad idea !!!!
    // general idea is that we just modify the referenced dataset
    // the exception is when "explicit" is specified, then we keep them seperate.
    NetcdfDataset newds;
    if (isExplicit || (refds == null)) {
      newds = new NetcdfDataset();
      if (refds == null)
        refds = newds;
      else
        newds.setReferencedFile( refds); // gotta set so it gets closed !!

    } else { // modify
      newds = refds;
    }

    readNetcdf( ncmlLocation, newds, refds, netcdfElem, cancelTask);

    if (enhance)
      newds.enhance();

    return newds;
  }

  ///////// Heres where the parsing work starts

  /**
   * parse a netcdf JDOM Element, and add contents to the newds NetcdfDataset.
   *
   * This is a bit tricky, because it handles several cases
   *  When newds == refds, we are just modifying newds.
   *  When newds != refds, we keep them seperate, and copy from refds to newds.
   *
   * The user may be defining new elements or modifying old ones. The only way to tell is by seeing
   *  if the elements already exist.
   *
   * @param ncmlLocation NcML URL location
   * @param newds add the info to this one
   * @param refds the referenced datset; may equal newds
   * @param netcdfElem JDOM netcdf element
   * @param cancelTask allow user to cancel the task; may be null
   *
   * @throws IOException
   */
  public void readNetcdf( String ncmlLocation, NetcdfDataset newds, NetcdfDataset refds, Element netcdfElem, CancelTask cancelTask) throws IOException {

    //                    refds != null               refds == null
    //  explicit            explicit                      new (ref=new)
    //  readMeatadata       modify (new=ref)              new (ref=new)
    //
    //  see if its an explicit only

    if (debugOpen) System.out.println("NcMLReader.readNetcdf ncml= "+ncmlLocation+" referencedDatasetUri= "+refds.getLocation());
    if (ncmlLocation != null) newds.setLocation( ncmlLocation);
    newds.setId( netcdfElem.getAttributeValue("id"));
    newds.setTitle( netcdfElem.getAttributeValue("title"));

    String addRecords = netcdfElem.getAttributeValue("addRecords");
    if ((addRecords != null) && addRecords.equalsIgnoreCase("true"))
      newds.addRecordStructure();

    // aggregation first
    Element aggElem = netcdfElem.getChild("aggregation", ncNS);
    if (aggElem != null) {
      Aggregation agg = readAgg(aggElem, ncmlLocation, newds, cancelTask);
      newds.setAggregation( agg);
      agg.finish( cancelTask);
    }

    // the root group
   readGroup( newds, refds, null, null, netcdfElem);

    // enhance means do scale/offset and add CoordSystems
    String enhanceS = netcdfElem.getAttributeValue("enhance");
    if ((enhanceS != null) && enhanceS.equalsIgnoreCase("true"))
      newds.enhance();  // LOOK not sure

    newds.finish();
  }

  ////////////////////////////////////////////////////////////////////////

  /**
   * Read an NcML attribute element.
   * @param parent Group or Variable
   * @param refParent Group or Variable in reference dataset
   * @param attElem ncml attribute element
   */
  protected void readAtt( Object parent, Object refParent, Element attElem) {
    String name = attElem.getAttributeValue("name");
    String nameInFile = attElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    ucar.nc2.Attribute att = findAttribute( refParent, nameInFile);
    if (att == null) { // new
      if (debugConstruct) System.out.println(" add new att = "+name);
      ucar.ma2.Array values = readAttributeValues( attElem);
      if (values == null) return; // LOOK need error reporting
      addAttribute( parent, new ucar.nc2.Attribute( name, values));

    } else {
      att.setName( name);
      ucar.ma2.Array values = readAttributeValues( attElem);
      if (values != null)
        att.setValues( values);

      if (parent != refParent) // explicit
        addAttribute( parent, att);

      if (debugConstruct) System.out.println(" modify existing att = "+name);
    }
  }

  protected ucar.ma2.Array readAttributeValues(Element s) {
    String valString = s.getAttributeValue("value");
    if (valString == null) return null;
    valString = StringUtil.unquoteXmlAttribute(valString);

    String type = s.getAttributeValue("type");
    DataType dtype = (type == null) ? DataType.STRING : DataType.getType( type);
    if (dtype == DataType.CHAR) dtype = DataType.STRING;

    String sep = s.getAttributeValue("separator");
    if ((sep == null) && (dtype == DataType.STRING)) {
      ArrayList list = new ArrayList();
      list.add(valString);
      return NetcdfDataset.makeArray( dtype, list);
    }

    if (sep == null) sep = " "; // default whitespace separated

    ArrayList stringValues = new ArrayList();
    StringTokenizer tokn = new StringTokenizer(valString, sep);
    while (tokn.hasMoreTokens())
      stringValues.add( tokn.nextToken());

    return NetcdfDataset.makeArray( dtype, stringValues);
  }

  protected ucar.nc2.Attribute findAttribute( Object parent, String name) {
    if (parent == null)
      return null;
    if (parent instanceof Group)
      return ((Group) parent).findAttribute(name);
    else if (parent instanceof Variable)
      return ((Variable) parent).findAttribute(name);
    return null;
  }

  protected void addAttribute( Object parent, ucar.nc2.Attribute att) {
    if (parent instanceof Group)
      ((Group) parent).addAttribute(att);
    else if (parent instanceof Variable)
      ((Variable) parent).addAttribute(att);
  }

  /**
   * Read an NcML dimension element.
   * @param g put dimension into this group
   * @param refg parent Group in referenced dataset
   * @param dimElem ncml dimension element
   */
  protected void readDim( Group g, Group refg, Element dimElem) {
    String name = dimElem.getAttributeValue("name");
    String nameInFile = dimElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

        // see if it already exists
    Dimension dim = refg.findDimension( nameInFile);
    if (dim == null) { // nope - create it
      String lengthS = dimElem.getAttributeValue("length");
      String isUnlimitedS = dimElem.getAttributeValue("isUnlimited");
      String isSharedS = dimElem.getAttributeValue("isShared");
      String isUnknownS = dimElem.getAttributeValue("isVariableLength");

      boolean isUnlimited = (isUnlimitedS != null) && isUnlimitedS.equalsIgnoreCase("true");
      boolean isUnknown = (isUnknownS != null) && isUnknownS.equalsIgnoreCase("true");
      boolean isShared = true;
      if ((isSharedS != null) && isSharedS.equalsIgnoreCase("false"))
        isShared = false;

      int len = Integer.parseInt( lengthS);
      if ((isUnknownS != null) && isUnknownS.equalsIgnoreCase("false"))
        len = Dimension.UNKNOWN.getLength();

      if (debugConstruct) System.out.println(" add new dim = "+name);
      g.addDimension( new Dimension( name, len, isShared, isUnlimited, isUnknown));

    } else { // yes - modify it
      dim.setName( name);

      String lengthS = dimElem.getAttributeValue("length");
      String isUnlimitedS = dimElem.getAttributeValue("isUnlimited");
      String isSharedS = dimElem.getAttributeValue("isShared");
      String isUnknownS = dimElem.getAttributeValue("isVariableLength");

      if (isUnlimitedS != null)
        dim.setUnlimited( isUnlimitedS.equalsIgnoreCase("true"));

      if (isSharedS != null)
        dim.setShared( !isSharedS.equalsIgnoreCase("false"));

      if (isUnknownS != null)
        dim.setVariableLength( isUnknownS.equalsIgnoreCase("true"));

      if ((lengthS != null) && !dim.isVariableLength()) {
        int len = Integer.parseInt( lengthS);
        dim.setLength( len);
      }

      if (debugConstruct) System.out.println(" modify existing dim = "+name);

      if (g != refg) // explicit, copy to new
        g.addDimension( dim);
    }
  }

  /**
   * Read the NcML group element, and nested elements.
   * @param newds new dataset
   * @param refds referenced dataset
   * @param parent Group
   * @param refParent parent Group in referenced dataset
   * @param groupElem ncml group element
   */
  protected void readGroup( NetcdfDataset newds, NetcdfDataset refds, Group parent, Group refParent, Element groupElem)  {
    String name = groupElem.getAttributeValue("name");
    String nameInFile = groupElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    Group g, refg;
    if (parent == null) {
      g = newds.getRootGroup();
      refg = refds.getRootGroup();
      if (debugConstruct) System.out.println(" root group ");

    } else {
      // see if it exists in referenced dataset
      refg = refParent.findGroup( nameInFile);
      if (refg == null) { // new
        g =  new Group( newds, parent, name);
        parent.addGroup( g);
        if (debugConstruct) System.out.println(" add new group = "+name);

      } else {

        if (parent != refParent) { // explicit
          g =  new Group( newds, parent, name);
          parent.addGroup( g);
          if (debugConstruct) System.out.println(" transfer existing group = "+name);

        } else { // modify
          g = refg;
          if (!nameInFile.equals(name))
            g.setName( name); // LOOK this renames all its variables !!

          if (debugConstruct) System.out.println(" modify existing group = "+name);
        }
      }
    }

        // look for attributes
    java.util.List attList = groupElem.getChildren("attribute", ncNS);
    for (int j=0; j< attList.size(); j++) {
      readAtt( g, refg, (Element) attList.get(j));
    }

    // look for dimensions
    java.util.List dimList = groupElem.getChildren("dimension", ncNS);
    for (int j=0; j< dimList.size(); j++) {
      readDim(g, refg, (Element) dimList.get(j));
     }

    // look for variables
    java.util.List varList = groupElem.getChildren("variable", ncNS);
    for (int j=0; j< varList.size(); j++) {
      readVariable(newds, g, refg, (Element) varList.get(j));
    }

    // process remove command
    java.util.List removeList = groupElem.getChildren("remove", ncNS);
    for (int j=0; j< removeList.size(); j++) {
      Element e = (Element) removeList.get(j);
      cmdRemove(g, e.getAttributeValue("type"), e.getAttributeValue("name"));
    }

    // look for nested groups
    java.util.List groupList = groupElem.getChildren("group", ncNS);
    for (int j=0; j< groupList.size(); j++) {
      readGroup(newds, refds, g, refg, (Element) groupList.get(j));
      if (debugConstruct) System.out.println(" add group = "+g.getName());
    }
  }

  /* private boolean debugView = false, debugConvert = false;
  protected VariableDS readVariable2( NetcdfDataset ds, Element varElem) {
    VariableDS v = readVariable( ds, varElem);

    // look for logical views
    java.util.List viewList = varElem.getChildren("logicalView", ncNS);
    for (int j=0; j< viewList.size(); j++) {
      Element viewElem = (Element) viewList.get(j);
      String value = viewElem.getAttributeValue("section");
      if (value != null) {
        v.setLogicalView("section", value);
        if (debugView) System.out.println("set view = "+value);
      }
    }

    // look for unit conversion
    Element unitElem = varElem.getChild("units", ncNS);
    if (unitElem != null) {
      String value = unitElem.getAttributeValue("convertTo");
      if (value != null) {
        v.setConvertUnit(value);
        if (debugConvert) System.out.println("setConvertUnit on "+v.getName()+" to <" + value+">");
      }
    }

    return v;
     } */

  /**
   * Read the NcML variable element, and nested elements.
   * @param g parent Group
   * @param refg referenced dataset parent Group - may be same (modify) or different (explicit)
   * @param varElem ncml variable element
   */
  protected void readVariable( NetcdfDataset ds, Group g, Group refg, Element varElem) {
    String name = varElem.getAttributeValue("name");
    String nameInFile = varElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    Variable refv = refg.findVariable( nameInFile);
    if (refv == null) { // new
      if (debugConstruct) System.out.println(" add new var = "+name);
      g.addVariable( readVariableNew(ds, g, null, varElem));
      return;
    }

    // exists already
    DataType dtype;
    String typeS = varElem.getAttributeValue("type");
    if (typeS != null)
      dtype = DataType.getType(typeS);
    else
      dtype = refv.getDataType();

    String shape = varElem.getAttributeValue("shape");
    if (shape == null) shape = refv.getDimensionsString();

    Variable v;
    if (refg == g) { // modify
      v = refv;
      v.setName( name);
      v.setDataType( dtype);
      v.setDimensions( shape);
      if (debugConstruct) System.out.println(" modify existing var = "+nameInFile);

    } else { //explicit
      if (refv instanceof Structure) {
        v = new StructureDS(ds, g, null, name, shape, null, null);
      } else {
        v = new VariableDS(ds, g, null, name, dtype, shape, null, null);
      }
      v.setIOVar( refv);
      if (debugConstruct) System.out.println(" modify explicit var = "+nameInFile);
      g.addVariable( v);
    }

    java.util.List attList = varElem.getChildren("attribute", ncNS);
    for (int j=0; j< attList.size(); j++) {
      readAtt(v, refv, (Element) attList.get(j));
    }

     // process remove command
    java.util.List removeList = varElem.getChildren("remove", ncNS);
    for (int j=0; j< removeList.size(); j++) {
      Element e = (Element) removeList.get(j);
      cmdRemove(v, e.getAttributeValue("type"), e.getAttributeValue("name"));
    }

    if (v.getDataType() == DataType.STRUCTURE) {
      // deal with nested variables
      StructureDS s = (StructureDS) v;
      StructureDS refS = (StructureDS) refv;
      java.util.List varList = varElem.getChildren("variable", ncNS);
      for (int j=0; j< varList.size(); j++) {
        readVariableNested(ds, s, refS, (Element) varList.get(j));
      }

    } else {

      // deal with values
      Element valueElem = varElem.getChild("values", ncNS);
      if (valueElem != null) {
        readValues( ds, v, varElem, valueElem);

      } else {
        // see if  we need to munge existing data. use case : aggregation
        if (v.hasCachedData()) {
          Array data;
          try {
            data = v.read();
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
          }
          if (data.getClass() != v.getDataType().getPrimitiveClassType()) {
            Array newData = Array.factory(v.getDataType(), v.getShape());
            MAMath.copy( newData, data);
            v.setCachedData( newData, false);
          }
        }
      }

    }

     // now that we have attributes finalized, redo the enhance
    if (enhance && (v instanceof VariableDS))
      ((VariableDS) v).enhance();

  }

  /**
   * Read a NcML variable element, and nested elements, when it creates a new Variable.
   * @param g parent Group
   * @param parentS parent Structure
   * @param varElem ncml variable element
   * @return return new Variable
   */
  protected Variable readVariableNew( NetcdfDataset ds, Group g, Structure parentS, Element varElem) {
    String name = varElem.getAttributeValue("name");
    String type = varElem.getAttributeValue("type");
    String shape = varElem.getAttributeValue("shape");

    DataType dtype = DataType.getType(type);

    Variable v;

    if (dtype == DataType.STRUCTURE) {
      StructureDS s = new StructureDS( ds, g, parentS, name, shape, null, null);
      v = s;
       // look for nested variables
      java.util.List varList = varElem.getChildren("variable", ncNS);
      for (int j=0; j< varList.size(); j++) {
        readVariableNested(ds, s, s, (Element) varList.get(j));
      }

    } else {
      v = new VariableDS( ds, g, parentS, name, dtype, shape, null, null);
      Element valueElem = varElem.getChild("values", ncNS);
          // deal with values
      if (valueElem != null)
        readValues( ds, v, varElem, valueElem);
    }

    // look for attributes
    java.util.List attList = varElem.getChildren("attribute", ncNS);
    for (int j=0; j< attList.size(); j++) {
      readAtt(v, null, (Element) attList.get(j));
    }

     // now that we have attributes finalized, redo the enhance
    if (enhance && (v instanceof VariableDS))
      ((VariableDS) v).enhance();

    return v;
  }

 /**
  * Read the NcML variable element, and nested elements.
  * @param parentS parent Structure
  * @param varElem ncml variable element
  */
  protected void readVariableNested( NetcdfDataset ds, Structure parentS, Structure refStruct, Element varElem) {
   String name = varElem.getAttributeValue("name");
   String nameInFile = varElem.getAttributeValue("orgName");
   if (nameInFile == null) nameInFile = name;

   // see if it already exists
   Variable refv = refStruct.findVariable( nameInFile);
   if (refv == null) { // new
     if (debugConstruct) System.out.println(" add new var = "+name);
     Variable nested = readVariableNew(ds, parentS.getParentGroup(), parentS, varElem);
     parentS.addMemberVariable( nested);
     return;
   }

   Variable v;
   if (parentS == refStruct) { // modify
     v = refv;
     v.setName( name);

   } else { //explicit
     if (refv instanceof Structure) {
       v = new StructureDS(ds, parentS.getParentGroup(), parentS, name, refv.getDimensionsString(), null, null);
     } else {
       v = new VariableDS(ds, parentS.getParentGroup(), parentS, name, refv.getDataType(), refv.getDimensionsString(), null, null);
     }
     v.setIOVar( refv);
     parentS.addMemberVariable( v);
   }

   if (debugConstruct) System.out.println(" modify existing var = "+nameInFile);

   String typeS = varElem.getAttributeValue("type");
   if (typeS != null) {
     DataType dtype = DataType.getType(typeS);
     v.setDataType( dtype);
   }

   String shape = varElem.getAttributeValue("shape");
   if (shape != null) {
     v.setDimensions( shape);
   }

   java.util.List attList = varElem.getChildren("attribute", ncNS);
   for (int j=0; j< attList.size(); j++) {
     readAtt(v, refv, (Element) attList.get(j));
   }

    // process remove command
   java.util.List removeList = varElem.getChildren("remove", ncNS);
   for (int j=0; j< removeList.size(); j++) {
     Element e = (Element) removeList.get(j);
     cmdRemove(v, e.getAttributeValue("type"), e.getAttributeValue("name"));
   }

   if (v.getDataType() == DataType.STRUCTURE) {
     // deal with nested variables
     StructureDS s = (StructureDS) v;
     StructureDS refS = (StructureDS) refv;
     java.util.List varList = varElem.getChildren("variable", ncNS);
     for (int j=0; j< varList.size(); j++) {
       readVariableNested(ds, s, refS, (Element) varList.get(j));
     }

   } else {

         // deal with values
     Element valueElem = varElem.getChild("values", ncNS);
     if (valueElem != null)
       readValues( ds, v, varElem, valueElem);
    }

    // now that we have attributes finalized, redo the enhance
    if (enhance && (v instanceof VariableDS))
      ((VariableDS) v).enhance();
  }

  protected void readValues( NetcdfDataset ds, Variable v, Element varElem, Element valuesElem) {
    ArrayList valList = new ArrayList();

    // should really be a seperate element
    String startS = valuesElem.getAttributeValue("start");
    String incrS = valuesElem.getAttributeValue("increment");
    String nptsS = valuesElem.getAttributeValue("npts");
    int npts = (nptsS == null) ? (int) v.getSize() : Integer.parseInt(nptsS);

    // either start, increment are specified
    if ((startS != null) && (incrS != null)) {
      double start = Double.parseDouble( startS);
      double incr = Double.parseDouble( incrS);
      ds.setValues( v, npts, start, incr);
      return;
    }

    String values = varElem.getChildText("values", ncNS);
    String sep = valuesElem.getAttributeValue("separator");
    if (sep == null) sep = " ";

    if (v.getDataType() == DataType.CHAR) {
      int nhave = values.length();
      int nwant = (int) v.getSize();
      char[] data = new char[nwant];
      int min = Math.min(nhave, nwant);
      for (int i=0; i<min; i++) {
         data[i] = values.charAt(i);
      }
      Array dataArray = Array.factory( DataType.CHAR.getPrimitiveClassType(), v.getShape(), data);
      v.setCachedData( dataArray, true);

    } else {
      // or a list of values
      StringTokenizer tokn = new StringTokenizer(values, sep);
      while (tokn.hasMoreTokens())
        valList.add(tokn.nextToken());
      ds.setValues( v, valList);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private Aggregation readAgg( Element aggElem, String ncmlLocation, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    String dimName = aggElem.getAttributeValue("dimName");
    String type = aggElem.getAttributeValue("type");
    String recheck = aggElem.getAttributeValue("recheckEvery");
    String timeUnitsChange = aggElem.getAttributeValue("timeUnitsChange");
    String fmrcDefinition = aggElem.getAttributeValue("fmrcDefinition");

    String forecastDate = aggElem.getAttributeValue("forecastDate");
    String forecastDateVariable = aggElem.getAttributeValue("forecastDateVariable");
    String forecastOffset = aggElem.getAttributeValue("forecastOffset");
    String forecastTimeOffset = aggElem.getAttributeValue("forecastTimeOffset");
    String referenceDateVariable = aggElem.getAttributeValue("referenceDateVariable");

    Aggregation agg;
    if (type.equals("forecastModelRunCollection")) {
      AggregationFmrCollection aggc = new AggregationFmrCollection(newds, dimName, type, recheck);
      agg = aggc;

      if (timeUnitsChange != null)
        aggc.setTimeUnitsChange(timeUnitsChange.equalsIgnoreCase("true"));

      if (fmrcDefinition != null)
        aggc.setInventoryDefinition(fmrcDefinition);

    } else if (type.equals("forecastModelRun")) {
      AggregationFmr aggf = new AggregationFmr(newds, dimName, type, recheck);
      agg = aggf;

      if (forecastDate != null)
        aggf.setForecastDate(forecastDate, forecastDateVariable);
      else if (forecastOffset != null)
        aggf.setForecastOffset(forecastOffset);
      else if (forecastTimeOffset != null)
        aggf.setForecastTimeOffset(forecastTimeOffset, forecastDateVariable, referenceDateVariable);

    } else {
      agg = new Aggregation(newds, dimName, type, recheck);
    }

        // look for variable names
    java.util.List list = aggElem.getChildren("variableAgg", ncNS);
    for (int j=0; j< list.size(); j++) {
      Element e = (Element) list.get(j);
      String varName = e.getAttributeValue("name");
      agg.addVariable( varName);
    }

     // nested netcdf elements
    java.util.List ncList = aggElem.getChildren("netcdf", ncNS);
    for (int j=0; j< ncList.size(); j++) {
      Element netcdfElemNested = (Element) ncList.get(j);
      String location = netcdfElemNested.getAttributeValue("location");
      if (location == null)
        location = netcdfElemNested.getAttributeValue("uri");

      if (agg.getType() == Aggregation.Type.UNION) {
        NetcdfDataset unionDs = readNcML( ncmlLocation, location, netcdfElemNested, cancelTask);
        agg.addDatasetUnion( unionDs);
        transferDataset(unionDs, newds, null);
      } else {
        String ncoords = netcdfElemNested.getAttributeValue("ncoords");
        String coordValueS = netcdfElemNested.getAttributeValue("coordValue");
        NcmlElementReader reader = new NcmlElementReader(ncmlLocation, location, netcdfElemNested);
        String cacheName = ncmlLocation + "#" + Integer.toString( netcdfElemNested.hashCode());
        agg.addDataset( cacheName, location, ncoords, coordValueS, reader, cancelTask);
      }
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      if (debugAggDetail) System.out.println(" debugAgg: nested dataset = "+location);
    }

    // nested scan elements
   java.util.List dirList = aggElem.getChildren("scan", ncNS);
   for (int j=0; j< dirList.size(); j++) {
     Element scanElem = (Element) dirList.get(j);
     String dirLocation = scanElem.getAttributeValue("location");
     String suffix = scanElem.getAttributeValue("suffix");
     String dateFormatMark = scanElem.getAttributeValue("dateFormatMark");
     String enhance = scanElem.getAttributeValue("enhance");
     String subdirs = scanElem.getAttributeValue("subdirs");

     agg.addDirectoryScan( dirLocation, suffix, dateFormatMark, enhance, subdirs);

     if ((cancelTask != null) && cancelTask.isCancel())
       return null;
     if (debugAggDetail) System.out.println(" debugAgg: nested dirLocation = "+dirLocation);
   }

    return agg;
  }

  private class NcmlElementReader implements NetcdfFileFactory {
    private Element netcdfElem;
    private String ncmlLocation, location;

    NcmlElementReader(String ncmlLocation, String location, Element netcdfElem) {
      this.ncmlLocation = ncmlLocation;
      this.location = location;
      this.netcdfElem = netcdfElem;
    }

    public NetcdfFile open(String cacheName, int buffer_size, CancelTask cancelTask, Object spiObject) throws IOException {
      if (debugAggDetail) System.out.println(" NcmlElementReader open nested dataset " + cacheName);
      return readNcML(ncmlLocation, location, netcdfElem, cancelTask);
    }
  }

  /* private class NetcdfDatasetReader implements NetcdfDatasetFactory {
    public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
      System.out.println(" NetcdfDatasetReader open nested dataset " + location);
      return NetcdfDataset.openDataset( location, false, cancelTask);
    }
  } */


 /* private void aggExistingDimension( NetcdfDataset newds, Aggregation agg, CancelTask cancelTask) throws IOException {
    // create aggregation dimension
    Dimension aggDim = new Dimension( agg.getDimensionName(), agg.getTotalCoords(), true);
    newds.removeDimension( null, agg.getDimensionName()); // remove previous declaration, if any
    newds.addDimension( null, aggDim);
    if (debugAgg) System.out.println(" add aggExisting dimension = "+aggDim);

    // open a "typical"  nested dataset and copy it to newds
    NetcdfFile typical = agg.getTypicalDataset();
    transferDataset(typical, newds);

    // now we can create the real aggExisting variables
    // all variables with the named aggregation dimension
    String dimName = agg.getDimensionName();
    List vars = typical.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (v.getRank() < 1)
        continue;
      Dimension d = v.getDimension(0);
      if (!dimName.equals(d.getName()))
        continue;

      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(),
          v.getDimensionsString(), null, null);
      vagg.setAggregation( agg);
      transferVariableAttributes( v, vagg);

      newds.removeVariable( null, v.getShortName());
      newds.addVariable( null, vagg);
    }
  }

  private void aggNewDimension( NetcdfDataset newds, Aggregation agg, CancelTask cancelTask) throws IOException {
    // create aggregation dimension
    String dimName = agg.getDimensionName();
    Dimension aggDim = new Dimension( dimName, agg.getTotalCoords(), true);
    newds.removeDimension( null, dimName); // remove previous declaration, if any
    newds.addDimension( null, aggDim);
    if (debugAgg) System.out.println(" add aggNew dimension = "+aggDim);

    // create aggregation coordinate variable
    DataType coordType = null;
    Variable coordVar = newds.getRootGroup().findVariable(dimName);
    if (coordVar == null) {
      coordType = agg.getCoordinateType();
      coordVar = new VariableDS(newds, null, null, dimName, coordType, dimName, null, null);
      newds.addVariable(null, coordVar);
    } else {
      coordType = coordVar.getDataType();
      coordVar.setDimensions(dimName); // reset its dimension
    }

    if (agg.isDate()) {
      coordVar.addAttribute( new ucar.nc2.Attribute(_Coordinate.AxisType", "Time"));
    }

    // if not already set, set its values
    if (!coordVar.hasCachedData()) {
      int[] shape = new int[] { agg.getTotalCoords() };
      Array coordData = Array.factory(coordType.getPrimitiveClassType(), shape);
      Index ima = coordData.getIndex();
      List nestedDataset = agg.getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        if (coordType == DataType.STRING)
          coordData.setObject(ima.set(i), nested.getCoordValueString());
        else
          coordData.setDouble(ima.set(i), nested.getCoordValue());
      }
      coordVar.setCachedData( coordData, true);
    } else {
      Array data = coordVar.read();
      IndexIterator ii = data.getIndexIterator();
      List nestedDataset = agg.getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        nested.setCoordValue( ii.getDoubleNext());
      }
    }

    // open a "typical"  nested dataset and copy it to newds
    NetcdfFile typical = agg.getTypicalDataset();
    transferDataset(typical, newds);

    // now we can create all the aggNew variables
    // use only named variables
    List vars = agg.getVariables();
    for (int i=0; i<vars.size(); i++) {
      String varname = (String) vars.get(i);
      Variable v = newds.getRootGroup().findVariable(varname);
      if (v == null) {
        System.out.println("aggNewDimension cant find variable "+varname);
        continue;
      }

      // construct new variable, replace old one
      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(),
          dimName +" "+ v.getDimensionsString(), null, null);
      vagg.setAggregation( agg);
      transferVariableAttributes( v, vagg);

      newds.removeVariable( null, v.getShortName());
      newds.addVariable( null, vagg);
    }
  } */


  /* protected Variable readCoordVariable( NetcdfDataset ds, Element varElem) {
    String name = varElem.getAttributeValue("name");
    String type = varElem.getAttributeValue("type");
    String shape;
    if (type.equals("string"))
      shape = name+" "+name+"_len";
    else
      shape = name;

    VariableDS v = new VariableDS( ds, name, type, shape, null);

    // look for attributes
    java.util.List attList = varElem.getChildren("attribute", ncNS);
    for (int j=0; j< attList.size(); j++) {
      Attribute a = readAtt((Element) attList.get(j));
      v.addAttribute( a);
    }

    // look for values
    String values = varElem.getAttributeValue("value");
    if (values == null)
      values = varElem.getChildText("values", ncNS);
    if (values != null) {
      ArrayList valList = new ArrayList();
      String sep = varElem.getAttributeValue("separator");
      if (sep == null) sep = " ";
      StringTokenizer tokn = new StringTokenizer(values, sep);
      while (tokn.hasMoreTokens())
        valList.add(tokn.nextToken());
      v.setValues( valList);
    }

    return v;
  } */

  /* protected void readMetadata( NetcdfDataset ds, Element readElem, CancelTask cancelTask) throws IOException {
    //String name = readElem.getAttributeValue("name"); // not implemented yet
    //String type = readElem.getAttributeValue("type"); // not implemented yet

    // read dataset
    NetcdfDataset referencedDataset = ds.openReferencedDataset(cancelTask);
    transferDataset( referencedDataset, ds);
    if (debugCmd) System.out.println("CMD readMetadata "+referencedDataset.getLocation());

    /* remove elements just read
    java.util.List removeList = readElem.getChildren("remove", ncNS);
    for (int j=0; j< removeList.size(); j++) {
      Element e = (Element) removeList.get(j);
      cmdRemove(ds, e.getAttributeValue("type"), e.getAttributeValue("name"));
    }

    // look for renames
    java.util.List renameList = readElem.getChildren("rename", ncNS);
    for (int j=0; j< renameList.size(); j++) {
      Element e = (Element) renameList.get(j);
      cmdRename(ds, e.getAttributeValue("type"), e.getAttributeValue("nameInFile"),
        e.getAttributeValue("rename"));
    }

    java.util.List varCmdList = readElem.getChildren("readVariable", ncNS);
    for (int j=0; j< varCmdList.size(); j++) {
      Element e = (Element) varCmdList.get(j);
      readReadVariable( ds, e);
    }

  } */

  /* protected void readReadVariable( NetcdfDataset ds, Element readVarElem) {
      String varName = readVarElem.getAttributeValue("name");

      String rename = readVarElem.getAttributeValue("rename");
      if (rename != null) {
        cmdRename( ds, "variable", varName, rename);
        varName = rename;
      }

      VariableDS v = (VariableDS) ds.findVariable(varName);
      if (v == null) {
        System.out.println("NetcdfDataset (readReadVariable) cant find variable= "+varName);
        return;
      }

      java.util.List removeList = readVarElem.getChildren("removeAttribute", ncNS);
      for (int j=0; j< removeList.size(); j++) {
        Element e = (Element) removeList.get(j);
        cmdRemoveVarAtt(ds, v, e.getAttributeValue("name"));
      }

      java.util.List renameList = readVarElem.getChildren("renameAttribute", ncNS);
      for (int j=0; j< renameList.size(); j++) {
        Element e = (Element) renameList.get(j);
        cmdRenameVarAtt(ds, v, e.getAttributeValue("nameInFile"), e.getAttributeValue("rename"));
      }

      java.util.List addList = readVarElem.getChildren("attribute", ncNS);
      for (int j=0; j< addList.size(); j++) {
        Element e = (Element) addList.get(j);
        cmdAddVarAtt(ds, v, e);
      }
    } */

  /////////////////////////////////////////////
  // command procesing

  /**
   * Copy contents of "from" to "to", as long as "to" doesnt already have
   * an elements of that name.
   *
  protected void addElements(NetcdfFile from, NetcdfDataset ds) {

    // transfer everything from existing file into this dataset
    // that doesnt already exist
    Iterator iterDim = from.getDimensions().iterator();
    while (iterDim.hasNext()) {
      Dimension d = (Dimension) iterDim.next();
      if (!ds.dimensions().contains(d)) {
        ds.dimensions().add( d);
      }
    }

    Iterator iterVar = from.getVariables().iterator();
    while (iterVar.hasNext()) {
      Variable v = (Variable) iterVar.next();
      if (!ds.variables().contains(v)) {
        ds.variables().add( v);
        if (debugAgg) System.out.println(" copy var "+v.getName());
      }
    }

    Iterator iterAtt = from.getGlobalAttributes().iterator();
    while (iterAtt.hasNext()) {
      ucar.nc2.Attribute a = (ucar.nc2.Attribute) iterAtt.next();
      if (!ds.globalAttributes().contains(a))
        ds.globalAttributes().add( a);
    }
  }

  /**
   * Copy contents of "src" to "target".
   * Dimensions and Variables are replaced with equivalent elements.
   * Attribute doesnt have to be replaced because its immutable, so its copied over.
   * @param src
   * @param target
   * @param replaceCheck if null, only add if a Variable of the same name doesnt already exist, otherwise
   */
  static void transferDataset(NetcdfFile src, NetcdfDataset target, ReplaceVariableCheck replaceCheck) {
    transferGroup( src, src.getRootGroup(), target.getRootGroup(), replaceCheck);
  }

  static private void transferGroup( NetcdfFile ds, Group src, Group target, ReplaceVariableCheck replaceCheck) {

    // group attributes
    Iterator iterAtt = src.getAttributes().iterator();
    while (iterAtt.hasNext()) {
      ucar.nc2.Attribute a = (ucar.nc2.Attribute) iterAtt.next();
      if (null == target.findAttribute( a.getName()))
        target.addAttribute(a);
    }

    // dimensions
    Iterator iterDim = src.getDimensions().iterator();
    while (iterDim.hasNext()) {
      Dimension d = (Dimension) iterDim.next();
      if (null == target.findDimensionLocal(d.getName())) {
        Dimension newd = new Dimension(d.getName(), d.getLength(), d.isShared(), d.isUnlimited(), d.isVariableLength());
        target.addDimension( newd);
      }
    }

    // variables
    Iterator iterVar = src.getVariables().iterator();
    while (iterVar.hasNext()) {
      Variable v = (Variable) iterVar.next();
      Variable targetV = target.findVariable(v.getShortName());
      boolean replace = (replaceCheck == null) ? false : replaceCheck.replace(v);

      if (replace || (null == targetV))  {
        if (!(v instanceof VariableDS)) v = new VariableDS(target, v, false); // LOOK will fail on Structure

        if (null != targetV) target.remove( targetV);
        target.addVariable( v); // reparent
        v.setDimensions( v.getDimensionsString()); // rediscover dimensions

      } else if (!targetV.hasCachedData() && (targetV.getIOVar() == null)) {
        // this is the case where we defined the variable, but didnt set its data. we now set it with the first nested
        // dataset that has a variable with the same name
        targetV.setIOVar(v);
      }
    }

    // nested groups
    for (Iterator iter = src.getGroups().iterator(); iter.hasNext(); ) {
      Group srcNested = (Group) iter.next();
      Group nested = new Group( ds, target, srcNested.getName());
      target.addGroup( nested);
      transferGroup( ds, srcNested, nested, replaceCheck);
    }
  }

  static void transferVariableAttributes( Variable src, Variable target) {
    Iterator iterAtt = src.getAttributes().iterator();
    while (iterAtt.hasNext()) {
      ucar.nc2.Attribute a = (ucar.nc2.Attribute) iterAtt.next();
      if (null == target.findAttribute( a.getName()))
        target.addAttribute(a);
    }
  }

  static public void transferGroupAttributes( Group src, Group target) {
    Iterator iterAtt = src.getAttributes().iterator();
    while (iterAtt.hasNext()) {
      ucar.nc2.Attribute a = (ucar.nc2.Attribute) iterAtt.next();
      if (null == target.findAttribute( a.getName()))
        target.addAttribute(a);
    }
  }

  private void cmdRemove(Group g, String type, String name) {
    if (type.equals("dimension"))  {
      Dimension dim = g.findDimension(name);
      if (dim != null) {
        g.remove( dim);
        if (debugCmd) System.out.println("CMD remove "+type+" "+name);
      } else
        System.out.println("CMD remove "+type+" CANT find "+name);

    } else if (type.equals("variable"))  {
      Variable v = g.findVariable(name);
      if (v != null) {
        g.remove( v);
        if (debugCmd) System.out.println("CMD remove "+type+" "+name);
      } else
        System.out.println("CMD remove "+type+" CANT find "+name);

    } else if (type.equals("attribute"))  {
      ucar.nc2.Attribute a = g.findAttribute(name);
      if (a != null) {
        g.remove( a);
        if (debugCmd) System.out.println("CMD remove "+type+" "+name);
      } else
        System.out.println("CMD remove "+type+" CANT find "+name);
    }
  }

  private void cmdRemove(Variable v, String type, String name) {
   if (type.equals("attribute"))  {
      ucar.nc2.Attribute a = v.findAttribute(name);
      if (a != null) {
        v.remove( a);
        if (debugCmd) System.out.println("CMD remove "+type+" "+name);
      } else
        System.out.println("CMD remove "+type+" CANT find "+name);

    } else if (type.equals("variable") && v instanceof Structure)  {
      Structure s = (Structure) v;
      Variable nested = s.findVariable(name);
      if (nested != null) {
        s.removeMemberVariable( nested);
        if (debugCmd) System.out.println("CMD remove "+type+" "+name);
      } else
        System.out.println("CMD remove "+type+" CANT find "+name);
      
    }
  }

  /* protected void cmdRename(NetcdfDataset ds, String type, String name, String rename) {
    if (type.equals("variable")) {
      VariableDS v = (VariableDS) ds.findVariable(name);
      if (v == null) {
        System.out.println("NetcdfDataset cmdRename cant find variable= "+name);
        return;
      }
      v.rename( rename);
      v.setAlias( name);
      if (debugCmd) System.out.println("CMD rename var "+name+" to "+rename);

    } else if (type.equals("attribute")) {
      ucar.nc2.Attribute oldAtt = ds.findGlobalAttribute( name);
      if (null == oldAtt) {
        System.out.println("NetcdfDataset cmdRename cant find global attribute "+name);
        return;
      }

      oldAtt.rename( rename);
      if (debugCmd) System.out.println("CMD rename att "+name+" renamed to "+rename);

    } else if (type.equals("dimension")) {
      ucar.nc2.Dimension d = ds.findDimension( name);
      if (null == d) {
        System.out.println("NetcdfDataset cmdRename cant find dimension "+name);
        return;
      }
      d.rename( rename);
      if (debugCmd) System.out.println("CMD rename dim "+name+" renamed to "+rename);
    }
  }

  protected void cmdAddVarAtt(NetcdfDataset ds, VariableDS v, Element e) {
    ucar.nc2.Attribute newAtt = readAtt(e);
    ucar.nc2.Attribute oldAtt = v.findAttribute( newAtt.getName());
    if (null != oldAtt)
      v.removeAttribute( oldAtt);
    ds.addVariableAttribute( v, newAtt);
    if (debugCmd) System.out.println("CMD variable added att "+newAtt);
  }

  protected void cmdRemoveVarAtt(NetcdfDataset ds, VariableDS v, String attName) {
    ucar.nc2.Attribute oldAtt = v.findAttribute( attName);
    if (null != oldAtt) {
      v.removeAttribute( oldAtt);
      if (debugCmd) System.out.println("CMD variable removed att "+attName);
    } else
      System.out.println("NetcdfDataset cant find attribute "+attName+" variable "+v.getName());
  }

  protected void cmdRenameVarAtt(NetcdfDataset ds, Variable v, String attName, String newName) {
    ucar.nc2.Attribute oldAtt = v.findAttribute( attName);
    if (null != oldAtt) {
      oldAtt.rename( newName);
      if (debugCmd) System.out.println("CMD variable att "+attName+" renamed to "+newName);
    } else
      System.out.println("NetcdfDataset cmdRenameVarAtt cant find attribute "+attName+
        " variable "+v.getName());
  } */

  /**
   * Read an NcML file and write an equivilent NetcdfFile to a physical file, using Netcdf-3 file format.
   *
   * @param ncmlLocation read this NcML file
   * @param fileOutName write to this local file
   * @throws IOException
   * @see ucar.nc2.FileWriter#writeToFile
   */
  public static void writeNcMLToFile(String ncmlLocation, String fileOutName) throws IOException {
    NetcdfDataset ncd = (NetcdfDataset) NetcdfDataset.acquireFile(ncmlLocation, null);
    //int dataMode = (ncd.getReferencedFile() != null) ? 1 : 2;
    NetcdfFile ncdnew = ucar.nc2.FileWriter.writeToFile(ncd, fileOutName);
    ncd.close();
    ncdnew.close();
  }

  /**
   * Read an NcML and write an equivilent NetcdfFile to a physical file, using Netcdf-3 file format.
   * The NcML may have a referenced dataset in the location URL, in which case the underlying data
   * (modified by the NcML is written to the new file. If the NcML does not have a referenced dataset,
   * then the new file is filled with fill values, like ncgen.
   *
   * @param ncml read NcML from this input stream
   * @param fileOutName write to this local file
   * @throws IOException
   * @see ucar.nc2.FileWriter#writeToFile
   */
  public static void writeNcMLToFile(InputStream ncml, String fileOutName) throws IOException {
    NetcdfDataset ncd = NcMLReader.readNcML (ncml, null);
    NetcdfFile ncdnew = ucar.nc2.FileWriter.writeToFile(ncd, fileOutName, true);
    ncd.close();
    ncdnew.close();
  }

  public static void main( String arg[]) {
    String ncmlFile = "C:/dev/netcdf-java-2.2/test/data/ncml/aggDirectory.xml";
    String ncmlFileOut = "C:\\TEMP\\New Folder\\aggDirectory.nc";
    try {
      //NetcdfDataset ncd = NcMLReader.readNcML (ncmlFile, null);
      //ncd.writeNcMLG(System.out, true, null);
      //System.out.println("NcML = "+ncmlFile);
      InputStream in = new FileInputStream(ncmlFile);
      writeNcMLToFile( in, ncmlFileOut);

    } catch (Exception ioe) {
      System.out.println("error = "+ncmlFile);
      ioe.printStackTrace();
    }
  }

}

/* protected VariableDS readCoordAxis( NetcdfDataset ds, Group g, Structure parentStructure, Element varElem) {
    VariableDS vds = readVariable( ds, g, parentStructure,  varElem);
    CoordinateAxis axis = CoordinateAxis.factory(ds, vds);

    String axisType = varElem.getAttributeValue("axisType");
    String positive = varElem.getAttributeValue("positive");
    String boundaryRef = varElem.getAttributeValue("boundaryRef");

    if (axisType != null)
      axis.setAxisType(AxisType.getType(axisType));
    if (positive != null)
      axis.setPositive(positive);
    if (boundaryRef != null)
      axis.setBoundaryRef(boundaryRef);

    return axis;
  }

  protected CoordinateSystem readCoordSystem( NetcdfDataset ds, Element csElem) {
    ArrayList axes = new ArrayList();
    ArrayList coordTrans = new ArrayList();

    // look for coordinate axis references
    java.util.List list = csElem.getChildren("coordinateAxisRef", ncNS);
    for (int j=0; j< list.size(); j++) {
     Element elem = (Element) list.get(j);
     String axisName = elem.getAttributeValue("ref");
     CoordinateAxis axis = ds.findCoordinateAxis(axisName);
     axes.add( axis);
     if (debug) System.out.println(" add coordinateAxisRef = "+axisName);
    }

     // look for coordinate transforms
    list = csElem.getChildren("coordinateTransform", ncNS);
    for (int j=0; j< list.size(); j++) {
     CoordinateTransform ct = readCoordTransform(ds, (Element) list.get(j));
     ds.addCoordinateTransform( ct);
     coordTrans.add( ct);
     if (debug) System.out.println(" add coordinateTransforms = "+ct.getName());
    }

    // look for coordinate transform references
    list = csElem.getChildren("coordinateTransformRef", ncNS);
    for (int j=0; j< list.size(); j++) {
     Element elem = (Element) list.get(j);
     String name = elem.getAttributeValue("ref");
     CoordinateTransform axis = ds.findCoordinateTransform(name);
     coordTrans.add( axis);
     if (debug) System.out.println(" add coordinateTransformRef = "+name);
    }

    return new CoordinateSystem(axes, coordTrans);
  }

  protected CoordinateTransform readCoordTransform( NetcdfDataset ds, Element ctElem) {
    String name = ctElem.getAttributeValue("name");
    String authority = ctElem.getAttributeValue("authority");
    String typeS = ctElem.getAttributeValue("type");

    TransformType type = (typeS == null) ? null : TransformType.getType( typeS);
    CoordinateTransform ct = new CoordinateTransform (name, authority, type);

    return ct;
  } */
