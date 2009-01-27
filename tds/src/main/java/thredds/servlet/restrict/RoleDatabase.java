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

package thredds.servlet.restrict;

import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Element;

import java.io.*;
import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class Description.
 *
 * @author caron
 */
public class RoleDatabase implements RoleSource {
  private HashMap<String, User> users = new HashMap<String, User>();

  RoleDatabase( String filename) throws IOException {

    InputStream is = new BufferedInputStream( new FileInputStream( filename));
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    //   <user username="ccsmData" roles="ccsmData, restrictedDatasetUser"/>

    Element rootElem = doc.getRootElement();
    List elems = rootElem.getChildren("user");
    for (Object elem1 : elems) {
      Element elem = (Element) elem1;
      String username = elem.getAttributeValue("username");
      User user = new User(username);
      String roles = elem.getAttributeValue("roles");
      StringTokenizer stoke = new StringTokenizer(roles, ", ");
      while (stoke.hasMoreTokens()) {
        String role = stoke.nextToken();
        user.add(role);
      }
      users.put(username, user);
    }

  }

  private class User {
    String name;
    ArrayList<String> roles = new ArrayList<String>();
    User( String name) {
      this.name = name;
    }
    void add( String role) {
      roles.add(role);
    }
  }

  public boolean hasRole( String username, String role)  {
    User user = users.get( username);
    if (user == null) return false;
    for (String role1 : user.roles) {
      if (role.equals(role1))
        return true;
    }
    return false;
  }

}
