/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.iosp.netcdf3;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.unidata.io.RandomAccessFile;

import java.util.*;
import java.io.IOException;

/**
 * Netcdf header reading and writing for version 3 file format.
 * This is used by N3iosp.
 *
 * @author caron
 */

public class N3header {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3header.class);
  static private final long MAX_UNSIGNED_INT = 0x00000000ffffffffL;

  static final byte[] MAGIC = new byte[]{0x43, 0x44, 0x46, 0x01};
  static final byte[] MAGIC_LONG = new byte[]{0x43, 0x44, 0x46, 0x02}; // 64-bit offset format : only affects the variable offset value
  static final int MAGIC_DIM = 10;
  static final int MAGIC_VAR = 11;
  static final int MAGIC_ATT = 12;

  static public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    // this is the first time we try to read the file - if there's a problem we get a IOException
    raf.seek(0);
    byte[] b = new byte[4];
    raf.readFully(b);
    for (int i = 0; i < 3; i++)
      if (b[i] != MAGIC[i])
        return false;
    return ((b[3] == 1) || (b[3] == 2));
  }

  static public boolean disallowFileTruncation = false;  // see NetcdfFile.setDebugFlags
  static public boolean debugHeaderSize = false;  // see NetcdfFile.setDebugFlags

  private static boolean debugVariablePos = false;
  private static boolean debugStreaming = false;

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  ucar.unidata.io.RandomAccessFile raf;
  private ucar.nc2.NetcdfFile ncfile;
  private List<Variable> uvars = new ArrayList<Variable>(); // vars that have the unlimited dimension
  private Dimension udim; // the unlimited dimension

  // N3iosp needs access to these
  boolean isStreaming = false; // is streaming (numrecs = -1)
  int numrecs = 0; // number of records written
  long recsize = 0; // size of each record (padded)
  long recStart = Integer.MAX_VALUE; // where the record data starts

  private boolean useLongOffset;
  private long nonRecordDataSize; // size of non-record variables
  private long dataStart = Long.MAX_VALUE; // where the data starts

  private long globalAttsPos = 0; // global attributes start here - used for update

  /* Notes
    - dimensions are signed or unsigned ? in java, must be signed, so are limited to 2^31, not 2^32
    " Each fixed-size variable and the data for one record's worth of a single record variable are limited in size to a little less
     that 4 GiB, which is twice the size limit in versions earlier than netCDF 3.6."
   */

  /**
   * Read the header and populate the ncfile
   *
   * @param raf    read from this file
   * @param ncfile fill this NetcdfFile object (originally empty)
   * @param fout    optional for debug message, may be null
   * @throws IOException on read error
   */
  void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, Formatter fout) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    //this.out = (fout == null) ? new Formatter(System.out) : fout;

    long actualSize = raf.length();
    nonRecordDataSize = 0; // length of non-record data
    recsize = 0; // length of single record
    recStart = Integer.MAX_VALUE; // where the record data starts

    // netcdf magic number
    long pos = 0;
    raf.order(RandomAccessFile.BIG_ENDIAN);
    raf.seek(pos);

    byte[] b = new byte[4];
    raf.readFully(b);
    for (int i = 0; i < 3; i++)
      if (b[i] != MAGIC[i])
        throw new IOException("Not a netCDF file "+raf.getLocation());
    if ((b[3] != 1) && (b[3] != 2))
      throw new IOException("Not a netCDF file "+raf.getLocation());
    useLongOffset = (b[3] == 2);

    // number of records
    numrecs = raf.readInt();
    if (fout != null) fout.format("numrecs= %d%n", numrecs);
    if (numrecs == -1) {
      isStreaming = true;
      numrecs = 0;
    }

    // dimensions
    int numdims = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_DIM)
        throw new IOException("Misformed netCDF file - dim magic number wrong "+raf.getLocation());
      numdims = raf.readInt();
      if (fout != null) fout.format("numdims= %d%n", numdims);
    }

    for (int i = 0; i < numdims; i++) {
      if (fout != null) fout.format("  dim %d pos= %d%n", i, raf.getFilePointer());
      String name = readString();
      int len = raf.readInt();
      Dimension dim;
      if (len == 0) {
        dim = new Dimension(name, numrecs, true, true, false);
        udim = dim;
      } else {
        dim = new Dimension(name, len, true, false, false);
      }

      ncfile.addDimension(null, dim);
      if (fout != null) fout.format(" added dimension %s%n", dim);
    }

    // global attributes
    globalAttsPos = raf.getFilePointer();
    readAtts(ncfile.getRootGroup(), fout);

    // variables
    int nvars = 0;
    magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_VAR)
        throw new IOException("Misformed netCDF file  - var magic number wrong "+raf.getLocation());
      nvars = raf.readInt();
      if (fout != null) fout.format("numdims= %d%n", numdims);
    }
    if (fout != null) fout.format("num variables= %d%n", nvars);

    // loop over variables
    for (int i = 0; i < nvars; i++) {
      long startPos = raf.getFilePointer();
      String name = readString();
      Variable var = new Variable(ncfile, ncfile.getRootGroup(), null, name);

      // get element count in non-record dimensions
      long velems = 1;
      boolean isRecord = false;
      int rank = raf.readInt();
      List<Dimension> dims = new ArrayList<Dimension>();
      for (int j = 0; j < rank; j++) {
        int dimIndex = raf.readInt();
        Dimension dim = ncfile.getRootGroup().getDimensions().get(dimIndex); // note relies on ordering
        if (dim.isUnlimited()) {
          isRecord = true;
          uvars.add(var); // track record variables
        } else
          velems *= dim.getLength();

        dims.add(dim);
      }
      var.setDimensions(dims);

      if (fout != null) {
        fout.format("---name=<%s> dims = [", name);
        for ( Dimension dim : dims)
          fout.format("%s ", dim.getShortName());
        fout.format("]%n");
      }

      // variable attributes
      long varAttsPos = raf.getFilePointer();
      readAtts(var, fout);

      // data type
      int type = raf.readInt();
      DataType dataType = getDataType(type);
      var.setDataType(dataType);

      // size and beginning data position in file
      long vsize = (long) raf.readInt();
      long begin = useLongOffset ? raf.readLong() : (long) raf.readInt();

      if (fout != null) {
        fout.format(" name= %s type=%d vsize=%s velems=%d begin= %d isRecord=%s attsPos=%d%n", name, type, vsize, velems, begin, isRecord, varAttsPos);
        long calcVsize = (velems + padding(velems)) * dataType.getSize();
        if (vsize != calcVsize)
          fout.format(" *** readVsize %d != calcVsize %d%n", vsize, calcVsize);
      }
      if (vsize < 0) {
        vsize = (velems + padding(velems)) * dataType.getSize();
      }

      var.setSPobject(new Vinfo(vsize, begin, isRecord, varAttsPos));

      // track how big each record is
      if (isRecord) {
        recsize += vsize;
        recStart = Math.min(recStart, begin);
      } else {
        nonRecordDataSize = Math.max(nonRecordDataSize, begin + vsize);
      }

      dataStart = Math.min(dataStart, begin);

      if (debugVariablePos)
        System.out.printf("%s begin at=%d end=%d  isRecord=%s nonRecordDataSize=%d%n", var.getFullName(), begin, (begin + vsize), isRecord, nonRecordDataSize);
      if (fout != null)
        fout.format("%s begin at=%d end=%d  isRecord=%s nonRecordDataSize=%d%n", var.getFullName(), begin, (begin + vsize), isRecord, nonRecordDataSize);
      if (debugHeaderSize)
        System.out.printf("%s header size=%d data size= %d%n", var.getFullName(), (raf.getFilePointer() - startPos), vsize);

      ncfile.addVariable(null, var);
    }

    pos = raf.getFilePointer();

    // if nvars == 0
    if (dataStart == Long.MAX_VALUE) {
      dataStart = pos;
    }

    if (nonRecordDataSize > 0) // if there are non-record variables
      nonRecordDataSize -= dataStart;
    if (uvars.size() == 0) // if there are no record variables
      recStart = 0;

    // Check if file affected by bug CDM-52 (netCDF-Java library used incorrect padding when
    // the file contained only one record variable and it was of type byte, char, or short).
    if ( uvars.size() == 1 ) {
      Variable uvar = uvars.get( 0);
      DataType dtype = uvar.getDataType();
      if ( ( dtype == DataType.CHAR ) || ( dtype == DataType.BYTE ) || ( dtype == DataType.SHORT ) ) {
        long vsize = uvar.getDataType().getSize(); // works for all netcdf-3 data types
        for ( Dimension curDim : uvar.getDimensions() ) {
          if ( !curDim.isUnlimited() )
            vsize *= curDim.getLength();
        }
        Vinfo vinfo = (Vinfo) uvar.getSPobject();
        if ( vsize != vinfo.vsize ) {
          // log.info( "Misformed netCDF file - file written with incorrect padding for record variable (CDM-52): fvsize=" + vinfo.vsize+"!= calc size =" + vsize );
          recsize =  vsize;
          vinfo.vsize = vsize;
        }
      }
    }

    if (debugHeaderSize) {
      System.out.println("  filePointer = " + pos + " dataStart=" + dataStart);
      System.out.println("  recStart = " + recStart + " dataStart+nonRecordDataSize =" + (dataStart + nonRecordDataSize));
      System.out.println("  nonRecordDataSize size= " + nonRecordDataSize);
      System.out.println("  recsize= " + recsize);
      System.out.println("  numrecs= " + numrecs);
      System.out.println("  actualSize= " + actualSize);
    }

    // check for streaming file - numrecs must be calculated
    if (isStreaming) {
      long recordSpace = actualSize - recStart;
      numrecs = recsize == 0 ? 0 : (int) (recordSpace / recsize);
      if (debugStreaming) {
        long extra = recsize == 0 ? 0 : recordSpace % recsize;
        System.out.println(" isStreaming recordSpace=" + recordSpace + " numrecs=" + numrecs + " has extra bytes = " + extra);
      }

      // set it in the unlimited dimension, all of the record variables
      if (udim != null) {
        udim.setLength(this.numrecs);
        for (Variable uvar : uvars) {
          uvar.resetShape();
          uvar.invalidateCache();
        }
      }
    }

    // check for truncated files
    // theres a "wart" that allows a file to be up to 3 bytes smaller than you expect.
    long calcSize = dataStart + nonRecordDataSize + recsize * numrecs;
    if (calcSize > actualSize + 3) {
      if (disallowFileTruncation)
        throw new IOException("File is truncated calculated size= " + calcSize + " actual = " + actualSize);
      else {
        //log.info("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
        raf.setExtendMode();
      }
    }

  }

  long calcFileSize() {
    if (udim != null)
      return recStart + recsize * numrecs;
    else
      return dataStart + nonRecordDataSize;
  }

  void showDetail(Formatter out) throws IOException {
    long actual = raf.length();
    out.format("  raf length= %s %n", actual);
    out.format("  isStreaming= %s %n", isStreaming);
    out.format("  useLongOffset= %s %n", useLongOffset);
    out.format("  dataStart= %d%n", dataStart);
    out.format("  nonRecordData size= %d %n", nonRecordDataSize);
    out.format("  unlimited dimension = %s %n", udim);

    if (udim != null) {
      out.format("  record Data starts = %d %n", recStart);
      out.format("  recsize = %d %n", recsize);
      out.format("  numrecs = %d %n", numrecs);
    }

    long calcSize = calcFileSize();
    out.format("  computedSize = %d %n", calcSize);
    if (actual < calcSize)
      out.format("  TRUNCATED!! actual size = %d (%d bytes) %n", actual, (calcSize-actual));
    else if (actual != calcSize)
      out.format(" actual size larger = %d (%d byte extra) %n", actual, (actual-calcSize));

    out.format("%n  %20s____start_____size__unlim%n", "name");
    for (Variable v : ncfile.getVariables()) {
      Vinfo vinfo = (Vinfo) v.getSPobject();
      out.format("  %20s %8d %8d  %s %n", v.getShortName(), vinfo.begin, vinfo.vsize, vinfo.isRecord);
    }
  }

  synchronized boolean removeRecordStructure() {
    boolean found = false;
    for (Variable v : uvars) {
      if (v.getFullName().equals("record")) {
        uvars.remove(v);
        ncfile.getRootGroup().getVariables().remove(v);
        found = true;
        break;
      }
    }

    ncfile.finish();
    return found;
  }

  synchronized boolean makeRecordStructure() {
    // create record structure
    if (uvars.size() > 0) {
      Structure recordStructure = new Structure(ncfile, ncfile.getRootGroup(), null, "record");
      recordStructure.setDimensions(udim.getShortName());
      for (Variable v : uvars) {
        Variable memberV;
        try {
          memberV = v.slice(0, 0); // set unlimited dimension to 0
        } catch (InvalidRangeException e) {
          log.warn("N3header.makeRecordStructure cant slice variable " +v+ " "+e.getMessage());
          return false;
        }
        memberV.setParentStructure(recordStructure);
        //memberV.createNewCache(); // decouple caching - could use this ??

        //remove record dimension
        //List<Dimension> dims = new ArrayList<Dimension>(v.getDimensions());
        //dims.remove(0);
        //memberV.setDimensions(dims);

        recordStructure.addMemberVariable(memberV);
      }

      uvars.add(recordStructure);
      ncfile.getRootGroup().addVariable(recordStructure);
      ncfile.finish();
      return true;
    }

    return false;
  }


  private int readAtts(AttributeContainer atts, Formatter fout) throws IOException {
    int natts = 0;
    int magic = raf.readInt();
    if (magic == 0) {
      raf.readInt(); // skip 32 bits
    } else {
      if (magic != MAGIC_ATT)
        throw new IOException("Misformed netCDF file  - att magic number wrong");
      natts = raf.readInt();
    }
    if (fout != null) fout.format(" num atts= %d%n", natts);

    for (int i = 0; i < natts; i++) {
      if (fout != null) fout.format("***att %d pos= %d%n", i, raf.getFilePointer());
      String name = readString();
      int type = raf.readInt();
      Attribute att;

      if (type == 2) {
        if (fout != null) fout.format(" begin read String val pos= %d%n", raf.getFilePointer());
        String val = readString();
        if (val == null) val = "";
        if (fout != null) fout.format(" end read String val pos= %d%n", raf.getFilePointer());
        att = new Attribute(name, val);

      } else {
        if (fout != null) fout.format(" begin read val pos= %d%n", raf.getFilePointer());
        int nelems = raf.readInt();

        DataType dtype = getDataType(type);

        if (nelems == 0) {
          att = new Attribute(name, dtype, false); // empty - no values

        } else {
          int[] shape = {nelems};
          Array arr = Array.factory(dtype.getPrimitiveClassType(), shape);
          IndexIterator ii = arr.getIndexIterator();
          int nbytes = 0;
          for (int j = 0; j < nelems; j++)
            nbytes += readAttributeValue(dtype, ii);

          att = new Attribute(name, arr); // no validation !!
          skip(nbytes);
        }

        if (fout != null) fout.format(" end read val pos= %d%n", raf.getFilePointer());
      }

      atts.addAttribute(att);
      if (fout != null) fout.format("  %s%n", att);
    }

    return natts;
  }

  private int readAttributeValue(DataType type, IndexIterator ii) throws IOException {
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
  private String readString() throws IOException {
    int nelems = raf.readInt();
    byte[] b = new byte[nelems];
    raf.readFully(b);
    skip(nelems); // pad to 4 byte boundary

    if (nelems == 0)
      return null;

    // null terminates
    int count = 0;
    while (count < nelems) {
      if (b[count] == 0) break;
      count++;
    }

    return new String(b, 0, count, CDM.utf8Charset); // all strings are considered to be UTF-8 unicode.
  }

  // skip to a 4 byte boundary in the file
  private void skip(int nbytes) throws IOException {
    int pad = padding(nbytes);
    if (pad > 0)
      raf.seek(raf.getFilePointer() + pad);
  }

  // find number of bytes needed to pad to a 4 byte boundary
  static int padding(int nbytes) {
    int pad = nbytes % 4;
    if (pad != 0) pad = 4 - pad;
    return pad;
  }

  // find number of bytes needed to pad to a 4 byte boundary
  static int padding(long nbytes) {
    int pad = (int) (nbytes % 4);
    if (pad != 0) pad = 4 - pad;
    return pad;
  }

  private DataType getDataType(int type) {
    switch (type) {
      case 1:
        return DataType.BYTE;
      case 2:
        return DataType.CHAR;
      case 3:
        return DataType.SHORT;
      case 4:
        return DataType.INT;
      case 5:
        return DataType.FLOAT;
      case 6:
        return DataType.DOUBLE;
      default:
        throw new IllegalArgumentException("unknown type == " + type);
    }
  }

  static int getType(DataType dt) {
    if (dt == DataType.BYTE) return 1;
    else if ((dt == DataType.CHAR) || (dt == DataType.STRING)) return 2;
    else if (dt == DataType.SHORT) return 3;
    else if (dt == DataType.INT) return 4;
    else if (dt == DataType.FLOAT) return 5;
    else if (dt == DataType.DOUBLE) return 6;

    throw new IllegalArgumentException("unknown DataType == " + dt);
  }

  /**
   * Write the header out, based on ncfile structures.
   *
   * @param raf       write to this file
   * @param ncfile    the header of this NetcdfFile
   * @param extra     if > 0, pad header with extra bytes
   * @param largeFile if large file format
   * @param fout      debugging output sent to here
   * @throws IOException on write error
   */
  void create(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, int extra, boolean largeFile, Formatter fout) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    writeHeader(extra, largeFile, false, fout);
  }

  /**
   * Sneaky way to make the header bigger, if there is room for it
   *
   * @param largeFile  is large file format
   * @param fout  put debug messages here, mnay be null
   * @return true if it worked
   * @throws IOException
   */
  boolean rewriteHeader(boolean largeFile, Formatter fout) throws IOException {
    int want = sizeHeader(largeFile);
    if (want > dataStart)
      return false;
    
    writeHeader(0, largeFile, true, fout);
    return true;
  }

  void writeHeader(int extra, boolean largeFile, boolean keepDataStart, Formatter fout) throws IOException {
    this.useLongOffset = largeFile;
    nonRecordDataSize = 0; // length of non-record data
    recsize = 0; // length of single record
    recStart = Long.MAX_VALUE; // where the record data starts

    // magic number
    raf.seek(0);
    raf.write(largeFile ? N3header.MAGIC_LONG : N3header.MAGIC);

    // numrecs
    raf.writeInt(0);

    // dims
    List<Dimension> dims = ncfile.getDimensions();
    int numdims = dims.size();
    if (numdims == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(N3header.MAGIC_DIM);
      raf.writeInt(numdims);
    }
    for (int i = 0; i < numdims; i++) {
      Dimension dim = dims.get(i);
      if (fout != null) fout.format("  dim %d pos %d%n", i, raf.getFilePointer());
      writeString(dim.getShortName());
      raf.writeInt(dim.isUnlimited() ? 0 : dim.getLength());
      if (dim.isUnlimited()) udim = dim;
    }

    // global attributes
    globalAttsPos = raf.getFilePointer();
    writeAtts(ncfile.getGlobalAttributes(), fout);

    // variables
    List<Variable> vars = ncfile.getVariables();

    // Track record variables.
    for ( Variable curVar: vars) {
      if ( curVar.isUnlimited()) {
        uvars.add( curVar);
      }
    }
    writeVars(vars, largeFile, fout);

    // now calculate where things go
    if (!keepDataStart) {
      dataStart = raf.getFilePointer();
      if (extra > 0)
        dataStart += extra;
    }
    long pos = dataStart;

    // non-record variable starting positions
    for (Variable var : vars) {
      Vinfo vinfo = (Vinfo) var.getSPobject();
      if (!vinfo.isRecord) {
        raf.seek(vinfo.begin);

        if (largeFile)
          raf.writeLong(pos);
        else {
          if (pos > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Variable starting pos="+pos+" may not exceed "+ Integer.MAX_VALUE);          
          raf.writeInt((int) pos);
        }

        vinfo.begin = pos;
        if (fout != null)
          fout.format("  %s begin at = %d end= %d%n", var.getFullName(), vinfo.begin, (vinfo.begin + vinfo.vsize));
        pos += vinfo.vsize;

        // track how big each record is
        nonRecordDataSize = Math.max(nonRecordDataSize, vinfo.begin + vinfo.vsize);
      }
    }

    recStart = pos; // record variables start here

    // record variable starting positions
    for (Variable var : vars) {
      Vinfo vinfo = (Vinfo) var.getSPobject();
      if (vinfo.isRecord) {
        raf.seek(vinfo.begin);

        if (largeFile)
          raf.writeLong(pos);
        else
          raf.writeInt((int) pos);

        vinfo.begin = pos;
        if (fout != null) fout.format(" %s record begin at = %d%n", var.getFullName(), dataStart);
        pos += vinfo.vsize;

        // track how big each record is
        recsize += vinfo.vsize;
        recStart = Math.min(recStart, vinfo.begin);
      }
    }

    if (nonRecordDataSize > 0) // if there are non-record variables
      nonRecordDataSize -= dataStart;
    if (uvars.size() == 0) // if there are no record variables
      recStart = 0;
  }

  // calculate the size writing a header would take
  int sizeHeader(boolean largeFile) {
    int size = 4; // magic number
    size += 4; // numrecs

    // dims
    size += 8; // magic, ndims
    for (Dimension dim  : ncfile.getDimensions())
      size += sizeString(dim.getShortName()) + 4; // name, len

    // global attributes
    size += sizeAtts(ncfile.getGlobalAttributes());

    // variables
    size += 8; // magic, nvars
    for (Variable var : ncfile.getVariables()) {
      size += sizeString(var.getShortName());

      // dimensions
      size += 4; // ndims
      size += 4 * var.getDimensions().size(); // dim id

      // variable attributes
      size += sizeAtts(var.getAttributes());

      size += 8; // data type, variable size
      size += (largeFile) ? 8 : 4;
    }

    return size;
  }

  private void writeAtts(List<Attribute> atts, Formatter fout) throws IOException {

    int n = atts.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_ATT);
      raf.writeInt(n);
    }

    for (int i = 0; i < n; i++) {
      if (fout != null) fout.format("***att %d pos= %d%n", i, raf.getFilePointer());
      Attribute att = atts.get(i);

      writeString(att.getShortName());
      int type = getType(att.getDataType());
      raf.writeInt(type);

      if (type == 2) {
        writeStringValues(att);
      } else {
        int nelems = att.getLength();
        raf.writeInt(nelems);
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += writeAttributeValue(att.getNumericValue(j));
        pad(nbytes, (byte) 0);
        if (fout != null) fout.format(" end write val pos= %d%n", raf.getFilePointer());
      }
      if (fout != null) fout.format("  %s%n", att);
    }
  }

  private int sizeAtts(List<Attribute> atts) {
    int size = 8; // magic, natts

    for (Attribute att : atts) {
      size += sizeString(att.getShortName());
      size += 4; // type

      int type = getType(att.getDataType());
      if (type == 2) {
        size += sizeStringValues(att);
      } else {
        size += 4; // nelems
        int nelems = att.getLength();
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += sizeAttributeValue(att.getNumericValue(j));
        size += nbytes;
        size += padding(nbytes);
      }
    }
    return size;
  }

  private void writeStringValues(Attribute att) throws IOException {
    int n = att.getLength();
    if (n == 1)
      writeString(att.getStringValue());
    else {
      StringBuilder values = new StringBuilder();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      writeString(values.toString());
    }
  }

  private int sizeStringValues(Attribute att) {
    int size = 0;
    int n = att.getLength();
    if (n == 1)
      size += sizeString(att.getStringValue());
    else {
      StringBuilder values = new StringBuilder();
      for (int i = 0; i < n; i++)
        values.append(att.getStringValue(i));
      size += sizeString(values.toString());
    }

    return size;
  }

  private int writeAttributeValue(Number numValue) throws IOException {
    if (numValue instanceof Byte) {
      raf.write(numValue.byteValue());
      return 1;

    } else if (numValue instanceof Short) {
      raf.writeShort(numValue.shortValue());
      return 2;

    } else if (numValue instanceof Integer) {
      raf.writeInt(numValue.intValue());
      return 4;

    } else if (numValue instanceof Float) {
      raf.writeFloat(numValue.floatValue());
      return 4;

    } else if (numValue instanceof Double) {
      raf.writeDouble(numValue.doubleValue());
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private int sizeAttributeValue(Number numValue) {

    if (numValue instanceof Byte) {
      return 1;

    } else if (numValue instanceof Short) {
      return 2;

    } else if (numValue instanceof Integer) {
      return 4;

    } else if (numValue instanceof Float) {
      return 4;

    } else if (numValue instanceof Double) {
      return 8;
    }

    throw new IllegalStateException("unknown attribute type == " + numValue.getClass().getName());
  }

  private void writeVars(List<Variable> vars, boolean largeFile, Formatter fout) throws IOException {
    int n = vars.size();
    if (n == 0) {
      raf.writeInt(0);
      raf.writeInt(0);
    } else {
      raf.writeInt(MAGIC_VAR);
      raf.writeInt(n);
    }

    for (int i = 0; i < n; i++) {
      Variable var = vars.get(i);
      writeString(var.getShortName());

      // dimensions
      long vsize = var.getDataType().getSize(); // works for all netcdf-3 data types
      List<Dimension> dims = var.getDimensions();
      raf.writeInt(dims.size());
      for (Dimension dim : dims) {
        int dimIndex = findDimensionIndex(ncfile, dim);
        raf.writeInt(dimIndex);

        if (!dim.isUnlimited())
          vsize *= dim.getLength();
      }
      long unpaddedVsize = vsize;
      vsize += padding(vsize);

      // variable attributes
      long varAttsPos = raf.getFilePointer();
      writeAtts(var.getAttributes(), fout);

      // data type, variable size, beginning file position
      DataType dtype = var.getDataType();
      int type = getType(dtype);
      raf.writeInt(type);

      int vsizeWrite = (vsize < MAX_UNSIGNED_INT) ? (int) vsize : -1;
      raf.writeInt(vsizeWrite);
      long pos = raf.getFilePointer();
      if (largeFile)
        raf.writeLong(0); // come back to this later
      else
        raf.writeInt(0); // come back to this later

      // From nc3 file format specification
      // (http://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF-Classic-Format):
      //     Note on padding: In the special case of only a single record variable of character,
      //     byte, or short type, no padding is used between data values.
      // 2/15/2011: we will continue to write the (incorrect) padded vsize into the header, but we will use the unpadded size to read/write
      if ( uvars.size() == 1 && uvars.get(0) == var )
        if ( ( dtype == DataType.CHAR ) || ( dtype == DataType.BYTE ) || ( dtype == DataType.SHORT ) )
          vsize = unpaddedVsize;

      var.setSPobject(new Vinfo(vsize, pos, var.isUnlimited(), varAttsPos));
    }
  }

  // write a string then pad to 4 byte boundary
  private void writeString(String s) throws IOException {
 //   if (s.length() == 0)
   //   System.out.println("HEY");
    byte[] b = s.getBytes(CDM.utf8Charset); // all strings are encoded in UTF-8 Unicode.
    raf.writeInt(b.length);
    raf.write(b);
    pad(b.length, (byte) 0);
  }

  private int sizeString(String s) {
    int size = s.length() + 4;
    return size + padding(s.length());
  }

  private int findDimensionIndex(NetcdfFile ncfile, Dimension wantDim) {
    List<Dimension> dims = ncfile.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = dims.get(i);
      if (dim.equals(wantDim)) return i;
    }
    throw new IllegalStateException("unknown Dimension == " + wantDim);
  }

  // pad to a 4 byte boundary
  private void pad(int nbytes, byte fill) throws IOException {
    int pad = padding(nbytes);
    for (int i = 0; i < pad; i++)
      raf.write(fill);
  }

  void writeNumrecs() throws IOException {
    // set number of records in the header
    raf.seek(4);
    raf.writeInt(numrecs);
  }

  void setNumrecs(int n) throws IOException {
    this.numrecs = n;
  }

  synchronized boolean synchNumrecs() throws IOException {
    // check number of records in the header
    // gotta bypass the RAF buffer
    int n = raf.readIntUnbuffered(4);
    if (n == this.numrecs)
      return false;
    if (n < 0) // streaming
      return false;

    // update everything
    this.numrecs = n;

    // set it in the unlimited dimension
    udim.setLength(this.numrecs);

    // set it in all of the record variables
    for (Variable uvar : uvars) {
      uvar.resetShape();
      uvar.invalidateCache();
    }

    return true;
  }


  // variable info for reading/writing
  static class Vinfo {
    long vsize; // size of array in bytes. if isRecord, size per record.
    long begin; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?
    long attsPos = 0; //  attributes start here - used for update

    Vinfo(long vsize, long begin, boolean isRecord, long attsPos) {
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
      this.attsPos = attsPos;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  // tricky  - perhaps deprecate ?

  void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    long pos;
    if (v2 == null)
      pos = findAtt(globalAttsPos, att.getShortName());
    else {
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      pos = findAtt(vinfo.attsPos, att.getShortName());
    }

    raf.seek(pos);
    int type = raf.readInt();
    DataType have = getDataType(type);
    DataType want = att.getDataType();
    if (want == DataType.STRING) want = DataType.CHAR;
    if (want != have) {
        throw new IllegalArgumentException("Update Attribute must have same type or original = " + have + " att = " + att);
    }

    if (type == 2) {  // String
      String s = att.getStringValue();
      int org = raf.readInt();
      int size = org + padding(org); // ok to use the padding
      int max = Math.min(size, s.length()); // cant make any longer than size
      if (max > org) { // adjust if its using the padding, but not if its shorter
        raf.seek(pos + 4);
        raf.writeInt(max);
      }

      byte[] b = new byte[size];
      for (int i = 0; i < max; i++)
        b[i] = (byte) s.charAt(i);
      raf.write(b);

    } else {
      int nelems = raf.readInt();
      int max = Math.min(nelems, att.getLength()); // cant make any longer
      for (int j = 0; j < max; j++)
        writeAttributeValue(att.getNumericValue(j));
    }
  }

  private long findAtt(long start_pos, String want) throws IOException {
    raf.seek(start_pos + 4);

    int natts = raf.readInt();
    for (int i = 0; i < natts; i++) {
      String name = readString();
      if (name.equals(want))
        return raf.getFilePointer();

      int type = raf.readInt();

      if (type == 2) {
        readString();
      } else {
        int nelems = raf.readInt();
        DataType dtype = getDataType(type);
        int[] shape = {nelems};
        Array arr = Array.factory(dtype.getPrimitiveClassType(), shape);
        IndexIterator ii = arr.getIndexIterator();
        int nbytes = 0;
        for (int j = 0; j < nelems; j++)
          nbytes += readAttributeValue(dtype, ii);
        skip(nbytes);
      }
    }

    throw new IllegalArgumentException("no such attribute " + want);
  }


}