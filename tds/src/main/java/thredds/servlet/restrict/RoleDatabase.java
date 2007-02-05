// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
 * @version $Revision$ $Date$
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
    for (int i = 0; i < user.roles.size(); i++) {
      if (role.equals(user.roles.get(i)))
        return true;
    }
    return false;
  }

}
