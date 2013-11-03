package ucar.nc2.ft.point.writer;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.StructureMembers;
import ucar.nc2.VariableSimpleAdapter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.units.DateUnit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 11/2/13
 */
public class TestWriterCFPointCollection {


  // from jeff mcw
  @Test
  public void testJeffm() throws IOException {
    StructureMembers structureMembers = new StructureMembers("point obs");
    VariableSimpleAdapter var = new VariableSimpleAdapter(structureMembers.addMember("test", "test", "m", DataType.DOUBLE, new int[]{1}));
    List<VariableSimpleIF> vars = new ArrayList<VariableSimpleIF>();
    vars.add(var);

    try {
      WriterCFPointCollection writer = new WriterCFPointCollection("test.nc", "point data");
      writer.writeHeader(vars, DateUnit.getUnixDateUnit(), "m");
      writer.finish();
      assert false;

    } catch (Exception e) {
      System.out.printf("%s%n", e.getMessage()); // expect this
      assert true;
    }
  }
}
