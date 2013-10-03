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
package ucar.nc2.ui.widget;

import java.awt.event.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import javax.swing.JButton;


import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.auth.RFC2617Scheme;
import ucar.util.prefs.ui.*;

/**
 * This can be used both for java.net authentication:
 *   java.net.Authenticator.setDefault(new thredds.ui.UrlAuthenticatorDialog(frame));
 *
 * or for org.apache.http authentication:
 *    httpclient.getParams().setParameter( CredentialsProvider.PROVIDER, new UrlAuthenticatorDialog( null));
 *
 * @author John Caron
 */
public class UrlAuthenticatorDialog extends Authenticator implements CredentialsProvider
{

  private IndependentDialog dialog;
  private UsernamePasswordCredentials pwa = null;
  private Field.Text serverF, realmF, userF;
  private Field.Password passwF;
  private boolean debug = false;

  private AuthScope anyscope = new  AuthScope(AuthScope.ANY);

  /** constructor
     @param parent JFrame
   */
  public UrlAuthenticatorDialog(javax.swing.JFrame parent) {
    PrefPanel pp = new PrefPanel("UrlAuthenticatorDialog", null);
    serverF = pp.addTextField("server", "Server", "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww");
    realmF = pp.addTextField("realm", "Realm", "");
    serverF.setEditable(false);
    realmF.setEditable(false);

    userF = pp.addTextField("user", "User", "");
    passwF = pp.addPasswordField("password", "Password", "");
    pp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pwa = new UsernamePasswordCredentials(userF.getText(), new String(passwF.getPassword()));
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

  public void clear()
  {
  }

  public void setCredentials(AuthScope scope, Credentials cred)
  {
      //todo: provider.setCredentials(scope,cred);
  }

    // java.net calls this:                           g
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

    return new PasswordAuthentication(pwa.getUserName(),pwa.getPassword().toCharArray());
  }


  // http client calls this:
 public Credentials getCredentials(AuthScope scope) //AuthScheme scheme, String host, int port, boolean proxy)
 {
    //todo: if (!(scope.getScheme()RFC2617Scheme))
      //throw new UnsupportedOperationException( "Unsupported authentication scheme: " +
      //                  scope.getScheme().toString()) ;

    if (debug) {
      System.out.println(scope.getHost() + ":" + scope.getPort() + " requires authentication with the realm '" + scope.getRealm() + "'");
    }

    serverF.setText(scope.getHost()+":"+scope.getPort());
    realmF.setText(scope.getRealm());
    dialog.setVisible( true);
    if (pwa == null) throw new IllegalStateException();

    if (debug) {
      System.out.println("user= ("+pwa.getUserName()+")");
      System.out.println("password= ("+new String(pwa.getPassword())+")");
    }

    return new UsernamePasswordCredentials(pwa.getUserName(), new String(pwa.getPassword()));

    /* if (null != httpSession) {
      AuthScope authScope = new AuthScope( host, port, scheme.getRealm());
      httpSession.setDefaultCredentials( authScope, cred);
    } */

    // return cred;
  }

  /* private HttpSession httpSession = null;
  public void setHttpSession(HttpSession httpSession) {
    this.httpSession = httpSession;
  } */
}

