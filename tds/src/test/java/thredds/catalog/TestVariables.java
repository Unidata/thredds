package thredds.catalog;

import junit.framework.*;
import java.util.*;

/** Test catalog read JUnit framework. */

public class TestVariables extends TestCase {
  private static boolean showValidation = false;

  public TestVariables( String name) {
    super(name);
  }

  String urlString = "testHarvest.xml";

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
