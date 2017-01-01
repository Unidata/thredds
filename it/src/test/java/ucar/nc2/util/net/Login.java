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

package ucar.nc2.util.net;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import ucar.httpservices.HTTPAuthUtil;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Login extends JFrame implements CredentialsProvider
{
    static final boolean DEBUG = true;

    protected String dfaltuser = null;
    protected String dfaltpwd = null;

    // In order to act as a proper CredentialsProvider, this class
    // needs to support the setCredentials operation properly.
    // This means that it must provide an internal database of
    // AuthScope->Credentials.
    // Because this class is interactive, entries in the internal database
    // must only be set by explicit calls to setCredentials().
    // Why do we need this? Because this provider will be used everywhere
    // in the path to the server to provide credentials at intermediate point.
    // So, it must be possible to set proxy credentials as well as
    // possible redirections (e.g. OAUTH2).

    private Map<AuthScope, Credentials> creds = new HashMap<>();


    /**
     * constructor
     */

    public Login(String user, String pwd)
    {
        this.setAlwaysOnTop(true);
        this.dfaltuser = user;
        this.dfaltpwd = pwd;
    }

    public void clear()
    {
        this.creds.clear();
    }

    public void setCredentials(AuthScope scope, Credentials cred)
    {
        this.creds.put(scope, cred);
    }

    public Credentials getCredentials(AuthScope scope)
    {
        Credentials credentials = this.creds.get(scope);
        if(credentials == null) {
            AuthScope bestMatch = HTTPAuthUtil.bestmatch(scope,this.creds.keySet());
            if(bestMatch != null)
                credentials = this.creds.get(bestMatch);
        }
        if(credentials != null)
            return credentials;

        String up = login(this.dfaltuser, this.dfaltpwd, scope);
        if(up == null) throw new IllegalStateException();
        String[] pieces = up.split("[:]");
        UsernamePasswordCredentials upc = null;
        if(pieces.length == 2) {
            if(DEBUG) {
                System.out.println("user= (" + pieces[0] + ")");
                System.out.println("password= (" + pieces[1] + ")");
            }
            // Is this really necessary?
            upc = new UsernamePasswordCredentials(pieces[0], pieces[1]);
        } else
            upc = null;
        return upc;
    }

    protected String login(String defaltuser, String dfaltpwd, AuthScope scope)
    {
        String user;
        String pwd;
        for(; ; ) {
            user = null;
            pwd = null;
            JPanel userPanel = new JPanel();
            userPanel.setLayout(new GridLayout(2, 3));
            userPanel.setPreferredSize(new Dimension(500,0));
            JLabel usernameLbl = new JLabel("Username:");
            JLabel passwordLbl = new JLabel("Password:");
            JTextField userFld = new JTextField();
            if(dfaltuser != null)
                userFld.setText(dfaltuser);
            JTextField passwordFld = new JTextField();
            if(dfaltpwd != null)
                passwordFld.setText(dfaltpwd);
            userPanel.add(usernameLbl);
            userPanel.add(userFld);
            userPanel.add(new JLabel(""));
            userPanel.add(passwordLbl);
            userPanel.add(passwordFld);
            userPanel.add(new JLabel(""));
            userPanel.setVisible(true);
            StringBuilder title = new StringBuilder();
            title.append("Enter password for: ");
            title.append(scope.getHost());
            if(scope.getPort() > 0) {
                title.append(":");
                title.append(scope.getPort());
            }
            int optionindex = JOptionPane.showConfirmDialog(null, userPanel, title.toString(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            switch (optionindex) {
                case 0: // ok
                    user = userFld.getText();
                    pwd = passwordFld.getText();
                    if(user != null && pwd != null)
                        return user + ":" + pwd;
                    break;
                default: // treat rest as cancel
                    break;
            }
            break;
        }
        return null;
    }
}

