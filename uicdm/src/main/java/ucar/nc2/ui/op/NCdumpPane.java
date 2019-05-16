/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.IsMissingEvaluator;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Variable;
import ucar.nc2.dt.image.image.ImageArrayAdapter;
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.ui.widget.*;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.ComboBox;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.channels.WritableByteChannel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * dump data using NetcdfFile.readSection()
 *
 * @author caron
 */
public class NCdumpPane extends TextHistoryPane {

    private static final org.slf4j.Logger logger
                            = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ImageViewer_WindowSize = "ImageViewer_WindowSize";

    private PreferencesExt prefs;
    private ComboBox cb;
    private CommonTask task;
    private StopButton stopButton;
    private FileManager fileChooser;

    private NetcdfFile ds;

    private IndependentWindow imageWindow;
    private ImageViewPanel imageView;

/**
 *
 */
    public NCdumpPane(PreferencesExt prefs) {
        super(true);
        this.prefs = prefs;
        fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

        cb = new ComboBox(prefs);

        final JButton getButton = new JButton("NCdump");
        getButton.setToolTipText("show selected data values");
        getButton.addActionListener(e -> ncdump( (String) cb.getSelectedItem()));

        final JButton imageButton = new JButton("Image");
        imageButton.setToolTipText("view selected data as Image");
        imageButton.addActionListener(e -> showImage((String) cb.getSelectedItem()));

        final JButton binButton = new JButton("Write");
        binButton.setToolTipText("write binary data to file");
        binButton.addActionListener(e -> {
            String binaryFilePath = fileChooser.chooseFilenameToSave("data.bin");
            if (binaryFilePath != null) {
                writeBinaryData((String) cb.getSelectedItem(), new File(binaryFilePath));
            }
        });

        stopButton = new StopButton("stop NCdump");
        stopButton.addActionListener(e -> {
            // logger.debug(" ncdump event={}", e.getActionCommand());
            ta.setText( task.v.toString());
            ta.append("\n data:\n");
            ta.append(task.contents);

            if (e.getActionCommand().equals("success")) {
                cb.setSelectedItem(task.command); // add to combobox
            }
        });

        final JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttPanel.add( getButton);
        buttPanel.add( imageButton);
        buttPanel.add( binButton);
        buttPanel.add( stopButton);

        final JPanel topPanel = new JPanel( new BorderLayout());
        topPanel.add(new JLabel("Variable:"), BorderLayout.WEST);
        topPanel.add(cb, BorderLayout.CENTER);
        topPanel.add(buttPanel, BorderLayout.EAST);

        // setLayout( new BorderLayout());
        add( topPanel, BorderLayout.NORTH);
        // add( new JScrollPane(ta), BorderLayout.CENTER);
    }

/**
 *
 */
    public void setContext(NetcdfFile ds, String command) {
        this.ds = ds;
        cb.addItem(command);
    }

/**
 *
 */
    private void ncdump(String command) {
        if (ds == null) { return; }
        if (command == null) { return; }

        task = new NCdumpTask(command);
        if (task.v  != null) {
            stopButton.startProgressMonitorTask( task);
        }
    }

/**
 *
 */
    private void showImage(String command) {
        if (ds == null) { return; }
        if (command == null) { return; }

        if (imageWindow == null) {
            makeImageViewer();
        }

        task = new GetContentsTask(command);
        if (task.v  != null) {
            stopButton.startProgressMonitorTask( task);
        }
  }

/**
 */
    private void writeBinaryData(String variableSection, File name) {
        ParsedSectionSpec cer;
        try {
            cer = ParsedSectionSpec.parseVariableSection(ds, variableSection);
            if (cer.child != null) { return; }
        }
        catch (InvalidRangeException e) {
          e.printStackTrace();
          return;
        }

        if (name == null) { return; }

        try (FileOutputStream stream = new FileOutputStream(name)) {
            final WritableByteChannel channel = stream.getChannel();
            cer.v.readToByteChannel(cer.section, channel);
            System.out.printf("Write ok to %s%n", name);
        }
        catch (InvalidRangeException | IOException e) {
            e.printStackTrace();
        }
  }

/**
 *
 */
    private void makeImageViewer() {
        imageWindow = new IndependentWindow("Image Viewer", BAMutil.getImage("nj22/ImageData"));
        imageView = new ImageViewPanel( null);
        imageWindow.setComponent( new JScrollPane(imageView));
        //imageWindow.setComponent( imageView);
        final Rectangle b = (Rectangle) prefs.getBean(ImageViewer_WindowSize, new Rectangle(99, 33, 700, 900));
        // logger.debu("bounds in = {}", b);
        imageWindow.setBounds( b);
    }

/**
 *
 */
    public void save() {
        cb.save();
        fileChooser.save();

        if (imageWindow != null) {
            prefs.putBeanObject(ImageViewer_WindowSize, imageWindow.getBounds());
            // logger.debug("bounds out = {}", imageWindow.getBounds());
        }
  }

/**
 *
 */
    public void clear() {
        ta.setText(null);
    }

/**
 *
 */
    public String getText() {
        return ta.getText();
    }

/**
 *
 */
    public void gotoTop() {
        ta.setCaretPosition(0);
    }

/**
 *
 */
    public void setText(String text) {
        ta.setText(text);
    }

/**
 *
 */
    private abstract class CommonTask extends ProgressMonitorTask implements ucar.nc2.util.CancelTask {
        String contents, command;
        Variable v;
        Array data;
        IsMissingEvaluator eval;

    /**
     *
     */
        CommonTask(String command) {
            this.command = command;
            try {
                ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(ds, command);
                while (cer != null) {  // get inner variable
                    v = cer.v;
                    cer = cer.child;
                }
                if (v instanceof IsMissingEvaluator) {
                    eval = (IsMissingEvaluator) v;
                }
            }
            catch (Exception e) {
                ta.setText(e.getMessage());
            }
        }
    }

/**
 *
 */
    private class GetContentsTask extends CommonTask {
    /**
     *
     */
        GetContentsTask(String command) {
            super(command);
        }

    /**
     *
     */
        public void run() {
            StringWriter sw = new StringWriter(100000);
            PrintWriter ps = new PrintWriter(sw);
            try {
                data = ds.readSection(command);

                if (data != null) {
                  imageView.setImage(ImageArrayAdapter.makeGrayscaleImage( task.data, eval));
                  imageWindow.show();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                e.printStackTrace(new PrintWriter(sw));
                contents = sw.toString();

                setError(e.getMessage());
                done = true;
                return;
            }

            if (cancel) {
                ps.println("\n***Cancelled by User");
            }
            contents = sw.toString();

            success = !cancel;
            done = true;
        }
    }

/**
 *
 */
    private class NCdumpTask extends CommonTask {
    /**
     *
     */

        NCdumpTask(String command) {
            super(command);
        }

    /**
     *
     */
        public void run() {
            try {
                data = ds.readSection(command);
                contents = NCdumpW.toString( data, null, this);
            }
            catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter(100000);
                e.printStackTrace(new PrintWriter(sw));
                contents = sw.toString();

                setError(e.getMessage());
                done = true;
                return;
            }

            if (cancel) {
                contents = "\n***Cancelled by User";
            }

            success = !cancel;
            done = true;
        }
    }
}
