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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
        Credentials creds = cache.get(scope);
        if(creds == null) {
            AuthScope bestMatch = HTTPAuthUtil.bestmatch(scope, cache.keySet());
            if(bestMatch != null) {
                creds = cache.get(bestMatch);
            }
        }
        if(creds != null)
            return creds;
        // Ok, ask and cache
        String up = login(this);
        if(up == null) throw new IllegalStateException();
        String[] pieces = up.split("[:]");
        if(DEBUG) {
            System.out.println("user= (" + pieces[0] + ")");
            System.out.println("password= (" + pieces[1] + ")");
        }
        // Is this really necessary?
        UsernamePasswordCredentials upc = new UsernamePasswordCredentials(pieces[0], pieces[1]);
        cache.put(scope, upc);
        return upc;
    }

    protected String login(JFrame frame)
    {
        String user;
        String pwd;
        for(; ; ) {
            user = null;
            pwd = null;
            LoginDialog dialog = new LoginDialog(frame);
            dialog.setVisible(true);
            if(dialog.cancelled) return null;
            user = dialog.user;
            pwd = dialog.pwd;
            if(user != null && pwd != null)
                break;
        }
        return user + ":" + pwd;
    }

    static class LoginDialog extends JDialog
    {
        protected final JLabel jlblUsername = new JLabel("Username");
        protected final JLabel jlblPassword = new JLabel("Password");
        protected final JTextField jtfUsername = new JTextField(15);
        protected final JTextField jpfPassword = new JTextField(15);
        protected final JButton jbtOk = new JButton("Login");
        protected final JButton jbtCancel = new JButton("Cancel");
        protected final JLabel jlblStatus = new JLabel(" ");

        public LoginDialog()
        {
            this(null);
        }

        public String user = null;
        public String pwd = null;
        public boolean cancelled;

        public LoginDialog(final JFrame parent)
        {
            super(parent, false);

            user = null;
            pwd = null;
            cancelled = false;

            JPanel p3 = new JPanel(new GridLayout(2, 1));
            p3.add(jlblUsername);
            p3.add(jlblPassword);

            JPanel p4 = new JPanel(new GridLayout(2, 1));
            p4.add(jtfUsername);
            p4.add(jpfPassword);

            JPanel p1 = new JPanel();
            p1.add(p3);
            p1.add(p4);

            JPanel p2 = new JPanel();
            p2.add(jbtOk);
            p2.add(jbtCancel);

            JPanel p5 = new JPanel(new BorderLayout());
            p5.add(p2, BorderLayout.CENTER);
            p5.add(jlblStatus, BorderLayout.NORTH);
            jlblStatus.setForeground(Color.RED);
            jlblStatus.setHorizontalAlignment(SwingConstants.CENTER);

            setLayout(new BorderLayout());
            add(p1, BorderLayout.CENTER);
            add(p5, BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                }
            });

            jbtOk.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    user = jtfUsername.getText();
                    pwd = jpfPassword.getText();
                }
            });
            jbtCancel.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    setVisible(false);
                    parent.dispose();
                    cancelled = true;
                }
            });
        }
    }
}
