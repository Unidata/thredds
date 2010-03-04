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
package ucar.nc2.iosp.netcdf3;

import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.nc2.*;
import ucar.nc2.iosp.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * IOServiceProvider implementation abstract base class to read/write "version 3" netcdf files.
 * AKA "file format version 1" files.
 *
 * @author caron
 * @see N3raf concrete class
 */

public abstract class N3iosp extends AbstractIOServiceProvider implements IOServiceProviderWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(N3iosp.class);

  // Default fill values, used unless _FillValue variable attribute is set.
  static public final byte NC_FILL_BYTE = -127;
  static public final char NC_FILL_CHAR = (char) 0;
  static public final short NC_FILL_SHORT = (short) -32767;
  static public final int NC_FILL_INT = -2147483647;
  static public final long NC_FILL_LONG = -9223372036854775806L;
  static public final float NC_FILL_FLOAT = 9.9692099683868690e+36f; /* near 15 * 2^119 */
  static public final double NC_FILL_DOUBLE = 9.9692099683868690e+36;
  static public final String FillValue = "_FillValue";

  /* CLASSIC
     The maximum size of a record in the classic format in versions 3.5.1 and earlier is 2^32 - 4 bytes.
     In versions 3.6.0 and later, there is no such restriction on total record size for the classic format
     or 64-bit offset format.

     If you don't use the unlimited dimension, only one variable can exceed 2 GiB in size, but it can be as
       large as the underlying file system permits. It must be the last variable in the dataset, and the offset
       to the beginning of this variable must be less than about 2 GiB.

     The limit is really 2^31 - 4. If you were to specify a variable size of 2^31 -3, for example, it would be
       rounded up to the nearest multiple of 4 bytes, which would be 2^31, which is larger than the largest
       signed integer, 2^31 - 1.

     If you use the unlimited dimension, record variables may exceed 2 GiB in size, as long as the offset of the
       start of each record variable within a record is less than 2 GiB - 4.
   */

  /* LARGE FILE
     Assuming an operating system with Large File Support, the following restrictions apply to the netCDF 64-bit offset format.

     No fixed-size variable can require more than 2^32 - 4 bytes of storage for its data, unless it is the last
     fixed-size variable and there are no record variables. When there are no record variables, the last
     fixed-size variable can be any size supported by the file system, e.g. terabytes.

     A 64-bit offset format netCDF file can have up to 2^32 - 1 fixed sized variables, each under 4GiB in size.
     If there are no record variables in the file the last fixed variable can be any size.

     No record variable can require more than 2^32 - 4 bytes of storage for each record's worth of data,
     unless it is the last record variable. A 64-bit offset format netCDF file can have up to 2^32 - 1 records,
     of up to 2^32 - 1 variables, as long as the size of one record's data for each record variable except the
     last is less than 4 GiB - 4.

     Note also that all netCDF variables and records are padded to 4 byte boundaries.
   */

  /**
   * Each fixed-size variable and the data for one record's worth of a single record variable are limited
   *    to a little less than 4 GiB.
   */
  static public final long MAX_VARSIZE = (long) 2 * Integer.MAX_VALUE - 2; // 4,294,967,292

  /**
   * The maximum number of records is 2^32-1.
   */
  static public final int MAX_NUMRECS = Integer.MAX_VALUE;

  static private boolean syncExtendOnly = false;

  /**
   * Set a static property.
   * Supported static properties: <ul>
   * <li> syncExtendOnly = "true" : assume all file changes are syncExtend only.
   * </ul>
   *
   * @param name  property name
   * @param value property value
   */
  static public void setProperty(String name, String value) {
    if (name.equalsIgnoreCase("syncExtendOnly"))
      syncExtendOnly = value.equalsIgnoreCase("true");
  }

/*
 * LOOK do we need to implement this ??
 *
 * Verify that a name string is valid syntax.  The allowed name
 * syntax (in RE form) is:
 *
 * ([a-zA-Z0-9_]|{UTF8})([^\x00-\x1F\x7F/]|{UTF8})*
 *
 * where UTF8 represents a multibyte UTF-8 encoding.  Also, no
 * trailing spaces are permitted in names.  This definition
 * must be consistent with the one in ncgen.l.  We do not allow '/'
 * because HDF5 does not permit slashes in names as slash is used as a
 * group separator.  If UTF-8 is supported, then a multi-byte UTF-8
 * character can occur anywhere within an identifier.  We later
 * normalize UTF-8 strings to NFC to facilitate matching and queries.
 *
public String NC_check_name(String name) {
	int skip;
	int ch;
	const char *cp = name;
	ssize_t utf8_stat;

	assert(name != NULL);

	if(*name == 0		// empty names disallowed
	   || strchr(cp, '/'))	// '/' can't be in a name
		return NC_EBADNAME;

	/* check validity of any UTF-8
	utf8_stat = utf8proc_check((const unsigned char *)name);
	if (utf8_stat < 0)
	    return NC_EBADNAME;

	/* First char must be [a-z][A-Z][0-9]_ | UTF8
	ch = (uchar)*cp;
	if(ch <= 0x7f) {
	    if(   !('A' <= ch && ch <= 'Z')
	       && !('a' <= ch && ch <= 'z')
	       && !('0' <= ch && ch <= '9')
	       && ch != '_' )
		return NC_EBADNAME;
	    cp++;
	} else {
	    if((skip = nextUTF8(cp)) < 0)
		return NC_EBADNAME;
	    cp += skip;
	}

	while(*cp != 0) {
	    ch = (uchar)*cp;
	    /* handle simple 0x00-0x7f characters here
	    if(ch <= 0x7f) {
                if( ch < ' ' || ch > 0x7E) /* control char or DEL
		  return NC_EBADNAME;
		cp++;
	    } else {
		if((skip = nextUTF8(cp)) < 0) return NC_EBADNAME;
		cp += skip;
	    }
	    if(cp - name > NC_MAX_NAME)
		return NC_EMAXNAME;
	}
	if(ch <= 0x7f && isspace(ch)) // trailing spaces disallowed
	    return NC_EBADNAME;
	return NC_NOERR;
} */

  /**
   * Convert a name to a legal netcdf-3 name.
   * ([a-zA-Z0-9_]|{UTF8})([^\x00-\x1F\x7F/]|{UTF8})*
   * @param name convert this name
   * @return converted name
   */
  static public String makeValidNetcdfObjectName(String name) {
    StringBuilder sb = new StringBuilder(name.trim()); // remove starting and trailing blanks

    while (sb.length() > 0) {
      char c = sb.charAt(0);
      if (Character.isLetter(c) || Character.isDigit(c) || (c == '_')) break;
      sb.deleteCharAt(0);
    }

    int pos = 1;
    while (pos < sb.length()) {
      int c = sb.codePointAt(pos);
      if (((c >= 0) && (c < 0x20)) || (c == 0x7f)) {
        sb.delete(pos, pos + 1);
        pos--;
      }
      pos++;
    }

    if (sb.length() == 0)
      throw new IllegalArgumentException("Illegal name");
    return sb.toString();
  }

  static public String makeValidNetcdfObjectNameOld(String name) {
    StringBuilder sb = new StringBuilder(name);

    while (sb.length() > 0) {
      char c = sb.charAt(0);
      if (Character.isLetter(c) || (c == '_')) break;
      if (Character.isDigit(c)) {
        sb.insert(0, 'N');
        break;
      }
      sb.deleteCharAt(0);
    }

    int i = 1;
    while (i < sb.length()) {
      char c = sb.charAt(i);
      if (c == ' ')
        sb.setCharAt(i, '_');
      else {
        boolean ok = Character.isLetterOrDigit(c) || (c == '-') || (c == '_');
        //        || (c == '@') || (c == ':') || (c == '(') || (c == ')') || (c == '+') || (c == '.');
        if (!ok) {
          sb.delete(i, i + 1);
          i--;
          // sb.setCharAt(i, '-');
        }
      }
      i++;
    }

    return sb.toString();
  }

  /////////////////////////////////////////////////
  // name pattern matching
  //static private final String special1 = "_\\.@\\+\\-";
  //static private final String special2 = " ";
  //static private final Pattern objectNamePattern = Pattern.compile("[a-zA-Z0-9_][a-zA-Z0-9_@\\.\\-\\+]*");
  static private final Pattern objectNamePattern = Pattern.compile("[a-zA-Z0-9_][a-zA-Z0-9_@\\:\\(\\)\\.\\-\\+]*");

  /**
   * Determine if the given name can be used for a Dimension, Attribute, or Variable name.
   * @param name test this.
   * @return  true if valid name.
   */
  static public boolean isValidNetcdf3ObjectName(String name) {
    Matcher m = objectNamePattern.matcher(name);
    return m.matches();
  }

  /**
   * Valid Netcdf Object name as a regular expression.
   * @return regular expression pattern describing valid Netcdf Object names.
   */
  static public Pattern getValidNetcdf3ObjectNamePattern() {
    return objectNamePattern;
  }

  /**
   * Convert a name to a legal netcdf name.
   * From the user manual:
   * "The names of dimensions, variables and attributes consist of arbitrary sequences of
   * alphanumeric characters (as well as underscore '_' and hyphen '-'), beginning with a letter
   * or underscore. (However names commencing with underscore are reserved for system use.)
   * Case is significant in netCDF names."
   * <p/>
   * Algorithm:
   * <ol>
   * <li>leading character: if alpha or underscore, ok; if digit, prepend "N"; otherwise discard
   * <li>other characters: if space, change to underscore; other delete.
   * </ol>
   * @param name convert this name
   * @return converted name
   */
  static public String createValidNetcdf3ObjectName(String name) {
    StringBuilder sb = new StringBuilder(name);

    //LOOK: could escape characters, as in DODS (%xx) ??

    while (sb.length() > 0) {
      char c = sb.charAt(0);
      if (Character.isLetter(c) || (c == '_')) break;
      if (Character.isDigit(c)) {
        sb.insert(0, 'N');
        break;
      }
      sb.deleteCharAt(0);
    }

    int i = 1;
    while (i < sb.length()) {
      char c = sb.charAt(i);
      if (c == ' ')
        sb.setCharAt(i, '_');
      else {
        boolean ok = Character.isLetterOrDigit(c) || (c == '-') || (c == '_') ||
                (c == '@') || (c == ':') || (c == '(') || (c == ')') || (c == '+') || (c == '.');
        if (!ok) {
          sb.delete(i, i + 1);
          i--;
          // sb.setCharAt(i, '-');
        }
      }
      i++;
    }

    return sb.toString();
  }


  /////////////////////////////////////////////////////////////////////////////////////////////////

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return N3header.isValidFile(raf);
  }

  protected ucar.nc2.NetcdfFile ncfile;
  protected boolean readonly;

  protected N3header header;
  //protected int numrecs;
  //protected long recsize;
  protected long lastModified; // used by sync

  // used for writing only
  // protected long fileUsed = 0; // how much of the file is written to ?
  // protected long recStart = 0; // where the record data starts

  protected boolean debug = false, debugSize = false, debugSPIO = false, debugRecord = false, debugSync = false;
  protected boolean showHeaderBytes = false;

  @Override
  public String getDetailInfo() {
    try {
      Formatter fout = new Formatter();
      double size = raf.length() / (1000.0 * 1000.0);
      fout.format(" raf = %s%n", raf.getLocation());
      fout.format(" size= %d (%s Mb)%n%n", raf.length(), Format.dfrac(size, 3));
      header.showDetail(fout);
      return fout.toString();
      
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  // properties
  protected boolean useRecordStructure;

  //////////////////////////////////////////////////////////////////////////////////////
  // read existing file

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    String location = raf.getLocation();
    if (!location.startsWith("http:")) {
      File file = new File(location);
      if (file.exists())
        lastModified = file.lastModified();
    }

    // its a netcdf-3 file
    raf.order(RandomAccessFile.BIG_ENDIAN);
    header = new N3header();

    header.read(raf, ncfile, null); // read header here
    //numrecs = header.numrecs;
    //recsize = header.recsize;
    //recStart = header.recStart;

    _open(raf);

    ncfile.finish();
  }


  public void setFill(boolean fill) {
    this.fill = fill;
  }

  /////////////////////////////////////////////////////////////////////////////
  // data reading

  public Array readData(ucar.nc2.Variable v2, Section section) throws IOException, InvalidRangeException {
    if (v2 instanceof Structure)
      return readRecordData((Structure) v2, section);

    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section) :
      new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);

    if (layout.getTotalNelems() == 0) {
      return Array.factory(dataType.getPrimitiveClassType(), section.getShape());
    }

    Object data = readData(layout, dataType);
    return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);
  }

  /**
   * Read data from record structure. For N3, this is the only possible structure, and there can be no nesting.
   * Read all variables for each record, put in ByteBuffer.
   *
   * @param s the record structure
   * @param section the record range to read
   * @return an ArrayStructure, with all the data read in.
   * @throws IOException on error
   */
  private ucar.ma2.Array readRecordData(ucar.nc2.Structure s, Section section) throws java.io.IOException {
    //if (s.isSubset())
    //  return readRecordDataSubset(s, section);

    // has to be 1D
    Range recordRange = section.getRange(0);

    // create the ArrayStructure
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      m.setDataParam((int) (vinfo.begin - header.recStart));
    }

    // protect agains too large of reads
    if (header.recsize > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Cant read records when recsize > "+Integer.MAX_VALUE);
    long nrecs = section.computeSize();
    if (nrecs * header.recsize > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Too large read: nrecs * recsize= "+(nrecs * header.recsize) +"bytes exceeds "+Integer.MAX_VALUE);

    members.setStructureSize((int) header.recsize);
    ArrayStructureBB structureArray = new ArrayStructureBB(members, new int[]{recordRange.length()});

    // note dependency on raf; should probably defer to subclass
    // loop over records
    byte[] result = structureArray.getByteBuffer().array();
    int count = 0;
    for (int recnum = recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      if (debugRecord) System.out.println(" read record " + recnum);
      raf.seek(header.recStart + recnum * header.recsize); // where the record starts

      if (recnum != header.numrecs - 1)
        raf.readFully(result, (int) (count * header.recsize), (int) header.recsize);
      else
        raf.read(result, (int) (count * header.recsize), (int) header.recsize); // "wart" allows file to be one byte short. since its always padding, we allow
      count++;
    }

    return structureArray;
  }

  /**
   * Read data from record structure, that has been subsetted.
   * Read one record at at time, put requested variable into ArrayStructureMA.
   *
   * @param s the record structure
   * @param section the record range to read
   * @return an ArrayStructure, with all the data read in.
   * @throws IOException on error
   */
  private ucar.ma2.Array readRecordDataSubset(ucar.nc2.Structure s, Section section) throws java.io.IOException {
    Range recordRange = section.getRange(0);
    int nrecords = recordRange.length();

    // create the ArrayStructureMA
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      m.setDataParam((int) (vinfo.begin - header.recStart)); // offset from start of record

      // construct the full shape
      int rank = m.getShape().length;
      int[] fullShape = new int[rank + 1];
      fullShape[0] = nrecords;  // the first dimension
      System.arraycopy(m.getShape(), 0, fullShape, 1, rank); // the remaining dimensions

      Array data = Array.factory(m.getDataType(), fullShape);
      m.setDataArray( data);
      m.setDataObject( data.getIndexIterator());
    }

    //LOOK this is all wrong - why using recsize ???
    return null;
    /* members.setStructureSize(recsize);
    ArrayStructureMA structureArray = new ArrayStructureMA(members, new int[]{nrecords});

    // note dependency on raf; should probably defer to subclass
    // loop over records
    byte[] record = new byte[ recsize];
    ByteBuffer bb = ByteBuffer.wrap(record);
    for (int recnum = recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      if (debugRecord) System.out.println(" readRecordDataSubset recno= " + recnum);

      // read one record
      raf.seek(recStart + recnum * recsize); // where the record starts
      if (recnum != numrecs - 1)
        raf.readFully(record, 0, recsize);
      else
        raf.read(record, 0, recsize); // "wart" allows file to be one byte short. since its always padding, we allow

      // transfer desired variable(s) to result array(s)
      for (StructureMembers.Member m : members.getMembers()) {
        IndexIterator dataIter = (IndexIterator) m.getDataObject();
        IospHelper.copyFromByteBuffer(bb, m, dataIter);
      }
    }

    return structureArray;  */
  }

  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    // construct the full shape for use by RegularIndexer
    int[] fullShape = new int[v2.getRank() + 1];
    fullShape[0] = header.numrecs;  // the first dimension
    System.arraycopy(v2.getShape(), 0, fullShape, 1, v2.getRank()); // the remaining dimensions

    Layout layout = new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, fullShape, section);
    Object dataObject = readData(layout, dataType);
    return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), dataObject);
  }

  public long readToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    if (v2 instanceof Structure)
      return readRecordData((Structure) v2, section, channel);

    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section) :
      new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);

    return readData(layout, dataType, channel);
  }

  private long readRecordData(ucar.nc2.Structure s, Section section, WritableByteChannel out) throws java.io.IOException, InvalidRangeException {
    long count = 0;

    /* RegularIndexer index = new RegularIndexer( s.getShape(), recsize, recStart, section, recsize);
    while (index.hasNext()) {
       Indexer.Chunk chunk = index.next();
       count += raf.readBytes( out, chunk.getFilePos(), chunk.getNelems() * s.getElementSize());
     }  */

    // not sure this works but should give an idea of timing
    Range recordRange = section.getRange(0);
    int stride = recordRange.stride();
    if (stride == 1) {
      int first = recordRange.first();
      int n = recordRange.length();
      if (false) System.out.println(" read record " + first+" "+ n * header.recsize+" bytes ");
      return raf.readToByteChannel(out, header.recStart + first * header.recsize, n * header.recsize);

    }  else {
      for (int recnum = recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
        if (debugRecord) System.out.println(" read record " + recnum);
        raf.seek(header.recStart + recnum * header.recsize); // where the record starts
        count += raf.readToByteChannel(out, header.recStart + recnum * header.recsize, header.recsize);
      }
    }

    return count;
  }

  // convert byte array to char array, assuming UTF-8 encoding

  static protected char[] convertByteToCharUTF(byte[] byteArray) {
    Charset c = Charset.forName("UTF-8");
    CharBuffer output = c.decode(ByteBuffer.wrap(byteArray));
    return output.array();
  }

  // convert char array to byte array, assuming UTF-8 encoding
  static protected byte[] convertCharToByteUTF(char[] from) {
    Charset c = Charset.forName("UTF-8");
    ByteBuffer output = c.encode(CharBuffer.wrap(from));
    return output.array();
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // create new file

  protected boolean fill = true;
  protected HashMap dimHash = new HashMap(50);

  public void create(String filename, ucar.nc2.NetcdfFile ncfile, int extra, long preallocateSize, boolean largeFile) throws IOException {
    this.ncfile = ncfile;
    this.readonly = false;

    // finish any structures
    ncfile.finish();

    raf = new ucar.unidata.io.RandomAccessFile(filename, "rw");
    raf.order(RandomAccessFile.BIG_ENDIAN);

    if (preallocateSize > 0) {
      java.io.RandomAccessFile myRaf = raf.getRandomAccessFile();
      myRaf.setLength(preallocateSize);
    }

    header = new N3header();
    header.create(raf, ncfile, extra, largeFile, null);

    //recsize = header.recsize;   // record size
    //recStart = header.recStart; // record variables start here
    //fileUsed = headerParser.getMinLength(); // track what is actually used

    _create(raf);

    if (fill)
      fillNonRecordVariables();
    //else
    //  raf.setMinLength(recStart); // make sure file length is long enough, even if not written to.
  }

  public boolean rewriteHeader(boolean largeFile) throws IOException {
    return header.rewriteHeader(largeFile, null);
  }


  //////////////////////////////////////////////////////////////////////////////////////
  // write

  public void writeData(Variable v2, Section section, Array values) throws java.io.IOException, InvalidRangeException {

    N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
    DataType dataType = v2.getDataType();

    if (v2.isUnlimited()) {
      Range firstRange = section.getRange(0);
      setNumrecs(firstRange.last() + 1);
    }

    if (v2 instanceof Structure) {
      writeRecordData((Structure) v2, section, values);

    } else {
      Layout layout = (!v2.isUnlimited()) ? new LayoutRegular(vinfo.begin, v2.getElementSize(), v2.getShape(), section) :
        new LayoutRegularSegmented(vinfo.begin, v2.getElementSize(), header.recsize, v2.getShape(), section);
      writeData(values, layout, dataType);
    }
  }

  private void writeRecordData(ucar.nc2.Structure s, Section section, Array values) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    if (!(values instanceof ArrayStructure))
      throw new IllegalArgumentException("writeRecordData: data must be ArrayStructure");
    ArrayStructure structureData = (ArrayStructure) values;

    List<Variable> vars = s.getVariables();
    StructureMembers members = structureData.getStructureMembers();

    Range recordRange = section.getRange(0);
    int count = 0;
    for (int recnum = recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      // System.out.println("  wrote "+recnum+" begin at "+begin);

      // loop over members
      for (Variable v2 : vars) {
        StructureMembers.Member m = members.findMember(v2.getShortName());
        if (null == m)
          continue; // this means that the data is missing from the ArrayStructure

        N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
        long begin = vinfo.begin + recnum * header.recsize;
        Layout layout = new LayoutRegular(begin, v2.getElementSize(), v2.getShape(), v2.getShapeAsSection());

        // Indexer index =  RegularLayout.factory(begin, v2.getElementSize(), -1, v2.getShape(), v2.getShapeAsSection());

        Array data = structureData.getArray(count, m);
        writeData(data, layout, v2.getDataType());
      }

      count++;
    }
  }


  protected void setNumrecs(int n) throws IOException, InvalidRangeException {
    if (n <= header.numrecs) return;
    int startRec = header.numrecs;

    if (debugSize) System.out.println("extend records to = " + n);
    //fileUsed = recStart + recsize * n;
    header.setNumrecs(n);
    //this.numrecs = n;

    // need to let unlimited dimension know of new shape
    for (Dimension dim : ncfile.getDimensions()) {
      if (dim.isUnlimited())
        dim.setLength(n);
    }

    // need to let all unlimited variables know of new shape
    for (Variable v : ncfile.getVariables()) {
      if (v.isUnlimited()) {
        v.resetShape();
        v.setCachedData(null, false);
      }
    }

    // extend file, handle filling
    if (fill)
      fillRecordVariables(startRec, n);
    else
      raf.setMinLength( header.calcFileSize());
  }

  /**
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter.  Strings are padded to 4 byte boundaries, ok to use padding if it exists.
   * For numerics: must have same number of values.
   *
   * @param v2  variable, or null for fglobal attribute
   * @param att replace with this value
   * @throws IOException
   */
  public void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException {
    header.updateAttribute(v2, att);
  }

  /////////////////////////////////////////////////////////////

  // fill buffer with fill value

  protected void fillNonRecordVariables() throws IOException {
    // run through each variable
    for (Variable v : ncfile.getVariables()) {
      if (v.isUnlimited()) continue;
      try {
        writeData(v, v.getShapeAsSection(), makeConstantArray(v));
      } catch (InvalidRangeException e) {
        e.printStackTrace();  // shouldnt happen
      }
    }
  }

  protected void fillRecordVariables(int recStart, int recEnd) throws IOException, InvalidRangeException {
    // do each record completely, should be a bit more efficient

    for (int i = recStart; i < recEnd; i++) { // do one record at a time
      Range r = new Range(i, i);

      // run through each variable
      for (Variable v : ncfile.getVariables()) {
        if (!v.isUnlimited() || (v instanceof Structure)) continue;
        Section recordSection = new Section( v.getRanges());
        recordSection.setRange(0, r);
        writeData(v, recordSection, makeConstantArray(v));
      }
    }
  }

  private Array makeConstantArray(Variable v) {
    Class classType = v.getDataType().getPrimitiveClassType();
    //int [] shape = v.getShape();
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
      storageP[0] = (att != null) && (att.getStringValue().length() > 0) ? att.getStringValue().charAt(0) : NC_FILL_CHAR;
      storage = storageP;
    }

    return Array.factoryConstant(classType, v.getShape(), storage);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean syncExtend() throws IOException {
    boolean result = header.synchNumrecs();
    if (result && log.isDebugEnabled())
      log.debug(" N3iosp syncExtend " + raf.getLocation() + " numrecs =" + header.numrecs);
    return result;
  }

  @Override
  public boolean sync() throws IOException {
    if (syncExtendOnly)
      return syncExtend();

    if (lastModified == 0) // ?? HttpRandomAccessFile
      return false;

    File file = new File(raf.getLocation());
    if (file.exists()) {
      long currentModified = file.lastModified();
      if (currentModified == lastModified)
        return false;

      // so things have been modified, heres where we need to reread the header !!
      ncfile.empty();
      open(raf, ncfile, null);
      if (log.isDebugEnabled())
        log.debug(" N3iosp resynced " + raf.getLocation() + " currentModified=" + currentModified + " lastModified= " + lastModified);
      return true;
    }

    // can this happen ?
    throw new IOException("File does not exist");
  }

  public void flush() throws java.io.IOException {
    raf.flush();
    header.writeNumrecs();
    raf.flush();
  }

  public void close() throws java.io.IOException {
    if (raf != null) {
      long size = header.calcFileSize();
      raf.setMinLength( size);
      raf.close();
    }
    raf = null;
  }

  /**
   * Debug info for this object.
   */
  @Override
  public String toStringDebug(Object o) {
    return null;
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (null == header)
      return null;
    if (message == NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)
      return header.makeRecordStructure();
    else if (message == NetcdfFile.IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE)
      return header.removeRecordStructure();

    return null;
  }

  public String getFileTypeId() { return "netCDF"; }

  public String getFileTypeDescription()  { return "NetCDF classic format"; }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff we need the subclass to implement

  /**
   * Read data subset from file for a variable, create primitive array.
   *
   * @param index    handles skipping around in the file.
   * @param dataType dataType of the variable
   * @return primitive array with data read in
   * @throws java.io.IOException on error
   */
  abstract protected Object readData(Layout index, DataType dataType) throws IOException;

  abstract protected long readData(Layout index, DataType dataType, WritableByteChannel out) throws IOException;


  /**
   * Write data subset to file for a variable, create primitive array.
   *
   * @param aa       write data in this Array.
   * @param index    handles skipping around in the file.
   * @param dataType dataType of the variable
   * @throws java.io.IOException on error
   */
  abstract protected void writeData(Array aa, Layout index, DataType dataType) throws IOException;

  abstract protected void _open(ucar.unidata.io.RandomAccessFile raf) throws java.io.IOException;

  abstract protected void _create(ucar.unidata.io.RandomAccessFile raf) throws java.io.IOException;

}
