// $Id: UrlAuthenticatorDialog.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.ui;

import java.awt.event.*;
import javax.swing.JButton;
import java.net.*;
import ucar.util.prefs.ui.*;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.RFC2617Scheme;
import org.apache.commons.httpclient.auth.AuthScope;
import thredds.util.net.HttpSession;

/**
 * This can be used both for java.net authentication:
 *   java.net.Authenticator.setDefault(new thredds.ui.UrlAuthenticatorDialog(frame));
 *
 * or for org.apache.commons.httpclient authentication:
 *    httpclient.getParams().setParameter( CredentialsProvider.PROVIDER, new UrlAuthenticatorDialog( null));
 *
 * @author John Caron
 * @version $Id: UrlAuthenticatorDialog.java 50 2006-07-12 16:30:06Z caron $
 */
public class UrlAuthenticatorDialog extends Authenticator implements
        thredds.util.net.CredentialsProviderExt {

  private IndependentDialog dialog;
  private PasswordAuthentication pwa = null;
  private PrefPanel pp;
  private Field.Text serverF, realmF, userF;
  private Field.Password passwF;
  private boolean debug = false;

  /** constructor
     @param parent JFrame
   */
  public UrlAuthenticatorDialog(javax.swing.JFrame parent) {
    pp = new PrefPanel("UrlAuthenticatorDialog", null);
    serverF = pp.addTextField("server", "Server", "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww");
    realmF = pp.addTextField("realm", "Realm", "");
    serverF.setEditable(false);
    realmF.setEditable(false);

    userF = pp.addTextField("user", "User", "");
    passwF = pp.addPasswordField("password", "Password", "");
    pp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pwa = new PasswordAuthentication(userF.getText(), passwF.getPassword());
        dialog.setVisible( false);
      }
    });
      // button to dismiss
    JButton cancel = new JButton("Cancel");
    pp.addButton(cancel);
    cancel.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        pwa = null;
        dialog.setVisible( false);
      }
    });
    pp.finish();

    dialog = new IndependentDialog(parent, true, "HTTP Authentication", pp);
    dialog.setLocationRelativeTo(parent);
    dialog.setLocation(100, 100);
  }

  // java.net calls this:
  protected PasswordAuthentication getPasswordAuthentication() {

    if (debug) {
      System.out.println("site= " + getRequestingSite());
      System.out.println("port= " + getRequestingPort());
      System.out.println("protocol= " + getRequestingProtocol());
      System.out.println("prompt= " + getRequestingPrompt());
      System.out.println("scheme= " + getRequestingScheme());
    }

    serverF.setText(getRequestingHost()+":"+getRequestingPort());
    realmF.setText(getRequestingPrompt());
    dialog.setVisible( true);

     if (debug && pwa != null) {
      System.out.println("user= ("+pwa.getUserName()+")");
      System.out.println("password= ("+new String(pwa.getPassword())+")");
    }

    return pwa;
  }

  // http client calls this:
  public Credentials getCredentials(AuthScheme scheme, String host, int port, boolean proxy) throws CredentialsNotAvailableException {
    if (scheme == null)
      throw new CredentialsNotAvailableException( "Null authentication scheme: ");

    if (!(scheme instanceof RFC2617Scheme))
      throw new CredentialsNotAvailableException( "Unsupported authentication scheme: " +
                        scheme.getSchemeName());

    if (debug) {
      System.out.println(host + ":" + port + " requires authentication with the realm '" + scheme.getRealm() + "'");
    }

    serverF.setText(host+":"+port);
    realmF.setText(scheme.getRealm());
    dialog.setVisible( true);
    if (pwa == null) throw new CredentialsNotAvailableException();

    if (debug) {
      System.out.println("user= ("+pwa.getUserName()+")");
      System.out.println("password= ("+new String(pwa.getPassword())+")");
    }

    UsernamePasswordCredentials cred = new UsernamePasswordCredentials(pwa.getUserName(), new String(pwa.getPassword()));

    if (null != httpSession) {
      AuthScope authScope = new AuthScope( host, port, scheme.getRealm());
      httpSession.setDefaultCredentials( authScope, cred);
    }

    return cred;
  }

  private HttpSession httpSession = null;
  public void setHttpSession(HttpSession httpSession) {
    this.httpSession = httpSession;
  }
}

