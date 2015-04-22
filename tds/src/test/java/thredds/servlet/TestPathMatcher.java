package thredds.servlet;

import org.junit.Test;
import thredds.core.PathMatcher;

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
    PathMatcher<Integer> m = new PathMatcher<>();
    m.put("/thredds/dods/test/longer", 1);
    m.put("/thredds/dods/test", 2);
    m.put("/thredds/dods/tester", 3);
    m.put("/thredds/dods/short", 4);
    m.put("/actionable", 5);
    m.put("myworld", 6);
    m.put("mynot", 7);
    m.put("ncmodels", 8);
    m.put("ncmodels/bzipped", 9);


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
