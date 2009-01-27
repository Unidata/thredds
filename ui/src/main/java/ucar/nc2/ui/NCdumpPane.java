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

package ucar.nc2.ui;

import ucar.nc2.*;
import ucar.nc2.dt.image.ImageArrayAdapter;
import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;

import ucar.nc2.ui.image.ImageViewPanel;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

/**
 * dump data using NetcdfFile.readSection()
 *
 * @author caron
 */

public class NCdumpPane extends thredds.ui.TextHistoryPane {
  private static final String ImageViewer_WindowSize = "ImageViewer_WindowSize";

  private PreferencesExt prefs;
  private ucar.util.prefs.ui.ComboBox cb;
  private CommonTask task;
  private StopButton stopButton;

  private NetcdfFile ds;
 // private VariableIF v;

  public NCdumpPane(PreferencesExt prefs) {
    super(true);
    this.prefs = prefs;

    cb = new ComboBox(prefs);

    JButton getButton = new JButton("NCdump");
    getButton.setToolTipText("show selected data values");
    getButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ncdump( (String) cb.getSelectedItem());
      }
    });

    JButton imageButton = new JButton("Image");
    imageButton.setToolTipText("view selected data as Image");
    imageButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showImage( (String) cb.getSelectedItem());
      }
    });

    stopButton = new StopButton("stop NCdump");
    stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // System.out.println(" ncdump event="+e.getActionCommand());
        ta.setText( task.v.toString());
        ta.append("\n data:\n");
        ta.append(task.contents);

        if (e.getActionCommand().equals("success")) {
          cb.setSelectedItem(task.command); // add to combobox
        }
      }
    });

    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    buttPanel.add( getButton);
    buttPanel.add( imageButton);
    buttPanel.add( stopButton);

    JPanel topPanel = new JPanel( new BorderLayout());
    topPanel.add(new JLabel("Variable:"), BorderLayout.WEST);
    topPanel.add(cb, BorderLayout.CENTER);
    topPanel.add(buttPanel, BorderLayout.EAST);

    // setLayout( new BorderLayout());
    add( topPanel, BorderLayout.NORTH);
    // add( new JScrollPane(ta), BorderLayout.CENTER);
  }

  public void setContext(NetcdfFile ds, String command) {
    this.ds = ds;
    cb.addItem(command);
  }

  private void ncdump(String command) {
    if (ds == null) return;
    if (command == null) return;

    task = new NCdumpTask(command);
    if (task.v  != null)
      stopButton.startProgressMonitorTask( task);
  }

  private void showImage(String command) {
    if (ds == null) return;
    if (command == null) return;

    if (imageWindow == null)
      makeImageViewer();

    task = new GetContentsTask(command);
    if (task.v  != null)
      stopButton.startProgressMonitorTask( task);
  }

  private IndependentWindow imageWindow = null;
  private ImageViewPanel imageView = null;

  private void makeImageViewer() {
    imageWindow = new IndependentWindow("Image Viewer", BAMutil.getImage("ImageData"));
    imageView = new ImageViewPanel( null);
    imageWindow.setComponent( new JScrollPane(imageView));
    //imageWindow.setComponent( imageView);
    Rectangle b = (Rectangle) prefs.getBean(ImageViewer_WindowSize, new Rectangle(99, 33, 700, 900));
    //System.out.println("bounds in = "+b);
    imageWindow.setBounds( b);
  }


  public void save() {
    cb.save();
    if (imageWindow != null) {
      prefs.putBeanObject(ImageViewer_WindowSize, imageWindow.getBounds());
      //System.out.println("bounds out = "+imageWindow.getBounds());
    }
  }

  public void clear() {
    ta.setText(null);
  }

  public String getText() {
    return ta.getText();
  }

  public void gotoTop() {
    ta.setCaretPosition(0);
  }

  public void setText(String text) {
    ta.setText(text);
  }

  private abstract class CommonTask extends thredds.ui.ProgressMonitorTask implements ucar.nc2.util.CancelTask {
    String contents, command;
    ucar.nc2.Variable v = null;
    ucar.ma2.Array data;

    CommonTask(String command) {
      this.command = command;
      try {
        ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ds, command);
        while (cer != null) {  // get inner variable
          v = cer.v;
          cer = cer.child;
        }
      } catch (Exception e) {
        ta.setText(e.getMessage());
      }
     }
  }

  private class GetContentsTask extends CommonTask {
    GetContentsTask(String command) {
      super(command);
    }

    public void run() {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
      PrintStream ps = new PrintStream(bos);
      try {
        data = ds.readSection(command);

        if (data != null) {
          imageView.setImage(ImageArrayAdapter.makeGrayscaleImage( task.data));
          imageWindow.show();
        }

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace( new PrintStream(bos));
        contents = bos.toString();

        setError(e.getMessage());
        done = true;
        return;
      }

      if (cancel)
        ps.println("\n***Cancelled by User");
      contents = bos.toString();

      success = !cancel;
      done = true;
    }
  }

  private class NCdumpTask extends CommonTask {

    NCdumpTask(String command) {
      super(command);
    }

    public void run() {
      try {
        data = ds.readSection(command);
        contents = NCdumpW.printArray( data, null, this);

      } catch (Exception e) {
        e.printStackTrace();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
        e.printStackTrace( new PrintStream(bos));
        contents = bos.toString();

        setError(e.getMessage());
        done = true;
        return;
      }

      if (cancel)
        contents = "\n***Cancelled by User";

      success = !cancel;
      done = true;
    }
  }

}