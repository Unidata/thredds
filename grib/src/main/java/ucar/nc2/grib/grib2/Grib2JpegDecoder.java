/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.grib.grib2;

import jj2000.j2k.quantization.dequantizer.*;
import jj2000.j2k.image.invcomptransf.*;
import jj2000.j2k.fileformat.reader.*;
import jj2000.j2k.codestream.reader.*;
import jj2000.j2k.wavelet.synthesis.*;
import jj2000.j2k.entropy.decoder.*;
import jj2000.j2k.decoder.*;
import jj2000.j2k.image.output.*;
import jj2000.j2k.codestream.*;
import jj2000.j2k.image.*;
import jj2000.j2k.util.*;
import jj2000.j2k.roi.*;
import jj2000.j2k.io.*;

import colorspace.*;
import icc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.IOException;
import java.io.EOFException;
import java.io.ByteArrayInputStream;

/**
 * Adaptation of jj2000.j2k.decoder.Decoder, in order to read input from memory.
 *
 * @author robb kambic
 * @author caron  rewritten Aug 2014
 */
public class Grib2JpegDecoder {
  static private final Logger logger = LoggerFactory.getLogger(Grib2JpegDecoder.class);

  private boolean debug;
  private int rate;

  /**
   * Return the packBytes ie number bytes / number 1,2 or 4
   */
  private int packBytes;

  /**
   * Parses the inputstream to analyze the box structure of the JP2 file.
   */
  private ColorSpace csMap = null;

  /**
   * The exit code of the run method
   */
  private int exitCode;

  /**
   * The parameter list (arguments)
   */
  private ParameterList pl;

  /**
   * Instantiates a decoder object, width the 'argv' command
   * line arguments. It also initializes the default parameters. If the
   * argument list is empty an IllegalArgumentException is thrown. If an
   * error occurs while parsing the arguments error messages are written to
   * stderr and the run exit code is set to non-zero, see getExitCode()
   *
   * @throws IllegalArgumentException If 'argv' is empty
   * @see Grib2JpegDecoder#getExitCode
   */
  public Grib2JpegDecoder(int nbits, boolean debug) {
    this.rate = nbits;
    this.debug = debug;

    // not sure if these are needed in the bowels of jj2000
    String[] argv = new String[6];
    argv[0] = "-rate";
    argv[1] = Integer.toString(nbits);
    argv[2] = "-verbose";
    argv[3] = "off";
    argv[4] = "-debug" ;
    argv[5] = "on" ;

    // Initialize default parameters
    //System.err.println("calling Grib2JpegDecoder with argv argument");
    /*
    The default parameter list (with modules arguments)
   */
    ParameterList defpl = new ParameterList();
    String[][] param = Grib2JpegDecoder.getAllParameters();

    for (int i = param.length - 1; i >= 0; i--) {
      if (param[i][3] != null)
        defpl.put(param[i][0], param[i][3]);
    }

    // Create parameter list using defaults
    pl = new ParameterList(defpl);

    if (argv.length == 0) {
      throw new IllegalArgumentException("No arguments!");
    }

    // Parse arguments from argv
    try {
      pl.parseArgs(argv);
    } catch (StringFormatException e) {
      System.err.println("An error occurred while parsing the arguments:\n" + e.getMessage());
    }
  } // end Grib2JpegDecoder constructor

  /**
   * Returns the exit code of the class. This is only initialized after the
   * constructor and when the run method returns.
   *
   * @return The exit code of the constructor and the run() method.
   */
  public int getExitCode() {
    return exitCode;
  }

  private boolean hasSignedProblem = false;
  public boolean hasSignedProblem() {
    return hasSignedProblem;
  }

  /**
   * Runs the decoder. After completion the exit code is set, a non-zero
   * value indicates that an error occurred.
   *
   * @see #getExitCode
   */
  public void decode(byte buf[]) throws IOException {
    // int dataSize = buf.length;
    final boolean verbose = false;
    int res; // resolution level to reconstruct
    FileFormatReader ff;
    HeaderDecoder hd;
    EntropyDecoder entdec;
    ROIDeScaler roids;
    Dequantizer deq;
    InverseWT invWT;
    InvCompTransf ictransf;
    //ImgWriter imwriter[];
    ImgDataConverter converter;
    DecoderSpecs decSpec;
    BlkImgDataSrc palettized;
    BlkImgDataSrc channels;
    BlkImgDataSrc resampled;
    BlkImgDataSrc color;
    int i;
    int depth[];

    try {

      // create a ByteArrayInputStream from byte array for ISRandomAccessIO
      ByteArrayInputStream bais = new ByteArrayInputStream(buf);
      RandomAccessIO in = new ISRandomAccessIO(bais, buf.length, 1, buf.length);

      // **** File Format ****
      // If the codestream is wrapped in the jp2 fileformat, Read the
      // file format wrapper
      ff = new FileFormatReader(in);
      ff.readFileFormat();
      if (ff.JP2FFUsed) {
        in.seek(ff.getFirstCodeStreamPos());
        System.out.println("ff.JP2FFUsed is used");  // LOOK probably not
      }

      // +----------------------------+
      // | Instantiate decoding chain |
      // +----------------------------+

      // **** Header decoder ****
      // Instantiate header decoder and read main header
      /*
    Information contained in the codestream's headers
   */
      HeaderInfo hi = new HeaderInfo();
      try {
        hd = new HeaderDecoder(in, pl, hi);
      } catch (EOFException e) {
        error("Codestream too short or bad header, unable to decode.", 2, e);
        throw e;
      }

      int nCompCod = hd.getNumComps();
      decSpec = hd.getDecoderSpecs();

      // Get demixed bitdepths
      depth = new int[nCompCod];
      for (i = 0; i < nCompCod; i++) {
        depth[i] = hd.getOriginalBitDepth(i);
      }

      // **** Bit stream reader ****
      BitstreamReaderAgent breader = BitstreamReaderAgent.createInstance(in, hd, pl, decSpec, false, hi);

      // **** Entropy decoder ****
      try {
        entdec = hd.createEntropyDecoder(breader, pl);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate entropy decoder" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
        return;
      }

      // **** ROI de-scaler ****
      try {
        roids = hd.createROIDeScaler(entdec, pl, decSpec);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate roi de-scaler." + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
        return;
      }

      // **** Dequantizer ****
      try {
        deq = hd.createDequantizer(roids, depth, decSpec);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate dequantizer" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2, e);
        return;
      }

      // **** Inverse wavelet transform ***
      try {
        // full page inverse wavelet transform
        invWT = InverseWT.createInstance(deq, decSpec);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate inverse wavelet transform" +
                ((e.getMessage() != null) ?
                        (":\n" + e.getMessage()) : ""), 2, e);
        return;
      }

      res = breader.getImgRes();
      invWT.setImgResLevel(res);

      // **** Data converter **** (after inverse transform module)
      converter = new ImgDataConverter(invWT, 0);

      // **** Inverse component transformation ****
      ictransf = new InvCompTransf(converter, decSpec, depth, pl);

      // **** Color space mapping ****
      if (ff.JP2FFUsed && pl.getParameter("nocolorspace").equals("off")) {
        try {
          csMap = new ColorSpace(in, hd, pl);
          channels = hd.createChannelDefinitionMapper(ictransf, csMap);
          resampled = hd.createResampler(channels, csMap);
          palettized = hd.createPalettizedColorSpaceMapper(resampled, csMap);
          color = hd.createColorSpaceMapper(palettized, csMap);

        } catch (IllegalArgumentException e) {
          error("Could not instantiate ICC profiler" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 1, e);
          return;
        } catch (ColorSpaceException e) {
          error("error processing jp2 colorspace information" + ((e.getMessage() != null) ? (": " + e.getMessage()) : "    "), 1, e);
          return;
        }
      } else { // Skip colorspace mapping
        color = ictransf;
      }

      // This is the last image in the decoding chain and should be
      // assigned by the last transformation:
      BlkImgDataSrc decodedImage = color;
      if (color == null) {
        decodedImage = ictransf;
      }

      int nCompImg = decodedImage.getNumComps();

      // code to get data
      // **** Decode and write/display result ****

      ImgWriter[] imwriter = new ImgWriter[nCompImg];

      // Now write the image to the array (decodes as needed)
      boolean isSigned;
      for (i = 0; i < imwriter.length; i++) {
        try {
          if (csMap != null) {
            isSigned = csMap.isOutputSigned(i);
            imwriter[i] = new ImgWriterArray(decodedImage, i, csMap.isOutputSigned(i));
            //System.out.println( "csMap!=null" );
          } else {
            isSigned = hd.isOriginalSigned(i);
            imwriter[i] = new ImgWriterArray(decodedImage, i, hd.isOriginalSigned(i));
            //System.out.println( "csMap==null" );
          }
        } catch (IOException e) {
          if (debug) e.printStackTrace();
          return;
        }

        try {
          imwriter[i].writeAll(); // write data to array
          ImgWriterArray iwa = (ImgWriterArray) imwriter[i];
          data = iwa.getGdata();
          // unSigned data processing here
          //System.out.println("[INFO]: isSigned = " + isSigned);
          if (!isSigned) {
            //float unSignIt = (float) java.lang.Math.pow((double) 2.0, fnb - 1); // LOOK WTF ?
            int nb = depth[i];
            int levShift = 1 << (nb - 1);      // check
            if (nb != rate) hasSignedProblem = true;

            for (int j = 0; j < data.length; j++)
              data[j] += levShift;
          }
          packBytes = iwa.getPackBytes();
        } catch (IOException e) {
          if (debug) e.printStackTrace();
          return;
        }
      } // end for(i=0; i<imwriter.length; i++)

      // **** Print some resulting info ****
      if (verbose) {
        // Print actually read bitrate
        // if file format used add the read file format bytes
        float bitrate = breader.getActualRate();
        int numBytes = breader.getActualNbytes();
        if (ff.JP2FFUsed) {
          int imageSize = (int) ((8.0f * numBytes) / bitrate);
          numBytes += ff.getFirstCodeStreamPos();
          bitrate = (numBytes * 8.0f) / imageSize;
        }

        if (pl.getIntParameter("ncb_quit") == -1) {
          System.out.println("Actual bit rate = " + bitrate + " bpp (i.e. " + numBytes + " bytes)");
        } else {
          System.out.println("Number of packet body bytes read = " + numBytes);
        }
      }

    } catch (IllegalArgumentException e) {
      error(e.getMessage(), 2);
      if (debug) e.printStackTrace();


    } catch (RuntimeException e) {
      if (e.getMessage() != null) {
        error("An uncaught runtime exception has occurred:\n" +
                e.getMessage(), 2);
      } else {
        error("An uncaught runtime exception has occurred.", 2);
      }
      throw new IOException(e);

    } catch (Throwable e) {
      throw new IOException(e);
      //error("An uncaught exception has occurred.", 2);
      //if (debug) e.printStackTrace();
    }
  } // end decode

  private void error(String msg, int code) {
    exitCode = code;
    logger.debug(msg);
  }

  private void error(String msg, int code, Throwable ex) {
    exitCode = code;
    logger.debug(msg);
    if (debug) ex.printStackTrace();
  }

  /**
   * Return the packBytes ie number bytes / number 1,2 or 4
   */
  public int getPackBytes() {
    return packBytes;
  }

  /**
   * Return the "raw" decoded data as an int array
   */
  public int[] getGdata() {
    return data;
  }
  private int[] data;

  /**
   * Returns all the parameters used in the decoding chain. It calls
   * parameter from each module and store them in one array (one row per
   * parameter and 4 columns).
   *
   * @return All decoding parameters
   */
  public static String[][] getAllParameters() {
    List<String[]> vec = new ArrayList<>();
    int i;

    String[][] str = BitstreamReaderAgent.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    str = EntropyDecoder.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    str = ROIDeScaler.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    str = Dequantizer.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    str = InvCompTransf.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    str = HeaderDecoder.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    str = ICCProfiler.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    str = jj2000.j2k.decoder.Decoder.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.add(str[i]);

    String[][] result = new String[vec.size()][];
    for (i = vec.size() - 1; i >= 0; i--)  // fill it backwards
      result[i] = vec.get(i);

    return result;
  }

    /**
     * This class extends the ImgWriter abstract class for writing Array .
     * <p/>
     * <u>Data:</u> The image binary values appear one after the other (in raster
     * order) immediately after the last header character ('\n') and are
     * byte-aligned (they are packed into 1,2 or 4 bytes per sample, depending
     * upon the bit-depth value).
     * </p>
     * <p/>
     * <p> If the data is unsigned, level shifting is applied adding 2^(bit depth
     * - 1)</p>
     * <p/>
     * <p><u>NOTE</u>: This class is not thread safe, for reasons of internal
     * buffering.</p>
     *
     * @see ImgWriter
     * @see BlkImgDataSrc
     */
    private static class ImgWriterArray extends ImgWriter {

        /**
         * Whether the data must be signed when writing or not. In the latter
         * case inverse level shifting must be applied
         */
        boolean isSigned;

        /**
         * The bit-depth of the input file (must be between 1 and 31)
         */
        private int bitDepth;

        /**
         * A DataBlk, just used to avoid allocating a new one each time it is
         * needed
         */
        private DataBlkInt db = new DataBlkInt();

        /** The number of fractional bits in the source data */
        //private int fb;

        /**
         * The index of the component from where to get the data
         */
        private int c;

        /**
         * The pack length of one sample (in bytes, according to the output
         * bit-depth
         */
        private int packBytes;

        /**
         * Creates a new writer to the specified Array object, to write data from
         * the specified component.
         * <p/>
         * <p>The size of the image that is written to the file is the size of the
         * component from which to get the data, specified by b, not the size of
         * the source image (they differ if there is some sub-sampling).</p>
         *
         * @param imgSrc   The source from where to get the image data to write.
         * @param c        The index of the component from where to get the data.
         * @param isSigned Whether the data are signed or not (needed only when
         *                 writing header).
         * @see DataBlk
         */
        public ImgWriterArray(BlkImgDataSrc imgSrc, int c, boolean isSigned) throws IOException {
            //Initialize
            this.c = c;
            this.isSigned = isSigned;
            //System.out.println("sign = " + isSigned );
            src = imgSrc;
            w = src.getImgWidth();
            h = src.getImgHeight();
            //System.out.println(" constructor iwa w=" + w +" h=" +h ) ;

            bitDepth = src.getNomRangeBits(this.c);
            if ((bitDepth <= 0) || (bitDepth > 31)) {
                throw new IOException("Array supports only bit-depth between 1 and 31");
            }
            if (bitDepth <= 8) {
                packBytes = 1;
            } else if (bitDepth <= 16) {
                packBytes = 2;
            } else { // <= 31
                packBytes = 4;
            }

        } // end ImgWriterArray

        /**
         * Closes the underlying file or network connection to where the data is
         * written. Any call to other methods of the class become illegal after a
         * call to this one.
         *
         * @throws IOException If an I/O error occurs.
         */
        public void close() throws IOException {
        }

        /**
         * Writes the data of the specified area to the file, coordinates are
         * relative to the current tile of the source. Before writing, the
         * coefficients are limited to the nominal range and packed into 1,2 or 4
         * bytes (according to the bit-depth).
         * <p/>
         * <p>If the data is unisigned, level shifting is applied adding 2^(bit depth - 1)</p>
         * <p/>
         * <p>This method may not be called concurrently from different threads.</p>
         * <p/>
         * <p>If the data returned from the BlkImgDataSrc source is progressive,
         * then it is requested over and over until it is not progressive
         * anymore.</p>
         *
         * @param ulx    The horizontal coordinate of the upper-left corner of the
         *               area to write, relative to the current tile.
         * @param uly    The vertical coordinate of the upper-left corner of the area
         *               to write, relative to the current tile.
         * @param w  The width of the area to write.
         * @param h The height of the area to write.
         * @throws IOException If an I/O error occurs.
         */
        public void write(int ulx, int uly, int w, int h) throws IOException {

            //System.out.println( " ulx=" + ulx +" uly=" + uly +" w=" + w +" h=" + h);
            // Initialize db
            db.ulx = ulx;
            db.uly = uly;
            db.w = w;
            db.h = h;
            if (db.data != null && db.data.length < w * h) {
                // A new one will be allocated by getInternCompData()
                db.data = null;
            }
            // Request the data and make sure it is not
            // progressive
            do {
                db = (DataBlkInt) src.getInternCompData(db, c);
                //System.out.println( "Progressive Comp c =" + c );
            } while (db.progressive);

            //System.out.println( "Comp c =" + c );
            //System.out.println( "db.data.length = " + db.data.length );

        } // end int ulx, int uly, int w, int h

        public void writeAll() throws IOException {
            // Find the list of tile to decode.
            Coord nT = src.getNumTiles(null);
            //System.out.println( "nTiles = " + nT );

            // Loop on vertical tiles
            for (int y = 0; y < nT.y; y++) {
                // Loop on horizontal tiles
                for (int x = 0; x < nT.x; x++) {
                    //System.out.println( "setTiles(x,y) = " + x + ", " + y );
                    src.setTile(x, y);
                    write(0, 0, src.getImgWidth(), src.getImgHeight());
                } // End loop on horizontal tiles
            } // End loop on vertical tiles
        }

        /**
         * Writes the source's current tile to the output. The requests of data
         * issued to the source BlkImgDataSrc object are done by strips, in order
         * to reduce memory usage.
         * <p/>
         * <p>If the data returned from the BlkImgDataSrc source is progressive,
         * then it is requested over and over until it is not progressive
         * anymore.</p>
         *
         * @throws IOException If an I/O error occurs.
         * @see DataBlk
         */
        public void write() throws IOException {
            int i;
            int tIdx = src.getTileIdx();
            int tw = src.getTileCompWidth(tIdx, c);  // Tile width
            int th = src.getTileCompHeight(tIdx, c);  // Tile height
            // Write in strips
            for (i = 0; i < th; i += DEF_STRIP_HEIGHT) {
                write(0, i, tw, (th - i < DEF_STRIP_HEIGHT) ? th - i : DEF_STRIP_HEIGHT);
            }
        }

        /**
         * The pack length of one sample (in bytes, according to the output bit-depth
         */
        public int getPackBytes() {
            return packBytes;
        }

        /**
         * the jpeg data decoded into an int array
         *
         * @return a int[]
         */
        public int[] getGdata() {
            //return gdata;
            return db.data;
        }

        public void flush() {
        }

        /**
         * Returns a string of information about the object, more than 1 line
         * long. The information string includes information from the underlying
         * RandomAccessFile (its toString() method is called in turn).
         *
         * @return A string of information about the object.
         */
        public String toString() {
            return "ImgWriterArray: WxH = " + w + "x" + h + ", Component = " +
                    c + ", Bit-depth = " + bitDepth + ", signed = " + isSigned +
                    "\n";
        }
    } // end ImgWriterArray

} // end Grib2JpegDecoder