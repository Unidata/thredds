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
package thredds.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities for extracting path information from request.
 * Handles servlet and spring controller cases
 *
 * @author edavis
 * @since 4.0
 */
public class TdsPathUtils {

  // For "removePrefix/path" style servlet mappings.
  public static String extractPath(HttpServletRequest req, String removePrefix) {

    // may be in pathInfo (Servlet) or servletPath (Controller)
    String dataPath = req.getPathInfo();
    String reqPath = req.getServletPath();
    if (dataPath == null) {
      dataPath = req.getServletPath();
    }
    if (dataPath == null)  // not sure if this is possible
      return "";

    // removePrefix or "/"+removePrefix
    if (removePrefix != null) {
      if (dataPath.startsWith(removePrefix)) {
        dataPath = dataPath.substring(removePrefix.length());

      } else if (dataPath.startsWith("/")) {
        dataPath = dataPath.substring(1);
        if (dataPath.startsWith(removePrefix))
          dataPath = dataPath.substring(removePrefix.length());
      }

    }

    if (dataPath.startsWith("/"))
      dataPath = dataPath.substring(1);

    if (dataPath.contains(".."))  // LOOK what about escapes ??
      throw new IllegalArgumentException("path cannot contain '..'");

    return dataPath;
  }

  public static String extractPath(HttpServletRequest req, String removePrefix, String[] endings) {
    String path = extractPath(req, removePrefix);
    if (endings == null) return path;

    for (String ending : endings) {
      if (path.endsWith(ending)) {
        int len = path.length() - ending.length();
        path = path.substring(0, len);
        break;
      }
    }

    return path;
  }

}
