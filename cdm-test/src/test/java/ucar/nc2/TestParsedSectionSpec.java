package ucar.nc2;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**                    `
 * Describe
 *
 * @author caron
 * @since 7/15/11
 */
public class TestParsedSectionSpec extends TestCase {

  public void testVariableSection() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmLocalTestDataDir + "testWrite.nc"); // TestLocalNC2.openFile("testWrite.nc")
    Variable v = ncfile.findVariable("temperature");
    assert v != null;

    ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature");
    System.out.printf("%s%n", spec);
    assert spec.section.equals(v.getShapeAsSection());

    spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature(1,0:127:2)");
    System.out.printf("%s%n", spec);
    Section s = new Section("1,0:127:2");
    assert spec.section.equals(s) : spec.section + " != " + s;

    spec = ParsedSectionSpec.parseVariableSection(ncfile, "temperature(:,0:127:2)");
    System.out.printf("%s%n", spec);
    s = new Section("0:63,0:127:2");
    assert spec.section.equals(s) : spec.section + " != " + s;

    ncfile.close();
  }

  @Category(NeedsCdmUnitTest.class)
  public void testGroupAndMembers() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/netcdf4/compound/simple_nc4.nc4");
    Variable v = ncfile.findVariable("grp1/data");
    assert v != null;

    ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "grp1/data");
    System.out.printf("%s%n", spec);
    assert spec.section.equals(v.getShapeAsSection());

    spec = ParsedSectionSpec.parseVariableSection(ncfile, "grp2/data.i1");
    System.out.printf("%s%n", spec);

    Variable s = ncfile.findVariable("grp2/data");
    assert spec.section.equals(s.getShapeAsSection());

    v = ncfile.findVariable("grp2/data.i1");
    assert spec.child.section.equals(v.getShapeAsSection());

    ncfile.close();
  }

  @Category(NeedsCdmUnitTest.class)
  public void testEscaping() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml");
    Group g = ncfile.findGroup("group.name");
    assert g != null;

    Variable v = g.findVariable("var.name");
    assert v != null;

    Variable v2 = ncfile.findVariable("group.name/var.name");
    assert v2 == null;

    v2 = ncfile.findVariable("group\\.name/var\\.name");
    assert v2 != null;
    assert v2.equals(v);

    ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\.name/var\\.name");
    System.out.printf("%s%n", spec);
    assert spec.section.equals(v2.getShapeAsSection());

    spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\.name/var\\.name(1,0:0)");
    System.out.printf("%s%n", spec);
    Section s = new Section("1,0");
    assert spec.section.equals(s);

    ncfile.close();
  }

  @Category(NeedsCdmUnitTest.class)
  public void testEscaping2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir + "ncml/escapeNames.ncml");
    Group g = ncfile.findGroup("group(name");
    assert g != null;

    Variable v = g.findVariable("var(name");
    assert v != null;

    Variable v2 = ncfile.findVariable("group(name/var(name");
    assert v2 != null;
    assert v2.equals(v);

    v2 = ncfile.findVariable("group\\(name/var\\(name");
    assert v2 != null;
    assert v2.equals(v);

    ParsedSectionSpec spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\(name/var\\(name");
    System.out.printf("%s%n", spec);
    assert spec.section.equals(v2.getShapeAsSection());

    spec = ParsedSectionSpec.parseVariableSection(ncfile, "group\\(name/var\\(name(1,0:0)");
    System.out.printf("%s%n", spec);
    Section s = new Section("1,0");
    assert spec.section.equals(s);

    ncfile.close();
  }

}
