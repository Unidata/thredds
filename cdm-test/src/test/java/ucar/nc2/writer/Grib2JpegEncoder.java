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

package ucar.nc2.writer;

import jj2000.j2k.JJ2KInfo;
import jj2000.j2k.codestream.writer.CodestreamWriter;
import jj2000.j2k.codestream.writer.FileCodestreamWriter;
import jj2000.j2k.codestream.writer.HeaderEncoder;
import jj2000.j2k.codestream.writer.PktEncoder;
import jj2000.j2k.encoder.Encoder;
import jj2000.j2k.encoder.EncoderSpecs;
import jj2000.j2k.entropy.encoder.EntropyCoder;
import jj2000.j2k.entropy.encoder.PostCompRateAllocator;
import jj2000.j2k.fileformat.writer.FileFormatWriter;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.ImgDataJoiner;
import jj2000.j2k.image.Tiler;
import jj2000.j2k.image.forwcomptransf.ForwCompTransf;
import jj2000.j2k.image.input.ImgReader;
import jj2000.j2k.image.input.ImgReaderPGM;
import jj2000.j2k.image.input.ImgReaderPGX;
import jj2000.j2k.image.input.ImgReaderPPM;
import jj2000.j2k.quantization.quantizer.Quantizer;
import jj2000.j2k.roi.encoder.ROIScaler;
import jj2000.j2k.util.*;
import jj2000.j2k.wavelet.analysis.AnWTFilter;
import jj2000.j2k.wavelet.analysis.ForwardWT;

import java.io.*;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Describe
 *
 * @author caron
 * @since 8/29/2014
 */
public class Grib2JpegEncoder {

  /**
   * The exit code of the run method
   */
  private int exitCode;

  /**
   * The parameter list (arguments)
   */
  //private ParameterList pl;

  /**
   * The default parameter list (arguments)
   */
  //private ParameterList defpl;

  /**
   * Returns the exit code of the class. This is only initialized after the
   * constructor and when the run method returns.
   *
   * @return The exit code of the constructor and the run() method.
   */
  public int getExitCode() {
    return exitCode;
  }

  private ParameterList getParameterList(int nbits) {

        // not sure if these are needed in the bowels of jj2000
    String[] argv = new String[] {
            "-rate", Integer.toString(nbits),
            "-verbose", "off",
            "-file_format", "off",
           // "-lossless", "on",            // "Cannot use '-rate' and '-lossless' option at  the same time.

    };

    // Initialize default parameters
    ParameterList defpl = new ParameterList();
    String[][] param = Encoder.getAllParameters();

    for (int i = param.length - 1; i >= 0; i--) {
      if (param[i][3] != null) {
        defpl.put(param[i][0], param[i][3]);
      }
    }

    // Create parameter list using defaults
    ParameterList pl = new ParameterList(defpl);

    // Parse arguments from argv
    pl.parseArgs(argv);

    return pl;
  }

  int nbits;
  boolean debug;

  public Grib2JpegEncoder(int nbits, boolean debug) {
    this.nbits = nbits;
    this.debug = debug;
  }


  /**
   * Runs the encoder. After completion the exit code is set, a non-zero
   * value indicates that an error ocurred.
   *
   * @see #getExitCode
   */
  public void run() {
    boolean verbose;
    boolean useFileFormat = false;
    boolean pphTile = false;
    boolean pphMain = false;
    boolean tempSop = false;
    boolean tempEph = false;
    ImgReader imreader[];
    String inext, infile;
    StreamTokenizer stok;
    StringTokenizer sgtok;
    int ncomp;
    boolean ppminput;
    Vector imreadervec;
    boolean imsigned[];
    BlkImgDataSrc imgsrc;
    int i;
    int imgcmpidxs[];
    int tw, th;
    int refx, refy;
    int trefx, trefy;
    int pktspertp = 0; // LOOK
    Tiler imgtiler;
    BlkImgDataSrc cursrc;
    ForwCompTransf fctransf;
    ImgDataConverter converter;
    EncoderSpecs encSpec;
    ForwardWT dwt;
    Quantizer quant;
    ROIScaler rois;
    EntropyCoder ecoder;
    PostCompRateAllocator ralloc;
    HeaderEncoder headenc;
    CodestreamWriter bwriter;
    FileFormatWriter ffw;
    // String outname;
    int fileLength;

    ParameterList pl = getParameterList(nbits);
    float rate = nbits; // LOOK ??

    try {

      // **** ImgReader ****
      sgtok = new StringTokenizer(pl.getParameter("i"), ",");
      ncomp = 0;
      ppminput = false;
      imreadervec = new Vector();
      int nTokens = sgtok.countTokens();

      for (int n = 0; n < nTokens; n++) {
        infile = sgtok.nextToken();
        try {
          if (imreadervec.size() < ncomp) {
            error("With PPM input format only 1 input file can be specified", 2);
            return;
          }
          if (infile.lastIndexOf('.') != -1) {
            inext = infile.substring(infile.lastIndexOf('.'),
                    infile.length());
          } else {
            inext = null;
          }

          if (".PGM".equalsIgnoreCase(inext)) { // PGM file
            imreadervec.addElement(new ImgReaderPGM(infile));
            ncomp += 1;
          } else if (".PPM".equalsIgnoreCase(inext)) { // PPM file
            if (ncomp > 0) {
              error("With PPM input format only 1 input " +
                      "file can be specified", 2);
              return;
            }
            imreadervec.addElement(new ImgReaderPPM(infile));
            ppminput = true;
            ncomp += 3;
          } else { // Should be PGX
            imreadervec.addElement(new ImgReaderPGX(infile));
            ncomp += 1;
          }

        } catch (IOException e) {
          error("Could not open or read from file " + infile + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 3);
          if (pl.getParameter("debug").equals("on")) {
            e.printStackTrace();
          } else {
            error("Use '-debug' option for more details", 2);
          }
          return;

        } finally {
          if (exitCode != 0) {
            // Close the other files
            while (imreadervec.size() > 0) {
              try {
                ((ImgReader) imreadervec.elementAt(imreadervec.size() - 1)).close();
                imreadervec.removeElementAt(imreadervec.size() - 1);
              } catch (Exception e) {
              }
            }
          }
        }
      }


      imreader = new ImgReader[imreadervec.size()];
      imreadervec.copyInto(imreader);
      imreadervec.removeAllElements();
      imsigned = new boolean[ncomp];

      // **** ImgDataJoiner (if needed) ****
      if (ppminput || ncomp == 1) { // Just one input
        imgsrc = imreader[0];
        for (i = 0; i < ncomp; i++) {
          imsigned[i] = imreader[0].isOrigSigned(i);
        }
      } else { // More than one reader => join all readers into 1
        imgcmpidxs = new int[ncomp];
        for (i = 0; i < ncomp; i++) {
          imsigned[i] = imreader[i].isOrigSigned(0);
        }
        imgsrc = new ImgDataJoiner(imreader, imgcmpidxs);
      }

      // **** Tiler ****
      // get nominal tile dimensions
      stok = new StreamTokenizer(new StringReader(pl.getParameter("tiles")));
      stok.eolIsSignificant(false);

      stok.nextToken();
      if (stok.ttype != StreamTokenizer.TT_NUMBER) {
        error("An error occurred while parsing the tiles option: " + pl.getParameter("tiles"), 2);
        return;
      }
      tw = (int) stok.nval;
      stok.nextToken();
      if (stok.ttype != StreamTokenizer.TT_NUMBER) {
        error("An error occurred while parsing the tiles option: " + pl.getParameter("tiles"), 2);
        return;
      }
      th = (int) stok.nval;

      // Get image reference point
      sgtok = new StringTokenizer(pl.getParameter("ref"));
      try {
        refx = Integer.parseInt(sgtok.nextToken());
        refy = Integer.parseInt(sgtok.nextToken());
      } catch (NoSuchElementException e) {
        throw new IllegalArgumentException("Error while parsing 'ref' option");
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid number type in 'ref' option");
      }
      if (refx < 0 || refy < 0) {
        throw new IllegalArgumentException("Invalid value in 'ref' option ");
      }

      // Get tiling reference point
      sgtok = new StringTokenizer(pl.getParameter("tref"));
      try {
        trefx = Integer.parseInt(sgtok.nextToken());
        trefy = Integer.parseInt(sgtok.nextToken());
      } catch (NoSuchElementException e) {
        throw new IllegalArgumentException("Error while parsing 'tref' option");
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid number type in " +
                "'tref' option");
      }
      if (trefx < 0 || trefy < 0 || trefx > refx || trefy > refy) {
        throw new IllegalArgumentException("Invalid value in 'tref' " +
                "option ");
      }

      // Instantiate tiler
      try {
        imgtiler = new Tiler(imgsrc, refx, refy, trefx, trefy, tw, th);
      } catch (IllegalArgumentException e) {
        error("Could not tile image" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }
      int ntiles = imgtiler.getNumTiles();

      // **** Encoder specifications ****
      encSpec = new EncoderSpecs(ntiles, ncomp, imgsrc, pl);

      // **** Component transformation ****
      if (ppminput && pl.getParameter("Mct") != null && pl.getParameter("Mct").equals("off")) {
        FacilityManager.getMsgLogger().
                printmsg(MsgLogger.WARNING,
                        "Input image is RGB and no color transform has " +
                                "been specified. Compression performance and " +
                                "image quality might be greatly degraded. Use " +
                                "the 'Mct' option to specify a color transform");
      }
      try {
        fctransf = new ForwCompTransf(imgtiler, encSpec);

      } catch (IllegalArgumentException e) {
        error("Could not instantiate forward component transformation" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** ImgDataConverter ****
      converter = new ImgDataConverter(fctransf);


      // **** ForwardWT ****
      try {
        dwt = ForwardWT.createInstance(converter, pl, encSpec);
      } catch (IllegalArgumentException e) {
        error("Could not instantiate wavelet transform" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** Quantizer ****
      try {
        quant = Quantizer.createInstance(dwt, encSpec);
      } catch (IllegalArgumentException e) {
        error("Could not instantiate quantizer" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** ROIScaler ****
      try {
        rois = ROIScaler.createInstance(quant, pl, encSpec);
      } catch (IllegalArgumentException e) {
        error("Could not instantiate ROI scaler" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** EntropyCoder ****
      try {
        ecoder = EntropyCoder.createInstance(rois, pl, encSpec.cblks,
                encSpec.pss, encSpec.bms,
                encSpec.mqrs, encSpec.rts,
                encSpec.css, encSpec.sss,
                encSpec.lcs, encSpec.tts);
      } catch (IllegalArgumentException e) {
        error("Could not instantiate entropy coder" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      String outname = null;

      // **** CodestreamWriter ****
      try {
        // Rely on rate allocator to limit amount of data
        bwriter = new FileCodestreamWriter(outname, Integer.MAX_VALUE);
      } catch (IOException e) {
        error("Could not open output file" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** Rate allocator ****
      try {
        ralloc = PostCompRateAllocator.createInstance(ecoder, pl, rate,
                bwriter, encSpec);
      } catch (IllegalArgumentException e) {
        error("Could not instantiate rate allocator" + ((e.getMessage() != null) ? (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** HeaderEncoder ****
      headenc = new HeaderEncoder(imgsrc, imsigned, dwt, imgtiler, encSpec, rois, ralloc, pl);
      ralloc.setHeaderEncoder(headenc);

      // **** Write header to be able to estimate header overhead ****
      headenc.encodeMainHeader();

      // **** Initialize rate allocator, with proper header
      // overhead. This will also encode all the data ****
      ralloc.initialize();

      // **** Write header (final) ****
      headenc.reset();
      headenc.encodeMainHeader();

      // Insert header into the codestream
      bwriter.commitBitstreamHeader(headenc);

      // **** Now do the rate-allocation and write result ****
      ralloc.runAndWrite();

      // **** Done ****
      bwriter.close();

      // **** Calculate file length ****
      fileLength = bwriter.getLength();

      // **** Tile-parts and packed packet headers ****
      if (pktspertp > 0 || pphTile || pphMain) {
        int headInc;
        try {
          CodestreamManipulator cm = new CodestreamManipulator(outname, ntiles, pktspertp, pphMain, pphTile, tempSop, tempEph);
          fileLength += cm.doCodestreamManipulation();
          String res = "";
          if (pktspertp > 0) {
            FacilityManager.
                    getMsgLogger().println("Created tile-parts " +
                    "containing at most " +
                    pktspertp +
                    " packets per tile.", 4, 6);
          }
          if (pphTile) {
            FacilityManager.getMsgLogger().
                    println("Moved packet headers " +
                            "to tile headers", 4, 6);
          }
          if (pphMain) {
            FacilityManager.getMsgLogger().
                    println("Moved packet headers " +
                            "to main header", 4, 6);
          }
        } catch (IOException e) {
          error("Error while creating tileparts or packed packet" +
                  " headers" +
                  ((e.getMessage() != null) ?
                          (":\n" + e.getMessage()) : ""), 2);
          if (pl.getParameter("debug").equals("on")) {
            e.printStackTrace();
          } else {
            error("Use '-debug' option for more details", 2);
          }
          return;
        }
      }

      // **** File Format ****
      if (useFileFormat) {
        try {
          int nc = imgsrc.getNumComps();
          int[] bpc = new int[nc];
          for (int comp = 0; comp < nc; comp++) {
            bpc[comp] = imgsrc.getNomRangeBits(comp);
          }

          ffw = new FileFormatWriter(outname, imgsrc.getImgHeight(), imgsrc.getImgWidth(), nc, bpc, fileLength);
          fileLength += ffw.writeFileFormat();
        } catch (IOException e) {
          throw new Error("Error while writing JP2 file format");
        }
      }

      // **** Close image reader(s) ***
      for (i = 0; i < imreader.length; i++) {
        imreader[i].close();
      }

    } catch (Throwable e) {
      error("An unchecked exception has occurred: " + e.getMessage(), 2);
      if (debug) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Prints the error message 'msg' to standard err, prepending "ERROR" to
   * it, and sets the exitCode to 'code'. An exit code different than 0
   * indicates that there where problems.
   *
   * @param msg  The error message
   * @param code The exit code to set
   */
  private void error(String msg, int code) {
    exitCode = code;
    FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, msg);
  }

  /**
   * Prints the warning message 'msg' to standard err, prepending "WARNING"
   * to it.
   *
   * @param msg The error message
   */
  private void warning(String msg) {
    FacilityManager.getMsgLogger().printmsg(MsgLogger.WARNING, msg);
  }

}

