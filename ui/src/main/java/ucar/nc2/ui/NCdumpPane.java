// $Id$
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
 * A text widget that does get and put to a web URL.
 *
 * @author caron
 * @version $Revision$ $Date$
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
      NCdump.CEresult cer = null;
      try {
        cer = NCdump.parseVariableSection(ds, command);
        v = cer.v;
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
        data = ds.read(command, true);

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
      ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
      PrintStream ps = new PrintStream(bos);
      try {
        data = ds.read(command, true);
        NCdump.printArray( data, null, ps, this);

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

}