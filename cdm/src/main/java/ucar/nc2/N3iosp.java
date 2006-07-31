// $Id:N3iosp.java 51 2006-07-12 17:13:13Z caron $
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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;

import java.util.*;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * IOServiceProvider implementation abstract base class to read/write "version 3" netcdf files.
 *  AKA "file format version 1" files.
 *
 * @see N3raf concrete class
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

abstract public class N3iosp implements ucar.nc2.IOServiceProviderWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3iosp.class);

  // Default fill values, used unless _FillValue variable attribute is set.
  static public final byte NC_FILL_BYTE = -127;
  static public final char NC_FILL_CHAR = (char) 0;
  static public final short NC_FILL_SHORT = (short) -32767;
  static public final int NC_FILL_INT = -2147483647;
  static public final float NC_FILL_FLOAT = 9.9692099683868690e+36f; /* near 15 * 2^119 */
  static public final double NC_FILL_DOUBLE = 9.9692099683868690e+36;
  static public final String FillValue = "_FillValue";

  static private boolean syncExtendOnly = false;

  /**
   * Set a static property.
   * Supported static properties: <ul>
   *   <li> syncExtendOnly = "true" : assume all file changes are syncExtend only.
   *   </ul>
   * @param name property name
   * @param value property value
   */
  static public void setProperty( String name, String value) {
    if (name.equalsIgnoreCase("syncExtendOnly"))
      syncExtendOnly = value.equalsIgnoreCase("true");
  }

  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return N3header.isValidFile( raf);
  }

  protected ucar.nc2.NetcdfFile ncfile;
  protected boolean readonly;

  protected ucar.unidata.io.RandomAccessFile raf;
  protected N3header headerParser;
  protected int numrecs, recsize;
  protected long lastModified; // used by sync

  // used for writing only
  protected int fileUsed = 0; // how much of the file is written to ?
  protected int recStart = 0; // where the record data starts

  protected boolean debug = false, debugSize = false, debugSPIO = false, debugRecord = false, debugSync = false;
  protected boolean showHeaderBytes = false;

  private ByteArrayOutputStream bos = new ByteArrayOutputStream( 10000);
  protected PrintStream out = new PrintStream(bos);
  public String getDetailInfo() { return bos.toString(); }

  // properties
  protected boolean useRecordStructure;

  public void setSpecial( Object special) {}
  
  public void setProperties( List iospProperties) {
    for (int i = 0; i < iospProperties.size(); i++) {
      Attribute att = (Attribute) iospProperties.get(i);
      if (att.getName().equalsIgnoreCase("useRecordStructure"))
        useRecordStructure = att.getStringValue().equalsIgnoreCase("true");
      if (att.getName().equalsIgnoreCase("syncExtendOnly"))
        syncExtendOnly = att.getStringValue().equalsIgnoreCase("true");
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // read existing file

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    String location = raf.getLocation();
    if (!location.startsWith("http:")) {
      File file = new File( location);
      if (file.exists())
        lastModified = file.lastModified();
    }

    // its a netcdf-3 file
    raf.order(RandomAccessFile.BIG_ENDIAN);
    headerParser = new N3header();

    headerParser.read( raf, ncfile, null); // read header here
    numrecs = headerParser.numrecs;
    recsize = headerParser.recsize;
    recStart = headerParser.recStart;

    _open( raf);
  }

  /////////////////////////////////////////////////////////////////////////////
  // data reading

  public Array readData(ucar.nc2.Variable v2, java.util.List sectionList) throws IOException, InvalidRangeException  {
    if (v2 instanceof Structure)
      return readRecordData( (Structure) v2, sectionList);

    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    RegularIndexer index = new RegularIndexer( v2.getShape(), v2.getElementSize(), vinfo.begin, sectionList, v2.isUnlimited() ? recsize : -1);
    Object data = readData( index, dataType);
    return Array.factory( dataType.getPrimitiveClassType(), index.getWantShape(), data);
  }

  /**
   * Read data from record structure. For N3, this is the only possible structure, and there can be no nesting.
   * Read all variables for each record, for efficiency.
   *
   * @param s the record structure
   * @param sectionList the record range to read
   * @return an ArrayStructure, with all the data read in.
   *
   * @throws IOException
   */
  private ucar.ma2.Array readRecordData(ucar.nc2.Structure s, List sectionList) throws java.io.IOException {
    // has to be 1D
    Range recordRange = (Range) sectionList.get(0);

    // create the ArrayStructure
    StructureMembers members = makeStructureMembers(s);
    members.setStructureSize( recsize);
    ArrayStructureBB structureArray = new ArrayStructureBB( members, new int[] { recordRange.length()} );

    // note dependency on raf; should probably defer to subclass
    // loop over records
    byte [] result = (byte []) structureArray.getStorage();
    int count = 0;
    for (int recnum=recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      if (debugRecord) System.out.println(" read record "+recnum);
      raf.seek( recStart + recnum*recsize); // where the record starts

      if (recnum != numrecs-1)
        raf.readFully( result, count*recsize, recsize);
      else
        raf.read( result, count*recsize, recsize); // "wart" allows file to be one byte short. since its always padding, we allow
      count++;
    }

    return structureArray;
  }

  private StructureMembers makeStructureMembers(Structure s) {
    StructureMembers members = s.makeStructureMembers();
    Iterator iter = members.getMembers().iterator();
    while (iter.hasNext()) {
      StructureMembers.Member m = (StructureMembers.Member) iter.next();
      Variable v2 = s.findVariable(m.getName());
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      m.setDataParam((int) vinfo.begin - recStart);
    }
    return members;
  }

  /**
   * Read data from a Variable that is nested in the record Structure.
   *
   * @param v2 a nested Variable.
   * @param sectionList List of type Range specifying the section of data to read. There must be a Range for each
   *  Dimension in each parent, as well as in the Variable itself. Must be in order from outer to inner.
   * @return the requested data in a memory-resident Array
   */
  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, java.util.List sectionList)
         throws java.io.IOException, ucar.ma2.InvalidRangeException {

    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    // construct the full shape for use by RegularIndexer
    int[] varShape = v2.getShape();
    int[] fullShape = new int[ v2.getRank()+1];
    fullShape[0] = numrecs;
    for (int i=0; i<v2.getRank(); i++ ) {
      fullShape[i+1] = varShape[i];
    }

    Indexer index = new RegularIndexer( fullShape, v2.getElementSize(), vinfo.begin, sectionList, recsize);
    Object dataObject = readData( index, dataType);
    return Array.factory( dataType.getPrimitiveClassType(), Range.getShape(sectionList), dataObject);

    //if (flatten)
    //  return result;

    /* If flatten is false, wrap the result Array in an ArrayStructureMA
    StructureMembers members = new StructureMembers( v2.getName());
    StructureMembers.Member member = new StructureMembers.Member( v2.getShortName(), v2.getDescription(),
          v2.getUnitsString(), v2.getDataType(), v2.getShape());
    member.setDataObject( result);
    Range outerRange = (Range) sectionList.get(0);
    ArrayStructureMA structureArray = new ArrayStructureMA(members, new int[] {outerRange.length()});

    return structureArray; */

    /* Range recordRange = (Range) sectionList.get(0);
    Array result = Array.factory( StructureData.class, new int[] { recordRange.length()} );
    Index resultIndex = result.getIndex();

    // the inner section
    ArrayList innerSection = new ArrayList( sectionList);
    innerSection.remove(0);
    int[] innerShape = Range.getShape( innerSection);

    // loop over records
    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    int count = 0;
    for (int recnum=recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      StructureData sdata = new StructureData( v2.getParentStructure());
      result.setObject( resultIndex.set(count++), sdata);

      // get the data for just this variable, in just the current record
      Indexer index = new RegularIndexer( v2.getShape(), v2.getElementSize(), vinfo.begin+recnum*recsize, innerSection, -1);
      Object data = readData( index, v2.getDataType());
      Array dataArray = Array.factory( v2.getDataType().getPrimitiveClassType(), innerShape, data);

      sdata.addMember( v2, dataArray);
    }

    return result; */

  }

   /* If flatten is true, return an Array of the same type as the Variable.
    * The shape of the returned Array will include the shape of the Structure containing the variable.
  private Array readNestedDataFlatten(ucar.nc2.Variable v2, java.util.List sectionList) throws IOException, InvalidRangeException  {
    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    // construct the full shape for use by RegularIndexer
    int[] varShape = v2.getShape();
    int[] fullShape = new int[ v2.getRank()+1];
    fullShape[0] = numrecs;
    for (int i=0; i<v2.getRank(); i++ ) {
      fullShape[i+1] = varShape[i];
    }

    Indexer index = new RegularIndexer( fullShape, v2.getElementSize(), vinfo.begin, sectionList, recsize);
    Object dataObject = readData( index, dataType);
    return Array.factory( dataType.getPrimitiveClassType(), Range.getShape(sectionList), dataObject);
  } */

  // convert byte array to char array
  static protected char[] convertByteToChar( byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i=0; i<size; i++)
      cbuff[i] = (char) byteArray[i];
    return cbuff;
  }

   // convert char array to byte array
  static protected byte[] convertCharToByte( char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i=0; i<size; i++)
      to[i] = (byte) from[i];
    return to;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // create new file

  protected boolean fill;
  protected HashMap dimHash = new HashMap(50);

  public void create(String filename, ucar.nc2.NetcdfFile ncfile, boolean fill) throws IOException {
    this.ncfile = ncfile;
    this.fill = fill;
    this.readonly = false;

    // finish any structures
    ncfile.finish();

    raf = new ucar.unidata.io.RandomAccessFile(filename, "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);

    headerParser = new N3header();
    headerParser.create(raf, ncfile, fill, null);

    recsize = headerParser.recsize;   // record size
    recStart = headerParser.recStart; // record variables start here
    fileUsed = headerParser.recStart; // track what is actually used

    _create( raf);

    if (fill)
      fillNonRecordVariables();
    else
      raf.setMinLength( recStart); // make sure file length is long enough, even if not written to.
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // write

  public void writeData(Variable v2, java.util.List sectionList, Array values) throws java.io.IOException, InvalidRangeException {

    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    if (v2.isUnlimited()) {
      Range firstRange = (Range) sectionList.get(0);
      setNumrecs( firstRange.last()+1);
    }

    if (v2 instanceof Structure) {
      writeRecordData( (Structure) v2, sectionList, values);

    } else {
      Indexer index = new RegularIndexer( v2.getShape(), v2.getElementSize(), vinfo.begin, sectionList, v2.isUnlimited() ? recsize : -1);
      writeData( values, index, dataType);
    }
  }

  private void writeRecordData(ucar.nc2.Structure s, List sectionList, Array values) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    if (!(values instanceof ArrayStructure))
      throw new IllegalArgumentException("writeRecordData: data must be ArrayStructure");
    ArrayStructure structureData = (ArrayStructure) values;

    List vars = s.getVariables();
    StructureMembers members = structureData.getStructureMembers();

    Range recordRange = (Range) sectionList.get(0);
    int count = 0;
    for (int recnum=recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      // System.out.println("  wrote "+recnum+" begin at "+begin);

      // loop over members
      for (int i = 0; i < vars.size(); i++) {
        Variable v2 = (Variable) vars.get(i);
        N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
        long begin = vinfo.begin + recnum*recsize;
        Indexer index = new RegularIndexer( v2.getShape(), v2.getElementSize(), begin, null, -1); // LOOK special case RegularIndexer

        Array data = structureData.getArray( count, members.findMember( v2.getShortName()));
        writeData( data, index, v2.getDataType());
     }

      count++;
    }
  }


  protected void setNumrecs(int n) throws IOException, InvalidRangeException {
    if (n <= numrecs) return;
    int startRec = numrecs;

    if (debugSize) System.out.println("extend records to = "+n);
    fileUsed = recStart + recsize * n;
    headerParser.setNumrecs( n);
    this.numrecs = n;

    // need to let unlimited dimension know of new shape
    Iterator iter = ncfile.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension dim = (Dimension) iter.next();
      if (dim.isUnlimited())
       dim.setLength(n);
    }

    // need to let all unlimited variables know of new shape
    iter = ncfile.getVariables().iterator();
    while (iter.hasNext()) {
      Variable v = (Variable) iter.next();
      if (v.isUnlimited()) {
        v.shape[0] = n;
      }
    }

    // handle filling
    if (fill)
      fillRecordVariables(startRec, n);
    else
      raf.setMinLength(fileUsed);
  }

  /////////////////////////////////////////////////////////////

  // fill buffer with fill value
  /* LOOK this is wrong
  protected void fillerup( int start, int end) throws IOException {
    int len = end - start;
    byte val = (byte) 0;
    raf.seek( start);
    for (int i=0; i<len; i++)
      raf.write( val);
  } */

  protected void fillNonRecordVariables() throws IOException {
    // run through each variable
    Iterator varIter = ncfile.getVariables().iterator();
    while (varIter.hasNext()) {
      Variable v = (Variable) varIter.next();
      if (v.isUnlimited()) continue;
      try {
        writeData(v, null, makeConstantArray( v));
      } catch (InvalidRangeException e) {
        e.printStackTrace();  // shouldnt happen
      }
    }
  }

  protected void fillRecordVariables(int recStart, int recEnd) throws IOException, InvalidRangeException {
    // do each record completely, should be a bit more efficient
    for (int i=recStart; i<recEnd; i++) {
      Range r = new Range(i,i);

      // run through each variable
      Iterator varIter = ncfile.getVariables().iterator();
      while (varIter.hasNext()) {
        Variable v = (Variable) varIter.next();
        if (!v.isUnlimited() || (v instanceof Structure)) continue;
        ArrayList ranges = new ArrayList();
        ranges.add(r);
        for (int j=1; j<v.getRank(); j++)
          ranges.add(null);
        writeData(v, ranges, makeConstantArray( v));
      }
    }
  }

  private Array makeConstantArray( Variable v) {
    Class classType = v.getDataType().getPrimitiveClassType();
    int [] shape = v.getShape();
    Attribute att = v.findAttribute("_FillValue");

    Object storage = null;
    if (classType == double.class) {
      double[] storageP = new double[1];
      storageP[0] = (att == null) ? NC_FILL_DOUBLE : att.getNumericValue().doubleValue();
      storage = storageP;

    } else if (classType == float.class) {
      float[] storageP = new float[1];
      storageP[0] = (att == null) ? NC_FILL_FLOAT : att.getNumericValue().floatValue();
      storage = storageP;

    } else if (classType == int.class) {
      int[] storageP = new int[1];
      storageP[0] = (att == null) ? NC_FILL_INT : att.getNumericValue().intValue();
      storage = storageP;

    } else if (classType == short.class) {
      short[] storageP = new short[1];
      storageP[0] = (att == null) ? NC_FILL_SHORT : att.getNumericValue().shortValue();
      storage = storageP;

    } else if (classType == byte.class) {
      byte[] storageP = new byte[1];
      storageP[0] = (att == null) ? NC_FILL_BYTE : att.getNumericValue().byteValue();
      storage = storageP;

    } else if (classType == char.class) {
      char[] storageP = new char[1];
      storageP[0] = (att == null) ? NC_FILL_CHAR : att.getStringValue().charAt(0);
      storage = storageP;
    }

    return Array.factoryConstant( classType, shape, storage);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  public boolean syncExtend() throws IOException {
    boolean result = headerParser.synchNumrecs();
    if (result && log.isDebugEnabled())
      log.debug(" N3iosp syncExtend "+raf.getLocation()+" numrecs ="+headerParser.numrecs);
    return result;
  }

  public boolean sync() throws IOException {
    if (syncExtendOnly)
      return syncExtend();

    if (lastModified == 0)
      return false;

    File file = new File( raf.getLocation());
    if (file.exists()) {
      long currentModified = file.lastModified();
      if (currentModified == lastModified)
        return false;

      // so things have been modified, heres where we need to reread the header !!
      ncfile.empty();
      open(raf, ncfile, null);
      if (log.isDebugEnabled()) log.debug(" N3iosp resynced "+raf.getLocation()+" currentModified="+currentModified+" lastModified= "+lastModified);
      return true;
    }

    // LOOK if not exist, probably should throw an IOException ??

    return false;
  }

  public void flush() throws java.io.IOException {
    raf.flush();
    headerParser.writeNumrecs();
    raf.flush();
  }

  public void close() throws java.io.IOException {
    raf.setMinLength(fileUsed);
    raf.close();
  }

  /** Debug info for this object. */
  public String toStringDebug(Object o) { return null; }


  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff we need the subclass to implement

   /**
    * Read data subset from file for a variable, create primitive array.
    * @param index handles skipping around in the file.
    * @param dataType dataType of the variable
    * @return primitive array with data read in
    */
  abstract protected Object readData( Indexer index, DataType dataType) throws IOException;

   /**
    * Write data subset to file for a variable, create primitive array.
    * @param aa write data in this Array.
    * @param index handles skipping around in the file.
    * @param dataType dataType of the variable
    */
  abstract protected void writeData( Array aa, Indexer index, DataType dataType) throws IOException;

  abstract protected void _open(ucar.unidata.io.RandomAccessFile raf) throws java.io.IOException;

  abstract protected void _create(ucar.unidata.io.RandomAccessFile raf) throws java.io.IOException;

}
