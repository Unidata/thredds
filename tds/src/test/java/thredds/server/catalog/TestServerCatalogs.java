/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package thredds.server.catalog;

import org.junit.Test;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.test.util.TestFileDirUtils;

import java.io.File;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 1/15/2015
 */
public class TestServerCatalogs {

    //private String dsScanDir = "src/test/data";
  private String dsScanDir = TestDir.cdmLocalTestDataDir;
  private String dsScanFilter = ".*\\.nc$";

  private String serviceName = "ncdods";
  private String baseURL = "http://localhost:8080/thredds/docsC";

  private File dsScanTmpDir;
  private File expectedResultsDir;

  private String configResourcePath = "/thredds/catalog";
  private String testInvDsScan_emptyServiceBase_ResourceName = "testInvDsScan.emptyServiceBase.result.xml";
  private String testInvDsScan_topLevelCat_ResourceName = "testInvDsScan.topLevelCat.result.xml";
  private String testInvDsScan_secondLevelCat_ResourceName = "testInvDsScan.secondLevelCat.result.xml";
  private String testInvDsScan_timeCoverage_ResourceName = "testInvDsScan.timeCoverage.result.xml";
  private String testInvDsScan_addIdTopLevel_ResourceName = "testInvDsScan.addIdTopLevel.result.xml";
  private String testInvDsScan_addIdLowerLevel_ResourceName = "testInvDsScan.addIdLowerLevel.result.xml";

  private String testInvDsScan_compoundServiceLower_ResourceName = "testInvDsScan.compoundServiceLower.result.xml";
  private String testInvDsScan_addDatasetSize_ResourceName = "testInvDsScan.addDatasetSize.result.xml";
  private String testInvDsScan_addLatest_ResourceName = "testInvDsScan.addLatest.result.xml";

  private String testInvDsScan_compoundServerFilterProblem_1_ResourceName = "testInvDsScan.compoundServerFilterProblem.1.result.xml";
  private String testInvDsScan_compoundServerFilterProblem_2_ResourceName = "testInvDsScan.compoundServerFilterProblem.2.result.xml";

}
