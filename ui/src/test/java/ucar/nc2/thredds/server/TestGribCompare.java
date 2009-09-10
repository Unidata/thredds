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
package ucar.nc2.thredds.server;

import thredds.catalog.*;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CompareNetcdf;

import java.io.IOException;
import java.util.*;


public class TestGribCompare {

  private static void compare(String dsName1, String dsName2, Formatter f) throws IOException {
    CompareNetcdf compare = new CompareNetcdf(true, false, true);
    NetcdfFile ncfile1 = null;
    NetcdfFile ncfile2 = null;
    try {
      ncfile1 = NetcdfDataset.openFile(dsName1, null);
      ncfile2 = NetcdfDataset.openFile(dsName2, null);
      compare.compare(ncfile1, ncfile2, f);
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      if (ncfile1 != null) ncfile1.close();
      if (ncfile2 != null) ncfile2.close();
    }
  }


  private static String findDataset(String catUrl, Formatter f) throws IOException {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    InvCatalogImpl cat = catFactory.readXML(catUrl);
    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    f.format("catalog < %s >%n", catUrl);
    if (!isValid) {
      f.format(" validation output= %s%n", buff);
      return null;
    }

    InvDataset top = cat.getDataset();
    List<InvDataset> datasets = top.getDatasets();
    if (datasets.size() == 0) {
      f.format(" no datasets%n");
      return null;
    }

    int k = random.nextInt(datasets.size());
    InvDataset ds = datasets.get(k);
    InvAccess access = ds.getAccess(ServiceType.OPENDAP);
    if (access == null) {
      f.format(" no opendap access for %s%n", ds.getFullName());
      return null;
    }

    return access.getUnresolvedUrlName();

  }

  static String serverOld = "http://motherlode.ucar.edu:8080";
  static String serverNew = "http://motherlode.ucar.edu:9080";
  static Random random = new Random(System.currentTimeMillis());

  public static void main(String args[]) throws Exception {
    Formatter f = new Formatter(System.out);
    for (String dsname : FmrcDefinition.fmrcDatasets) {
      String ds = findDataset(serverOld + "/thredds/fmrc/" + dsname + "/runs/catalog.xml", f);
      f.format(" URL1= %s%n", serverOld + ds);
      f.format(" URL2= %s%n", serverNew + ds);
      compare(serverOld + ds, serverNew + ds, f);
    }

  }

}
