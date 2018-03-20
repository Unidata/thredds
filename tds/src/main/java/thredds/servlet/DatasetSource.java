/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet;

import ucar.nc2.NetcdfFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Alternative to IOSP when you need the HttpServletRequest to create the file.
 *
 * @author caron
 * @see "http://www.unidata.ucar.edu/projects/THREDDS/tech/tds4.2/reference/DatasetSource.html"
 */
public interface DatasetSource {

  boolean isMine( HttpServletRequest req);

  /**
   *
   * @param req the servlet request
   * @param res the servlet response
   * @return NetcdfFile or null if the source handles the response itself
   * @throws java.io.FileNotFoundException is the
   * @throws IOException
   */
  NetcdfFile getNetcdfFile( HttpServletRequest req, HttpServletResponse res) throws IOException;

}
