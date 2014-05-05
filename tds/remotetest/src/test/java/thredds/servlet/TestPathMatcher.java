package thredds.servlet;

import org.junit.Test;

/**
 * Test PathMatcher
 *
 * @author caron
 * @since 10/30/13
 */
public class TestPathMatcher {

  private void doit( PathMatcher m, String s, boolean hasMatch) {
    Object result = m.match(s);
    assert (result != null) == hasMatch : s +" match " + result;
  }
      
  @Test
  public void tester() {
    PathMatcher m = new PathMatcher();
    m.put("/thredds/dods/test/longer", null);
    m.put("/thredds/dods/test", null);
    m.put("/thredds/dods/tester", null);
    m.put("/thredds/dods/short", null);
    m.put("/actionable", null);
    m.put("myworld", null);
    m.put("mynot", null);
    m.put("ncmodels", null);
    m.put("ncmodels/bzipped", null);


    doit(m, "nope", false);
    doit(m, "/thredds/dods/test", true);
    doit(m, "/thredds/dods/test/lo", true);
    doit(m, "/thredds/dods/test/longer/donger", true);
    doit(m, "myworldly", true);
    doit(m, "/my", false);
    doit(m, "mysnot", false);
    doit(m, "ncmodels/canonical", true);
  }
}
