/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.grads;


import ucar.nc2.constants.CDM;
import ucar.unidata.io.KMPMatch;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Class to hold information from a GrADS Data Descriptor File
 *
 * @author Don Murray - CU/CIRES
 * @see "http://www.iges.org/grads/gadoc/descriptorfile.html"
 */
public class GradsDataDescriptorFile {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GradsDataDescriptorFile.class);


  /**
   * DSET identifier
   */
  public static final String DSET = "DSET";

  /**
   * DTYPE identifier
   */
  public static final String DTYPE = "DTYPE";

  /**
   * INDEX identifier
   */
  public static final String INDEX = "INDEX";

  /**
   * TITLE identifier
   */
  public static final String TITLE = "TITLE";

  /**
   * UNDEF identifier
   */
  public static final String UNDEF = "UNDEF";

  /**
   * UNPACK identifier
   */
  public static final String UNPACK = "UNPACK";

  /**
   * FILEHEADER identifier
   */
  public static final String FILEHEADER = "FILEHEADER";

  /**
   * XYHEADER identifier
   */
  public static final String XYHEADER = "XYHEADER";

  /**
   * THEADER identifier
   */
  public static final String THEADER = "THEADER";

  /**
   * HEADERBYTES identifier
   */
  public static final String HEADERBYTES = "HEADERBYTES";

  /**
   * TRAILERBYTES identifier
   */
  public static final String TRAILERBYTES = "TRAILERBYTES";

  /**
   * OPTIONS identifier
   */
  public static final String OPTIONS = "OPTIONS";

  /**
   * XDEF identifier
   */
  public static final String XDEF = "XDEF";

  /**
   * YDEF identifier
   */
  public static final String YDEF = "YDEF";

  /**
   * ZDEF identifier
   */
  public static final String ZDEF = "ZDEF";

  /**
   * TDEF identifier
   */
  public static final String TDEF = "TDEF";

  /**
   * EDEF identifier
   */
  public static final String EDEF = "EDEF";

  /**
   * EDEF identifier
   */
  public static final String PDEF = "PDEF";

  /**
   * ENDEDEF identifier
   */
  public static final String ENDEDEF = "ENDEDEF";

  /**
   * VARS identifier
   */
  public static final String VARS = "VARS";

  /**
   * ENDVARS identifier
   */
  public static final String ENDVARS = "ENDVARS";

  /**
   * y revesed identifier
   */
  private static final String YREV = "YREV";

  /**
   * template identifier
   */
  private static final String TEMPLATE = "TEMPLATE";

  /**
   * chsub identifier
   */
  private static final String CHSUB = "CHSUB";

  /**
   * chsub identifier
   */
  private static final String CHSUB_TEMPLATE_ID = "%ch";

  /**
   * big endian identifier
   */
  private static final String BIG_ENDIAN = "BIG_ENDIAN";

  /**
   * little endian identifier
   */
  private static final String LITTLE_ENDIAN = "LITTLE_ENDIAN";

  /**
   * little endian identifier
   */
  private static final String BYTESWAPPED = "BYTESWAPPED";

  /**
   * sequential identifier
   */
  private static final String SEQUENTIAL = "SEQUENTIAL";

  /**
   * NO template type
   */
  public static final int NO_TEMPLATE = 0;

  /**
   * time template type
   */
  public static final int TIME_TEMPLATE = 1;

  /**
   * Ensemble template type
   */
  public static final int ENS_TEMPLATE = 2;

  /**
   * Ensemble and time template type
   */
  public static final int ENS_TIME_TEMPLATE = 3;

  static private final KMPMatch matchDSET = new KMPMatch("DSET".getBytes(CDM.utf8Charset));
  static private final KMPMatch matchdset = new KMPMatch("dset".getBytes(CDM.utf8Charset));
  static private final KMPMatch matchENDVARS = new KMPMatch("ENDVARS".getBytes(CDM.utf8Charset));
  static private final KMPMatch matchendvars = new KMPMatch("endvars".getBytes(CDM.utf8Charset));
  public static boolean failFast(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    boolean ok = raf.searchForward(matchDSET, 1000); // look in first 1K
    if (!ok) {
      raf.seek(0);
      ok = raf.searchForward(matchdset, 1000); // look in first 1K
      if (!ok) return true;
    }

    long pos = raf.getFilePointer();
    ok = raf.searchForward(matchENDVARS, 20000); // look in next 20K
    if (!ok) {
      raf.seek(pos);
      ok = raf.searchForward(matchendvars, 20000); // look in next 20K
    }
    return !ok;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////

  boolean error;

  /**
   * the file that this relates to
   */
  private String ddFile;

  /**
   * the data file that this points to
   */
  private String dataFile;

  /**
   * Is this a big endian file?
   */
  boolean bigEndian = false;

  /**
   * missing data value
   */
  private double missingData = Double.NaN;

  /**
   * number of xy header bytes
   */
  private int xyHeaderBytes = 0;

  /**
   * number of file header bytes
   */
  private int fileHeaderBytes = 0;

  /**
   * number of time header bytes
   */
  private int timeHeaderBytes = 0;

  /**
   * number of time trilobytes
   */
  private int timeTrailerBytes = 0;

  /**
   * data type
   */
  private String dataType = null;

  /**
   * The list of variables
   */
  List<GradsVariable> variableList;

  /**
   * The list of dimensions
   */
  List<GradsDimension> dimList;

  /**
   * The list of dimensions
   */
  List<GradsAttribute> attrList;

  /**
   * The XDEF dimension
   */
  private GradsDimension xDim;

  /**
   * The YDEF dimension
   */
  private GradsDimension yDim;

  /**
   * The ZDEF dimension
   */
  private GradsDimension zDim;

  /**
   * The TDEF dimension
   */
  private GradsTimeDimension tDim;

  /**
   * The EDEF dimension
   */
  private GradsEnsembleDimension eDim;

  /**
   * The title
   */
  private String title = null;

  /**
   * grids per timestep
   */
  private int gridsPerTimeStep = 0;

  /**
   * timesteps per file
   */
  private int timeStepsPerFile = 0;

  /**
   * is this a template file
   */
  private boolean isTemplate = false;

  /**
   * type of template
   */
  private int templateType = 0;

  /**
   * is this a sequential file
   */
  private boolean isSequential = false;

  /**
   * is y reversed
   */
  private boolean yReversed = false;

  /**
   * the list of filenames that this ctl points to
   */
  List<String> fileNames;

  /**
   * defines a projection
   */
  private boolean hasProjection = false;

  /**
   * list of chsub parameters
   */
  private List<Chsub> chsubs = null;

  /**
   * the path to the ddf
   */
  private String pathToDDF = null;

  /**
   * Create a GradsDataDescriptorFile from the file
   *
   * @param filename the name of the file
   * @throws IOException problem reading/parsing the file
   */
  public GradsDataDescriptorFile(String filename, int maxLines) throws IOException {
    ddFile = filename;
    parseDDF(maxLines);
    if (error) return;
    getFileNames();
  }

  /**
   * Parse the ctl file
   *
   * @throws IOException problem reading the file
   */
  private void parseDDF(int maxLines) throws IOException {

    //long start2 = System.currentTimeMillis();

    variableList = new ArrayList<>();
    dimList = new ArrayList<>();
    attrList = new ArrayList<>();

    // LOOK not using raf - opened file again
    int count = 0;
    try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(ddFile), CDM.utf8Charset))) {
      boolean inVarSection = false;
      boolean inEnsSection = false;
      String line;
      String original;
      GradsDimension curDim = null;

      while ((original = r.readLine()) != null) {
        count++;
        if (count > maxLines) {
          error = true;
          return;
        }

        original = original.trim();
        if (original.isEmpty()) {
          continue;
        }
        line = original.toLowerCase();

        if (line.startsWith("@ ")) {
          attrList.add(GradsAttribute.parseAttribute(original));
          continue;
        }

        // ignore attribute metadata and comments
        if (line.startsWith("*")) {
          continue;
        }

        if (inEnsSection) {
          if (line.startsWith(ENDEDEF.toLowerCase())) {
            inEnsSection = false;
            continue;  // done skipping ensemble definitions
          }
          // parse the ensemble info
        }
        if (inVarSection) {
          if (line.startsWith(ENDVARS.toLowerCase())) {
            inVarSection = false;
            continue;  // done parsing variables
          }
          GradsVariable var = new GradsVariable(original);
          int numLevels = var.getNumLevels();
          if (numLevels == 0) {
            numLevels = 1;
          }
          gridsPerTimeStep += numLevels;

          // parse a variable
          variableList.add(var);

        } else {
          // not in var section or edef section, look for general metadata
          StringTokenizer st = new StringTokenizer(original);

          String label = st.nextToken();

          // TODO: Handle other options
          if (label.equalsIgnoreCase(OPTIONS)) {
            curDim = null;
            while (st.hasMoreTokens()) {
              String token = st.nextToken();
              if (token.equalsIgnoreCase(BIG_ENDIAN)) {
                bigEndian = true;
              } else if (token.equalsIgnoreCase(LITTLE_ENDIAN)) {
                bigEndian = false;
              } else if (token.equalsIgnoreCase(BYTESWAPPED)) {
                swapByteOrder();
              } else if (token.equalsIgnoreCase(YREV)) {
                yReversed = true;
              } else if (token.equalsIgnoreCase(TEMPLATE)) {
                isTemplate = true;
              } else if (token.equalsIgnoreCase(SEQUENTIAL)) {
                isSequential = true;
              }
            }
          } else if (label.equalsIgnoreCase(CHSUB)) {
            int start = Integer.parseInt(st.nextToken());
            int end = Integer.parseInt(st.nextToken());
            String sub = st.nextToken();
            addChsub(new Chsub(start, end, sub));

          } else if (label.equalsIgnoreCase(DSET)) {
            curDim = null;
            dataFile = st.nextToken();

          } else if (label.equalsIgnoreCase(UNDEF)) {
            curDim = null;
            missingData = Double.parseDouble(st.nextToken());

          } else if (label.equalsIgnoreCase(XYHEADER)) {
            curDim = null;
            xyHeaderBytes = Integer.parseInt(st.nextToken());

          } else if (label.equalsIgnoreCase(FILEHEADER)) {
            curDim = null;
            fileHeaderBytes = Integer.parseInt(st.nextToken());

          } else if (label.equalsIgnoreCase(XDEF)) {
            int xSize = Integer.valueOf(st.nextToken());
            String xMapping = st.nextToken();
            xDim = new GradsDimension(label, xSize, xMapping);
            curDim = xDim;
            dimList.add(xDim);

          } else if (label.equalsIgnoreCase(YDEF)) {
            int ySize = Integer.valueOf(st.nextToken());
            String yMapping = st.nextToken();
            yDim = new GradsDimension(label, ySize, yMapping);
            curDim = yDim;
            dimList.add(yDim);

          } else if (label.equalsIgnoreCase(ZDEF)) {
            int zSize = Integer.valueOf(st.nextToken());
            String zMapping = st.nextToken();
            zDim = new GradsDimension(label, zSize, zMapping);
            curDim = zDim;
            dimList.add(zDim);

          } else if (label.equalsIgnoreCase(TDEF)) {
            int tSize = Integer.valueOf(st.nextToken());
            // we can read the following directly
            // since tdef never uses "levels"
            String tMapping = st.nextToken();
            tDim = new GradsTimeDimension(label, tSize, tMapping);
            curDim = tDim;
            dimList.add(tDim);

          } else if (label.equalsIgnoreCase(EDEF)) {
            int eSize = Integer.valueOf(st.nextToken());
            // Check if EDEF entry is the short or extended version
            if (st.nextToken().equalsIgnoreCase(GradsEnsembleDimension.NAMES)) {
              inEnsSection = false;
              String eMapping = GradsEnsembleDimension.NAMES;
              eDim = new GradsEnsembleDimension(label, eSize, eMapping);
              curDim = eDim;
              dimList.add(curDim);

            } else {
              // TODO: handle list of ensembles
              curDim = null;
              inEnsSection = true;
            }

          } else if (label.equalsIgnoreCase(PDEF)) {
            curDim = null;
            hasProjection = true;

          } else if (label.equalsIgnoreCase(VARS)) {
            curDim = null;
            inVarSection = true;

          } else if (label.equalsIgnoreCase(DTYPE)) {
            curDim = null;
            dataType = st.nextToken();

          } else if (label.equalsIgnoreCase(TITLE)) {
            curDim = null;
            title = original.substring(original.indexOf(" ")).trim();

          } else if (curDim != null) {
            curDim.addLevel(label);
          }
          // get the rest of the tokens
          if (curDim != null) {
            while (st.hasMoreTokens()) {
              curDim.addLevel(st.nextToken());
            }
          }
        }

      }  // end parsing loop
      //System.out.println("Time to parse file = "
      //                   + (System.currentTimeMillis() - start2));

      // update the units for the zDimension if they are specified as
      // an attribute
      if (zDim != null) {
        for (GradsAttribute attr : attrList) {
          if (attr.getVariable().equalsIgnoreCase(ZDEF) &&
                  attr.getType().equalsIgnoreCase(GradsAttribute.STRING) &&
                  attr.getName().equalsIgnoreCase("units")) {
            zDim.setUnit(attr.getValue());
            break;
          }
        }
      }

    } catch (IOException ioe) {
      log.error("Error parsing metadata for " + ddFile);
      throw new IOException("error parsing metadata for " + ddFile);
    }
  }

  /**
   * Swap the byte order from the system default
   */
  private void swapByteOrder() {
    // NB: we are setting bigEndian to be opposite the system arch
    String arch = System.getProperty("os.arch");
    if (arch.equals("x86") ||                    // Windows, Linux
            arch.equals("arm") ||                // Window CE
            arch.equals("x86_64") ||         // Windows64, Mac OS-X
            arch.equals("amd64") ||      // Linux64?
            arch.equals("alpha")) {  // Utrix, VAX, DECOS
      bigEndian = true;
    } else {
      bigEndian = false;
    }
  }

  /**
   * Get the dimensions
   *
   * @return the dimensions
   */
  public List<GradsDimension> getDimensions() {
    return dimList;
  }

  /**
   * Get the variables
   *
   * @return the variables
   */
  public List<GradsVariable> getVariables() {
    return variableList;
  }

  /**
   * Get the attributes
   *
   * @return the attributes
   */
  public List<GradsAttribute> getAttributes() {
    return attrList;
  }

  /**
   * Get the ensemble dimension
   *
   * @return the ensemble dimension
   */
  public GradsEnsembleDimension getEnsembleDimension() {
    return eDim;
  }

  /**
   * Get the time dimension
   *
   * @return the time dimension
   */
  public GradsTimeDimension getTimeDimension() {
    return tDim;
  }

  /**
   * Get the Z dimension
   *
   * @return the Z dimension
   */
  public GradsDimension getZDimension() {
    return zDim;
  }

  /**
   * Get the Y dimension
   *
   * @return the Y dimension
   */
  public GradsDimension getYDimension() {
    return yDim;
  }

  /**
   * Get the X dimension
   *
   * @return the X dimension
   */
  public GradsDimension getXDimension() {
    return xDim;
  }

  /**
   * Get the data file path
   *
   * @return the data file path
   */
  public String getDataFile() {
    return dataFile;
  }

  /**
   * Get the data descriptor file path
   *
   * @return the data descriptor file path
   */
  public String getDataDescriptorFile() {
    return ddFile;
  }

  /**
   * Get the missing value
   *
   * @return the missing value
   */
  public double getMissingValue() {
    return missingData;
  }

  /**
   * Get the number of grids per timestep
   *
   * @return the number of grids per timestep
   */
  public int getGridsPerTimeStep() {
    return gridsPerTimeStep;
  }

  /**
   * Get the number of timesteps per file and the starting offset
   *
   * @param filename the filename to check
   * @return the starting index and number of times in that file
   */
  public int[] getTimeStepsPerFile(String filename) {
    if (chsubs != null) {
      for (Chsub ch : chsubs) {
        if (filename.contains(ch.subString)) {
          return new int[]{ch.numTimes, ch.startTimeIndex};
        }
      }
    }
    return new int[]{timeStepsPerFile, 0};
  }


  /**
   * Get whether this is using a template or not
   *
   * @return whether this is using a template or not
   */
  public boolean isTemplate() {
    return isTemplate;
  }

  /**
   * Get the type of template this is
   *
   * @return the type of template
   */
  public int getTemplateType() {
    return templateType;
  }

  /**
   * Get whether this is using a projection or no
   *
   * @return whether this is using a projection or not
   */
  public boolean hasProjection() {
    return hasProjection;
  }

  /**
   * Get whether this is a sequentially written file
   *
   * @return true if this is sequential
   */
  public boolean isSequential() {
    return isSequential;
  }

  /**
   * Get whether y axis is reversed or not
   *
   * @return whether y axis is reversed or not
   */
  public boolean isYReversed() {
    return yReversed;
  }

  /**
   * Get the number of xy header bytes
   *
   * @return the number of xy header bytes
   */
  public int getXYHeaderBytes() {
    return xyHeaderBytes;
  }

  /**
   * Get the number of file header bytes
   *
   * @return the number of file header bytes
   */
  public int getFileHeaderBytes() {
    return fileHeaderBytes;
  }

  /**
   * Get the number of time header bytes
   *
   * @return the number of time header bytes
   */
  public int getTimeHeaderBytes() {
    return timeHeaderBytes;
  }

  /**
   * Get the number of time trailer bytes
   *
   * @return the number of time trailer bytes
   */
  public int getTimeTrailerBytes() {
    return timeTrailerBytes;
  }

  /**
   * Is this a big endian file
   *
   * @return true if big endian
   */
  public boolean isBigEndian() {
    return bigEndian;
  }

  /**
   * Get the title
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Get the data type.  Only support raw binary
   *
   * @return type or null
   */
  public String getDataType() {
    return dataType;
  }

  /**
   * Return a String representation of this object
   *
   * @return a String representation of this object
   */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("Parsed: ");
    buf.append(ddFile);
    buf.append("\n");
    buf.append("Data file: ");
    buf.append(dataFile);
    buf.append("\n");
    for (GradsDimension dim : dimList) {
      buf.append(dim.toString());
    }
    for (GradsVariable var : variableList) {
      buf.append(var.toString());
    }

    return buf.toString();
  }

  /**
   * Get the file name for the particular time and ensemble index
   *
   * @param eIndex ensemble index
   * @param tIndex time index
   * @return the appropriate filename
   */
  public String getFileName(int eIndex, int tIndex) {

    String dataFilePath = dataFile;

    if ((getTemplateType() == ENS_TEMPLATE) || (getTemplateType() == ENS_TIME_TEMPLATE)) {
      dataFilePath = getEnsembleDimension().replaceFileTemplate(dataFilePath, eIndex);
    }

    dataFilePath = getTimeDimension().replaceFileTemplate(dataFilePath, tIndex);
    if ((chsubs != null) && (dataFilePath.contains(CHSUB_TEMPLATE_ID))) {
      for (Chsub ch : chsubs) {
        if ((tIndex >= ch.startTimeIndex) && (tIndex <= ch.endTimeIndex)) {
          dataFilePath = dataFilePath.replace(CHSUB_TEMPLATE_ID, ch.subString);
          break;
        }
      }
    }
    return getFullPath(dataFilePath);
  }

  /**
   * Get the list of filenames
   *
   * @return the filenames
   * @throws IOException file does not exist.
   */
  private List<String> getFileNames() throws IOException {
    if (fileNames == null) {
      fileNames = new ArrayList<>();
      timeStepsPerFile = tDim.getSize();
      if (!isTemplate()) {  // single file
        fileNames.add(getFullPath(getDataFile()));
      } else {               // figure out template type
        long start = System.currentTimeMillis();
        List<String> fileSet = new ArrayList<>();
        String template = getDataFile();
        if (GradsTimeDimension.hasTimeTemplate(template)) {
          if (template.contains(GradsEnsembleDimension.ENS_TEMPLATE_ID)) {
            templateType = ENS_TIME_TEMPLATE;
          } else {
            templateType = TIME_TEMPLATE;
          }
        } else {  // not time - either ens or chsub
          if (template.contains(GradsEnsembleDimension.ENS_TEMPLATE_ID)) {
            templateType = ENS_TEMPLATE;
          } else {
            templateType = TIME_TEMPLATE;
          }
        }
        if (templateType == ENS_TEMPLATE) {
          for (int e = 0; e < eDim.getSize(); e++) {
            fileSet.add(
                    getFullPath(
                            eDim.replaceFileTemplate(template, e)));
          }
        } else if ((templateType == TIME_TEMPLATE)
                || (templateType == ENS_TIME_TEMPLATE)) {
          int numens = (templateType == TIME_TEMPLATE)
                  ? 1
                  : eDim.getSize();
          for (int t = 0; t < tDim.getSize(); t++) {
            for (int e = 0; e < numens; e++) {
              String file = getFileName(e, t);
              if (!fileSet.contains(file)) {
                fileSet.add(file);
              }
            }
          }
          // this'll be a bogus number if chsub was used
          timeStepsPerFile = tDim.getSize()
                  / (fileSet.size() / numens);
        }
        //System.out.println("Time to generate file list = "
        //                   + (System.currentTimeMillis() - start));
        fileNames.addAll(fileSet);
      }
      //long start2 = System.currentTimeMillis();
      // now make sure they exist
      for (String file : fileNames) {
        File f = new File(file);
        if (!f.exists()) {
          log.error("File: " + f + " does not exist");
          throw new IOException("File: " + f + " does not exist");
        }
      }
      //System.out.println("Time to check file list = "
      //                   + (System.currentTimeMillis() - start2));
    }
    return fileNames;
  }

  /**
   * Get the path to the Data Descriptor File
   *
   * @return the path to the Data Descriptor File
   */
  private String getDDFPath() {
    if (pathToDDF == null) {
      int lastSlash = ddFile.lastIndexOf("/");
      if (lastSlash < 0) {
        lastSlash = ddFile.lastIndexOf(File.separator);
      }
      pathToDDF = (lastSlash < 0)
              ? ""
              : ddFile.substring(0, lastSlash + 1);


    }
    return pathToDDF;
  }

  /**
   * Get the full path for a given filename
   *
   * @param filename the raw filename
   * @return the full filename
   */
  private String getFullPath(String filename) {

    String file;
    String ddfPath = getDDFPath();
    if (filename.startsWith("^")) {
      file = filename.replace("^", "");
      file = ddfPath + file;
    } else {
      File f = new File(filename);
      if (!f.isAbsolute()) {
        file = ddfPath + filename;
      } else {
        file = filename;
      }
    }

    return file;
  }

  /**
   * Add a Chsub
   *
   * @param sub the chsub
   */
  private void addChsub(Chsub sub) {
    if (chsubs == null) {
      chsubs = new ArrayList<>();
    }
    chsubs.add(sub);
  }

  /**
   * Class to handle the CHSUB parameters
   */
  protected static class Chsub {

    /**
     * start time index (0 based)
     */
    protected int startTimeIndex = 0;

    /**
     * end time index (0 based)
     */
    protected int endTimeIndex = 0;

    /**
     * number of times
     */
    protected int numTimes = 0;

    /**
     * substitution string
     */
    protected String subString = null;

    /**
     * Create a new Chsub
     *
     * @param start the start index (1 based)
     * @param end   the start index (1 based)
     * @param sub   the subsitution string
     */
    Chsub(int start, int end, String sub) {
      startTimeIndex = start - 1;  // zero based
      endTimeIndex = end - 1;    // zero based
      numTimes = endTimeIndex - startTimeIndex + 1;
      subString = sub;
    }

    /**
     * Get a String representation of this CHSUB
     *
     * @return a String representation of this CHSUB
     */
    public String toString() {
      return "CHSUB " + startTimeIndex + " " + endTimeIndex + " "
              + subString;
    }
  }

}

