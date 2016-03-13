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

    protected Map<AuthScope, Credentials> cache = new HashMap<>();

    /**
     * constructor
     */
    public Login()
    {
    }

    public void clear()
    {
        this.cache.clear();
    }

    public void setCredentials(AuthScope scope, Credentials cred)
    {
        cache.put(scope, cred);
    }

    public Credentials getCredentials(AuthScope scope)
    {
        if(false) {
            Credentials creds = cache.get(scope);
            if(creds == null) {
                AuthScope bestMatch = HTTPAuthUtil.bestmatch(scope, cache.keySet());
                if(bestMatch != null) {
                    creds = cache.get(bestMatch);
                }
            }
            if(creds != null)
                return creds;
        }
        // Ok, ask and cache
        String up = login();
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
            //cache.put(scope, upc);
        } else
            upc = null;
        return upc;
    }

    protected String login()
    {
        String user;
        String pwd;
        for(; ; ) {
            user = null;
            pwd = null;

            JPanel userPanel = new JPanel();
            userPanel.setLayout(new GridLayout(2, 2));
            JLabel usernameLbl = new JLabel("Username:");
            JLabel passwordLbl = new JLabel("Password:");
            JTextField userFld = new JTextField();
            JPasswordField passwordFld = new JPasswordField();
            userPanel.add(usernameLbl);
            userPanel.add(userFld);
            userPanel.add(passwordLbl);
            userPanel.add(passwordFld);
            userPanel.setSize(1000, 500);
            userPanel.setVisible(true);
            int input = JOptionPane.showConfirmDialog(null, userPanel, "Enter your password:"
                    , JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            user = userFld.getText();
            pwd = passwordFld.getText();
            if(user != null && pwd != null)
                break;
        }
        return user + ":" + pwd;
    }
}

