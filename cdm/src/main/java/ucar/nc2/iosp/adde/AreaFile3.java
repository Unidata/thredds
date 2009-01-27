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
package ucar.nc2.iosp.adde;

// import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.GVARnav;
import edu.wisc.ssec.mcidas.MOLLnav;
import edu.wisc.ssec.mcidas.MSATnav;
import edu.wisc.ssec.mcidas.RADRnav;
import edu.wisc.ssec.mcidas.RECTnav;
import edu.wisc.ssec.mcidas.GMSXnav;
import edu.wisc.ssec.mcidas.GOESnav;
import edu.wisc.ssec.mcidas.PSnav;
import edu.wisc.ssec.mcidas.MERCnav;
import edu.wisc.ssec.mcidas.LAMBnav;
import edu.wisc.ssec.mcidas.TANCnav;

import ucar.ma2.*;
import ucar.nc2.util.net.URLStreamHandlerFactory;

import java.io.*;
import java.lang.*;
import java.net.URL;

/**
 * AreaFile interface with McIDAS 'area' file format image data. The
 * data is made into an ucar.ma2.Array object.
 *
 * This will allow 'area' format data to be read from disk; the
 * navigation block is made available (see GVARnav for example).
 *
 * This implementation does not do calibration (other than
 * accounting for its presence in the data).  Also, the 'valcode'
 * is not checked on each line.
 *
 * @author John Caron : adapt to MultiArray, add getAreaNavigation
 * @author Tom Whittaker & Tommy Jasmin at SSEC
 *
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class AreaFile3 {

  // these should be static in AREAnav
  private static final int indexLine=1;
  private static final int indexEle=0;
  private static final int indexLat=0;
  private static final int indexLon=1;

  private int numBands, numLines, numElems, dataSize;
  private int datLoc, linePrefixLength, lineDataLength, lineLength;
  private long position = 0;

  private int[] dir;
  private int[] navBlock;
  private int[] cal;
  private int[] aux;

  private Array data;
  private AreaDirectory areaDirectory = null;
  private AREAnav areaNavigation = null;
  private String imageSource;

  private boolean showBBcalc = false;

  /**
   * Creates an AreaFile object that allows reading
   * of McIDAS 'area' file format image data from a URL
   *
   * @param urlString - the adde url String
   *
   */

  public AreaFile3(String urlString) throws java.io.IOException, java.net.MalformedURLException {
    long timeStart = System.currentTimeMillis();

    URL url = URLStreamHandlerFactory.makeURL( urlString);
    DataInputStream af = new DataInputStream(new BufferedInputStream(url.openStream()));
    readMetaData( af);
    readData( af);

    /* long timeEnd = System.currentTimeMillis();
    if (DebugFlags.isSet("AreaFile/readTiming"))
      System.out.println("Time to read AreaFile = "+ .001*(timeEnd - timeStart)+" sec");

    if ((log != null) && Debug.isSet("AreaFile/showFileStats")) {
      log.println("  numBands = "+ numBands);
      log.println("  numLines = "+ numLines);
      log.println("  numElems = "+ numElems);
      log.println("  dataElementSize = "+ dataSize);
      log.println("  memory = "+ numBands*numLines*numElems*dataSize);
    }

    if ((log != null) && Debug.isSet("AreaFile/showAreaNav")) {
      try {
        AREAnav anav = getAreaNavigation();
        log.println("  nav = "+ anav.getClass().getName()+"=="+anav);
        log.println("  bb = "+ getBoundingBox());
      } catch (AreaFileException afe) {
        throw new IOException( afe.getMessage());
      }
    } */
  }

  public Array getData() { return data; }

  /**
   *  Read the metadata for an area file (directory, nav,  and cal).
   */

  private void readMetaData(DataInputStream af) throws java.io.IOException {

    int i;
    boolean flipwords = false;

    // read dir
    dir = new int[AreaFile.AD_DIRSIZE];
    for (i=0; i < AreaFile.AD_DIRSIZE; i++)
      dir[i] = af.readInt();

    position += AreaFile.AD_DIRSIZE * 4;

    // see if the directory needs to be byte-flipped
    if (dir[AreaFile.AD_VERSION] != AreaFile.VERSION_NUMBER) {
      McIDASUtil.flip(dir,0,19);
      // check again
      if (dir[AreaFile.AD_VERSION] != AreaFile.VERSION_NUMBER)
         throw new IOException("Invalid version number - probably not an AREA file");

      // word 20 may contain characters -- if small integer, flip it...
      if ( (dir[20] & 0xffff) == 0) McIDASUtil.flip(dir,20,20);
      McIDASUtil.flip(dir,21,23);
      // words 24-31 contain memo field
      McIDASUtil.flip(dir,32,50);
      // words 51-2 contain cal info
      McIDASUtil.flip(dir,53,55);
      // word 56 contains original source type (ascii)
      McIDASUtil.flip(dir,57,63);
      flipwords = true;
    }
    try {
      areaDirectory = new AreaDirectory(dir);
    } catch (AreaFileException afe) {
      throw new IOException( afe.getMessage());
    }

    // pull together some values needed by other methods
    numBands = dir[AreaFile.AD_NUMBANDS];
    numLines = dir[AreaFile.AD_NUMLINES];
    numElems = dir[AreaFile.AD_NUMELEMS];
    dataSize = dir[AreaFile.AD_DATAWIDTH];

    int navLoc = dir[AreaFile.AD_NAVOFFSET];
    int calLoc = dir[AreaFile.AD_CALOFFSET];
    int auxLoc = dir[AreaFile.AD_AUXOFFSET];
    datLoc = dir[AreaFile.AD_DATAOFFSET];

    linePrefixLength = dir[AreaFile.AD_DOCLENGTH] + dir[AreaFile.AD_CALLENGTH] + dir[AreaFile.AD_LEVLENGTH];
    if (dir[AreaFile.AD_VALCODE] != 0) linePrefixLength = linePrefixLength + 4;
    if (linePrefixLength != dir[AreaFile.AD_PFXSIZE])
      throw new IOException("Invalid line prefix length in AREA file.");
    lineDataLength = numBands * numElems * dir[AreaFile.AD_DATAWIDTH];
    lineLength = linePrefixLength + lineDataLength;

    int navbytes = 0, calbytes = 0, auxbytes = 0;
    if (datLoc > 0 && datLoc != McIDASUtil.MCMISSING) {
      navbytes = datLoc - navLoc;
      calbytes = datLoc - calLoc;
      auxbytes = datLoc - auxLoc;
    }
    if (auxLoc > 0 && auxLoc != McIDASUtil.MCMISSING) {
      navbytes = auxLoc - navLoc;
      calbytes = auxLoc - calLoc;
    }
    if (calLoc > 0 && calLoc != McIDASUtil.MCMISSING ) {
      navbytes = calLoc - navLoc;
    }

    // Read in nav block
    if (navLoc > 0 && navbytes > 0) {
      navBlock = new int[navbytes/4];

      long newPosition = (long) navLoc;
      int skipByteCount = (int) (newPosition - position);
      af.skipBytes(skipByteCount);

      for (i=0; i<navbytes/4; i++)
        navBlock[i] = af.readInt();

      if (flipwords) flipnav( navBlock);
      position = navLoc + navbytes;
    }


    // Read in cal block
    if (calLoc > 0 && calbytes > 0) {
      cal = new int[calbytes/4];

      long newPosition = (long)calLoc;
      int skipByteCount = (int) (newPosition - position);
      af.skipBytes(skipByteCount);

      for (i=0; i<calbytes/4; i++)
        cal[i] = af.readInt();

      // if (flipwords) flipcal(cal);
      position = calLoc + calbytes;
    }

    // Read in aux block
    if (auxLoc > 0 && auxbytes > 0) {
      aux = new int[auxbytes/4];
      long newPosition = (long) auxLoc;
      int skipByteCount = (int) (newPosition - position);
      af.skipBytes(skipByteCount);
      for (i = 0; i < auxbytes/4; i++)
        aux[i] = af.readInt();
      position = auxLoc + auxbytes;
    }

    return;
  }

  /**
   * Returns the directory block
   *
   * @return an integer array containing the area directory
   *
   */
  public int[] getDir() {
    return dir;
  }


  /**
   * Returns the AreaDirectory object for this AreaFile
   *
   * @return AreaDirectory
   */
  public AreaDirectory getAreaDirectory() {
    return areaDirectory;
  }

  /**
   * Returns calibration block
   *
   * @return an integer array containing the nav block data
   *
   * @exception AreaFileException if there is a problem
   *                              reading the calibration
   *
   */

  public int[] getCal() throws AreaFileException {
    return cal;
  }


  /**
   * Returns AUX block
   *
   * @return an integer array containing the aux block data
   *
   * @exception AreaFileException if there is a problem
   *                              reading the aux block
   *
   */

  public int[] getAux() throws AreaFileException {
    return aux;
  }

  private void readData(DataInputStream af) {
    int i,j,k;

    // create ucar.ma2.Array as 3D int array
    if (dataSize == 1) {
      // assume unsigned byte, must be promoted (2x storage !!)
      data = new ArrayShort.D3(numBands, numLines, numElems);
    } else if (dataSize == 2) {
      data = new ArrayShort.D3(numBands, numLines, numElems);
    } else if (dataSize == 4) {
      data = new ArrayInt.D3(numBands, numLines, numElems);
    }
    Index ima = data.getIndex();

    for (i = 0; i<numLines; i++) {

      try {
        long newPosition = (long) (datLoc + linePrefixLength + i*lineLength) ;
        int skipByteCount = (int) (newPosition - position);
        af.skipBytes(skipByteCount);
        position = newPosition;

      } catch (IOException e) {
        e.printStackTrace();
        break;
      }

      for (j = 0; j<numElems; j++) {
        for (k=0; k<numBands; k++) {

          if (j > lineDataLength) {
            data.setInt( ima.set(k, i, j), 0);
          } else {

            try {
              if (dataSize == 1) {
                short ival = (short) af.readByte();
                if (ival < 0) ival += 256;
                data.setShort( ima.set(k, i, j), ival);
                position = position + 1;
              }
              if (dataSize == 2) {
                data.setShort( ima.set(k, i, j), af.readShort());
                position = position + 2;
              }
              if (dataSize == 4) {
                data.setInt( ima.set(k, i, j), af.readInt());
                position = position + 4;
              }
            }
            catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }

    }

    return ;
  }

  /**
   * selectively flip the bytes of words in nav block
   */
  private void flipnav(int[] nav) {

    // first word is always the satellite id in ASCII
    // check on which type:

    if (nav[0] == AREAnav.GVAR) {

      McIDASUtil.flip(nav,2,126);
      McIDASUtil.flip(nav,129,254);
      McIDASUtil.flip(nav,257,382);
      McIDASUtil.flip(nav,385,510);
      McIDASUtil.flip(nav,513,638);
    }

    else if (nav[0] == AREAnav.DMSP) {
      McIDASUtil.flip(nav,1,43);
      McIDASUtil.flip(nav,45,51);
    }

    else if (nav[0] == AREAnav.POES) {
      McIDASUtil.flip(nav,1,119);
    }

    else {
      McIDASUtil.flip(nav,1,nav.length-1);
    }

    return;
  }

  /**
   * Return an AREAnav based on the input nav block.
   */
  public AREAnav getAreaNavigation( ) throws AreaFileException {
    if (areaNavigation != null)
      return areaNavigation;

    try {
        switch (navBlock[0]) {
            case AREAnav.GVAR:
                areaNavigation = new GVARnav(navBlock);
                break;
            case AREAnav.MOLL:
                areaNavigation = new MOLLnav(navBlock);
                break;
            case AREAnav.MSAT:
                areaNavigation = new MSATnav(navBlock);
                break;
            case AREAnav.RADR:
                areaNavigation = new RADRnav(navBlock);
                break;
            case AREAnav.RECT:
                areaNavigation = new RECTnav(navBlock);
                break;
            case AREAnav.GMSX:
                areaNavigation = new GMSXnav(navBlock);
                break;
            case AREAnav.GOES:
                areaNavigation = new GOESnav(navBlock);
                break;
            case AREAnav.PS:
                areaNavigation = new PSnav(navBlock);
                break;
            case AREAnav.MERC:
                areaNavigation = new MERCnav(navBlock);
                break;
            case AREAnav.LAMB:
                areaNavigation = new LAMBnav(navBlock);
                break;
            case AREAnav.TANC:
                areaNavigation = new TANCnav(navBlock);
                break;
            default:
                throw new AreaFileException(
                     "AreaFile2.getAreaNav: Unknown navigation type" + navBlock[0]);
        }
    } catch (IllegalArgumentException excp) {
      throw new AreaFileException( "AreaFile2.getAreaNav: bad nav block "+excp.getMessage());
    }

    areaNavigation.setImageStart(dir[5], dir[6]);
    areaNavigation.setRes(dir[11], dir[12]);
    areaNavigation.setStart(1,1);
    areaNavigation.setMag(1,1);

    return areaNavigation;
  }

  public java.awt.geom.Rectangle2D getBoundingBox() throws AreaFileException {
    AREAnav anav = getAreaNavigation();

    //double[][] linelem = makePerimeter(numElems, numLines);
    double[][] linelem = makeArea(numElems, numLines);
    int size = linelem[0].length;

    // convert to lat/lon
    double[][] latlon =  anav.toLatLon(linelem);

    // get bounding box
    double maxLon = -Double.MAX_VALUE;
    double maxLat = Double.MIN_VALUE;
    double minLon = -Double.MAX_VALUE;
    double minLat = Double.MAX_VALUE;
    for (int i=0; i<size; i++) {
      double lat = latlon[indexLat][i];
      double lon = latlon[indexLon][i];

      if (showBBcalc && (i % 300 == 0))
        System.out.println("  "+linelem[0][i]+" (elem) "+ linelem[1][i]+ " (line) = "+
          latlon[0][i]+" (lat) "+latlon[1][i]+" (lon) ");

      if (!Double.isNaN(lat)) {
        if (lat > maxLat) maxLat = lat;
        if (lat < minLat) minLat = lat;
      }
      if (!Double.isNaN(lon)) {
        if (lon > maxLon) maxLon = lon;
        if (lon < minLon) minLon = lon;
      }
    }
    return new java.awt.geom.Rectangle2D.Double(minLon, minLat, maxLon-minLon, maxLat-minLat);
  }

  private double [][] makePerimeter(int numElems, int numLines) {
    int size = 2*(numElems+numLines);
    double[][] linelem = new double[2][size];

    int count = 0;

    // top line
    for (int i=0; i<numElems; i++) {
      linelem[indexLine][count] = 0;
      linelem[indexEle][count] = i;
      count++;
    }

    // bottom line
    for (int i=0; i<numElems; i++) {
      linelem[indexLine][count] = numLines-1;
      linelem[indexEle][count] = i;
      count++;
    }

    // right side
    for (int j=0; j<numLines; j++) {
      linelem[indexLine][count] = j;
      linelem[indexEle][count] = 0;
      count++;
    }

    // left side
    for (int j=0; j<numLines; j++) {
      linelem[indexLine][count] = j;
      linelem[indexEle][count] = numElems-1;
      count++;
    }

    return linelem;
  }

  private double [][] makeArea(int numElems, int numLines) {
    int size = numElems * numLines;
    double[][] linelem = new double[2][size];
    int count = 0;

    // top line
    for (int j=0; j<numLines; j++) {
      for (int i=0; i<numElems; i++) {
        linelem[indexLine][count] = j;
        linelem[indexEle][count] = i;
        count++;
      }
    }

    return linelem;
  }

}
