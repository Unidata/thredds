/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.image;

/**
 *
 * @author caron
 */
public class Tools {
  private static boolean debug = false;

  static void log(String s) {
    if (debug)System.out.println(s);
  }
}