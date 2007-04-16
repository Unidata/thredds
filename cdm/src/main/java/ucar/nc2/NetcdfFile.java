// $Id:NetcdfFile.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2;

import ucar.ma2.*;
import ucar.unidata.util.StringUtil;
import ucar.unidata.io.UncompressInputStream;
import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.nc2.util.DiskCache;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.util.zip.ZipInputStream;
import java.util.zip.GZIPInputStream;
import java.util.regex.*;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

/**
 * Read-only scientific datasets that are accessible through the netCDF API.
 * <p> Be sure to close the file when done, best practive is to enclose in a try/finally block:
 * <pre>
 * NetcdfFile ncfile = null;
 * try {
 * ncfile = NetcdfFile.open(fileName);
 * ...
 * } finally {
 * ncfile.close();
 * }
 * </pre>
 * <p/>
 * <p>Be sure to close the file after opening, eg:
 * <pre>
 *  NetcdfFile ncfile = null;
 * try {
 * ncfile = NetcdfFile.open(fileName);
 * ...
 * } finally {
 * if (null != ncfile) ncfile.close();
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
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class NetcdfFile {
  //static private org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(NetcdfFile.class);
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFile.class);

  static private int default_buffersize = 8092;
  static private ArrayList registeredProviders = new ArrayList();
  static private boolean debugSPI = false, debugCompress = false;
  static boolean debugStructureIterator = false;

  // this is so that we can run without specialized IOServiceProviders, but they will
  // still get automatically loaded if they are present.
  static {
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
   */
  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugSPI = debugFlag.isSet("NetcdfFile/debugSPI");
    debugCompress = debugFlag.isSet("NetcdfFile/debugCompress");
    debugStructureIterator = debugFlag.isSet("NetcdfFile/structureIterator");
    N3header.disallowFileTruncation = debugFlag.isSet("NetcdfFile/disallowFileTruncation");
    N3header.debugHeaderSize = debugFlag.isSet("NetcdfFile/debugHeaderSize");

    H5header.setDebugFlags(debugFlag);
    H5iosp.setDebugFlags(debugFlag);
  }

  /**
   * debugging
   */
  static public void setDebugOutputStream(PrintStream printStream) {
    H5header.setDebugOutputStream(printStream);
  }

  /**
   * Open an existing netcdf file (read only).
   *
   * @param location location of file.
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
   * @throws IOException
   */
  static public NetcdfFile open(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return open(location, -1, cancelTask);
  }

  /**
   * Open an existing file (read only), with option of cancelling, setting the RandomAccessFile buffer size for efficiency.
   *
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException
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
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfFile object, or null if cant find IOServiceProver
   * @throws IOException
   */
  static public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    ucar.unidata.io.RandomAccessFile raf = getRaf(location, buffer_size);

    try {
      return open(raf, location, cancelTask, spiObject);
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
      } catch (IOException e) {
        log.warn("Failed to uncompress " + uriString + "; try it as a regular file.");
       //allow to fall through to open the "compressed" file directly - may be a misnamed suffix
      }

      if (uncompressedFileName != null) { 
        // open uncompressed file as a RandomAccessFile.
        raf = new ucar.unidata.io.RandomAccessFile(uncompressedFileName, "r", buffer_size);

      } else {
        // normal case - not compressed
        raf = new ucar.unidata.io.RandomAccessFile(uriString, "r", buffer_size);
      }
    }

    return raf;
  }

  static private String makeUncompressed(String filename) throws IOException {
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
    } catch (IOException e) {

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
   */
  public static NetcdfFile openInMemory(String location, byte[] data) throws IOException {
    ucar.unidata.io.InMemoryRandomAccessFile raf = new ucar.unidata.io.InMemoryRandomAccessFile(location, data);
    return open(raf, location, null, null);
  }

  private static NetcdfFile open(ucar.unidata.io.RandomAccessFile raf, String location, ucar.nc2.util.CancelTask cancelTask,
          Object spiObject) throws IOException {

    IOServiceProvider spi = null;
    if (debugSPI) System.out.println("NetcdfFile try to open = " + location);

    // avoid opening file more than once, so pass around the raf.
    if (N3header.isValidFile(raf)) {
      spi = SPFactory.getServiceProvider();

    } else if (H5header.isValidFile(raf)) {
      spi = new ucar.nc2.H5iosp();

    } else {
      // look for registered providers
      for (int i = 0; i < registeredProviders.size(); i++) {
        IOServiceProvider registeredSpi = (IOServiceProvider) registeredProviders.get(i);
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

    if (spiObject != null)
      spi.setSpecial(spiObject);

    if (log.isDebugEnabled())
      log.debug("Using IOSP " + spi.getClass().getName());

    return new NetcdfFile(spi, raf, location, cancelTask);
  }

  // experimental - pass in the iosp
  static public NetcdfFile open(String location, String className, int bufferSize, CancelTask cancelTask, String iospParam)
          throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {

    Class iospClass = NetcdfFile.class.getClassLoader().loadClass(className);
    IOServiceProvider spi = (IOServiceProvider) iospClass.newInstance(); // fail fast

    if (iospParam != null)
      spi.setSpecial(iospParam);

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

  static public boolean isValidNetcdfObjectName(String name) {
    Matcher m = objectNamePattern.matcher(name);
    return m.matches();
  }

  /**
   * Valid Netcdf Object name as a egular expression.
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
  protected int cacheState = 0; // 0 = not cached, 1 = NetcdfFileCache, 2 = NetcdfDatasetCache, 3 = Fmrc
  protected IOServiceProvider spi;

  // "global view" is derived from the group information.
  protected ArrayList variables;
  protected ArrayList dimensions;
  protected ArrayList gattributes;

  /**
   * is the dataset already closed?
   */
  public synchronized boolean isClosed() {
    return isClosed;
  }

  /**
   * Close all resources (files, sockets, etc) associated with this file.
   * If the underlying file was acquired, it will be released, otherwise closed.
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
   *
   * @return 0 = not cached, 1 = NetcdfFileCache, 2 = NetcdfDatasetCache, 3 = Fmrc
   */
  public int getCacheState() {
    return cacheState;
  }

  /**
   * Used by NetcdfFileCache.
   */
  protected void setCacheState(int cacheState) {
    this.cacheState = cacheState;
  }

  /**
   * Get the name used in the cache, if any
   */
  public String getCacheName() {
    return cacheName;
  }

  /**
   * Used by NetcdfFileCache. Do not use.
   */
  protected void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  /**
   * Get the dataset location. This is a URL, or a file pathname.
   */
  public String getLocation() {
    return location;
  }

  /**
   * Get the globally unique dataset identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Get the human-readable title.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Get the root group.
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
  public java.util.List getVariables() {
    return new ArrayList(variables);
  }

  /**
   * Retrieve the Variable with the specified (full) name, which is not a member of a Structure.
   *
   * @param name full name, starting from root group.
   * @return the Variable, or null if not found
   */
  public Variable findTopVariable(String name) {
    if (name == null) return null;

    for (int i = 0; i < variables.size(); i++) {
      Variable v = (Variable) variables.get(i);
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
      v = ((Structure) v).findVariable(name);
      if (v == null) return null;
    }
    return v;
  }

  /* public Variable findNestedVariable(String[] names) {
   return findNestedVariable( Arrays.asList( names).iterator());
 }

 public Variable findNestedVariable(Iterator names) {
   if (!names.hasNext()) return null;
   String name = (String) names.next();
   Variable nested = findVariable( name);
   if (nested == null) return null;
   if (!names.hasNext()) return nested;
   if (nested.getDataType() != DataType.STRUCTURE) return null;
   Structure s = (Structure) nested;
   return s.findNestedVariable( names);
 } */

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
  public List getDimensions() {
    return new ArrayList(dimensions);
  }

  /**
   * Retrieve a dimension by fullName.
   *
   * @param name dimension full name, (using parent group names if not in the root group)
   * @return the dimension, or null if not found
   */
  public Dimension findDimension(String name) {
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension d = (Dimension) dimensions.get(i);
      if (name.equals(d.getName()))
        return d;
    }
    return null;
  }

  /**
   * Return true if this file has an unlimited (record) dimension.
   */
  public boolean hasUnlimitedDimension() {
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension d = (Dimension) dimensions.get(i);
      if (d.isUnlimited()) return true;
    }
    return false;
  }

  /**
   * Return the unlimited (record) dimension, or null if not exist.
   * If there are multiple unlimited dimensions, it will return the first one.
   */
  public Dimension getUnlimitedDimension() {
    for (int i = 0; i < dimensions.size(); i++) {
      Dimension d = (Dimension) dimensions.get(i);
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
  public java.util.List getGlobalAttributes() {
    return new ArrayList(gattributes);
  }

  /**
   * Look up global Attribute by (full) name.
   *
   * @param name the name of the attribute
   * @return the attribute, or null if not found
   */
  public Attribute findGlobalAttribute(String name) {
    for (int i = 0; i < gattributes.size(); i++) {
      Attribute a = (Attribute) gattributes.get(i);
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
    for (int i = 0; i < gattributes.size(); i++) {
      Attribute a = (Attribute) gattributes.get(i);
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
      att = findGlobalAttributeIgnoreCase(attName);
    else
      att = v.findAttributeIgnoreCase(attName);

    if ((att != null) && att.isString())
      attValue = att.getStringValue();

    /* if (null == attValue) {                    // not found, look for global attribute
      att = findGlobalAttributeIgnoreCase(attName);
      if ((att != null) && att.isString())
        attValue = att.getStringValue();
    } */

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
   * @throws IOException
   */
  public java.util.List readArrays(java.util.List variables) throws IOException {
    java.util.List result = new java.util.ArrayList();
    for (int i = 0; i < variables.size(); i++) {
      result.add(((Variable) variables.get(i)).read());
    }
    return result;
  }

  /**
   * Read a variable using the given section specification, equivilent to readAllStructures() if
   * its a member of a Structure, or read() otherwise.
   *
   * @param variableSection the constraint expression. This must start with a top variable.
   * @param flatten         if true and its a member of a Structure, remove the surrounding StructureData.
   * @return Array data read.
   * @throws IOException
   * @throws InvalidRangeException
   * @see NCdump#parseVariableSection for syntax of constraint expression
   */
  public Array read(String variableSection, boolean flatten) throws IOException, InvalidRangeException {
    NCdump.CEresult cer = NCdump.parseVariableSection(this, variableSection);
    if (cer.hasInner)
      return cer.v.readAllStructures(cer.ranges, flatten);
    else
      return cer.v.read(cer.ranges);
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
   * Write the NcML representation: dont show coodinate values, use getPathName() for the uri attribute.
   *
   * @param os : write to this Output Stream.
   * @throws IOException
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
   * @throws IOException
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
   * @throws IOException
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
   *
   * @deprecated use NetcdfFile.open( location) or NetcdfFileCache.acquire( location)
   */
  public NetcdfFile(String filename) throws IOException {
    this.location = filename;
    ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(filename, "r");
    this.spi = SPFactory.getServiceProvider();
    spi.open(raf, this, null);
    finish();
  }

  /**
   * This is can only be used for netcdf-3 files served over HTTP
   *
   * @deprecated use NetcdfFile.open( http:location) or NetcdfFileCache.acquire( http:location)
   */
  public NetcdfFile(URL url) throws IOException {
    this.location = url.toString();
    ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.http.HTTPRandomAccessFile(location);
    this.spi = SPFactory.getServiceProvider();
    spi.open(raf, this, null);
    finish();
  }

  /**
   * Open an existing netcdf file (read only), using the specified iosp
   * @param location location of file. This is a URL string, or a local pathname.
   * @throws IOException
   */

  /**
   * Open an existing netcdf file (read only), using the specified iosp.
   *
   * @param iospClassName the name of the class implementing IOServiceProvider
   * @param iospParam     parameter to pass to the IOSP
   * @param location      location of file. This is a URL string, or a local pathname.
   * @param cancelTask    allow user to cancel
   * @throws IOException if error
   */
  protected NetcdfFile(String iospClassName, String iospParam, String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask)
          throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {

    Class iospClass = NetcdfFile.class.getClassLoader().loadClass(iospClassName);
    this.spi = (IOServiceProvider) iospClass.newInstance();
    if (debugSPI) System.out.println("NetcdfFile uses iosp = " + spi.getClass().getName());
    this.spi.setSpecial(iospParam);

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
   * @throws IOException
   */
  protected NetcdfFile(IOServiceProvider spi, ucar.unidata.io.RandomAccessFile raf, String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {

    this.spi = spi;
    this.location = location;

    if (debugSPI) System.out.println("NetcdfFile uses iosp = " + spi.getClass().getName());

    try {
      spi.open(raf, this, cancelTask);
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
   * For subclass construction. Call finish() when completed construction.
   */
  protected NetcdfFile() {
  }

  /**
   * Copy constructor: used by NetcdfDataset.
   * Shares the iosp.
   */
  protected NetcdfFile(NetcdfFile ncfile) {
    this.location = ncfile.getLocation();
    this.id = ncfile.getId();
    this.title = ncfile.getTitle();
    this.spi = ncfile.spi;
  }

  /**
   * Add a group attribute. If group is null, use root group
   */
  public void addAttribute(Group g, Attribute att) {
    if (g == null) g = rootGroup;
    g.addAttribute(att);
  }

  /**
   * Add a group to the parent group. If parent is null, use root group
   */
  public void addGroup(Group parent, Group g) {
    if (parent == null) parent = rootGroup;
    parent.addGroup(g);
  }

  /**
   * Add a shared Dimension to a Group. If group is null, use root group
   */
  public void addDimension(Group g, Dimension d) {
    if (g == null) g = rootGroup;
    g.addDimension(d);
  }

  /**
   * Remove a shared Dimension from a Group by name. If group is null, use root group.
   *
   * @return true if found and removed.
   */
  public boolean removeDimension(Group g, String dimName) {
    if (g == null) g = rootGroup;
    return g.removeDimension(dimName);
  }

  /**
   * Add a Variable to the given group. If group is null, use root group
   */
  public void addVariable(Group g, Variable v) {
    if (g == null) g = rootGroup;
    if (v != null) g.addVariable(v);
  }

  /**
   * Remove a Variable from the given group by name. If group is null, use root group.
   *
   * @return true is variable found and removed
   */
  public boolean removeVariable(Group g, String varName) {
    if (g == null) g = rootGroup;
    return g.removeVariable(varName);
  }

  /**
   * Add a variable attribute.
   */
  public void addVariableAttribute(Variable v, Attribute att) {
    v.addAttribute(att);
  }

  /**
   * Add a Variable to the given structure.
   */
  public void addMemberVariable(Structure s, Variable v) {
    if (v != null) s.addMemberVariable(v);
  }

  /**
   * If there is an unlimited dimension, make all variables that use it into an array of structures.
   * A Variable called "record" is added.
   * You can then access these through the record structure.
   *
   * @return true if record was actually added on this call.
   */
  public boolean addRecordStructure() {
    if (null != getRootGroup().findVariable("record"))
      return false;

    boolean didit = false;
    if ((spi instanceof N3iosp) && hasUnlimitedDimension() && !addedRecordStructure) {
      N3iosp n3iosp = (N3iosp) spi;
      didit = n3iosp.headerParser.addRecordStructure();
      addedRecordStructure = true;
    }
    return didit;
  }

  protected boolean addedRecordStructure = false;

  /**
   * Find out if if it has a record Structure.
   * Optimization for Netcdf-3 files.
   *
   * @return true if it has a record Structure
   */
  public boolean hasRecordStructure() {
    Variable v = findVariable("record");
    return (v != null) && (v.getDataType() == DataType.STRUCTURE);
  }

  /**
   * Replace a Dimension in a Variable.
   *
   * @param v replace in this Variable.
   * @param d replace existing dimension of the same name.
   */
  protected void replaceDimension(Variable v, Dimension d) {
    v.replaceDimension(d);
  }

  /**
   * Replace the group's list of variables. For copy construction.
   */
  protected void replaceGroupVariables(Group g, ArrayList vlist) {
    g.variables = vlist;
  }

  /**
   * Replace the structure's list of variables. For copy construction.
   */
  protected void replaceStructureMembers(Structure s, ArrayList vlist) {
    s.setMemberVariables(vlist);
  }

  /**
   * Set the globally unique dataset identifier.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Set the dataset "human readable" title.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Set the location, a URL or local filename.
   */
  public void setLocation(String location) {
    this.location = location;
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

  /**
   * Finish constructing the object model.
   * This construsts the "global" variables, attributes and dimensions.
   * It also looks for coordinate variables.
   */
  public void finish() {
    variables = new ArrayList();
    gattributes = new ArrayList();
    dimensions = new ArrayList();
    finishGroup(rootGroup);
  }

  /**
   * Completely empty the objects in the netcdf file.
   * Used for rereading the file on a sync().
   */
  public void empty() {
    variables = new ArrayList();
    gattributes = new ArrayList();
    dimensions = new ArrayList();
    rootGroup = null; // dorky - need this for following call
    rootGroup = new Group(this, null, "");
    addedRecordStructure = false;
  }

  private void finishGroup(Group g) {

    variables.addAll(g.variables);
    for (int i = 0; i < g.variables.size(); i++) {
      Variable v = (Variable) g.variables.get(i);
      v.calcIsCoordinateVariable();
      /* if (v.getName().equals("Time") && !v.isCoordinateAxis) {
        System.out.println("hah ");
        v.calcIsCoordinateVariable();
      } */
    }

    for (int i = 0; i < g.attributes.size(); i++) {
      Attribute oldAtt = (Attribute) g.attributes.get(i);
      String newName = makeFullNameWithString(g, oldAtt.getName());
      // newName = createValidNetcdfObjectName( newName);    // LOOK why are we doing this ???
      //System.out.println("  add att="+newName);
      gattributes.add(new Attribute(newName, oldAtt));
    }

    // LOOK this wont match the variables' dimensions if there are groups
    for (int i = 0; i < g.dimensions.size(); i++) {
      Dimension oldDim = (Dimension) g.dimensions.get(i);
      if (oldDim.isShared()) {
        if (g == rootGroup) {
          dimensions.add(oldDim);
        } else {
          String newName = makeFullNameWithString(g, oldDim.getName());
          dimensions.add(new Dimension(newName, oldDim));
        }
      }
    }

    List groups = g.getGroups();
    for (int i = 0; i < groups.size(); i++) {
      Group nested = (Group) groups.get(i);
      finishGroup(nested);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // Service Provider calls
  // ALL IO eventually goes through these calls.
  // LOOK: these should not be public !!! not hitting variable cache
  // used in NetcdfDataset - try to refactor

  // this is for reading non-member variables
  // section is null for full read

  /**
   * do not call this directly, use Variable.read() !!
   */
  public Array readData(ucar.nc2.Variable v, List section) throws IOException, InvalidRangeException {
    return spi.readData(v, section);
  }

  // this is for reading variables that are members of structures
  /**
   * do not call this directly, use Variable.readSection() !!
   */
  public Array readMemberData(ucar.nc2.Variable v, List section, boolean flatten) throws IOException, InvalidRangeException {
    Array result = spi.readNestedData(v, section);

    if (flatten) return result;

    // If flatten is false, wrap the result Array in an ArrayStructureMA
    StructureMembers members = new StructureMembers(v.getName());
    StructureMembers.Member member = new StructureMembers.Member(v.getShortName(), v.getDescription(),
            v.getUnitsString(), v.getDataType(), v.getShape());
    member.setDataObject(result);

    // LOOK this only works for a single structure, what about nested ?
    Range outerRange = (Range) section.get(0);
    return new ArrayStructureMA(members, new int[]{outerRange.length()});
  }

  /**
   * Debug info for this object.
   */
  protected String toStringDebug(Object o) {
    return (spi == null) ? "" : spi.toStringDebug(o);
  }

  /**
   * Show debug / underlying implementation details
   */
  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer(5000);
    sbuff.append("NetcdfFile location= " + getLocation() + "\n");
    sbuff.append("  title= " + getTitle() + "\n");
    sbuff.append("  id= " + getId() + "\n");

    if (spi == null) {
      sbuff.append("  has no iosp!\n");
    } else {
      sbuff.append("  iosp= " + spi.getClass().getName() + "\n\n");
      sbuff.append(spi.getDetailInfo());
    }

    return sbuff.toString();
  }

  /**
   * Is this a Netcdf-3 file ?
   */
  public boolean isNetcdf3FileFormat() {
    return (spi != null) && (spi instanceof N3iosp);
  }

  /**
   * Experimental - do not use
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

  /**
   * debug
   */
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
