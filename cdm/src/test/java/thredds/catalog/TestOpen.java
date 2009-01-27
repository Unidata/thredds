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

/* Test opening catalogs from web */

public class TestOpen extends TestCase {
  static private boolean showValidate = false;

  public TestOpen( String name) {
    super(name);
  }

  private InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);

  public String open(String catalogName, boolean shouldValidate) {

    StringBuilder buff = new StringBuilder();

    try {
      InvCatalog cat = catFactory.readXML(catalogName);
      boolean validate = cat.check( buff, false);
      if (!validate)
        System.out.println("TestOpen validate failed "+ catalogName+"\n"+ buff.toString()+"\n");

      if (validate != shouldValidate) {
        System.out.println("TestOpen ERROR validate "+ catalogName+" should be "+shouldValidate);
        System.out.println(" = <"+ buff.toString()+">");
        assert false;
      }

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

    return buff.toString();
  }

  public void testOpen() {
    // open( "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.xml", true);
    // open( "http://www.unidata.ucar.edu/projects/THREDDS/BARF/InvCatalog.0.6.xml", false);
    open( TestCatalogAll.makeFilepath("InvCatalog.0.6.xml"), true);
    open( TestCatalogAll.makeFilepath("InvCatalogBadDTD.xml"), true);
    open( TestCatalogAll.makeFilepath("InvCatalog.0.6d.xml"), false);
    open( TestCatalogAll.makeFilepath("ParseFails.xml"), false);
    open( TestCatalogAll.makeFilepath("TestAlias.xml"), true);
    open( TestCatalogAll.makeFilepath("BadService.xml"), true);
    open( TestCatalogAll.makeFilepath("enhancedCat.xml"), true); // */
    open( TestCatalogAll.makeFilepath("TestInherit.0.6.xml"), true);
  }

}
