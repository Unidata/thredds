/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.dt.grid;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since Apr 13, 2010
 */
public class TestGridClose {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String newVarName = "some_data";

  @Test
  public void testClose() throws IOException {
    File org = new File(TestDir.cdmLocalTestDataDir + "rankTest.nc");
    File copy = tempFolder.newFile("rankTest.nc");
    IO.copyFile(org, copy);

    String url = copy.getPath();

    openDatasetAndView(url);
    alterExistingFile(url);
    checkFile(url);
  }

  public void openDatasetAndView(String url) throws IOException {
    Array temp_data;
    try (GridDataset dataset = GridDataset.open(url)) {
      for (GridDatatype grid : dataset.getGrids()) {
        temp_data = grid.readDataSlice(0, 0, -1, -1);

        assert grid.getMinMaxSkipMissingData(temp_data).min == 0.0;   // min
        assert grid.getMinMaxSkipMissingData(temp_data).max == 42.0;  // max
      }
    }
  }

  public void alterExistingFile(String url) throws IOException {
    NetcdfFileWriteable file = null;
    try {
      file = NetcdfFileWriteable.openExisting(url, false);
      file.setRedefineMode(true);
      //Group rootGroup = file.getRootGroup();
      //Group headerDataGroup = new Group(file, rootGroup, "header_data");
      //file.addGroup(rootGroup, headerDataGroup);
      file.addVariable(null, newVarName, DataType.FLOAT, "z y x");
    } finally {
      if (file != null) {
        file.setRedefineMode(false);
        file.flush();
        file.close();
      }
    }
  }

  public void checkFile(String url) throws IOException {
    try (NetcdfFile file = NetcdfFile.open(url, null)) {
      assert file.findVariable(newVarName) != null : "cant find "+newVarName;
    }
  }
}
