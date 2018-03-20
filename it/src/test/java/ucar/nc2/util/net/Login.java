/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.net;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPAuthUtil;

import javax.swing.*;
import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class Login extends JFrame implements CredentialsProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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

