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
package thredds.catalog;

import junit.framework.*;
import java.util.*;

/** Test catalog read JUnit framework. */

public class TestVariables extends TestCase {
  private static boolean showValidation = false;

  public TestVariables( String name) {
    super(name);
  }

  String urlString = "TestHarvest.xml";

  public void testInline() {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    InvDataset ds = cat.findDatasetByID("solve1.dc8");
    assert ds != null;

    List list = ds.getVariables();
    assert list != null;
    assert list.size() >= 2;

    ThreddsMetadata.Variables vars = getType( list, "CF-1.0");
    assert vars != null;
    checkVariable( vars, "wv","Wind Speed");
    checkVariable( vars, "o3c", "Ozone Concentration");

    ThreddsMetadata.Variables dif = getType( list, "DIF");
    assert dif != null;
    checkVariable( dif, "wind_from_direction",
        "EARTH SCIENCE > Atmosphere > Atmosphere Winds > Surface Winds > wind_from_direction");

  }

  ThreddsMetadata.Variables getType(List list, String type) {
    for (int i=0; i<list.size(); i++) {
      ThreddsMetadata.Variables vars = (ThreddsMetadata.Variables) list.get(i);
      if (vars.getVocabulary().equals(type)) return vars;
    }
    return null;
  }

  void checkVariable(ThreddsMetadata.Variables vars, String name, String vname) {
    List list = vars.getVariableList();
    for (int i=0; i<list.size(); i++) {
      ThreddsMetadata.Variable var = (ThreddsMetadata.Variable) list.get(i);
      if (var.getName().equals(name)) {
        assert var.getVocabularyName().equals(vname);
        return;
      }
    }
    assert false;
  }

}
