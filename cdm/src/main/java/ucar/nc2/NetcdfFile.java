/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import ucar.ma2.*;
import ucar.unidata.util.StringUtil;
import ucar.unidata.io.UncompressInputStream;
import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.nc2.util.DiskCache;
import ucar.nc2.util.CancelTask;
import ucar.nc2.iosp.netcdf3.N3header;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.netcdf3.SPFactory;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.iosp.IOServiceProvider;

import java.util.*;
import java.util.zip.ZipInputStream;
import java.util.zip.GZIPInputStream;
import java.util.regex.*;
import java.net.URL;
import java.io.*;
import java.nio.channels.WritableByteChannel;

/**
 * Read-only scientific datasets that are accessible through the netCDF API.
 * Immutable after setImmutable is called.
 * <p> Be sure to close the file when done, best practice is to enclose in a try/finally block:
 * <pre>
 * NetcdfFile ncfile = null;
 * try {
 *  ncfile = NetcdfFile.open(fileName);
 *  ...
 * } finally {
 *  ncfile.close();
 * }
 * </pre>
 * <p/>
 * <h3>Naming</h3>
 * Each object has a name (aka "full name") that is unique within the entire netcdf file, and
 * a "short name" that is unique within the parent group.
 * These coincide for objects in the root group, and so are backwards compatible with version 3 files.
 * <ol>
 * <li>Variable: group1/group2/varname
 * <li>Structure member Variable: group1/group2/varname.s1.s2
 * <li>Group Attribute: group1/group2@attName
 * <li>Variable Attribute: group1/group2/varName@attName
 * </ol>
 *
 * @author caron
 */

public class NetcdfFile {
  static public final String IOSP_MESSAGE_ADD_RECORD_STRUCTURE = "AddRecordStructure";
  static public final String IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE = "RemoveRecordStructure";

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFile.class);

  static private int default_buffersize = 8092;
  static private ArrayList<IOServiceProvider> registeredProviders = new ArrayList<IOServiceProvider>();
  static private boolean debugSPI = false, debugCompress = false;
  static boolean debugStructureIterator = false;

  // this is so that we can run without specialized IOServiceProviders, but they will
  // still get automatically loaded if they are present.
  static {
    try {
      registerIOProvider("ucar.nc2.iosp.hdf5.H5iosp");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
    try {
      NetcdfFile.class.getClassLoader().loadClass("ucar.grib.grib1.Grib1Input"); // only load if grib.jar is present
      registerIOProvider("ucar.nc2.iosp.grib.Grib1ServiceProvider");
    } catch (Throwable e) {
      // log.warn("Cant load class: " + e);
      log.warn("Cant load class: " + e);
    }
    try {
      NetcdfFile.class.getClassLoader().loadClass("ucar.grib.grib2.Grib2Input"); // only load if grib.jar is present
      registerIOProvider("ucar.nc2.iosp.grib.Grib2ServiceProvider");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
    try {
      NetcdfFile.class.getClassLoader().loadClass("ucar.bufr.BufrInput"); // only load if bufr.jar is present
      registerIOProvider("ucar.nc2.iosp.bufr.BufrIosp");
    } catch (Throwable e) {
      // log.warn("Cant load class: " + e);
      log.warn("Cant load class: " + e);
    }
    try {
      registerIOProvider("ucar.nc2.iosp.gini.Giniiosp");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
    try {
      registerIOProvider("ucar.nc2.iosp.nexrad2.Nexrad2IOServiceProvider");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
    try {
      registerIOProvider("ucar.nc2.iosp.nids.Nidsiosp");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
    try {
      registerIOProvider("ucar.nc2.iosp.dorade.Doradeiosp");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
    try {
      registerIOProvider("ucar.nc2.iosp.dmsp.DMSPiosp");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
    try {
      registerIOProvider("ucar.nc2.iosp.cinrad.Cinrad2IOServiceProvider");
    } catch (Throwable e) {
      log.warn("Cant load class: " + e);
    }
  }

  /**
   * Register an IOServiceProvider, using its class string name.
   *
   * @param className Class that implements IOServiceProvider.
   * @throws IllegalAccessException if class is not accessible.
   * @throws InstantiationException if class doesnt have a no-arg constructor.
   * @throws ClassNotFoundException if class not found.
   */
  static public void registerIOProvider(String className) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
    Class ioClass = NetcdfFile.class.getClassLoader().loadClass(className);
    registerIOProvider(ioClass);
  }

  /**
   * Register an IOServiceProvider. A new instance will be created when one of its files is opened.
   *
   * @param iospClass Class that implements IOServiceProvider.
   * @throws IllegalAccessException if class is not accessible.
   * @throws InstantiationException if class doesnt have a no-arg constructor.
   * @throws ClassCastException     if class doesnt implement IOServiceProvider interface.
   */
  static public void registerIOProvider(Class iospClass) throws IllegalAccessException, InstantiationException {
    IOServiceProvider spi;
    spi = (IOServiceProvider) iospClass.newInstance(); // fail fast
    registeredProviders.add(spi);
  }

  /**
   * debugging
   * @param debugFlag debug flags
   */
  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugSPI = debugFlag.isSet("NetcdfFile/debugSPI");
    debugCompress = debugFlag.isSet("NetcdfFile/debugCompress");
    debugStructureIterator = debugFlag.isSet("NetcdfFile/structureIterator");
    N3header.disallowFileTruncation = debugFlag.isSet("NetcdfFile/disallowFileTruncation");
    N3header.debugHeaderSize = debugFlag.isSet("NetcdfFile/debugHeaderSize");

    //H5header.setDebugFlags(debugFlag);  LOOK
    //H5iosp.setDebugFlags(debugFlag);
  }

  /**
   * debugging
   * @param printStream write to this stream.
   */
  static public void setDebugOutputStream(PrintStream printStream) {
    H5iosp.setDebugOutputStream(printStream);
  }

  /**
   * Set properties. Currently recognized:
   *   "syncExtendOnly", "true" or "false" (default).  if true, can only extend file on a sync.
   *
   * @param name name of property
   * @param value value of property
   */
  static public void setProperty( String name, String value) {
    N3iosp.setProperty( name, value);
  }

  /**
   * Open an existing netcdf file (read only).
   *
   * @param location location of file.
   * @return the NetcdfFile.
   * @throws java.io.IOException if error
   */
  public static NetcdfFile open(String location) throws IOException {
    return open(location, null);
  }

  /**
   * Open an existing file (read only), with option of cancelling.
   *
   * @param location   location of the file.
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  static public NetcdfFile open(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return open(location, -1, cancelTask);
  }

  /**
   * Open an existing file (read only), with option of cancelling, setting the RandomAccessFile buffer size for efficiency.
   *
   * @param location location of file.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  static public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return open(location, buffer_size, cancelTask, null);
  }

  /**
   * Open an existing file (read only), with option of cancelling, setting the RandomAccessFile buffer size for efficiency,
   * with an optional special object for the iosp.
   *
   * @param location    location of file. This may be a
   *                    <ol>
   *                    <li>local netcdf-3 filename (with a file: prefix or no prefix)
   *                    <li>remote netcdf-3 filename (with an http: prefix)
   *                    <li>local netcdf-4 filename (with a file: prefix or no prefix)
   *                    <li>local hdf-5 filename (with a file: prefix or no prefix)
   *                    <li>local iosp filename (with a file: prefix or no prefix)
   *                    </ol>
   *                    If file ends with ".Z", ".zip", ".gzip", ".gz", or ".bz2", it will uncompress/unzip and write to new file without the suffix,
   *                    then use the uncompressed file. It will look for the uncompressed file before it does any of that. Generally it prefers to
   *                    place the uncompressed file in the same directory as the original file. If it does not have write permission on that directory,
   *                    it will use the directory defined by ucar.nc2.util.DiskCache class.
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param iospMessage  special iosp tweaking (sent before open is called), may be null
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException if error
   */
  static public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws IOException {

    ucar.unidata.io.RandomAccessFile raf = getRaf(location, buffer_size);

    try {
      return open(raf, location, cancelTask, iospMessage);
    } catch (IOException ioe) {
      raf.close();
      throw ioe;
    }
  }

  static private ucar.unidata.io.RandomAccessFile getRaf(String location, int buffer_size) throws IOException {

    String uriString = location.trim();

    if (buffer_size <= 0)
      buffer_size = default_buffersize;

    ucar.unidata.io.RandomAccessFile raf;
    if (uriString.startsWith("http:")) { // open through URL
      raf = new ucar.unidata.io.http.HTTPRandomAccessFile(uriString);

    } else {
      // get rid of crappy microsnot \ replace with happy /
      uriString = StringUtil.replace(uriString, '\\', "/");

      if (uriString.startsWith("file:")) {
        uriString = uriString.substring(5);
        /* File file;
        try {
          file = new File( new URI(uriString));
        } catch (Exception e) {
          throw new IOException(e.getMessage()+" uri= "+uriString);
        }
        uriString = file.getAbsolutePath(); */
      }

      String uncompressedFileName = null;
      try {
        uncompressedFileName = makeUncompressed(uriString);
      } catch (Exception e) {
        log.warn("Failed to uncompress " + uriString + " err= "+e.getMessage()+"; try as a regular file.");
       //allow to fall through to open the "compressed" file directly - may be a misnamed suffix
      }

      if (uncompressedFileName != null) { 
        // open uncompressed file as a RandomAccessFile.
        raf = new ucar.unidata.io.RandomAccessFile(uncompressedFileName, "r", buffer_size);
        //raf = new ucar.unidata.io.MMapRandomAccessFile(uncompressedFileName, "r");

      } else {
        // normal case - not compressed
        raf = new ucar.unidata.io.RandomAccessFile(uriString, "r", buffer_size);
        //raf = new ucar.unidata.io.MMapRandomAccessFile(uriString, "r");
      }
    }

    return raf;
  }

  static private String makeUncompressed(String filename) throws Exception {
    int pos = filename.lastIndexOf(".");
    if (pos < 0) return null;

    String suffix = filename.substring(pos + 1);
    String uncompressedFilename = filename.substring(0, pos);

    if (!suffix.equalsIgnoreCase("Z") && !suffix.equalsIgnoreCase("zip") && !suffix.equalsIgnoreCase("gzip")
            && !suffix.equalsIgnoreCase("gz") && !suffix.equalsIgnoreCase("bz2"))
      return null;

    // see if already decompressed, look in cache if need be
    File uncompressedFile = DiskCache.getFileStandardPolicy(uncompressedFilename);
    if (uncompressedFile.exists() && uncompressedFile.length() > 0) {
      if (debugCompress) System.out.println("found uncompressed " + uncompressedFile + " for " + filename);
      return uncompressedFile.getPath();
    }

    // make sure compressed file exists
    File file = new File(filename);
    if (!file.exists())
      return null; // bail out  */

    InputStream in = null;
    FileOutputStream fout = new FileOutputStream(uncompressedFile);

    try {
      if (suffix.equalsIgnoreCase("Z")) {
        in = new UncompressInputStream(  new FileInputStream(filename));
        copy(in, fout, 100000);
        if (debugCompress) System.out.println("uncompressed " + filename + " to " + uncompressedFile);

      } else if (suffix.equalsIgnoreCase("zip")) {
        in = new ZipInputStream(new FileInputStream(filename));
        copy(in, fout, 100000);
        if (debugCompress) System.out.println("unzipped " + filename + " to " + uncompressedFile);

      } else if (suffix.equalsIgnoreCase("bz2")) {
        in = new CBZip2InputStream(new FileInputStream(filename), true);
        copy(in, fout, 100000);
        if (debugCompress) System.out.println("unbzipped " + filename + " to " + uncompressedFile);

      } else if (suffix.equalsIgnoreCase("gzip") || suffix.equalsIgnoreCase("gz")) {

        in = new GZIPInputStream(new FileInputStream(filename));
        copy(in, fout, 100000);

        if (debugCompress) System.out.println("ungzipped " + filename + " to " + uncompressedFile);
      }
    } catch (Exception e) {

      // appears we have to close before we can delete
      if (fout != null) fout.close();
      fout = null;

      // dont leave bad files around
      if (uncompressedFile.exists()) {
        if (!uncompressedFile.delete())
          log.warn("failed to delete uncompressed file (IOException)"+uncompressedFile);
      }
      throw e;

    } finally {
      if (in != null) in.close();
      if (fout != null) fout.close();
    }

    return uncompressedFile.getPath();
  }

  static private void copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
    byte[] buffer = new byte[bufferSize];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) break;
      out.write(buffer, 0, bytesRead);
    }
  }

  /**
   * Open an in-memory netcdf file.
   *
   * @param location location of file, used as the name.
   * @param data     in-memory netcdf file
   * @return memory-resident NetcdfFile
   * @throws java.io.IOException if error
   */
  public static NetcdfFile openInMemory(String location, byte[] data) throws IOException {
    ucar.unidata.io.InMemoryRandomAccessFile raf = new ucar.unidata.io.InMemoryRandomAccessFile(location, data);
    return open(raf, location, null, null);
  }

  /**
   * Read a netcdf file into memory. All reads are then done from memory.
   * @param location location of CDM file, used as the name.
   * @return a NetcdfFile, which is completely in memory
   * @throws IOException if error reading file
   */
  public static NetcdfFile openInMemory(String location) throws IOException {
    File file = new File(location);
    ByteArrayOutputStream bos = new ByteArrayOutputStream( (int) file.length());
    InputStream in = new BufferedInputStream( new FileInputStream( location));
    thredds.util.IO.copy(in, bos);
    return openInMemory(location, bos.toByteArray());
  }


  private static NetcdfFile open(ucar.unidata.io.RandomAccessFile raf, String location, ucar.nc2.util.CancelTask cancelTask,
          Object iospMessage) throws IOException {

    IOServiceProvider spi = null;
    if (debugSPI) System.out.println("NetcdfFile try to open = " + location);

    // avoid opening file more than once, so pass around the raf.
    if (N3header.isValidFile(raf)) {
      spi = SPFactory.getServiceProvider();

    //} else if (H5header.isValidFile(raf)) {
      // spi = new ucar.nc2.iosp.hdf5.H5iosp();

    } else {
      // look for registered providers
      for (IOServiceProvider registeredSpi : registeredProviders) {
        if (debugSPI) System.out.println(" try iosp = " + registeredSpi.getClass().getName());

        if (registeredSpi.isValidFile(raf)) {
          // need a new instance for thread safety
          Class c = registeredSpi.getClass();
          try {
            spi = (IOServiceProvider) c.newInstance();
          } catch (InstantiationException e) {
            throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor."); // shouldnt happen
          } catch (IllegalAccessException e) {
            throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage()); // shouldnt happen
          }
          break;
        }
      }
    }

    if (spi == null) {
      raf.close();
      throw new IOException("Cant read " + location + ": not a valid NetCDF file.");
    }

    if (iospMessage != null)
      spi.sendIospMessage(iospMessage);

    if (log.isDebugEnabled())
      log.debug("Using IOSP " + spi.getClass().getName());

    return new NetcdfFile(spi, raf, location, cancelTask);
  }

  // experimental - pass in the iosp
  static public NetcdfFile open(String location, String iospClassName, int bufferSize, CancelTask cancelTask, Object iospMessage)
          throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {

    Class iospClass = NetcdfFile.class.getClassLoader().loadClass(iospClassName);
    IOServiceProvider spi = (IOServiceProvider) iospClass.newInstance(); // fail fast

    if (iospMessage != null)
      spi.sendIospMessage(iospMessage);

    // get rid of file prefix, if any
    String uriString = location.trim();
    if (uriString.startsWith("file://"))
      uriString = uriString.substring(7);
    else if (uriString.startsWith("file:"))
      uriString = uriString.substring(5);

    // get rid of crappy microsnot \ replace with happy /
    uriString = StringUtil.replace(uriString, '\\', "/");

    if (bufferSize <= 0)
      bufferSize = default_buffersize;
    ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(uriString, "r", bufferSize);

    return new NetcdfFile(spi, raf, location, cancelTask);
  }

  /////////////////////////////////////////////////
  // name pattern matching
  static private Pattern objectNamePattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_@:\\.\\-\\(\\)\\+]*");

  /**
   * Determine if the given name can be used for a Dimension, Attribute, or Variable name.
   * @param name test this.
   * @return  true if valid name.
   */
  static public boolean isValidNetcdfObjectName(String name) {
    Matcher m = objectNamePattern.matcher(name);
    return m.matches();
  }

  /**
   * Valid Netcdf Object name as a regular expression.
   * @return regular expression pattern describing valid Netcdf Object names.
   */
  static public String getValidNetcdfObjectNamePattern() {
    return objectNamePattern.pattern();
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
  static public String createValidNetcdfObjectName(String name) {
    StringBuffer sb = new StringBuffer(name);

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

  ////////////////////////////////////////////////////////////////////////////////////////////////
  protected String location, id, title, cacheName;
  protected Group rootGroup = new Group(this, null, "");
  protected boolean isClosed = false;
  private boolean immutable = false;

  protected int cacheState = 0; // 0 = not cached, 1 = NetcdfFileCache, 2 = NetcdfDatasetCache, 3 = Fmrc
  protected IOServiceProvider spi;

  // "global view" is derived from the group information.
  protected List<Variable> variables;
  protected List<Dimension> dimensions;
  protected List<Attribute> gattributes;

  /**
   * is the dataset already closed?
   * @return true if closed
   */
  public synchronized boolean isClosed() {
    return isClosed;
  }

  /**
   * Close all resources (files, sockets, etc) associated with this file.
   * If the underlying file was acquired, it will be released, otherwise closed.
   * @throws java.io.IOException if error closing
   */
  public synchronized void close() throws java.io.IOException {
    if (getCacheState() == 1) {
      NetcdfFileCache.release(this);
    } else {
      try {
        if ((null != spi) && !isClosed) spi.close();
      } finally {
        isClosed = true;
      }
    }
  }

  /**
   * Get the cache state.
   * @return 0 = not cached, 1 = NetcdfFileCache, 2 = NetcdfDatasetCache, 3 = Fmrc
   */
  public int getCacheState() {
    return cacheState;
  }

  /**
   * Used by NetcdfFileCache.
   * @param cacheState 0 = not cached, 1 = NetcdfFileCache, 2 = NetcdfDatasetCache, 3 = Fmrc
   */
  protected void setCacheState(int cacheState) {
    this.cacheState = cacheState;
  }

  /**
   * Get the name used in the cache, if any.
   * @return name in the cache.
   */
  public String getCacheName() {
    return cacheName;
  }

  /**
   * Used by NetcdfFileCache. Do not use.
   * @param cacheName name in the cache, should be unique for this NetcdfFile. Usually the location.
   */
  protected void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  /**
   * Get the NetcdfFile location. This is a URL, or a file pathname.
   * @return location URL or file pathname.
   */
  public String getLocation() {
    return location;
  }

  /**
   * Get the globally unique dataset identifier
   * @return id, or null if none.
   */
  public String getId() {
    return id;
  }

  /**
   * Get the human-readable title.
   * @return title, or null if none.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Get the root group.
   * @return root group
   */
  public Group getRootGroup() {
    return rootGroup;
  }

  /**
   * Get all of the variables in the file, in all groups.
   * This is part of "version 3 compatibility" interface.
   * Alternatively, use groups.
   *
   * @return List of type Variable.
   */
  public java.util.List<Variable> getVariables() {
    return variables;
  }

  /**
   * Retrieve the Variable with the specified (full) name, which is not a member of a Structure.
   *
   * @param name full name, starting from root group.
   * @return the Variable, or null if not found
   */
  public Variable findTopVariable(String name) {
    if (name == null) return null;

    for (Variable v : variables) {
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }

  /**
   * Find a variable, with the specified (full) name.
   * It may possibly be nested in multiple groups and/or structures.
   *
   * @param fullName eg "group/subgroup/name1.name2.name".
   * @return Variable or null if not found.
   */
  public Variable findVariable(String fullName) {
    StringTokenizer stoke = new StringTokenizer(fullName, ".");
    String selector = stoke.nextToken();
    if (selector == null) return null;

    Variable v = findTopVariable(selector);
    if (v == null) return null;

    while (stoke.hasMoreTokens()) {
      if (!(v instanceof Structure)) return null;
      String name = stoke.nextToken();
      v = ((Structure) v).findVariable(name);  // LOOK fishy
      if (v == null) return null;
    }
    return v;
  }

  /**
   * Get the shared Dimensions used in this file.
   * This is part of "version 3 compatibility" interface.
   * <p> If the dimensions are in a group, the dimension name will have the
   * group name, in order to disambiguate the dimensions. This means that
   * a Variable's dimensions will not match Dimensions in this list.
   * Therefore it is generally better to get the shared Dimensions from the
   * Groups.
   *
   * @return List of type Dimension.
   */
  public List<Dimension> getDimensions() {
    return immutable ? dimensions : new ArrayList<Dimension>(dimensions);
  }

  /**
   * Retrieve a dimension by fullName.
   *
   * @param name dimension full name, (using parent group names if not in the root group)
   * @return the dimension, or null if not found
   */
  public Dimension findDimension(String name) {
    for (Dimension d : dimensions) {
      if (name.equals(d.getName()))
        return d;
    }
    return null;
  }

  /**
   * Return true if this file has an unlimited (record) dimension.
   * @return if this file has an unlimited Dimension(s)
   */
  public boolean hasUnlimitedDimension() {
    return getUnlimitedDimension() != null;
  }

  /**
   * Return the unlimited (record) dimension, or null if not exist.
   * If there are multiple unlimited dimensions, it will return the first one.
   * @return the unlimited Dimension, or null if none.
   */
  public Dimension getUnlimitedDimension() {
    for (Dimension d : dimensions) {
      if (d.isUnlimited()) return d;
    }
    return null;
  }

  /**
   * Returns the set of global attributes associated with this file.
   * This is part of "version 3 compatibility" interface.
   * Alternatively, use groups.
   *
   * @return List of type Attribute
   */
  public java.util.List<Attribute> getGlobalAttributes() {
    return immutable ? gattributes : new ArrayList<Attribute>(gattributes);
  }

  /**
   * Look up global Attribute by (full) name.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findGlobalAttribute(String name) {
    for (Attribute a : gattributes) {
      if (name.equals(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Look up global Attribute by name, ignore case.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findGlobalAttributeIgnoreCase(String name) {
    for (Attribute a : gattributes) {
      if (name.equalsIgnoreCase(a.getName()))
        return a;
    }
    return null;
  }

  /**
   * Find a String-valued global or variable Attribute by
   * Attribute name (ignore case), return the Value of the Attribute.
   * If not found return defaultValue
   *
   * @param v            the variable or null for global attribute
   * @param attName      the (full) name of the attribute, case insensitive
   * @param defaultValue return this if attribute not found
   * @return the attribute value, or defaultValue if not found
   */
  public String findAttValueIgnoreCase(Variable v, String attName, String defaultValue) {
    String attValue = null;
    Attribute att;

    if (v == null)
      att = rootGroup.findAttributeIgnoreCase(attName);
    else
      att = v.findAttributeIgnoreCase(attName);

    if ((att != null) && att.isString())
      attValue = att.getStringValue();

    if (null == attValue)                     // not found, use default
      attValue = defaultValue;

    return attValue;
  }

  ////////////////////////////////////////////////////////////////////////////////
  // public I/O

  /**
   * Do a bulk read on a list of Variables and
   * return a corresponding list of Array that contains the results
   * of a full read on each Variable.
   * This is mostly here so DODSNetcdf can override it with one call to the server.
   *
   * @param variables List of type Variable
   * @return List of Array, one for each Variable in the input.
   * @throws IOException if read error
   */
  public java.util.List<Array> readArrays(java.util.List<Variable> variables) throws IOException {
    java.util.List<Array> result = new java.util.ArrayList<Array>();
    for (Variable variable : variables)
      result.add(variable.read());
    return result;
  }

  /**
   * Read a variable using the given section specification, equivilent to readAllStructures() if
   * its a member of a Structure, or read() otherwise.
   *
   * @param variableSection the constraint expression. This must start with a top variable.
   * @param flatten         if true and its a member of a Structure, remove the surrounding StructureData.
   * @return Array data read.
   * @throws IOException if error
   * @throws InvalidRangeException if variableSection is invalid
   * @see NCdump#parseVariableSection for syntax of constraint expression
   */
  public Array read(String variableSection, boolean flatten) throws IOException, InvalidRangeException {
    NCdump.CEresult cer = NCdump.parseVariableSection(this, variableSection);
    Section s = new Section(cer.ranges);
    if (cer.hasInner){
      return cer.v.readAllStructures(s, flatten);
    } else {
      return cer.v.read(s);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  /**
   * Write CDL representation to OutputStream.
   *
   * @param os     write to this OutputStream
   * @param strict if true, make it stricly CDL, otherwise, add a little extra info
   */
  public void writeCDL(java.io.OutputStream os, boolean strict) {
    PrintStream out = new PrintStream(os);
    toStringStart(out, strict);
    toStringEnd(out);
    out.flush();
  }

  /**
   * CDL representation of Netcdf header info.
   */
  public String toString() {
    ByteArrayOutputStream ba = new ByteArrayOutputStream(40000);
    PrintStream out = new PrintStream(ba);
    writeCDL(out, false);
    out.flush();
    return ba.toString();
  }

  protected void toStringStart(PrintStream out, boolean strict) {
    String name = getLocation();
    if (strict) {
      int pos = name.lastIndexOf('/');
      if (pos < 0) pos = name.lastIndexOf('\\');
      if (pos >= 0) name = name.substring(pos + 1);
      if (name.endsWith(".nc")) name = name.substring(0, name.length() - 3);
      if (name.endsWith(".cdl")) name = name.substring(0, name.length() - 4);
    }
    out.print("netcdf " + name + " {\n");
    rootGroup.writeCDL(out, "", strict);
  }

  protected void toStringEnd(PrintStream out) {
    out.print("}\n");
  }

  /**
   * Write the NcML representation: dont show coodinate values
   *
   * @param os : write to this Output Stream.
   * @param uri use this for the uri attribute; if null use getLocation(). // ??
   * @throws IOException if error
   * @see NCdump#writeNcML
   */
  public void writeNcML(java.io.OutputStream os, String uri) throws IOException {
    NCdump.writeNcML(this, os, false, uri);
  }

  /**
   * Extend the file if needed in a way that is compatible with the current metadata, that is,
   * does not invalidate structural metadata held by the application.
   * For example, if the unlimited dimension has grown.
   * All previous object references (variables, dimensions, etc) remain valid.
   *
   * @return true if file was extended.
   * @throws IOException if error
   */
  public boolean syncExtend() throws IOException {
    if (spi != null)
      return spi.syncExtend();
    return false;
  }

  /**
   * Check if file has changed, and reread metadata if needed.
   * All previous object references (variables, dimensions, etc) may become invalid - you must re-obtain.
   * DO NOT USE THIS ROUTINE YET - NOT FULLY TESTED
   *
   * @return true if file was changed.
   * @throws IOException if error
   */
  public boolean sync() throws IOException {
    if (spi != null)
      return spi.sync();
    return false;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // construction

  /**
   * This is can only be used for local netcdf-3 files.
   * @param filename location
   * @deprecated use NetcdfFile.open( location) or NetcdfFileCache.acquire( location)
   * @throws java.io.IOException if error
   */
  public NetcdfFile(String filename) throws IOException {
    this.location = filename;
    ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(filename, "r");
    //ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.MMapRandomAccessFile(filename, "r");
    this.spi = SPFactory.getServiceProvider();
    spi.open(raf, this, null);
    finish();
  }

  /**
   * This is can only be used for netcdf-3 files served over HTTP
   * @param url HTTP URL location
   * @deprecated use NetcdfFile.open( http:location) or NetcdfFileCache.acquire( http:location)
   * @throws java.io.IOException if error
   */
  public NetcdfFile(URL url) throws IOException {
    this.location = url.toString();
    ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.http.HTTPRandomAccessFile(location);
    this.spi = SPFactory.getServiceProvider();
    spi.open(raf, this, null);
    finish();
  }

  /**
   * Open an existing netcdf file (read only), using the specified iosp.
   * The ClassLoader for the NetcdfFile class is used.
   *
   * @param iospClassName the name of the class implementing IOServiceProvider
   * @param iospParam     parameter to pass to the IOSP (before open is called)
   * @param location      location of file. This is a URL string, or a local pathname.
   * @param buffer_size   use this buffer size on the RandomAccessFile
   * @param cancelTask    allow user to cancel
   * @throws ClassNotFoundException if the iospClassName cannot be found
   * @throws IllegalAccessException if the class or its nullary constructor is not accessible.
   * @throws InstantiationException if the class cannot be instatiated, eg if it has no nullary constructor
   * @throws IOException if I/O error
   */
  protected NetcdfFile(String iospClassName, String iospParam, String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask)
          throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {

    Class iospClass = getClass().getClassLoader().loadClass(iospClassName);
    this.spi = (IOServiceProvider) iospClass.newInstance();
    if (debugSPI) System.out.println("NetcdfFile uses iosp = " + spi.getClass().getName());
    spi.sendIospMessage(iospParam);

    this.location = location;
    ucar.unidata.io.RandomAccessFile raf = getRaf(location, buffer_size);

    try {
      this.spi.open(raf, this, cancelTask);
      finish();
    } catch (IOException e) {
      raf.close();
      throw e;
    }

    if (id == null)
      setId(findAttValueIgnoreCase(null, "_Id", null));
    if (title == null)
      setId(findAttValueIgnoreCase(null, "_Title", null));
  }

  /**
   * Open an existing netcdf file (read only).
   *
   * @param location location of file. This is a URL string, or a local pathname.
   * @param spi use this IOServiceProvider instance
   * @param raf read from this RandomAccessFile
   * @param cancelTask    allow user to cancel
   * @throws IOException if I/O error
   */
  protected NetcdfFile(IOServiceProvider spi, ucar.unidata.io.RandomAccessFile raf, String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {

    this.spi = spi;
    this.location = location;

    if (debugSPI) System.out.println("NetcdfFile uses iosp = " + spi.getClass().getName());

    try {
      spi.open(raf, this, cancelTask);
    } catch (IOException e) {
      raf.close();
      throw e;
    }

    if (id == null)
      setId(findAttValueIgnoreCase(null, "_Id", null));
    if (title == null)
      setId(findAttValueIgnoreCase(null, "_Title", null));

    finish();
  }

  /**
   * For subclass construction. Call finish() when completed construction.
   */
  protected NetcdfFile() {
  }

  /**
   * Copy constructor, used by NetcdfDataset.
   * Shares the iosp.
   * @param ncfile copy from here
   */
  protected NetcdfFile(NetcdfFile ncfile) {
    this.location = ncfile.getLocation();
    this.id = ncfile.getId();
    this.title = ncfile.getTitle();
    this.spi = ncfile.spi;
  }

  /**
   * Add an attribute to a group.
   * @param parent add to this group. If group is null, use root group
   * @param att add this attribute
   */
  public void addAttribute(Group parent, Attribute att) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (parent == null) parent = rootGroup;
    parent.addAttribute(att);
  }

  /**
   * Add a group to the parent group.
   * @param parent add to this group. If group is null, use root group
   * @param g add this group
   */
  public void addGroup(Group parent, Group g) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (parent == null) parent = rootGroup;
    parent.addGroup(g);
  }

  /**
   * Add a shared Dimension to a Group.
   * @param parent add to this group. If group is null, use root group
   * @param d add this Dimension
   */
  public void addDimension(Group parent, Dimension d) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (parent == null) parent = rootGroup;
    parent.addDimension(d);
  }

  /**
   * Remove a shared Dimension from a Group by name.
   * @param g remove from this group. If group is null, use root group
   * @param dimName name of Dimension to remove.
   * @return true if found and removed.
   */
  public boolean removeDimension(Group g, String dimName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (g == null) g = rootGroup;
    return g.removeDimension(dimName);
  }

  /**
   * Add a Variable to the given group.
   * @param g add to this group. If group is null, use root group
   * @param v add this Variable
   */
  public void addVariable(Group g, Variable v) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (g == null) g = rootGroup;
    if (v != null) g.addVariable(v);
  }

  /**
   * Remove a Variable from the given group by name.
   * @param g remove from this group. If group is null, use root group
   * @param varName name of variable to remove.
   * @return true is variable found and removed
   */
  public boolean removeVariable(Group g, String varName) {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (g == null) g = rootGroup;
    return g.removeVariable(varName);
  }

  /**
   * Add a variable attribute.
   * @param v add to this Variable.
   * @param att add this attribute
   */
  public void addVariableAttribute(Variable v, Attribute att) {
    v.addAttribute(att);
  }

  /*
   * Add a Variable to the given structure.
   * @param s add to this Structure
   * @param v add this Variable.
   * @deprecated use Structure.addMemberVariable(StructureMember)
   *
  public void addMemberVariable(Structure s, Variable v) {
    if (v != null) s.addMemberVariable(v);
  } */

  /**
   * Generic way to send a "message" to the underlying IOSP.
   * This message is sent after the file is open. To affect the creation of the file, you must send into the factory method.
   * @param message iosp specific message
   * Special:<ul>
   * <li>IOSP_MESSAGE_ADD_RECORD_STRUCTURE : tells Netcdf-3 files to make record (unlimited) variables into a structure.
   *  return true if it has a Nectdf-3 record structure
   * </ul>
   * @return iosp specific return, may be null
   */
  public Object sendIospMessage( Object message) {
    if (null == message) return null;

    if (message == IOSP_MESSAGE_ADD_RECORD_STRUCTURE) {
      Variable v = rootGroup.findVariable("record");
      boolean gotit = (v != null) && (v instanceof Structure);
      return gotit || makeRecordStructure();

    } else if (message == IOSP_MESSAGE_REMOVE_RECORD_STRUCTURE) {
      Variable v = rootGroup.findVariable( "record");
      boolean gotit = (v != null) && (v instanceof Structure);
      if (gotit) {
        rootGroup.remove( v);
        variables.remove( v);
      }
      return (gotit);
    }

    if (spi != null)
      return spi.sendIospMessage( message);
    return null;
  }

  /**
   * If there is an unlimited dimension, make all variables that use it into a Structure.
   * A Variable called "record" is added.
   * You can then access these through the record structure.
   *
   * @return true if it has a Nectdf-3 record structure
   */
  protected Boolean makeRecordStructure() {
    if (immutable) throw new IllegalStateException("Cant modify");

    Boolean didit = false;
    if ((spi instanceof N3iosp) && hasUnlimitedDimension()) {
      didit = (Boolean) spi.sendIospMessage(IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    }
    return didit;
  }

  //protected boolean addedRecordStructure = false;

  /**
   * Set the globally unique dataset identifier.
   * @param id the id
   */
  public void setId(String id) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.id = id;
  }

  /**
   * Set the dataset "human readable" title.
   * @param title the title
   */
  public void setTitle(String title) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.title = title;
  }

  /**
   * Set the location, a URL or local filename.
   * @param location the location
   */
  public void setLocation(String location) {
    if (immutable) throw new IllegalStateException("Cant modify");
    this.location = location;
  }
 
  /**
   * Make this immutable.
   * @return this
   */
  public NetcdfFile setImmutable() {
    if (immutable) return this;
    immutable = true;
    setImmutable(rootGroup);
    variables = Collections.unmodifiableList(variables);
    dimensions = Collections.unmodifiableList(dimensions);
    gattributes = Collections.unmodifiableList(gattributes);
    return this;
  }

  private void setImmutable(Group g) {
    for (Variable v : g.variables)
      v.setImmutable();

    for (Dimension d : g.dimensions)
      d.setImmutable();

    for (Group nested : g.getGroups())
      setImmutable(nested);

    g.setImmutable();
  }

  /**
   * Completely empty the objects in the netcdf file.
   * Used for rereading the file on a sync().
   */
  public void empty() {
    if (immutable) throw new IllegalStateException("Cant modify");
    variables = new ArrayList<Variable>();
    gattributes = new ArrayList<Attribute>();
    dimensions = new ArrayList<Dimension>();
    rootGroup = null; // dorky - need this for following call
    rootGroup = new Group(this, null, "");
    // addedRecordStructure = false;
  }

  /**
   * Finish constructing the object model.
   * This construsts the "global" variables, attributes and dimensions.
   * It also looks for coordinate variables.
   */
  public void finish() {
    if (immutable) throw new IllegalStateException("Cant modify");
    variables = new ArrayList<Variable>();
    gattributes = new ArrayList<Attribute>();
    dimensions = new ArrayList<Dimension>();
    finishGroup(rootGroup);
  }

  private void finishGroup(Group g) {

    variables.addAll(g.variables);
    /* for (Variable v : g.variables) {
      v.calcIsCoordinateVariable();
    } */

    for (Attribute oldAtt : g.attributes) {
      String newName = makeFullNameWithString(g, oldAtt.getName());
      gattributes.add(new Attribute(newName, oldAtt));
    }

    // LOOK this wont match the variables' dimensions if there are groups
    for (Dimension oldDim : g.dimensions) {
      if (oldDim.isShared()) {
        if (g == rootGroup) {
          dimensions.add(oldDim);
        } else {
          String newName = makeFullNameWithString(g, oldDim.getName());
          dimensions.add(new Dimension(newName, oldDim) );
        }
      }
    }

    List<Group> groups = g.getGroups();
    for (Group nested : groups) {
      finishGroup(nested);
    }

  }

  protected String makeFullNameWithString(Group parent, String name) {
    StringBuffer sbuff = new StringBuffer();
    appendGroupName(sbuff, parent);
    sbuff.append(name);
    return sbuff.toString();
  }

  static protected String makeFullName(Group parent, Variable v) {
    StringBuffer sbuff = new StringBuffer();
    appendGroupName(sbuff, parent);
    appendStructureName(sbuff, v);
    return sbuff.toString();
  }

  static private void appendGroupName(StringBuffer sbuff, Group g) {
    boolean isRoot = g.getParentGroup() == null;
    if (isRoot) return;

    if (g.getParentGroup() != null)
      appendGroupName(sbuff, g.getParentGroup());
    sbuff.append(g.getShortName());
    sbuff.append("/");
  }

  static private void appendStructureName(StringBuffer sbuff, Variable v) {
    if (v.isMemberOfStructure()) {
      appendStructureName(sbuff, v.getParentStructure());
      sbuff.append(".");
    }
    sbuff.append(v.getShortName());
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // Service Provider calls
  // ALL IO eventually goes through these calls.
  // LOOK: these should not be public !!! not hitting variable cache
  // used in NetcdfDataset - try to refactor

  // this is for reading non-member variables
  // section is null for full read

  /**
   * Do not call this directly, use Variable.read() !!
   * Ranges must be filled (no nulls)
   */
  protected Array readData(ucar.nc2.Variable v, Section ranges) throws IOException, InvalidRangeException {
    return spi.readData(v, ranges);
  }

  /** Experimental */
  public long readData(ucar.nc2.Variable v, Section section, WritableByteChannel out)
       throws java.io.IOException, ucar.ma2.InvalidRangeException {

    return spi.readData(v, section, out);
  }

  // this is for reading variables that are members of structures
  /**
   * Do not call this directly, use Variable.readSection() !!
   * Ranges must be filled (no nulls)
   */
  protected Array readMemberData(ucar.nc2.Variable v, Section ranges, boolean flatten) throws IOException, InvalidRangeException {
    Array result = spi.readNestedData(v, ranges);

    if (flatten) return result;

    // If flatten is false, wrap the result Array in an ArrayStructureMA
    StructureMembers members = new StructureMembers(v.getName());
    StructureMembers.Member member = new StructureMembers.Member(v.getShortName(), v.getDescription(),
            v.getUnitsString(), v.getDataType(), v.getShape());
    member.setDataObject(result);

    // LOOK this only works for a single structure, what about nested ?
    // LOOK what about scalar, rank - 0 ??
    Range outerRange = ranges.getRange(0);
    return new ArrayStructureMA(members, new int[]{outerRange.length()});
  }

  /**
   * Access to iosp debugging info.
   * @param o must be a Variable, Dimension, Attribute, or Group
   * @return debug info for this object.
   */
  protected String toStringDebug(Object o) {
    return (spi == null) ? "" : spi.toStringDebug(o);
  }

  /**
   * Access to iosp debugging info.
   * @return debug / underlying implementation details
   */
  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer(5000);
    sbuff.append("NetcdfFile location= ").append(getLocation()).append("\n");
    sbuff.append("  title= ").append(getTitle()).append("\n");
    sbuff.append("  id= ").append(getId()).append("\n");

    if (spi == null) {
      sbuff.append("  has no iosp!\n");
    } else {
      sbuff.append("  iosp= ").append(spi.getClass().getName()).append("\n\n");
      sbuff.append(spi.getDetailInfo());
    }

    return sbuff.toString();
  }

  /*
   * Is this a Netcdf-3 file ?
   *
  public boolean isNetcdf3FileFormat() {
    return (spi != null) && (spi instanceof N3iosp);
  } */

  /**
   * Experimental - DO NOT USE!!!
   * @return the IOSP for this NetcdfFile
   */
  public IOServiceProvider getIosp() {
    return spi;
  }

  // "safety net" use of finalize cf Bloch p 22
  // this will not be called if the file is in the cache, since it wont get GC'd
  protected void finalize() throws Throwable {
    try {
      if (!isClosed) close();
    } finally {
      super.finalize();
    }
  }

  public static void main(String[] arg) throws Exception {
    //NetcdfFile.registerIOProvider( ucar.nc2.grib.GribServiceProvider.class);

    try {
      String filename = "C:/data/test/20060904.1335.n18.nc";
      //String filename = "C:/dev/grib/data/ndfd.wmo";
      //String filename = "c:/data/radar/level2/6500KHGX20000610_000110.raw";
      NetcdfFile ncfile = NetcdfFile.open(filename);
      Attribute att = ncfile.findGlobalAttribute("pass_date\\units");

      System.out.println();
      System.out.println(att);

      //System.out.println( file.toStringV3());
      //file.writeNcML( System.out);
      ncfile.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
