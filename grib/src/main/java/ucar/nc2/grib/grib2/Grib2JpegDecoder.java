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

import java.util.*;
import java.io.IOException;
import java.io.EOFException;
import java.io.ByteArrayInputStream;

/**
 * This class is the main class of JJ2000's decoder. It instantiates all
 * objects and performs the decoding operations. It then writes the image to
 * an array for use in Grib2 getData routines
 * <p/>
 * <p>First the decoder should be initialized with a ParameterList object
 * given through the constructor. The when the run() method is invoked and the
 * decoder executes. The exit code of the class can be obtained with the
 * getExitCode() method, after the constructor and after the run method. A
 * non-zero value indicates that an error has occurred.</p>
 * <p/>
 * <p>The decoding chain corresponds to the following sequence of modules:</p>
 * <p/>
 * <ul>
 * <li>BitstreamReaderAgent</li>
 * <li>EntropyDecoder</li>
 * <li>ROIDeScaler</li>
 * <li>Dequantizer</li>
 * <li>InverseWT</li>
 * <li>ImgDataConverter</li>
 * <li>EnumratedColorSpaceMapper, SyccColorSpaceMapper or ICCProfiler</li>
 * <li>ComponentDemixer (if needed)</li>
 * <li>ImgDataAdapter (if ComponentDemixer is needed)</li>
 * <li>ImgWriter</li>
 * <li>BlkImgDataSrcImageProducer</li>
 * </ul>
 * <p/>
 * <p>The 2 last modules cannot be used at the same time and corresponds
 * respectively to the writing of decoded image into a file or the graphical
 * display of this same image.</p>
 * <p/>
 * <p>The behaviour of each module may be modified according to the current
 * tile-component. All the specifications are kept in modules extending
 * ModuleSpec and accessible through an instance of DecoderSpecs class.</p>
 *
 */
public class Grib2JpegDecoder {

  /**
   * Return the packBytes ie number bytes / number 1,2 or 4
   */
  private int packBytes;

  /**
   * the decoded image data
   */
  public int[] data;

  /**
   * Parses the inputstream to analyze the box structure of the JP2
   * file.
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
   * The default parameter list (with modules arguments)
   */
  private ParameterList defpl;

  /**
   * Information contained in the codestream's headers
   */
  private HeaderInfo hi;

  /**
   * The valid list of options prefixes
   */
  private final static char vprfxs[] = {BitstreamReaderAgent.OPT_PREFIX,
          EntropyDecoder.OPT_PREFIX,
          ROIDeScaler.OPT_PREFIX,
          Dequantizer.OPT_PREFIX,
          InvCompTransf.OPT_PREFIX,
          HeaderDecoder.OPT_PREFIX,
          ColorSpaceMapper.OPT_PREFIX
  };

  /**
   * The parameter information for this class
   */
  private final static String[][] pinfo = {
          {"u", "[on|off]",
                  "Prints usage information. " +
                          "If specified all other arguments (except 'v') are ignored", "off"},
          {"v", "[on|off]",
                  "Prints version and copyright information", "off"},
          {"verbose", "[on|off]",
                  "Prints information about the decoded codestream", "on"},
          {"pfile", "<filename>",
                  "Loads the arguments from the specified file. Arguments that are " +
                          "specified on the command line override the ones from the file.\n" +
                          "The arguments file is a simple text file with one argument per " +
                          "line of the following form:\n" +
                          "  <argument name>=<argument value>\n" +
                          "If the argument is of boolean type (i.e. its presence turns a " +
                          "feature on), then the 'on' value turns it on, while the 'off' " +
                          "value turns it off. The argument name does not include the '-' " +
                          "or '+' character. Long lines can be broken into several lines " +
                          "by terminating them with '\\'. Lines starting with '#' are " +
                          "considered as comments. This option is not recursive: any 'pfile' " +
                          "argument appearing in the file is ignored.", null},
          {"res", "<resolution level index>",
                  "The resolution level at which to reconstruct the image " +
                          " (0 means the lowest available resolution whereas the maximum " +
                          "resolution level corresponds to the original image resolution). " +
                          "If the given index" +
                          " is greater than the number of available resolution levels of the " +
                          "compressed image, the image is reconstructed at its highest " +
                          "resolution (among all tile-components). Note that this option" +
                          " affects only the inverse wavelet transform and not the number " +
                          " of bytes read by the codestream parser: this number of bytes " +
                          "depends only on options '-nbytes' or '-rate'.", null},
          {"i", "<filename or url>",
                  "The file containing the JPEG 2000 compressed data. This can be " +
                          "either a JPEG 2000 codestream or a JP2 file containing a " +
                          "JPEG 2000 " +
                          "codestream. In the latter case the first codestream in the file " +
                          "will be decoded. If an URL is specified (e.g., http://...) " +
                          "the data will be downloaded and cached in memory before decoding. " +
                          "This is intended for easy use in applets, but it is not a very " +
                          "efficient way of decoding network served data.", null},
          {"o", "<filename>",
                  "This is the name of the file to which the decompressed image " +
                          "is written. If no output filename is given, the image is " +
                          "displayed on the screen. " +
                          "Output file format is PGX by default. If the extension" +
                          " is '.pgm' then a PGM file is written as output, however this is " +
                          "only permitted if the component bitdepth does not exceed 8. If " +
                          "the extension is '.ppm' then a PPM file is written, however this " +
                          "is only permitted if there are 3 components and none of them has " +
                          "a bitdepth of more than 8. If there is more than 1 component, " +
                          "suffices '-1', '-2', '-3', ... are added to the file name, just " +
                          "before the extension, except for PPM files where all three " +
                          "components are written to the same file.", null},
          {"rate", "<decoding rate in bpp>",
                  "Specifies the decoding rate in bits per pixel (bpp) where the " +
                          "number of pixels is related to the image's original size (Note:" +
                          " this number is not affected by the '-res' option). If it is equal" +
                          "to -1, the whole codestream is decoded. " +
                          "The codestream is either parsed (default) or truncated depending " +
                          "the command line option '-parsing'. To specify the decoding " +
                          "rate in bytes, use '-nbytes' options instead.", "-1"},
          {"nbytes", "<decoding rate in bytes>",
                  "Specifies the decoding rate in bytes. " +
                          "The codestream is either parsed (default) or truncated depending " +
                          "the command line option '-parsing'. To specify the decoding " +
                          "rate in bits per pixel, use '-rate' options instead.", "-1"},
          {"parsing", null,
                  "Enable or not the parsing mode when decoding rate is specified " +
                          "('-nbytes' or '-rate' options). If it is false, the codestream " +
                          "is decoded as if it were truncated to the given rate. If it is " +
                          "true, the decoder creates, truncates and decodes a virtual layer" +
                          " progressive codestream with the same truncation points in each " +
                          "code-block.", "on"},
          {"ncb_quit", "<max number of code blocks>",
                  "Use the ncb and lbody quit conditions. If state information is " +
                          "found for more code blocks than is indicated with this option, " +
                          "the decoder " +
                          "will decode using only information found before that point. " +
                          "Using this otion implies that the 'rate' or 'nbyte' parameter " +
                          "is used to indicate the lbody parameter which is the number of " +
                          "packet body bytes the decoder will decode.", "-1"},
          {"l_quit", "<max number of layers>",
                  "Specifies the maximum number of layers to decode for any code-" +
                          "block", "-1"},
          {"m_quit", "<max number of bit planes>",
                  "Specifies the maximum number of bit planes to decode for any code" +
                          "-block", "-1"},
          {"poc_quit", null,
                  "Specifies the whether the decoder should only decode code-blocks " +
                          "included in the first progression order.", "off"},
          {"one_tp", null,
                  "Specifies whether the decoder should only decode the first " +
                          "tile part of each tile.", "off"},
          {"comp_transf", null,
                  "Specifies whether the component transform indicated in the " +
                          "codestream should be used.", "on"},
          {"debug", null,
                  "Print debugging messages when an error is encountered.", "off"},
          {"cdstr_info", null,
                  "Display information about the codestream. This information is: " +
                          "\n- Marker segments value in main and tile-part headers," +
                          "\n- Tile-part length and position within the code-stream.", "off"},
          {"nocolorspace", null,
                  "Ignore any colorspace information in the image.", "off"},
          {"colorspace_debug", null,
                  "Print debugging messages when an error is encountered in the" +
                          " colorspace module.", "off"}
  };

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
  public Grib2JpegDecoder(String argv[]) {

    // Initialize default parameters
    //System.err.println("calling Grib2JpegDecoder with argv argument");
    defpl = new ParameterList();
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
      System.err.println("An error occured while parsing the " +
              "arguments:\n" + e.getMessage());
      return;
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

  /**
   * Returns the parameters that are used in this class. It returns a 2D
   * String array. Each of the 1D arrays is for a different option, and they
   * have 3 elements. The first element is the option name, the second one
   * is the synopsis and the third one is a long description of what the
   * parameter is. The synopsis or description may be 'null', in which case
   * it is assumed that there is no synopsis or description of the option,
   * respectively.
   *
   * @return the options name, their synopsis and their explanation.
   */
  public static String[][] getParameterInfo() {
    return pinfo;
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
  public void decode(byte buf[]) {
    int dataSize = buf.length;
    boolean verbose;
    int res; // resolution level to reconstruct
    RandomAccessIO in;
    FileFormatReader ff;
    BitstreamReaderAgent breader;
    HeaderDecoder hd;
    EntropyDecoder entdec;
    ROIDeScaler roids;
    Dequantizer deq;
    InverseWT invWT;
    InvCompTransf ictransf;
    ImgWriter imwriter[];
    ImgDataConverter converter;
    DecoderSpecs decSpec;
    BlkImgDataSrc palettized;
    BlkImgDataSrc channels;
    BlkImgDataSrc resampled;
    BlkImgDataSrc color;
    int i;
    int depth[];

    int rate = pl.getIntParameter("rate");

    try {

      // **** Usage and version ****
      try {
        // Do we print version information?
        if (pl.getBooleanParameter("v")) {
          printVersionAndCopyright();
        }
        // Do we print usage information?
        if (pl.getParameter("u").equals("on")) {
          printUsage();
          return; // When printing usage => exit
        }
        // Do we print info ?
        verbose = pl.getBooleanParameter("verbose");
      } catch (StringFormatException e) {
        error("An error occurred while parsing the arguments:\n" +
                e.getMessage(), 1);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      } catch (NumberFormatException e) {
        error("An error occurred while parsing the arguments:\n" +
                e.getMessage(), 1);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** Check parameters ****
      try {
        pl.checkList(vprfxs, pl.toNameArray(pinfo));
      } catch (IllegalArgumentException e) {
        error(e.getMessage(), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // create a byte buf from raf for ISRandomAccessIO
      //System.out.println("raf processing Grib2JpegDecoder");

      //byte buf[] = new byte[ dataSize ];
      //raf.read( buf );
      ByteArrayInputStream bais = new ByteArrayInputStream(buf);

      in = new ISRandomAccessIO(bais, dataSize, 1, dataSize);


      // **** File Format ****
      // If the codestream is wrapped in the jp2 fileformat, Read the
      // file format wrapper
      ff = new FileFormatReader(in);
      ff.readFileFormat();
      if (ff.JP2FFUsed) {
        in.seek(ff.getFirstCodeStreamPos());
        //System.out.println("ff.JP2FFUsed is used");
      }

      // +----------------------------+
      // | Instantiate decoding chain |
      // +----------------------------+

      // **** Header decoder ****
      // Instantiate header decoder and read main header
      hi = new HeaderInfo();
      try {
        hd = new HeaderDecoder(in, pl, hi);
      } catch (EOFException e) {
        error("Codestream too short or bad header, " +
                "unable to decode.", 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      int nCompCod = hd.getNumComps();
      int nTiles = hi.siz.getNumTiles();
      decSpec = hd.getDecoderSpecs();

      // Report information
      StringBuffer info = new StringBuffer(nCompCod +
              " component(s) in codestream, " + nTiles + " tile(s)\n");
      if (verbose) {

        info.append("Image dimension: ");
        for (int c = 0; c < nCompCod; c++) {
          info.append(hi.siz.getCompImgWidth(c) + "x" +
                      hi.siz.getCompImgHeight(c) + " ");
        }

        if (nTiles != 1) {
          info.append("\nNom. Tile dim. (in canvas): " +
                      hi.siz.xtsiz + "x" + hi.siz.ytsiz);
        }
        System.out.println("[INFO]: " + info.toString());
      }
      if (pl.getBooleanParameter("cdstr_info")) {
        System.out.println("[INFO]: Main header:\n" +
                hi.toStringMainHeader());
      }

      // Get demixed bitdepths
      depth = new int[nCompCod];
      for (i = 0; i < nCompCod; i++) {
        depth[i] = hd.getOriginalBitDepth(i);
      }

      // **** Bit stream reader ****
      try {
        breader = BitstreamReaderAgent.
                createInstance(in, hd, pl, decSpec,
                        pl.getBooleanParameter("cdstr_info"), hi);
      } catch (IOException e) {
        error("Error while reading bit stream header or parsing " +
                "packets" + ((e.getMessage() != null) ?
                (":\n" + e.getMessage()) : ""), 4);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate bit stream reader" +
                ((e.getMessage() != null) ?
                        (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** Entropy decoder ****
      try {
        entdec = hd.createEntropyDecoder(breader, pl);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate entropy decoder" +
                ((e.getMessage() != null) ?
                        (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** ROI de-scaler ****
      try {
        roids = hd.createROIDeScaler(entdec, pl, decSpec);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate roi de-scaler." +
                ((e.getMessage() != null) ?
                        (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** Dequantizer ****
      try {
        deq = hd.createDequantizer(roids, depth, decSpec);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate dequantizer" +
                ((e.getMessage() != null) ?
                        (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
        return;
      }

      // **** Inverse wavelet transform ***
      try {
        // full page inverse wavelet transform
        invWT = InverseWT.createInstance(deq, decSpec);
      } catch (IllegalArgumentException e) {
        error("Cannot instantiate inverse wavelet transform" +
                ((e.getMessage() != null) ?
                        (":\n" + e.getMessage()) : ""), 2);
        if (pl.getParameter("debug").equals("on")) {
          e.printStackTrace();
        } else {
          error("Use '-debug' option for more details", 2);
        }
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
          channels = hd.
                  createChannelDefinitionMapper(ictransf, csMap);
          resampled = hd.createResampler(channels, csMap);
          palettized = hd.
                  createPalettizedColorSpaceMapper(resampled, csMap);
          color = hd.createColorSpaceMapper(palettized, csMap);

          if (csMap.debugging()) {
            System.out.println("[ERROR]: " + csMap);
            System.out.println("[ERROR]: " + channels);
            System.out.println("[ERROR]: " + resampled);
            System.out.println("[ERROR]: " + palettized);
            System.out.println("[ERROR]: " + color);
          }
        } catch (IllegalArgumentException e) {
          error("Could not instantiate ICC profiler" +
                  ((e.getMessage() != null) ?
                          (":\n" + e.getMessage()) : ""), 1, e);
          return;
        } catch (ColorSpaceException e) {
          error("error processing jp2 colorspace information" +
                  ((e.getMessage() != null) ?
                          (": " + e.getMessage()) : "    "), 1, e);
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

      // **** Report info ****
      int mrl = decSpec.dls.getMin();
      if (verbose) {
        if (mrl != res) {
          System.out.println("Reconstructing resolution " + res + " on " +
                  mrl + " (" + breader.getImgWidth(res) + "x" +
                  breader.getImgHeight(res) + ")");
        }
        if (rate != -1) {
          System.out.println("Target rate = "
                  + breader.getTargetRate() + " bpp (" +
                  breader.getTargetNbytes() + " bytes)");
        }
      }

      // code to get data
      // **** Decode and write/display result ****

      imwriter = new ImgWriter[nCompImg];
      // Now write the image to the array (decodes as needed)
      boolean isSigned = false;
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
          if (pl.getParameter("debug").equals("on")) {
            e.printStackTrace();
          } else {
            error("Use '-debug' option for more details", 2);
          }
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
          if (pl.getParameter("debug").equals("on")) {
            e.printStackTrace();
          } else {
            error("Use '-debug' option for more details", 2);
          }
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
          System.out.println("Actual bit rate = " + bitrate +
                  " bpp (i.e. " + numBytes + " bytes)");
        } else {
          System.out.println(
                  "Number of packet body bytes read = " + numBytes);
        }
      }

    } catch (IllegalArgumentException e) {
      error(e.getMessage(), 2);
      if (pl.getParameter("debug").equals("on"))
        e.printStackTrace();
      return;
    } catch (Error e) {
      if (e.getMessage() != null) {
        error(e.getMessage(), 2);
      } else {
        error("An error has occured during decoding.", 2);
      }

      if (pl.getParameter("debug").equals("on")) {
        e.printStackTrace();
      } else {
        error("Use '-debug' option for more details", 2);
      }
      return;
    } catch (RuntimeException e) {
      if (e.getMessage() != null) {
        error("An uncaught runtime exception has occurred:\n" +
                e.getMessage(), 2);
      } else {
        error("An uncaught runtime exception has occurred.", 2);
      }
      if (pl.getParameter("debug").equals("on")) {
        e.printStackTrace();
      } else {
        error("Use '-debug' option for more details", 2);
      }
      return;
    } catch (Throwable e) {
      error("An uncaught exception has occurred.", 2);
      if (pl.getParameter("debug").equals("on")) {
        e.printStackTrace();
      } else {
        error("Use '-debug' option for more details", 2);
      }
      return;
    }
  } // end decode

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
    System.out.println(msg);
  }

  /**
   * Prints the error message 'msg' to standard err, prepending
   * "ERROR" to it, and sets the exitCode to 'code'. An exit code
   * different than 0 indicates that there where problems. Either
   * the stacktrace or a "details" message is output depending on
   * the data of the "debug" parameter.
   *
   * @param msg  The error message
   * @param code The exit code to set
   * @param ex   The exception associated with the call
   */
  private void error(String msg, int code, Throwable ex) {
    exitCode = code;
    System.out.println(msg);
    if (pl.getParameter("debug").equals("on")) {
      ex.printStackTrace();
    } else {
      error("Use '-debug' option for more details", 2);
    }
  }

  /**
   * Return the packBytes ie number bytes / number 1,2 or 4
   */
  public int getPackBytes() {
    return packBytes;
  }

  /**
   * Return the decoded image as byte array
   */
  public int[] getGdata() {
    return data;
  }

  /**
   * Return the information found in the COM marker segments encountered in
   * the decoded codestream.
   */
  public String[] getCOMInfo() {
    if (hi == null) { // The codestream has not been read yet
      return null;
    }

    int nCOMMarkers = hi.getNumCOM();
    Enumeration com = hi.com.elements();
    String[] infoCOM = new String[nCOMMarkers];
    for (int i = 0; i < nCOMMarkers; i++) {
      infoCOM[i] = com.nextElement().toString();
    }
    return infoCOM;
  }

  /**
   * Returns all the parameters used in the decoding chain. It calls
   * parameter from each module and store them in one array (one row per
   * parameter and 4 columns).
   *
   * @return All decoding parameters
   * @see #getParameterInfo
   */
  public static String[][] getAllParameters() {
    Vector vec = new Vector();
    int i;

    String[][] str = BitstreamReaderAgent.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = EntropyDecoder.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = ROIDeScaler.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = Dequantizer.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = InvCompTransf.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = HeaderDecoder.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = ICCProfiler.getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = getParameterInfo();
    if (str != null) for (i = str.length - 1; i >= 0; i--) vec.addElement(str[i]);

    str = new String[vec.size()][4];
    if (str != null) for (i = str.length - 1; i >= 0; i--)
      str[i] = (String[]) vec.elementAt(i);

    return str;
  }

  /**
   * Prints version and copyright information to the logging facility
   */
  private void printVersionAndCopyright() {
    System.out.println("JJ2000's JPEG 2000 Grib2JpegDecoder\n");
  }

  /**
   * Prints the usage information to stdout. The usage information is
   * written for all modules in the decoder.
   */
  private void printUsage() {
    System.out.println("Usage:");
    System.out.println("Grib2JpegDecoder args...\n");
    System.out.println("The exit code of the decoder is non-zero " +
            "if an error occurs.");
    System.out.println("The following arguments are recongnized:\n");

    // Print decoding options
    printParamInfo(getAllParameters());
  }

  /**
   * Prints the parameters in 'pinfo' to the provided output, 'out', showing
   * the existing defaults. The message is printed to the logging facility
   * returned by FacilityManager.getMsgLogger(). The 'pinfo' argument is a
   * 2D String array. The first dimension contains String arrays, 1 for each
   * parameter. Each of these arrays has 3 elements, the first element is
   * the parameter name, the second element is the synopsis for the
   * parameter and the third one is a long description of the parameter. If
   * the synopsis or description is 'null' then no synopsis or description
   * is printed, respectively. If there is a default value for a parameter
   * it is also printed.
   *
   * @param pinfo The parameter information to write.
   */
  private void printParamInfo(String pinfo[][]) {
    String defval;

    for (int i = 0; i < pinfo.length; i++) {
      defval = defpl.getParameter(pinfo[i][0]);
      if (defval != null) { // There is a default value
        System.out.println("-" + pinfo[i][0] +
                ((pinfo[i][1] != null) ? " " + pinfo[i][1] + " " : " ") +
                "(default = " + defval + ")");
      } else { // There is no default value
        System.out.println("-" + pinfo[i][0] +
                ((pinfo[i][1] != null) ? " " + pinfo[i][1] : ""));
      }
      // Is there an explanatory message?
      if (pinfo[i][2] != null) {
        System.out.println(pinfo[i][2]);
      }
    }
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
                throw new IOException("Array supports only bit-depth between " +
                        "1 and 31");
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
         * The pack length of one sample (in bytes, according to the output
         * bit-depth
         */
        public int getPackBytes() {
            return packBytes;
        }

        /**
         * the jpeg data decoded into a array
         *
         * @return a byte[]
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