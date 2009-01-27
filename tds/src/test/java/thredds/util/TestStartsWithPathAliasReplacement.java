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
