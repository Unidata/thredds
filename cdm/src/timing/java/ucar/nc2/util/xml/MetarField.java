package ucar.nc2.util.xml;

import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.nc2.util.Misc;

import java.util.HashMap;

public class MetarField {

  static HashMap<String, MetarField> fields = new HashMap<String, MetarField>();
  boolean showFields = false;

  String name;
  boolean isText;
  double sum = 0.0;

  MetarField(String name) {
    this.name = name;
    fields.put(name, this);
    if (showFields) System.out.println(name + " added");
  }

  void sum(StructureData sdata, StructureMembers.Member m) {
    if (m.getDataType() == DataType.DOUBLE)
      sum(sdata.getScalarDouble(m));
    else if (m.getDataType() == DataType.FLOAT)
      sum(sdata.getScalarFloat(m));
    else if (m.getDataType() == DataType.INT)
      sum(sdata.getScalarInt(m));
  }

  void sum(String text) {
    if (isText) return;
    try {
      sum(Double.parseDouble(text));
    } catch (NumberFormatException e) {
      if (showFields) System.out.println(name + " is text");
      isText = true;
    }
  }

  void sum(double d) {
    if (!Misc.closeEnough(d, -99999.0))
      sum += d; // LOOK kludge for missing data
  }
}
