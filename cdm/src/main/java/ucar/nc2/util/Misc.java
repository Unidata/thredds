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

package ucar.nc2.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Miscellaneous static routines.
 *
 * @author caron
 */
public class Misc {

  static final protected String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  static final protected String slashalpha = "\\/" + alpha;

  //private static double maxAbsoluteError = 1.0e-6;
  public static final double maxReletiveError = 1.0e-6;

  /*  http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm
  public static boolean closeEnough( double v1, double v2) {
    double d1 = Math.abs(v1-v2);
    if (d1 < maxAbsoluteError) return true;

    double diff = (Math.abs(v2) > Math.abs(v1)) ? Math.abs((v1-v2)/v2) :  Math.abs((v1-v2)/v1);
    return diff < maxReletiveError;
  } */

  /* from testAll
  static public boolean closeEnough( double d1, double d2) {
    if (Math.abs(d1) < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  static public boolean closeEnough( double d1, double d2, double tol) {
    if (Math.abs(d1) < tol) return Math.abs(d1-d2) < tol;
    double pd = (d1-d2)/d1;
    return Math.abs(pd) < tol;
  }

  static public boolean closeEnough( float d1, float d2) {
    if (Math.abs(d1) < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  } */

  static public double howClose(double d1, double d2) {
    double pd = (d1 - d2) / d1;
    return Math.abs(pd);
  }

  private static final boolean show = false;  // debug

  /**
   * Check if numbers are equal with tolerance
   *
   * @param v1  first floating point number
   * @param v2  second floating point number
   * @param tol reletive tolerence
   * @return true if within tolerance
   */
  public static boolean closeEnough(double v1, double v2, double tol) {
    if (show) {
      double d1 = Math.abs(v1 - v2);
      double d3 = Math.abs(v1 / v2);
      double d2 = Math.abs((v1 / v2) - 1);
      System.out.println("v1= " + v1 + " v2=" + v2 + " diff=" + d1 + " abs(v1/v2)=" + d3 + " abs(v1/v2-1)=" + d2);
    }

    double diff = (v2 == 0.0) ? Math.abs(v1 - v2) : Math.abs(v1 / v2 - 1);
    return diff < tol;
  }

  /**
   * Check if numbers are equal with default tolerance
   *
   * @param v1 first floating point number
   * @param v2 second floating point number
   * @return true if within tolerance
   */
  public static boolean closeEnough(double v1, double v2) {
    if (v1 == v2) return true;
    if (Double.isNaN(v1) && Double.isNaN(v2)) return true;
    double diff = (v2 == 0.0) ? Math.abs(v1 - v2) : Math.abs(v1 / v2 - 1);
    return diff < maxReletiveError;
  }

  /**
   * Check if numbers are equal with default tolerance
   *
   * @param v1 first floating point number
   * @param v2 second floating point number
   * @return true if within tolerance
   */
  public static boolean closeEnough(float v1, float v2) {
    if (v1 == v2) return true;
    if (Float.isNaN(v1) && Float.isNaN(v2)) return true;
    double diff = (v2 == 0.0) ? Math.abs(v1) : Math.abs(v1 / v2 - 1);
    return diff < maxReletiveError;
  }

  /* private void printBytes(int n, Formatter fout) throws IOException {
    long savePos = raf.getFilePointer();
    long pos;
    for (pos = savePos; pos < savePos + n - 9; pos += 10) {
      fout.format("%d: ", pos);
      _printBytes(10, fout);
    }
    if (pos < savePos + n) {
      fout.format("%d: ", pos);
      _printBytes((int) (savePos + n - pos), fout);
    }
    raf.seek(savePos);
  }

  private void _printBytes(int n, Formatter fout) throws IOException {
    for (int i = 0; i < n; i++) {
      byte b = (byte) raf.read();
      int ub = (b < 0) ? b + 256 : b;
      fout.format(ub + "%d(%b) ", ub, b);
    }
    fout.format("\n");
  } */

  static public String showInts(int[] inta) {
    if (inta == null) return "null";
    Formatter f = new Formatter();
    for (int i : inta) f.format("%d, ", i);
    return f.toString();
  }

  static public void showInts(int[] inta, Formatter f) {
    if (inta == null) {
      f.format("null");
      return;
    }
    for (int i : inta) f.format("%d, ", i);
  }

  static public String showBytes(byte[] buff) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0) sbuff.append(" ");
      sbuff.append(ub);
    }
    return sbuff.toString();
  }

  static public void showBytes(byte[] buff, Formatter f) {
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      f.format("%3d ", ub);
    }
  }

  static public int getSize(Iterable ii) {
    if (ii instanceof Collection)
      return ((Collection) ii).size();
    int count = 0;
    for (Object i : ii) count++;
    return count;
  }

  static public List getList(Iterable ii) {
    if (ii instanceof List)
      return (List) ii;
    List<Object> result = new ArrayList<Object>();
    for (Object i : ii) result.add(i);
    return result;
  }

  //////////////////////////////////////////////////////////////////////

  /**
   * Filename of the user property file read from the "user.home" directory
   * if the "unidata.testdata2.path" and "unidata.upc.share.path" are not
   * available as system properties.
   */
  private static final String threddsPropFileName = "thredds.properties";
  private static final String testdataDirPropName = "unidata.testdata.path";
  private static String testdataDirPath = null;

  public static String getTestdataDirPath() {
    if (testdataDirPath == null)
      testdataDirPath = System.getProperty(testdataDirPropName);  // Check for system property

    if (testdataDirPath == null) {
      File userHomeDirFile = new File(System.getProperty("user.home"));
      File userThreddsPropsFile = new File(userHomeDirFile, threddsPropFileName);
      if (userThreddsPropsFile.exists() && userThreddsPropsFile.canRead()) {
        Properties userThreddsProps = new Properties();
        try {
          userThreddsProps.load(new FileInputStream(userThreddsPropsFile));
        } catch (IOException e) {
          System.out.println("**Failed loading user THREDDS property file: " + e.getMessage());
        }
        if (!userThreddsProps.isEmpty()) {
          testdataDirPath = userThreddsProps.getProperty(testdataDirPropName);
        }
      }
    }

    return testdataDirPath;
  }

  static public boolean compare(byte[] raw1, byte[] raw2, Formatter f) {
    if (raw1 == null || raw2 == null) return false;

    if (raw1.length != raw2.length) {
      f.format("length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    int ndiff = 0;
    for (int i = 0; i < len; i++) {
      if (raw1[i] != raw2[i]) {
        f.format(" %3d : %3d != %3d%n", i + 1, raw1[i], raw2[i]);
        ndiff++;
      }
    }
    f.format("tested %d bytes  diff = %d %n", len, ndiff);
    return ndiff == 0 && (raw1.length == raw2.length);
  }

  static public void compare(float[] raw1, float[] raw2, Formatter f) {
    if (raw1.length != raw2.length) {
      f.format("compareFloat: length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    int ndiff = 0;
    for (int i = 0; i < len; i++) {
      if (!Misc.closeEnough(raw1[i], raw2[i]) && !Double.isNaN(raw1[i]) && !Double.isNaN(raw2[i])) {
        f.format(" %5d : %3f != %3f%n", i, raw1[i], raw2[i]);
        ndiff++;
      }
    }
    f.format("tested %d floats diff = %d %n", len, ndiff);
  }

  // from Java7
  public static int compare(int x, int y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }

  public static int compare(long x, long y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }


    /**
     * Return the set of leading protocols for a url; may be more than one.
     * Watch out for Windows paths starting with a drive letter => protocol
     * names must all have a length > 1.
     * Watch out for '::'
     * Each captured protocol is saved without trailing ':'
     * Assume: the protocols MUST be terminated by the occurrence of '/'.
     *
     * @param url the url whose protocols to return
     * @return list of leading protocols without the trailing :
     */
    static public List<String> getProtocols(String url)
    {
        List<String> allprotocols = new ArrayList<>(); // all leading protocols upto path or host

        // Note, we cannot use split because of the context sensitivity
        // This code is quite ugly because of all the confounding cases
        // (e.g. windows path, embedded colons, etc.).
        // Specifically, the 'file:' protocol is a problem because
        // it has no many non-standard forms such as file:x/y file://x/y file:///x/y.
        StringBuilder buf = new StringBuilder(url);
        // If there are any leading protocols, then they must stop at the first '/'.
        int slashpos = buf.indexOf("/");
        // Check special case of file:<path> with no slashes after file:
        if(url.startsWith("file:") && "/\\".indexOf(url.charAt(5)) < 0) {
            allprotocols.add("file");
        } else if(slashpos >= 0) {
            // Remove everything after the first slash
            buf.delete(slashpos + 1, buf.length());
            for(;;) {
                int index = buf.indexOf(":");
                if(index < 0) break; // no more protocols
                // Validate protocol
                if(!validateprotocol(url, 0, index))
                    break;
                String protocol = buf.substring(0, index);  // not including trailing ':'
                allprotocols.add(protocol);
                buf.delete(0, index + 1); // remove the leading protocol
            }
        }
        return allprotocols;
    }


    static protected boolean
    validateprotocol(String url, int startpos, int endpos)
    {
        int len = endpos - startpos;
        if(len == 0) return false;
        char cs = url.charAt(startpos);
        char ce1 = url.charAt(endpos + 1);
        if(len == 1 //=>|protocol| == 1
            && alpha.indexOf(cs) >= 0 && "/\\".indexOf(ce1) >= 0)
            return false; // looks like windows drive letter
        // If trailing colon is not followed by alpha or /, then assume not url
        if(slashalpha.indexOf(ce1) < 0)
            return false;
        return true;
    }


  /**
   * test
   */
  public static void main(String args[]) {
    long val1 = -1;
    long val2 = 234872309;
    int val3 = 2348;
    int val4 = 32;
    Formatter f = new Formatter(System.out);
    f.format("  address            dataPos            offset size%n");
    f.format("  %#-18x %#-18x %5d  %4d%n", val1, val2, val3, val4);

  }


}
