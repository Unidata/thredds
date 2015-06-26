/*
 * $Id: HttpFormEntry.java,v 1.18 2007/07/06 20:45:31 jeffmc Exp $
 *
 * Copyright  1997-2015 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.unidata.util.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


/**
 * Class HttpFormEntry Represents a http form post input field.
 *
 * @author IDV Development Team
 * @version $Revision: 1.18 $
 */
public class HttpFormEntry
{
    //////////////////////////////////////////////////
    // Constants

    public static final int TYPE_NONE = -1;

    /**
     * Hidden input field type
     */
    public static final int TYPE_HIDDEN = 0;

    /**
     * Normal input field type
     */
    public static final int TYPE_INPUT = 1;

    /**
     * Multi-line input field type
     */
    public static final int TYPE_AREA = 2;

    /**
     * Just a label in the gui
     */
    public static final int TYPE_LABEL = 3;

    /**
     * File reference
     */
    public static final int TYPE_FILE = 4;

    protected static final Charset UTF8 = Charset.forName("UTF-8");
    protected static final Charset ASCII = Charset.forName("US-ASCII");

    //////////////////////////////////////////////////
    // Instance Fields

    /**
     * The type of input field
     */
    private int type;

    /**
     * The http post name
     */
    private String name;

    /**
     * THe label in the gui
     */
    private String label;

    /**
     * The initial value
     */
    private String value;

    /**
     * How many rows if this is a text area
     */
    private int rows;

    /**
     * How many columns if this is a text area
     */
    private int cols;

    /**
     * The component. (e.g., JTextField, JTextArea)
     */
    private JComponent component;

    /**
     * Is this field required
     */
    private boolean required = true;

    /**
     * file part source
     */
    private ContentBody filePartSource;

    /**
     * filename
     */
    private String fileName;

    /**
     * Create an entry that already holds the byte contents of a file.
     * Having an entry like this will result in a multi-part post
     *
     * @param name     The name of the file
     * @param fileName filename - this is the name that is posted
     * @param bytes    the bytes
     */
    public HttpFormEntry(String name, final String fileName,
                         final byte[] bytes)
    {
        this.name = name;
        type = TYPE_FILE;
        this.filePartSource = new ByteArrayBody(bytes, fileName);
        this.fileName = fileName;
    }

    public HttpFormEntry(String name, String label, String value)
    {
        this(TYPE_INPUT, name, label, value);
    }


    public static HttpFormEntry hidden(String name, String value)
    {
        return new HttpFormEntry(TYPE_HIDDEN, name, "", value);
    }

    /**
     * Create the entry
     *
     * @param name  The name
     * @param label The label
     */
    public HttpFormEntry(String name, String label)
    {
        this(TYPE_INPUT, name, label);
    }

    /**
     * Create the entry
     *
     * @param type  The type of this entry
     * @param name  The name
     * @param label The label
     */
    public HttpFormEntry(int type, String name, String label)
    {
        this(type, name, label, "", 1, 30);
    }

    /**
     * Create the entry
     *
     * @param type  The type of this entry
     * @param name  The name
     * @param label The label
     * @param value Initial value
     */
    public HttpFormEntry(int type, String name, String label, String value)
    {
        this(type, name, label, value, 1, 30, true);
    }

    /**
     * Create the entry
     *
     * @param type     The type of this entry
     * @param name     The name
     * @param label    The label
     * @param value    Initial value
     * @param required Is this entry required
     */
    public HttpFormEntry(int type, String name, String label, String value,
                         boolean required)
    {
        this(type, name, label, value, 1, 30, required);
    }

    /**
     * Create the entry
     *
     * @param type  The type of this entry
     * @param name  The name
     * @param label The label
     * @param rows  How may rows in the text area
     * @param cols  How many columns in the text area
     */
    public HttpFormEntry(int type, String name, String label, int rows,
                         int cols)
    {
        this(type, name, label, "", rows, cols);
    }

    /**
     * Create the entry
     *
     * @param type  The type of this entry
     * @param name  The name
     * @param label The label
     * @param value Initial value
     * @param rows  How many rows
     * @param cols  How many cols
     */
    public HttpFormEntry(int type, String name, String label, String value,
                         int rows, int cols)
    {
        this(type, name, label, value, rows, cols, true);
    }

    /**
     * Create the entry
     *
     * @param type     The type of this entry
     * @param name     The name
     * @param label    The label
     * @param value    Initial value
     * @param rows     How many rows
     * @param cols     How many cols
     * @param required Is this entry required
     */
    public HttpFormEntry(int type, String name, String label, String value,
                         int rows, int cols, boolean required)
    {
        this.type = type;
        this.name = name;
        this.label = label;
        this.value = value;
        this.rows = rows;
        this.cols = cols;
        this.required = required;
    }


    /**
     * Is this entry ok. That is, has their been input if it is required
     *
     * @return Is ok
     */
    public boolean ok()
    {
        if(type == TYPE_LABEL) {
            return true;
        }
        return !required || (getValue() != null && getValue().trim().length() > 0);
    }

    //////////////////////////////////////////////////
    // Accessors

    /**
     * Get the value the user entered.
     *
     * @return The input value
     */
    public String getValue()
    {
        if(type == TYPE_HIDDEN) {
            return value;
        }
        if(type == TYPE_LABEL) {
            return null;
        }
        if(component != null) {
            return ((JTextComponent) component).getText();
        }
        return value;
    }

    /**
     * Set the text value to the given newValue
     *
     * @param newValue The new text
     */
    public void setValue(String newValue)
    {
        if(type == TYPE_HIDDEN) {
            this.value = newValue;
        }
        if(type == TYPE_LABEL) {
        } else if(component != null) {
            ((JTextComponent) component).setText(newValue);
        }
    }

    /**
     * Get the name
     *
     * @return The name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the label
     *
     * @return The label
     */
    public String getLabel()
    {
        return label;
    }

    //////////////////////////////////////////////////
    // GUI

    /**
     * Add the label/gui component into the list of components
     *
     * @param guiComps A list.
     */
    public void addToGui(List guiComps)
    {
        if(type == TYPE_HIDDEN) {
            return;
        }
        if(type == TYPE_AREA) {
            guiComps.add(GuiUtils.top(GuiUtils.rLabel(label)));
            if(component == null) {
                component = new JTextArea(value, rows, cols);
                ((JTextArea) component).setLineWrap(true);
            }
            JScrollPane sp = new JScrollPane(component);
            sp.setPreferredSize(new Dimension(500, 200));
            guiComps.add(sp);
        } else if(type == TYPE_INPUT) {
            guiComps.add(GuiUtils.rLabel(label));
            if(component == null) {
                component = new JTextField(value, cols);
            }
            guiComps.add(component);
        } else if(type == TYPE_LABEL) {
            guiComps.add(new JLabel(""));
            guiComps.add(new JLabel(label));
        } else if(type == TYPE_FILE) {
            if(filePartSource == null) {
                guiComps.add(GuiUtils.rLabel(label));
                if(component == null) {
                    component = new JTextField(value);
                }
                JButton btn = GuiUtils.makeButton("Browse...", this,
                        "browse", component);
                GuiUtils.setHFill();
                guiComps.add(GuiUtils.doLayout(new Component[]{component,
                        btn}, 2, GuiUtils.WT_YN, GuiUtils.WT_N));
            }
        }
    }

    /**
     * Open a file browser associated with the text field
     *
     * @param fld the JTextField
     */
    public void browse(JTextField fld)
    {
        String f = fld.getText();
        JFileChooser chooser = new FileManager.MyFileChooser(f);
        if(chooser.showOpenDialog(fld) == JFileChooser.APPROVE_OPTION) {
            fld.setText(chooser.getSelectedFile().toString());
        }
    }

    /**
     * Create the GUI from the list of entries
     *
     * @param entries List of entries
     * @return The gui
     */
    public static JComponent makeUI(List<HttpFormEntry> entries)
    {
        List guiComps = new ArrayList();
        for(int i = 0; i < entries.size(); i++) {
            entries.get(i).addToGui(guiComps);
        }
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        return GuiUtils.doLayout(guiComps, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
    }

    /**
     * Check the entries to make sure they have been filled in
     *
     * @param entries list of entries
     * @return false if some are not filled in.
     */
    public static boolean checkEntries(List<HttpFormEntry> entries)
    {
        for(int i = 0; i < entries.size(); i++) {
            HttpFormEntry formEntry = entries.get(i);
            if(!formEntry.ok()) {
                LogUtil.userMessage("The entry: \"" + formEntry.getLabel()
                        + "\" is required");
                return false;
            }
        }
        return true;
    }


    /**
     * Show the UI in a modeful dialog.
     * Note: this method <b>should not</b> be called
     * from a swing process. It does a busy wait on the dialog and does not rely on
     * the modality of the dialog to do its wait.
     *
     * @param entries  List of entries
     * @param title    The dialog title
     * @param window   The parent window
     * @param extraTop If non-null then this is added to the top of the gui. It allows
     *                 you to provide a label, etc.
     * @return Did user press ok
     */
    public static boolean showUI(List<HttpFormEntry> entries, String title, Window window,
                                 JComponent extraTop)
    {
        return showUI(entries, title, window, extraTop, null);
    }

    /**
     * Show the UI in a modeful dialog.
     * Note: this method <b>should not</b> be called
     * from a swing process. It does a busy wait on the dialog and does not rely on
     * the modality of the dialog to do its wait.
     *
     * @param entries     List of entries
     * @param title       The dialog title
     * @param parent      The parent window
     * @param extraTop    If non-null then this is added to the top of the gui. It allows
     *                    you to provide a label, etc.
     * @param extraBottom Like extraTop but on the bottom of the window
     * @return Did user press ok
     */
    public static boolean showUI(List<HttpFormEntry> entries, String title, Window parent,
                                 JComponent extraTop,
                                 JComponent extraBottom)
    {

        JDialog dialog = GuiUtils.createDialog(parent, title, true);
        boolean ok = showUI(entries, extraTop, extraBottom, dialog,
                false);
        dialog.dispose();
        return ok;
    }

    /**
     * Show the UI in a modeful dialog. Note: The calling method is responsible
     * for disposing of the dialog. Also: This method <b>should not</b> be called
     * from a swing process. It does a busy wait on the dialog and does not rely on
     * the modality of the dialog to do its wait.
     *
     * @param entries          List of entries
     * @param extraTop         If non-null then this is added to the top of the gui.
     *                         It allows you to provide a label, etc.
     * @param extraBottom      Like extraTop but on the bottom of the window
     * @param dialog           the dialog
     * @param shouldDoBusyWait true to wait
     * @return Did user press ok
     */
    public static boolean showUI(final List<HttpFormEntry> entries, JComponent extraTop,
                                 JComponent extraBottom,
                                 final JDialog dialog,
                                 final boolean shouldDoBusyWait)
    {


        dialog.getContentPane().removeAll();
        JComponent contents = makeUI(entries);
        if(extraTop != null) {
            contents = GuiUtils.topCenter(extraTop, contents);
        }

        if(extraBottom != null) {
            contents = GuiUtils.centerBottom(contents, extraBottom);
        }
        final boolean[] done = {false};
        final boolean[] ok = {false};

        ActionListener listener = new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                String cmd = ae.getActionCommand();
                if(cmd.equals(GuiUtils.CMD_CANCEL)) {
                    done[0] = true;
                }
                if(cmd.equals(GuiUtils.CMD_SUBMIT)) {
                    if(checkEntries(entries)) {
                        done[0] = true;
                        ok[0] = true;
                    }
                }
                if(done[0] && !shouldDoBusyWait) {
                    dialog.dispose();
                }
            }
        };

        JComponent buttons = GuiUtils.makeButtons(listener,
                new String[]{GuiUtils.CMD_SUBMIT,
                        GuiUtils.CMD_CANCEL});
        contents = GuiUtils.centerBottom(contents, buttons);
        dialog.getContentPane().add(contents);
        dialog.pack();
        GuiUtils.showInCenter(dialog);
        if(shouldDoBusyWait) {
            while(!done[0]) {
                Misc.sleep(100);
            }
        }
        return ok[0];
    }

    //////////////////////////////////////////////////
    // Form Data

    /**
     * Get the file part
     *
     * @return the file part
     */
    private ContentBody getFilePart()
    {
        if(filePartSource == null) {
            final String filename = getValue();
            filePartSource = new FileBody(new File(filename));
        }
        return filePartSource;
    }

    /**
     * Post the file
     *
     * @return true if posted
     */
    public boolean okToPost()
    {
        if(type == TYPE_LABEL) {
            return false;
        }

        if(type == TYPE_FILE) {
            if(filePartSource == null) {
                return new File(getValue()).exists();
            }
        }
        return true;
    }


    /**
     * Post the given entries to the given url
     *
     * @param entries The entries
     * @param urlPath The url to post to
     * @return 2 element array. First element is non-null if there was an error.
     * Second element is non-null if no error. This is the returned html.
     */
    public static String[] doPost(List<HttpFormEntry> entries, String urlPath)
    {
        String[] results = null;
        try {
            try (HTTPMethod postMethod = getMethod(entries, urlPath)) {
                postMethod.getSession().setMaxRedirects(5);
                try {
                    postMethod.execute();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                String result = IOUtil.readContents(postMethod.getResponseBodyAsStream());
                int code = postMethod.getStatusCode();
                if(code >= 300)
                    results = new String[]{result, null, Integer.toString(code, 10)};
                else
                    results = new String[]{null, result, Integer.toString(code, 10)};
            }
        } catch (Exception exc) {
            throw new WrapperException("doing post", exc);
        }
        return results;
    }

    private static HTTPMethod getMethod(List<HttpFormEntry> entries, String urlPath)
            throws IOException
    {
        HTTPMethod postMethod = HTTPFactory.Post(urlPath);
        boolean anyFiles = false;
        int count = 0;
        List<HttpFormEntry> goodEntries = new ArrayList<>();
        for(int i = 0; i < entries.size(); i++) {
            HttpFormEntry formEntry = entries.get(i);
            if(!formEntry.okToPost()) {
                continue;
            }
            goodEntries.add(entries.get(i));
            if(formEntry.type == TYPE_FILE)
                anyFiles = true;
        }
        HttpEntity postentity = null;
        if(anyFiles) { // Convert to Multipart
            MultipartEntity mpe = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            for(int i = 0; i < goodEntries.size(); i++) {
                HttpFormEntry formEntry = goodEntries.get(i);
                ContentBody part = null;
                if(formEntry.type == TYPE_FILE) {
                    part = formEntry.getFilePart();
                } else {
                    //Not sure why but we have seen a couple of times
                    //the byte value '0' gets into one of these strings
                    //This causes an error.
                    String value = formEntry.getValue();
                    char with = new String(" ").charAt(0);
                    value = value.replace((char) 0, with);
                    try {
                        part = StringBody.create(value, "text/plain", ASCII);
                    } catch (Exception e) {
                        throw e;
                    }
                }
                mpe.addPart(formEntry.getName(), part);
            }
            postentity = mpe;
        } else {
            List<NameValuePair> params = new ArrayList<>();
            for(int i = 0; i < goodEntries.size(); i++) {
                HttpFormEntry formEntry = goodEntries.get(i);
                try {
                    params.add(new BasicNameValuePair(formEntry.getName(), formEntry.getValue()));
                } catch (Exception e) {
                    throw e;
                }
            }
            UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(params);
            postentity = uefe;
        }
        postMethod.setRequestContent(postentity);
        return postMethod;
    }


}

