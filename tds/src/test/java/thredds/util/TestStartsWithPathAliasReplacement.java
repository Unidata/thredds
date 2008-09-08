package thredds.util;

import junit.framework.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestStartsWithPathAliasReplacement extends TestCase
{

  public TestStartsWithPathAliasReplacement( String name )
  {
    super( name );
  }

  public void testNullCtorParams()
  {
    try { new StartsWithPathAliasReplacement( null, null);}
    catch ( IllegalArgumentException e ) { return ;}
    fail( "Did not throw expected IllegalArgumentException.");
  }

  public void testNullFirstCtorParam()
  {
    try { new StartsWithPathAliasReplacement( null, "/some/path/");}
    catch ( IllegalArgumentException e ) { return ;}
    fail( "Did not throw expected IllegalArgumentException.");
  }

  public void testNullSecondCtorParam()
  {
    try { new StartsWithPathAliasReplacement( "alias/path/", null);}
    catch ( IllegalArgumentException e ) { return ;}
    fail( "Did not throw expected IllegalArgumentException.");
  }

  public void testBasics()
  {
    String testPath = "alias/some/dir";
    String expectedPath = "/replacement/path/some/dir";

    String alias = "alias/";
    String replacementPath = "/replacement/path/";
    StartsWithPathAliasReplacement swpar = new StartsWithPathAliasReplacement( alias, replacementPath);
    assertTrue( "Given path [" + testPath + "] does not start with alias [" + swpar.getAlias() + "].",
                swpar.containsPathAlias( testPath ));
    assertTrue( "Alias replacement on given path [" + testPath + "] not as expected [" + expectedPath + "].",
                swpar.replacePathAlias( testPath ).equals( expectedPath ));

    alias = "alias";
    replacementPath = "/replacement/path/";
    swpar = new StartsWithPathAliasReplacement( alias, replacementPath);
    assertTrue( "Given path [" + testPath + "] does not start with alias [" + swpar.getAlias() + "].",
                swpar.containsPathAlias( testPath ));
    assertTrue( "Alias replacement on given path [" + testPath + "] not as expected [" + expectedPath + "].",
                swpar.replacePathAlias( testPath ).equals( expectedPath ));

    alias = "alias/";
    replacementPath = "/replacement/path";
    swpar = new StartsWithPathAliasReplacement( alias, replacementPath);
    assertTrue( "Given path [" + testPath + "] does not start with alias [" + swpar.getAlias() + "].",
                swpar.containsPathAlias( testPath ));
    assertTrue( "Alias replacement on given path [" + testPath + "] not as expected [" + expectedPath + "].",
                swpar.replacePathAlias( testPath ).equals( expectedPath ));

    alias = "alias";
    replacementPath = "/replacement/path";
    swpar = new StartsWithPathAliasReplacement( alias, replacementPath);
    assertTrue( "Given path [" + testPath + "] does not start with alias [" + swpar.getAlias() + "].",
                swpar.containsPathAlias( testPath ));
    assertTrue( "Alias replacement on given path [" + testPath + "] not as expected [" + expectedPath + "].",
                swpar.replacePathAlias( testPath ).equals( expectedPath ));

    testPath = "a/multi/segment/alias/some/dir";

    alias = "a/multi/segment/alias";
    replacementPath = "/replacement/path";
    swpar = new StartsWithPathAliasReplacement( alias, replacementPath);
    assertTrue( "Given path [" + testPath + "] does not start with alias [" + swpar.getAlias() + "].",
                swpar.containsPathAlias( testPath ));
    assertTrue( "Alias replacement on given path [" + testPath + "] not as expected [" + expectedPath + "].",
                swpar.replacePathAlias( testPath ).equals( expectedPath ));

    testPath = "/slash/starting/multi/segment/alias/some/dir";

    alias = "/slash/starting/multi/segment/alias";
    replacementPath = "/replacement/path";
    swpar = new StartsWithPathAliasReplacement( alias, replacementPath);
    assertTrue( "Given path [" + testPath + "] does not start with alias [" + swpar.getAlias() + "].",
                swpar.containsPathAlias( testPath ));
    assertTrue( "Alias replacement on given path [" + testPath + "] not as expected [" + expectedPath + "].",
                swpar.replacePathAlias( testPath ).equals( expectedPath ));
  }
}
