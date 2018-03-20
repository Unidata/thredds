/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;

import com.google.common.base.Preconditions;
import ucar.nc2.NetcdfFileWriter;

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities for extracting path information from request.
 * Handles servlet and spring controller cases
 */
public class TdsPathUtils {

  // For "removePrefix/path" style servlet mappings.
  public static String extractPath(HttpServletRequest req, String removePrefix) {

    // may be in pathInfo (Servlet) or servletPath (Controller)
    String dataPath = req.getPathInfo();
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

  ///////////////////////////////////////////////////

  public static String getFileNameForResponse(String pathInfo, NetcdfFileWriter.Version version) {
    Preconditions.checkNotNull(version, "version == null");
    return getFileNameForResponse(pathInfo, version.getSuffix());
  }

  public static String getFileNameForResponse(String pathInfo, String extension) {
    Preconditions.checkArgument(pathInfo != null && !pathInfo.isEmpty(), "pathInfo == %s", pathInfo);
    Preconditions.checkNotNull(extension, "extension == null");

    String parentDirName = getBaseName(doGetPath(pathInfo, 0));
    String baseName = getBaseName(pathInfo);

    String dotExtension = extension.startsWith(".") ? extension : "." + extension;

    if (!parentDirName.isEmpty()) {
      return parentDirName + "_" + baseName + dotExtension;
    } else {
      return baseName + dotExtension;
    }
  }

  // taken from apache.common.io.FilenameUtils so we dont need to suck in entire common.io library
  private static final char EXTENSION_SEPARATOR = '.';
  private static final char UNIX_SEPARATOR = '/';
  private static final char WINDOWS_SEPARATOR = '\\';

  private static String getBaseName(String filename) {
    return removeExtension( getName(filename));
  }

  private static String removeExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfExtension(filename);
    if (index == -1) {
      return filename;
    } else {
      return filename.substring(0, index);
    }
  }

  private static int indexOfExtension(String filename) {
    if (filename == null) {
      return -1;
    }
    int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
    int lastSeparator = indexOfLastSeparator(filename);
    return lastSeparator > extensionPos ? -1 : extensionPos;
  }

  private static String getName(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfLastSeparator(filename);
    return filename.substring(index + 1);
  }

  private static int indexOfLastSeparator(String filename) {
    if (filename == null) {
      return -1;
    }
    int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
    int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
    return Math.max(lastUnixPos, lastWindowsPos);
  }

  private static String doGetPath(String filename, int separatorAdd) {
    if (filename == null) {
      return null;
    }
    int prefix = getPrefixLength(filename);
    if (prefix < 0) {
      return null;
    }
    int index = indexOfLastSeparator(filename);
    int endIndex = index+separatorAdd;
    if (prefix >= filename.length() || index < 0 || prefix >= endIndex) {
      return "";
    }
    return filename.substring(prefix, endIndex);
  }

  private static int getPrefixLength(String filename) {
    if (filename == null) {
      return -1;
    }
    int len = filename.length();
    if (len == 0) {
      return 0;
    }
    char ch0 = filename.charAt(0);
    if (ch0 == ':') {
      return -1;
    }
    if (len == 1) {
      if (ch0 == '~') {
        return 2;  // return a length greater than the input
      }
      return isSeparator(ch0) ? 1 : 0;
    } else {
      if (ch0 == '~') {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
        if (posUnix == -1 && posWin == -1) {
          return len + 1;  // return a length greater than the input
        }
        posUnix = posUnix == -1 ? posWin : posUnix;
        posWin = posWin == -1 ? posUnix : posWin;
        return Math.min(posUnix, posWin) + 1;
      }
      char ch1 = filename.charAt(1);
      if (ch1 == ':') {
        ch0 = Character.toUpperCase(ch0);
        if (ch0 >= 'A' && ch0 <= 'Z') {
          if (len == 2 || !isSeparator(filename.charAt(2))) {
            return 2;
          }
          return 3;
        }
        return -1;

      } else if (isSeparator(ch0) && isSeparator(ch1)) {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
        if (posUnix == -1 && posWin == -1 || posUnix == 2 || posWin == 2) {
          return -1;
        }
        posUnix = posUnix == -1 ? posWin : posUnix;
        posWin = posWin == -1 ? posUnix : posWin;
        return Math.min(posUnix, posWin) + 1;
      } else {
        return isSeparator(ch0) ? 1 : 0;
      }
    }
  }

  private static boolean isSeparator(char ch) {
    return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
  }

}
