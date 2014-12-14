package ucar.nc2.ui;

import org.bounce.text.LineNumberMargin;
import org.bounce.text.ScrollableEditorPanel;
import org.bounce.text.xml.XMLDocument;
import org.bounce.text.xml.XMLEditorKit;
import org.bounce.text.xml.XMLStyleConstants;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.ComboBox;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Describe
 *
 * @author caron
 * @since 3/13/13
 */
public class NcmlEditor extends JPanel {
  private final static boolean debugNcmlWrite = false;

  private NetcdfDataset ds = null;
  private String ncmlLocation = null;
  private JEditorPane editor;
  private Map<String, String> protoMap = new HashMap<>(10);
  private ComboBox protoChooser;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private FileManager fileChooser;
  private NetcdfOutputChooser outChooser;

  private final AbstractButton coordButt;
  private boolean addCoords;

  private PreferencesExt prefs;
  ///////////////

  public NcmlEditor(JPanel buttPanel, PreferencesExt prefs) {
    this.prefs = prefs;
    fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

    AbstractAction coordAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        addCoords = (Boolean) getValue(BAMutil.STATE);
        String tooltip = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
        coordButt.setToolTipText(tooltip);
      }
    };
    addCoords = prefs.getBoolean("coordState", false);
    String tooltip2 = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
    BAMutil.setActionProperties(coordAction, "addCoords", tooltip2, true, 'C', -1);
    coordAction.putValue(BAMutil.STATE, Boolean.valueOf(addCoords));
    coordButt = BAMutil.addActionToContainer(buttPanel, coordAction);

    protoChooser = new ComboBox((PreferencesExt) prefs.node("protoChooser"));
    addProtoChoices();
    buttPanel.add(protoChooser);
    protoChooser.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String ptype = (String) protoChooser.getSelectedItem();
        String proto = protoMap.get(ptype);
        if (proto != null) {
          editor.setText(proto);
        }
      }
    });

    editor = new JEditorPane();

    // Instantiate a XMLEditorKit with wrapping enabled.
    XMLEditorKit kit = new XMLEditorKit(false);

    // Set the wrapping style.
    kit.setWrapStyleWord(true);

    editor.setEditorKit(kit);

    // Set the font style.
    editor.setFont(new Font("Monospaced", Font.PLAIN, 12));

    // Set the tab size
    editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);

    // Enable auto indentation.
    editor.getDocument().putProperty(XMLDocument.AUTO_INDENTATION_ATTRIBUTE, true);

    // Enable tag completion.
    editor.getDocument().putProperty(XMLDocument.TAG_COMPLETION_ATTRIBUTE, true);

    // Initialise the folding
    kit.setFolding(true);

    // Set a style
    kit.setStyle(XMLStyleConstants.ATTRIBUTE_NAME, Color.RED, Font.BOLD);

    // Put the editor in a panel that will force it to resize, when a different view is choosen.
    ScrollableEditorPanel editorPanel = new ScrollableEditorPanel(editor);

    JScrollPane scroller = new JScrollPane(editorPanel);

    // Add the number margin as a Row Header View
    scroller.setRowHeaderView(new LineNumberMargin(editor));

    AbstractAction wrapAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        XMLEditorKit kit = (XMLEditorKit) editor.getEditorKit();
        kit.setLineWrappingEnabled(!kit.isLineWrapping());
        editor.updateUI();
      }
    };
    BAMutil.setActionProperties(wrapAction, "Wrap", "Toggle Wrapping", false, 'W', -1);
    BAMutil.addActionToContainer(buttPanel, wrapAction);

    AbstractAction saveAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        String location = (ds == null) ? ncmlLocation : ds.getLocation();
        if (location == null) location = "test";
        int pos = location.lastIndexOf(".");
        if (pos > 0)
          location = location.substring(0, pos);
        String filename = fileChooser.chooseFilenameToSave(location + ".ncml");
        if (filename == null) return;
        if (doSaveNcml(editor.getText(), filename))
          ncmlLocation = filename;
      }
    };
    BAMutil.setActionProperties(saveAction, "Save", "Save NcML", false, 'S', -1);
    BAMutil.addActionToContainer(buttPanel, saveAction);

    AbstractAction netcdfAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
      if (outChooser == null) {
        outChooser = new NetcdfOutputChooser((Frame) null);
        outChooser.addPropertyChangeListener("OK", new PropertyChangeListener() {
           public void propertyChange(PropertyChangeEvent evt) {
             writeNetcdf((NetcdfOutputChooser.Data) evt.getNewValue());
           }
         });
      }

      String location = (ds == null) ? ncmlLocation : ds.getLocation();
      if (location == null) location = "test";
      int pos = location.lastIndexOf(".");
      if (pos > 0)
        location = location.substring(0, pos);

      outChooser.setOutputFilename(location);
      outChooser.setVisible(true);
      }
    };
    BAMutil.setActionProperties(netcdfAction, "netcdf", "Write netCDF file", false, 'N', -1);
    BAMutil.addActionToContainer(buttPanel, netcdfAction);

    AbstractAction transAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        doTransform(editor.getText());
      }
    };
    BAMutil.setActionProperties(transAction, "Import", "read textArea through NcMLReader\n write NcML back out via resulting dataset", false, 'T', -1);
    BAMutil.addActionToContainer(buttPanel, transAction);

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Check NcML", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        checkNcml(f);

        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    buttPanel.add(compareButton);

    setLayout(new BorderLayout());
    add(scroller, BorderLayout.CENTER);

        // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));
  }

  public void save() {
    fileChooser.save();
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public void closeOpenFiles() {
    try {
     if (ds != null) ds.close();
  } catch (IOException ioe) {
   }
     ds = null;
   }

  public boolean setNcml(String cmd) {
    if (cmd.endsWith(".xml") || cmd.endsWith(".ncml")) {
      if (!cmd.startsWith("http:") && !cmd.startsWith("file:"))
        cmd = "file:" + cmd;
      ncmlLocation = cmd;
      String text = IO.readURLcontents(cmd);
      editor.setText(text);
    } else {
      writeNcml(cmd);
    }
    return true;
  }

  // write ncml from given dataset
  boolean writeNcml(String location) {
    boolean err = false;

    closeOpenFiles();

    try {
      String result;
      ds = openDataset(location, addCoords, null);
      if (ds == null) {
        editor.setText("Failed to open <" + location + ">");
      } else {
        result = new NcMLWriter().writeXML(ds);
        editor.setText(result);
        editor.setCaretPosition(0);
      }

    } catch (FileNotFoundException ioe) {
      editor.setText("Failed to open <" + location + ">");
      err = true;

    } catch (Exception e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace();
      e.printStackTrace(new PrintWriter(sw));
      editor.setText(sw.toString());
      err = true;
    }

    return !err;
  }

  private NetcdfDataset openDataset(String location, boolean addCoords, CancelTask task) {
    try {
      return NetcdfDataset.openDataset(location, addCoords, task);

      //if (setUseRecordStructure)
     //   ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + ioe.getMessage());
      if (!(ioe instanceof java.io.FileNotFoundException)) ioe.printStackTrace();
      return null;
    }

  }

  void writeNetcdf(NetcdfOutputChooser.Data data) {
    //if (debugNcmlWrite) {
    //  System.out.printf("choices=%s%n", choice);
    //}

    String text = editor.getText();

    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(
              text.getBytes(CDM.utf8Charset));
      NcMLReader.writeNcMLToFile(bis, data.outputFilename,  data.version,
                    Nc4ChunkingStrategy.factory(data.chunkerType, data.deflate, data.shuffle)
      );
      JOptionPane.showMessageDialog(this, "File successfully written");

    } catch (Exception ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  // read text from textArea through NcMLReader
  // then write it back out via resulting dataset
  void doTransform(String text) {
    try {
      StringReader reader = new StringReader(text);
      NetcdfDataset ncd = NcMLReader.readNcML(reader, null);
      StringWriter sw = new StringWriter(10000);
      ncd.writeNcML(sw, null);
      editor.setText(sw.toString());
      editor.setCaretPosition(0);
      JOptionPane.showMessageDialog(this, "File successfully transformed");

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  // read text from textArea through NcMLReader
  // then write it back out via resulting dataset
  private void checkNcml(Formatter f) {
    if (ncmlLocation == null) return;
    try {
      NetcdfDataset ncd = NetcdfDataset.openDataset(ncmlLocation);
      ncd.check(f);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  boolean doSaveNcml(String text, String filename) {
    if (debugNcmlWrite) {
      System.out.println("filename=" + filename);
      System.out.println("text=" + text);
    }

    File out = new File(filename);
    if (out.exists()) {
      int val = JOptionPane.showConfirmDialog(null,
              filename + " already exists. Do you want to overwrite?", "WARNING",
              JOptionPane.YES_NO_OPTION);
      if (val != JOptionPane.YES_OPTION) return false;
    }

    try {
      IO.writeToFile(text, out);
      JOptionPane.showMessageDialog(this, "File successfully written");
      return true;
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
      return false;
    }
    // saveNcmlDialog.setVisible(false);
  }

  void addProtoChoices() {
    String xml =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                    "  <variable name='time' type='int' shape='time'>\n" +
                    "    <attribute name='long_name' type='string' value='time coordinate' />\n" +
                    "    <attribute name='units' type='string' value='days since 2001-8-31 00:00:00 UTC' />\n" +
                    "    <values start='0' increment='10' />\n" +
                    "  </variable>\n" +
                    "  <aggregation dimName='time' type='joinNew'>\n" +
                    "    <variableAgg name='T'/>\n" +
                    "    <scan location='src/test/data/ncml/nc/' suffix='.nc' subdirs='false'/>\n" +
                    "  </aggregation>\n" +
                    "</netcdf>";
    protoMap.put("joinNew", xml);
    protoChooser.addItem("joinNew");

    xml =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                    "  <aggregation dimName='time' type='joinExisting'>\n" +
                    "    <scan location='ncml/nc/pfeg/' suffix='.nc' />\n" +
                    "  </aggregation>\n" +
                    "</netcdf>";
    protoMap.put("joinExisting", xml);
    protoChooser.addItem("joinExisting");

  }

}