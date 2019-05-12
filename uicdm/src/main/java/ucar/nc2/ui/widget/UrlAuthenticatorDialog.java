/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.widget;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import ucar.ui.prefs.Field;
import ucar.ui.prefs.PrefPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import ucar.ui.widget.IndependentDialog;

/**
 * This can be used both for java.net authentication:
 *   java.net.Authenticator.setDefault(new thredds.ui.UrlAuthenticatorDialog(frame));
 * <p>
 * or for org.apache.http authentication:
 *    httpclient.getParams().setParameter( CredentialsProvider.PROVIDER, new UrlAuthenticatorDialog( null));
 *
 * @author John Caron
 *         Modified 12/30/2015 by DMH to utilize an internal map so it
 *         can support multiple AuthScopes to handle e.g. proxies like
 *         BasicCredentialsProvider
 */
public class UrlAuthenticatorDialog extends Authenticator implements CredentialsProvider
{

  private IndependentDialog dialog;
  private UsernamePasswordCredentials pwa = null;
  private Field.Text serverF, realmF, userF;
  private Field.Password passwF;
  private boolean debug = false;

  /**
   * constructor
   *
   * @param parent JFrame
   */
  public UrlAuthenticatorDialog(javax.swing.JFrame parent)
  {
    PrefPanel pp = new PrefPanel("UrlAuthenticatorDialog", null);
    serverF = pp.addTextField("server", "Server", "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww");
    realmF = pp.addTextField("realm", "Realm", "");
    serverF.setEditable(false);
    realmF.setEditable(false);

    userF = pp.addTextField("user", "User", "");
    passwF = pp.addPasswordField("password", "Password", "");
    pp.addActionListener(e -> {
        char[] pw = passwF.getPassword();
        if (pw == null) return;
        pwa = new UsernamePasswordCredentials(userF.getText(), new String(pw));
        dialog.setVisible( false);
    });
      // button to dismiss
    JButton cancel = new JButton("Cancel");
    pp.addButton(cancel);
    cancel.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
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
  }

    // java.net calls this:
  protected PasswordAuthentication getPasswordAuthentication()
  {
    if (pwa == null) throw new IllegalStateException();

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

     if (debug) {
      System.out.println("user= ("+pwa.getUserName()+")");
      System.out.println("password= ("+ pwa.getPassword() +")");
    }

    return new PasswordAuthentication(pwa.getUserName(), pwa.getPassword().toCharArray());
  }

  // http client calls this:
 public Credentials getCredentials(AuthScope scope)
 {
    serverF.setText(scope.getHost()+":"+scope.getPort());
    String realmName = scope.getRealm();
    if (realmName == null) {
      realmName = "THREDDS Data Server";
    }
    realmF.setText(realmName);
    dialog.setVisible( true);
    if (pwa == null) throw new IllegalStateException();
    if (debug) {
      System.out.println("user= ("+pwa.getUserName()+")");
      System.out.println("password= ("+ pwa.getPassword() +")");
    }
    // Is this really necessary?
    UsernamePasswordCredentials upc = new UsernamePasswordCredentials(pwa.getUserName(),
        pwa.getPassword());
    return upc;
  }
}

