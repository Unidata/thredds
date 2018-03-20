/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet.restrict;

import org.jdom2.input.SAXBuilder;
import org.jdom2.JDOMException;
import org.jdom2.Element;

import java.io.*;
import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Implements RoleSource by reading XML in format of tomcat-users.xml.
 * see PluggableRestrictedAccess.html 
 *
 * @author caron
 */
public class RoleDatabase implements RoleSource {
  private HashMap<String, User> users = new HashMap<>();

  RoleDatabase( String filename) throws IOException {

    InputStream is = new BufferedInputStream( new FileInputStream( filename));
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    //   <user username="ccsmData" roles="ccsmData, restrictedDatasetUser"/>

    Element rootElem = doc.getRootElement();
    List<Element> elems = rootElem.getChildren("user");
    for (Element elem : elems) {
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

  static private class User {
    String name;
    ArrayList<String> roles = new ArrayList<>();
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
