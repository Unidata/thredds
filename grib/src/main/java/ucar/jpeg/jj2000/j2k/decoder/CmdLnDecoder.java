/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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
package ucar.jpeg.jj2000.j2k.decoder;

import ucar.jpeg.jj2000.j2k.util.*;

import java.io.*;

/**
 * This class runs the JJ2000 decoder from the command line interface. It
 * parses command-line arguments in order to fill a ParameterList object. Then
 * this one is provided to a Decoder object.
 */
public class CmdLnDecoder {

  /**
   * The parameter list (with modules arguments)
   */
  private ParameterList pl;

  /**
   * The default parameter list (with modules arguments)
   */
  private ParameterList defpl;

  /**
   * The current Decoder object
   */
  private Decoder dec;

  /**
   * The starting point of the program. It calls the constructor with the
   * command line options in a String array.
   *
   * @param argv The command line parameters
   */
  public static void main(String argv[]) {
    if (argv.length == 0) {
      FacilityManager.getMsgLogger()
              .println("CmdLnDecoder: JJ2000's JPEG 2000 Decoder\n" +
                      "    use jj2000.j2k.decoder.CmdLnDecoder -u " +
                      "to get help\n", 0, 0);
      System.exit(1);
    }

    new CmdLnDecoder(argv);
  }

  /**
   * Instantiates a command line decoder object, width the 'argv' command
   * line arguments. It also initializes the default parameters. If the
   * argument list is empty an IllegalArgumentException is thrown. If an
   * error occurs while parsing the arguments error messages are written to
   * stderr and the run exit code is set to non-zero, see getExitCode()
   *
   * @throws IllegalArgumentException If 'argv' is empty
   * @see Decoder#getExitCode
   */
  public CmdLnDecoder(String argv[]) {
    // Initialize default parameters
    defpl = new ParameterList();
    String[][] param = Decoder.getAllParameters();

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
    // Parse the arguments from some file?
    if (pl.getParameter("pfile") != null) {
      // Load parameters from file
      InputStream is;
      try {
        is = new FileInputStream(pl.getParameter("pfile"));
        is = new BufferedInputStream(is);
        pl.load(is);
      } catch (FileNotFoundException e) {
        System.err.println("Could not load the argument file " +
                pl.getParameter("pfile"));
        return;
      } catch (IOException e) {
        System.err.println("An error ocurred while reading from " +
                "the argument " + "file " +
                pl.getParameter("pfile"));
        return;
      }
      try {
        is.close();
      } catch (IOException e) {
        System.out.println("[WARNING]: Could not close the argument" +
                " file after reading");
      }
      // Now reparse command line arguments so that they override file
      // arguments
      try {
        pl.parseArgs(argv);
      } catch (StringFormatException e) {
        System.err.println("An error occured while re-parsing the " +
                "arguments:\n" + e.getMessage());
        return;
      }
    }

    // Instantiate the Decoder object
    dec = new Decoder(pl);
    if (dec.getExitCode() != 0) { // An error ocurred
      System.exit(dec.getExitCode());
    }

    // Run the decoder
    try {
      dec.run((byte[])null); // LOOK broken
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      if (dec.getExitCode() != 0) {
        System.exit(dec.getExitCode());
      }
    }
  }
}
