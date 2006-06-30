// $Id: N3header.java,v 1.24 2006/03/09 22:18:46 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import java.util.*;
import java.io.IOException;
import java.io.PrintStream;


/**
 * Netcdf header reading and writing for version 3 file format.
 * This is used by N3iosp.
 */

class N3header {
  static final byte[] MAGIC = new byte[] {0x43, 0x44, 0x46, 0x01 };
  static final int MAGIC_DIM = 10;
  static final int MAGIC_VAR = 11;
  static final int MAGIC_ATT = 12;

  static boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException {
    // this is the firsst time we try to read the file - if theres a problem we get a IOException
    // try {
      raf.seek(0);
      byte[] b = new byte[4];
      raf.read(b);
      for (int i=0; i<3; i++)
        if (b[i] != MAGIC[i])
          return false;
        if ((b[3] != 1) && (b[3] != 2)) return false;
      return true;
    // } catch (IOException ioe) {
    //  return false;
    // }
  }

  static boolean disallowFileTruncation = false;  // see NetcdfFile.setDebugFlags
  static boolean debugHeaderSize = false;  // see NetcdfFile.setDebugFlags

  private boolean debug = false, debugPos = false, debugString = false, debugVariablePos = false;

  private ucar.unidata.io.RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private PrintStream out = System.out;
  private ArrayList uvars = new ArrayList(); // vars that have the unlimited dimension
  private Dimension udim; // the unlimited dimension

  int numrecs = 0; // number of records written
  int recsize = 0; // size of each record (padded)
  int dataStart = Integer.MAX_VALUE; // where the data starts
  int recStart = Integer.MAX_VALUE; // where the record data starts
  private long nonRecordData = 0; // length of non-record data
  private long actualSize, calcSize;

  private boolean useLongOffset = false;

  private Structure recordStructure = null;


  /**
   * Read the header and populate the ncfile
   * @param raf read from this file
   * @param ncfile fill this NetcdfFile object (originally empty)
   * @param out optional for debug message, may be null
   * @throws IOException
   */
  void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, PrintStream out) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    if (out == null)
      out = System.out;
    this.out = out;

    actualSize = raf.length();

    // netcdf magic number
    long pos = 0;
    raf.seek(pos);
    byte[] b = new byte[4];
    raf.read(b);
    for (int i=0; i<3; i++)
      if (b[i] != MAGIC[i])
        throw new IOException("Not a netCDF file");
    if ((b[3] != 1) && (b[3] != 2))
      throw new IOException("Not a netCDF file");
    useLongOffset = (b[3] == 2);

    // number of records
    numrecs = raf.readInt();
    if (debug) out.println("numrecs= "+numrecs);

    // dimensions
    int numdims = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      magic = raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_DIM)
        throw new IOException("Misformed netCDF file - dim magic number wrong");
      numdims = raf.readInt();
      if (debug) out.println("numdims= "+numdims);
    }

    for (int i=0; i<numdims; i++) {
      if (debugPos) out.println("  dim "+i+" pos= "+raf.getFilePointer());
      String name= readString();
      int len = raf.readInt();
      Dimension dim;
      if (len == 0) {
        dim = new Dimension( name, numrecs, true, true, false);
        udim = dim;
      } else {
        dim = new Dimension( name, len, true, false, false);
      }

      ncfile.addDimension( null, dim);
      if (debug) out.println(" added dimension "+dim);
    }

    // global attributes
    readAtts( ncfile.rootGroup.attributes);

    // variables
    int nvars = 0;
    magic = raf.readInt();
    if (magic == 0) {
      magic = raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_VAR)
        throw new IOException("Misformed netCDF file  - var magic number wrong");
      nvars = raf.readInt();
      if (debug) out.println("numdims= "+numdims);
    }
    if (debug) out.println("num variables= "+nvars);

    // loop over variables
    for (int i=0; i<nvars; i++) {
      String name= readString();
      Variable var = new Variable( ncfile, ncfile.rootGroup, null, name);

      // get dimensions
      int velems = 1;
      boolean isRecord = false;
      int rank = raf.readInt();
      ArrayList dims = new ArrayList();
      for (int j=0; j<rank; j++) {
        int dimIndex = raf.readInt();
        Dimension dim = (Dimension) ncfile.rootGroup.dimensions.get(dimIndex); // note relies on ordering
        if (dim.isUnlimited()) {
          isRecord = true;
          uvars.add( var); // track record variables
        } else
          velems *= dim.getLength();

        dims.add( dim);
      }
      var.setDimensions( dims);

      if (debug) {
        out.print("---name=<"+name+"> dims = [");
        for (int j=0; j<rank; j++) {
          Dimension dim = (Dimension) dims.get(j);
          out.print(dim.getName()+" ");
        }
        out.println("]");
      }

      // variable attributes
      readAtts( var.attributes);

      // data type
      int type = raf.readInt();
      var.setDataType( getDataType( type));

      // size and beginning data position in file
      int vsize = raf.readInt();
      long begin = useLongOffset ? raf.readLong() : (long) raf.readInt();
      if (debug) out.println(" name= "+name+" type="+type+" vsize="+vsize+" velems="+velems+" begin= "+begin+" isRecord="+isRecord+"\n");
      var.setSPobject( new Vinfo (vsize, begin, isRecord));

      // track how big each record is
      if (isRecord) {
        recsize += vsize;
        recStart = Math.min( recStart, (int) begin);
      } else {
        nonRecordData = Math.max( nonRecordData, begin+vsize);
      }

      dataStart = Math.min( dataStart, (int) begin);

      if (debugVariablePos)
        System.out.println(var.getName()+" begin at = " + begin+ " end="+(begin+vsize)+ " isRecord="+isRecord+" nonRecordData="+nonRecordData);

      ncfile.addVariable(null, var);
    }

    pos = (int) raf.getFilePointer();

    if (nonRecordData > 0) // if there are non-record variables
      nonRecordData -= dataStart;
    if (uvars.size() == 0) // if there are no record variables
      recStart = 0;

    if (debugHeaderSize) {
      System.out.println("  filePointer = "+ pos+" dataStart="+dataStart);
      System.out.println("  recStart = "+ recStart+" dataStart+nonRecordData ="+(dataStart+nonRecordData));
      System.out.println("  nonRecordData size= "+ nonRecordData);
      System.out.println("  recsize= "+ recsize);
      System.out.println("  numrecs= "+ numrecs);
    }

    // check for truncated files
    // theres a "wart" that allows a file to be up to 3 bytes smaller than you expect.
    calcSize = dataStart + nonRecordData + recsize * numrecs;
    if (calcSize > actualSize+3) {
      if (disallowFileTruncation)
        throw new IOException("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
      else {
        //System.out.println("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
        raf.setExtendMode();
      }
    }

    // finish
    ncfile.finish();
  }

  boolean addRecordStructure() {

    // create record structure
    if (uvars.size() > 0) {
      recordStructure = new Structure(ncfile, ncfile.rootGroup, null, "record");
      recordStructure.setDimensions(udim.getName());
      for (int i=0; i<uvars.size(); i++) {
        Variable v = (Variable) uvars.get(i);
        ArrayList dims = (ArrayList) v.getDimensions();
        dims.remove(0); //remove record dimension
        Variable recordV = new Variable(ncfile, ncfile.rootGroup, recordStructure, v.getShortName());
        recordV.setDataType( v.getDataType());
        recordV.setSPobject( v.getSPobject());
        recordV.attributes.addAll( v.getAttributes());
        recordV.setDimensions( dims);

        recordStructure.addMemberVariable( recordV);
      }

      ncfile.addVariable(ncfile.rootGroup, recordStructure);
      ncfile.finish();
      return true;
    }

    return false;
  }



  private int readAtts(ArrayList atts) throws IOException {
    int natts = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      magic = raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_ATT)
        throw new IOException("Misformed netCDF file  - att magic number wrong");
      natts = raf.readInt();
    }
    if (debug) out.println(" num atts= "+natts);

    for (int i=0; i<natts; i++) {
      if (debugPos) out.println("***att "+i+" pos= "+raf.getFilePointer());
      String name= readString();
      int type = raf.readInt();
      Attribute att = new Attribute( name);
      atts.add( att);

      if (type == 2) {
        if (debugPos) out.println(" begin read String val pos= "+raf.getFilePointer());
        String val = readString();
        if (debugPos) out.println(" end read String val pos= "+raf.getFilePointer());
        att.setStringValue( val);

      } else {
        if (debugPos) out.println(" begin read val pos= "+raf.getFilePointer());
        int nelems = raf.readInt();

        DataType dtype = getDataType( type);
        int [] shape = {nelems};
        Array arr = Array.factory(dtype.getPrimitiveClassType(), shape);
        IndexIterator ii = arr.getIndexIterator();
        int nbytes = 0;
        for (int j=0; j<nelems; j++)
          nbytes += readAttributeValue( dtype, ii);

        att.setValues( arr);

        skip( nbytes);
        if (debugPos) out.println(" end read val pos= "+raf.getFilePointer());
      }
      if (debug) out.println("  "+att.toString()+"\n");
    }

    return natts;
  }

  private int readAttributeValue( DataType type, IndexIterator ii) throws IOException {
    if (type == DataType.BYTE) {
        byte b = (byte) raf.read();
        //if (debug) out.println("   byte val = "+b);
        ii.setByteNext(b);
        return 1;

    } else if (type == DataType.CHAR) {
        char c = (char) raf.read();
        //if (debug) out.println("   char val = "+c);
        ii.setCharNext(c);
        return 1;

    } else if (type == DataType.SHORT) {
        short s = raf.readShort();
        //if (debug) out.println("   short val = "+s);
        ii.setShortNext(s);
        return 2;

    } else if (type == DataType.INT) {
        int i = raf.readInt();
        //if (debug) out.println("   int val = "+i);
        ii.setIntNext(i);
        return 4;

    } else if (type == DataType.FLOAT) {
        float f = raf.readFloat();
        //if (debug) out.println("   float val = "+f);
        ii.setFloatNext(f);
        return 4;

    } else if (type == DataType.DOUBLE) {
        double d = raf.readDouble();
        //if (debug) out.println("   double val = "+d);
        ii.setDoubleNext(d);
        return 8;
    }
    return 0;
  }

  // read a string = (nelems, byte array), then skip to 4 byte boundary
  private String readString()  throws IOException {
    int nelems = raf.readInt();
    if (debugString) printBytes( nelems);
    byte[] b = new byte[nelems];
    raf.read(b);
    skip(nelems);
    return new String( b);
  }

  // skip to a 4 byte boundary in the file
  private void skip( int nbytes) throws IOException {
    int pad = padding(nbytes);
    if (pad > 0)
      raf.seek( raf.getFilePointer() + pad);
  }

    // find number of bytes needed to pad to a 4 byte boundary
  private int padding( int nbytes) {
    int pad = nbytes % 4;
    if (pad != 0) pad = 4 - pad;
    return pad;
  }

  private void printBytes( int n)  throws IOException {
    long savePos = raf.getFilePointer();
    long pos;
    for (pos = savePos; pos<savePos+n-9; pos+=10) {
      out.print( pos +": ");
      _printBytes( 10);
    }
    if (pos < savePos+n) {
      out.print( pos +": ");
      _printBytes( (int) (savePos+n-pos));
    }
    raf.seek(savePos);
  }

  private void _printBytes( int n)  throws IOException {
    for (int i=0; i<n; i++) {
      byte b = (byte) raf.read();
      int ub = (b < 0) ? b + 256 : b;
      out.print( ub +"(");
      out.write(b);
      out.print( ") ");
    }
    out.println();
  }

  private DataType getDataType( int type) {
    switch (type) {
      case 1: return DataType.BYTE;
      case 2: return DataType.CHAR;
      case 3: return DataType.SHORT;
      case 4: return DataType.INT;
      case 5: return DataType.FLOAT;
      case 6: return DataType.DOUBLE;
    }
    throw new IllegalStateException("unknown type == "+type);
  }

  private int getType( DataType dt) {
    if (dt == DataType.BYTE) return 1;
    else if ((dt == DataType.CHAR) || (dt == DataType.STRING)) return 2;
    else if (dt == DataType.SHORT) return 3;
    else if (dt == DataType.INT) return 4;
    else if (dt == DataType.FLOAT) return 5;
    else if (dt == DataType.DOUBLE) return 6;

    throw new IllegalStateException("unknown DataType == "+dt);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  private boolean fill;
  private HashMap dimHash = new HashMap(50);

  /**
   * Write the header out, based on ncfile structures.
   * @param raf
   * @param ncfile
   * @param fill
   * @param out
   * @throws IOException
   */
  void create(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, boolean fill, PrintStream out) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    this.fill = fill;
    if (out != null) this.out = out;

    // make sure ncfile structures were finished
    ncfile.finish();

    // magic number
    raf.write( N3header.MAGIC);

    // numrecs
    raf.writeInt(0);

    // dims
    List dims = ncfile.getDimensions();
    int numdims = dims.size();
    if (numdims == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(N3header.MAGIC_DIM);
      raf.writeInt(numdims);
    }
    for (int i=0; i<numdims; i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (debugPos) out.println("  dim "+i+" pos= "+raf.getFilePointer());
      writeString(dim.getName());
      raf.writeInt(dim.isUnlimited() ? 0 : dim.getLength());
      if (dim.isUnlimited()) udim = dim;
    }

    // global attributes
    writeAtts( ncfile.getGlobalAttributes());

    // variables
    List vars = ncfile.getVariables();
    writeVars( vars);

    // now calculate where things go
    dataStart = (int) raf.getFilePointer(); // LOOK should be long ??
    int pos = dataStart;

    // non-record variable starting positions
    for (int i=0; i<vars.size(); i++) {
      Variable var = (Variable) vars.get(i);
      N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
      if (!vinfo.isRecord) {
        raf.seek(vinfo.begin);
        raf.writeInt(pos);
        vinfo.begin = pos;
        if (debugVariablePos)
          System.out.println(var.getName()+" begin at = " + vinfo.begin+ " end="+(vinfo.begin+vinfo.vsize));
        pos += vinfo.vsize;
      }
    }

    recStart = pos; // record variables start here

    // record variable starting positions
    for (int i=0; i<vars.size(); i++) {
      Variable var = (Variable) vars.get(i);
      N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
      if (vinfo.isRecord) {
        raf.seek(vinfo.begin);
        raf.writeInt(pos);
        vinfo.begin = pos;
        if (debug) System.out.println(var.getName()+" record begin at = " + dataStart);
        pos += vinfo.vsize;
        uvars.add( var); // track record variables
      }
    }

  }

  private void writeAtts(List atts) throws IOException {
    int n = atts.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_ATT);
      raf.writeInt(n);
    }

    for (int i=0; i<n; i++) {
      if (debugPos) out.println("***att "+i+" pos= "+raf.getFilePointer());
      Attribute att = (Attribute) atts.get(i);

      writeString(att.getName());
      int type = getType(att.getDataType());
      raf.writeInt( type);

      if (type == 2) {
        writeStringValues(att);
      } else {
        int nelems = att.getLength();
        raf.writeInt( nelems);
        int nbytes = 0;
        for (int j=0; j<nelems; j++)
          nbytes += writeAttributeValue( att.getNumericValue(j));
        pad( nbytes, (byte)0);
        if (debugPos) out.println(" end write val pos= "+raf.getFilePointer());
      }
      if (debug) out.println("  "+att.toString()+"\n");
    }
  }

  private void writeStringValues(Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1)
      writeString(att.getStringValue());
    else {
      StringBuffer values = new StringBuffer();
      for (int i=0; i<n;i++)
        values.append( att.getStringValue(i));
      writeString(values.toString());
    }
  }

  private int writeAttributeValue( Number numValue) throws IOException {
    if (numValue instanceof Byte) {
      raf.write( numValue.byteValue());
      return 1;

    } else if (numValue instanceof Short) {
      raf.writeShort( numValue.shortValue());
      return 2;

    } else if (numValue instanceof Integer) {
      raf.writeInt( numValue.intValue());
      return 4;

    } else if (numValue instanceof Float) {
      raf.writeFloat( numValue.floatValue());
      return 4;

    } else if (numValue instanceof Double) {
      raf.writeDouble( numValue.doubleValue());
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == "+numValue.getClass().getName());
  }

  private void writeVars(List vars) throws IOException {
    int n = vars.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_VAR);
      raf.writeInt(n);
    }

    for (int i=0; i<n; i++) {
      Variable var = (Variable) vars.get(i);
      writeString( var.getName());

      // dimensions
      int vsize = var.getDataType().getSize();
      List dims = var.getDimensions();
      raf.writeInt(dims.size());
      for (int j=0; j<dims.size(); j++) {
        Dimension dim = (Dimension) dims.get(j);
        int dimIndex = findDimensionIndex(ncfile, dim);
        raf.writeInt(dimIndex);

        if (!dim.isUnlimited())
          vsize *= dim.getLength();
      }
      vsize += padding( vsize);

      // variable attributes
      writeAtts( var.getAttributes());

      // data type, variable size, beginning file position
      int type = getType(var.getDataType());
      raf.writeInt( type);
      raf.writeInt( vsize);
      int pos = (int) raf.getFilePointer(); // LOOK should be long ??
      raf.writeInt(0); // come back to this later

      //if (debug) out.println(" name= "+name+" type="+type+" vsize="+vsize+" begin= "+begin+" isRecord="+isRecord+"\n");
      var.setSPobject( new Vinfo (vsize, pos, var.isUnlimited()));

      // keep track of the record size
      if (var.isUnlimited())
        recsize += vsize;
    }
  }

    // write a string then pad to 4 byte boundary
  private void writeString( String s)  throws IOException {
    byte[] b = s.getBytes();
    raf.writeInt( b.length);
    raf.write( b);
    pad( b.length, (byte)0);
  }

  private int findDimensionIndex(NetcdfFile ncfile, Dimension wantDim) {
    List dims = ncfile.getDimensions();
    for (int i=0; i<dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (dim.equals(wantDim)) return i;
    }
    throw new IllegalStateException("unknown Dimension == "+wantDim);
  }

    // pad to a 4 byte boundary
  private void pad( int nbytes, byte fill)  throws IOException {
    int pad = padding(nbytes);
    for (int i=0; i<pad; i++)
      raf.write( fill);
  }

  void writeNumrecs() throws IOException {
      // set number of records in the header
    raf.seek(4);
    raf.writeInt(numrecs);
  }

  void setNumrecs(int n) throws IOException {
     this.numrecs = n;

    // set it in the unlimited dimension
    udim.setLength(n);

    // set it in all of the record variables
    for (Iterator i = uvars.iterator(); i.hasNext(); ) {
      Variable v = (Variable) i.next();
      v.setDimensions( v.getDimensions());
    }
  }


  boolean synchNumrecs() throws IOException {
    // check number of records in the header

    // gotta bypass the RAF buffer
    int n = raf.readIntUnbuffered( 4);
    if (n == this.numrecs)
      return false;

    // update everything
    this.numrecs = n;

    // set it in the unlimited dimension
    udim.setLength(this.numrecs);

    // set it in all of the record variables
    for (Iterator i = uvars.iterator(); i.hasNext(); ) {
      Variable v = (Variable) i.next();
      v.setDimensions( v.getDimensions());
      v.setCachedData(null, false);
    }

    return true;
  }


  // variable info for reading/writing
  static class Vinfo {
    int vsize; // size of array in bytes. if isRecord, size per record.
    long begin; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?
    Vinfo( int vsize, long begin, boolean isRecord) {
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
    }
  }

}

/* Change History:
   $Log: N3header.java,v $
   Revision 1.24  2006/03/09 22:18:46  caron
   bug fixes for sync, dods.

   Revision 1.23  2006/01/11 16:15:47  caron
   syncExtend
   N3iosp, FileWriter writes by record

   Revision 1.22  2005/12/15 00:29:10  caron
   *** empty log message ***

   Revision 1.21  2005/12/09 04:24:41  caron
   Aggregation
   caching
   sync

   Revision 1.20  2005/11/03 19:30:24  caron
   no message

   Revision 1.19  2005/10/11 19:36:54  caron
   NcML add Records bug fixes
   iosp.isValidFile( ) throws IOException
   release 2.2.11

   Revision 1.18  2005/08/26 00:32:40  caron
   deal with NetCDF "non-canonical length" files

   Revision 1.17  2005/07/11 20:12:22  caron
   *** empty log message ***

   Revision 1.16  2005/07/08 18:49:56  caron
   handle DODS CHAR variables
   improve NcML-G
   handle String array attributes

   Revision 1.15  2005/05/19 21:56:02  caron
   turn off all debugging

   Revision 1.14  2005/05/19 00:10:45  caron
   fix file truncation "wart"

   Revision 1.13  2005/05/11 00:09:55  caron
   refactor StuctureData, dt.point

   Revision 1.12  2005/05/04 17:18:43  caron
   *** empty log message ***

   Revision 1.11  2005/05/03 20:09:29  caron
   more fixes to Point/Station
   clean up nc2.units, add unit tests

   Revision 1.10  2005/04/18 23:45:56  caron
   _unsigned
   FileCache
   minFileLength

   Revision 1.9  2005/03/21 22:06:39  caron
   fix truncation when no fill and no record variables written and non-record variables not fully written

   Revision 1.8  2004/10/12 02:57:06  caron
   refactor for grib1/grib2: move common functionality up to ucar.grib
   split GribServiceProvider

   Revision 1.7  2004/09/22 18:44:32  caron
   move common to ucar.unidata

   Revision 1.6  2004/09/22 13:46:35  caron
   *** empty log message ***

   Revision 1.5  2004/08/26 17:55:10  caron
   no message

   Revision 1.4  2004/08/16 20:53:45  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:17  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:10  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */