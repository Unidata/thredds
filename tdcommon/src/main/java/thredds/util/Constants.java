/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package thredds.util;

import java.io.File;

/**
 * Server side constants
 *
 * @author caron
 * @since 6/6/14
 */
public class Constants {
  public static final String Content_Disposition = "Content-Disposition";

  public static final String Content_Length = "Content-Length";

  //       res.setHeader("Content-Disposition", "attachment; filename=" + path + ".nc");
  public static String setContentDispositionValue(String filename) {
    return "attachment; filename=" + filename;
  }

  /**
   *
   * Get the string value of the file size in bytes
   *
   * @param file file object of the file to be returned
   * @return size of file in bytes
   */
  public static String getContentLengthValue(File file) {
    return Long.toString(file.length());
  }

    //       res.setHeader("Content-Disposition", "attachment; filename=" + path + ".nc");
  public static String setContentDispositionValue(String filename, String suffix) {
    int pos = filename.lastIndexOf('/');
    String outname = (pos > 0) ? filename.substring(pos+1) : filename;
    int pos2 = outname.lastIndexOf('.');
    outname = (pos > 0) ? outname.substring(0,pos2) : outname;
    outname = outname + suffix;
    return setContentDispositionValue(outname);
  }
}
