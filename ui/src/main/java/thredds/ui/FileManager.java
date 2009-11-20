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
package thredds.ui;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.ComboBox;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.filechooser.*;


/**
 * Cover for JFileChooser.
 * <p/>
 * <pre>
 * <p/>
   javax.swing.filechooser.FileFilter[] filters = new javax.swing.filechooser.FileFilter[2];
   filters[0] = new FileManager.HDF5ExtFilter();
   filters[1] = new FileManager.NetcdfExtFilter();
   fileChooser = new FileManager(parentFrame, null, filters, (PreferencesExt) prefs.node("FileManager"));

   AbstractAction fileAction =  new AbstractAction() {
     public void actionPerformed(ActionEvent e) {
       String filename = fileChooser.chooseFilename();
       if (filename == null) return;
       process(filename);
     }
   };
   BAMutil.setActionProperties( fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
   </pre>
 *
 * @author John Caron
 */

public class FileManager {
  private static final String BOUNDS = "Bounds";
  private static final String DEFAULT_DIR = "DefaultDir";
  private static final String DEFAULT_FILTER = "DefaultFilter";

  // regular
  private PreferencesExt prefs;
  private IndependentDialog w;
  private ucar.util.prefs.ui.ComboBox dirComboBox;
  private javax.swing.JFileChooser chooser = null;
  private java.util.List<String> defaultDirs = new ArrayList<String>();

  // for override
  protected JPanel main;
  protected boolean selectedURL = false;
  protected ComboBox urlComboBox;

  private boolean readOk = true, selectedFile = false;
  private static boolean debug = false, test = false;

  public FileManager(JFrame parent) {
    this(parent, null, null, null);
  }

  public FileManager(JFrame parent, String defDir) {
    this(parent, defDir, null, null);
  }

  public FileManager(JFrame parent, String defDir, String file_extension, String desc, PreferencesExt prefs) {
    this(parent, defDir, new FileFilter[]{new ExtFilter(file_extension, desc)}, prefs);
  }

  public FileManager(JFrame parent, String defDir, FileFilter[] filters, PreferencesExt prefs) {
    this.prefs = prefs;

    // where to start ?
    if (defDir != null)
      defaultDirs.add(defDir);
    else {
      String dirName = (prefs != null) ? prefs.get(DEFAULT_DIR, ".") : ".";
      defaultDirs.add(dirName);
    }

    // funky windows workaround
    String osName = System.getProperty("os.name");
    //System.out.println("OS ==  "+ osName+" def ="+defDir);
    boolean isWindose = (0 <= osName.indexOf("Windows"));
    if (isWindose)
      defaultDirs.add("C:/");

    File defaultDirectory = findDefaultDirectory(defaultDirs);
    try {
      if (isWindose) {
        if (test) System.out.println("FileManager 1 = " + defaultDirectory);
        chooser = new javax.swing.JFileChooser(defaultDirectory); // new WindowsAltFileSystemView());
      } else
        chooser = new javax.swing.JFileChooser(defaultDirectory);

    } catch (SecurityException se) {
      System.out.println("FileManager SecurityException " + se);
      readOk = false;
      JOptionPane.showMessageDialog(null, "Sorry, this Applet does not have disk read permission.");
    }

    chooser.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (debug) System.out.println("**** chooser event=" + e.getActionCommand() + "\n  " + e);
        //if (debug) System.out.println("  curr directory="+chooser.getCurrentDirectory());
        //if (debug) System.out.println("  selected file="+chooser.getSelectedFile());

        if (e.getActionCommand().equals("ApproveSelection"))
          selectedFile = true;
        w.setVisible(false);
      }
    });

    // set filters
    if (filters != null) {
      for (FileFilter filter : filters) {
        chooser.addChoosableFileFilter(filter);
      }
    }

    // saved file filter
    if (prefs != null) {
      String wantFilter = prefs.get(DEFAULT_FILTER, null);
      if (wantFilter != null) {
        for (FileFilter fileFilter : chooser.getChoosableFileFilters()) {
          if (fileFilter.getDescription().equals(wantFilter))
            chooser.setFileFilter(fileFilter);
        }
      }
    }

    // buttcons
    AbstractAction usedirAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        String item = (String) dirComboBox.getSelectedItem();
        // System.out.println(" cb =  "+item);
        if (item != null)
          chooser.setCurrentDirectory(new File(item));
      }
    };
    BAMutil.setActionProperties(usedirAction, "FingerDown", "use this directory", false, 'U', -1);

    AbstractAction savedirAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        File currDir = chooser.getCurrentDirectory();
        //System.out.println("  curr directory="+currDir);
        if (currDir != null)
          dirComboBox.addItem(currDir.getPath());
      }
    };
    BAMutil.setActionProperties(savedirAction, "FingerUp", "save current directory", false, 'S', -1);

    AbstractAction rescanAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        chooser.rescanCurrentDirectory();
      }
    };
    BAMutil.setActionProperties(rescanAction, "Undo", "refresh", false, 'R', -1);

    // put together the UI
    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    BAMutil.addActionToContainer(buttPanel, usedirAction);
    BAMutil.addActionToContainer(buttPanel, savedirAction);
    BAMutil.addActionToContainer(buttPanel, rescanAction);

    JPanel dirPanel = new JPanel(new BorderLayout());
    dirComboBox = new ucar.util.prefs.ui.ComboBox(prefs);
    dirComboBox.setEditable(true);
    dirPanel.add(new JLabel(" Directories: "), BorderLayout.WEST);
    dirPanel.add(dirComboBox, BorderLayout.CENTER);
    dirPanel.add(buttPanel, BorderLayout.EAST);

    main = new JPanel(new BorderLayout());
    main.add(dirPanel, BorderLayout.NORTH);
    main.add(chooser, BorderLayout.CENTER);

    urlComboBox = new ComboBox(prefs);
    urlComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          selectedURL = true;
      }
    });

    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    p.add( new JLabel("or a URL:"));
    p.add( urlComboBox);
    main.add(urlComboBox, BorderLayout.SOUTH);

    //w = new IndependentWindow("FileChooser", BAMutil.getImage("FileChooser"), main);
    w = new IndependentDialog(parent, true, "FileChooser", main);
    if (null != prefs) {
      Rectangle b = (Rectangle) prefs.getObject(BOUNDS);
      if (b != null)
        w.setBounds(b);
    }
  }

  public void save() {
    if (prefs == null) return;

    File currDir = chooser.getCurrentDirectory();
    if (currDir != null)
      prefs.put(DEFAULT_DIR, currDir.getPath());

    FileFilter currFilter = chooser.getFileFilter();
    if (currDir != null)
      prefs.put(DEFAULT_FILTER, currFilter.getDescription());

    if (dirComboBox != null)
      dirComboBox.save();

    prefs.putObject(BOUNDS, w.getBounds());
  }

  public JFileChooser getFileChooser() {
    return chooser;
  }

  /* public java.io.File chooseFile() {
   if (!readOk) return null;
   w.show();

   if (chooser.showOpenDialog( parent) == JFileChooser.APPROVE_OPTION) {
     File file = chooser.getSelectedFile();
     if (debug) System.out.println("FileManager result "+file.getPath());
     if (file != null)
       return file;
   }
   return null;
 } */

  public String chooseFilenameToSave(String defaultFilename) {
    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
    String result = (defaultFilename == null) ? chooseFilename() : chooseFilename(defaultFilename);
    chooser.setDialogType(JFileChooser.OPEN_DIALOG);

    return result;
  }

  public String chooseDirectory(String defaultDirectory) {
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    String result = (defaultDirectory == null) ? chooseFilename() : chooseFilename(defaultDirectory);
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    return result;
  }

  /**
   * Allow user to select file, then return the filename, in canonical form,
   * always using '/', never '\'
   *
   * @return chosen filename in canonical form, or null if nothing chosen.
   */
  public String chooseFilename() {
    if (!readOk) return null;
    selectedFile = false;
    selectedURL = false;
    w.setVisible(true); // modal, so blocks; listener calls hide(), which unblocks.

    if (selectedFile) {
      File file = chooser.getSelectedFile();
      if (file == null) return null;
      try {
        return file.getCanonicalPath().replace('\\', '/');
      } catch (IOException ioe) {
      } // return null
    }

    if (selectedURL) {
      return (String) urlComboBox.getSelectedItem();
    }

    return null;
  }

  public String chooseFilename(String defaultFilename) {
    chooser.setSelectedFile(new File(defaultFilename));
    return chooseFilename();
  }

  public File[] chooseFiles() {
    chooser.setMultiSelectionEnabled(true);
    selectedFile = false;
    w.setVisible(true);
    
    if (selectedFile)
      return chooser.getSelectedFiles();

    return null;
  }

  public String getCurrentDirectory() {
    return chooser.getCurrentDirectory().getPath();
  }

  public void setCurrentDirectory(String dirName) {
    File dir = new File(dirName);
    chooser.setCurrentDirectory(dir);
  }

  private File findDefaultDirectory(java.util.List<String> tryDefaultDirectories) {
    boolean readOK = true;
    for (String tryDefaultDirectory : tryDefaultDirectories) {
      try {
        if (debug) System.out.print("FileManager try " + tryDefaultDirectory);
        File dir = new File(tryDefaultDirectory);
        if (dir.exists()) {
          if (debug) System.out.println(" = ok ");
          return dir;
        } else {
          if (debug) System.out.println(" = no ");
        }
      } catch (SecurityException se) {
        if (debug) System.out.println("SecurityException in FileManager: " + se);
        readOK = false;
      }
    }

    if (!readOK)
      JOptionPane.showMessageDialog(null, "Sorry, this Applet does not have disk read permission.");
    return null;
  }

  public static class ExtFilter extends FileFilter {
    String file_extension;
    String desc;

    public ExtFilter(String file_extension, String desc) {
      this.file_extension = file_extension;
      this.desc = desc;
    }

    public boolean accept(File file) {
      if (null == file_extension)
        return true;
      String name = file.getName();
      return file.isDirectory() || name.endsWith(file_extension);
    }

    public String getDescription() {
      return desc;
    }
  }

  public static class NetcdfExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return file.isDirectory() || name.endsWith(".nc") || name.endsWith(".cdf");
    }

    public String getDescription() {
      return "netcdf";
    }
  }

  public static class HDF5ExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return file.isDirectory() || name.endsWith(".h5") || name.endsWith(".hdf");
    }

    public String getDescription() {
      return "hdf5";
    }
  }

  public static class XMLExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return file.isDirectory() || name.endsWith(".xml");
    }

    public String getDescription() {
      return "xml";
    }
  }

  public static void main(String args[]) throws IOException {

    JFrame frame = new JFrame("Test");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    final FileManager fm = new FileManager(frame);
    final JFileChooser fc = fm.chooser;
    fc.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("**** fm event=" + e.getActionCommand());
        System.out.println("  curr directory=" + fc.getCurrentDirectory());
        System.out.println("  selected file=" + fc.getSelectedFile());
      }
    });

    JButton butt = new JButton("accept");
    butt.addActionListener(new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("butt accept");
        //cb.accept();
      }
    });

    fm.chooseFilename();
    /* JPanel main = new JPanel();
   main.add(fm);
   //main.add(butt);

   frame.getContentPane().add(main);
   // cb.setPreferredSize(new java.awt.Dimension(500, 200));

   frame.pack();
   frame.setLocation(300, 300);
   frame.setVisible(true); */
  }
}

/* claimed workaround for WebStart

 http://forum.java.sun.com/read/56761/q_D4Xl3nS380AAZX5#LR


 Hi Surya,

Unfortunately it looks like you have run into an annoying bug in the Java 2 SDK v1.2.2 or v1.3 .
This bug will be fixed in a future Java 2 SDK release. In the meantime, there are a couple workarounds:

1) Use the JNLP API FileOpenService and FileSaveService to do file operations; the reference
implementation uses the workaround I list under 2) below.

2) Keep your current code, but use a customized FileSystemView that you supply to JFileChooser
when instantiating it on Windows. e.g. :

new JFileChooser(currentDirectory, new WindowsAltFileSystemView ())

Code for WindowsAltFileSystemView follows, with apologies for the formatting :

// This class is necessary due to an annoying bug on Windows NT where
// instantiating a JFileChooser with the default FileSystemView will
// cause a "drive A: not ready" error every time. I grabbed the
// Windows FileSystemView impl from the 1.3 SDK and modified it so
// as to not use java.io.File.listRoots() to get fileSystem roots.
// java.io.File.listRoots() does a SecurityManager.checkRead() which
// causes the OS to try to access drive A: even when there is no disk,
// causing an annoying "abort, retry, ignore" popup message every time
// we instantiate a JFileChooser!
//
// Instead of calling listRoots() we use a straightforward alternate
// method of getting file system roots.


 private class WindowsAltFileSystemView extends FileSystemView {

   /**
   * Returns true if the given file is a root.
   *
   public boolean isRoot(File f) {
     if(!f.isAbsolute())
       return false;

     String parentPath = f.getParent();
     if(parentPath == null) {
       return true;
     } else {
       File parent = new File(parentPath);
       return parent.equals(f);
     }
   }

   /**
   * creates a new folder with a default folder name.
   *
   public File createNewFolder(File containingDir) throws IOException {
     if (containingDir == null)
       throw new IOException("Containing directory is null:");

     File newFolder = null;
     // Using NT's default folder name
     newFolder = createFileObject(containingDir, "New Folder");
     int i = 2;
     while (newFolder.exists() && (i < 100)) {
       newFolder = createFileObject(containingDir, "New Folder (" + i + ")");
       i++;
     }

     if(newFolder.exists()) {
       throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
     } else {
       newFolder.mkdirs();
     }

     return newFolder;
   }

   /**
   * Returns whether a file is hidden or not. On Windows
   * there is currently no way to get this information from
   * io.File, therefore always return false.
   *
   public boolean isHiddenFile(File f) {
     return false;
   }

   /**
   * Returns all root partitians on this system. On Windows, this
   * will be the A: through Z: drives.
   *
   public File[] getRoots() {
     Vector rootsVector = new Vector();

     System.out.println(" getRoots ");

     // Create the A: drive whether it is mounted or not
     FileSystemRoot floppy = new FileSystemRoot("A" + ":"+ "\\");
     rootsVector.addElement(floppy);

     // Run through all possible mount points and check
     // for their existance.
     for (char c = 'C'; c <= 'Z'; c++) {
       char device[] = {c, ':', '\\'};
       String deviceName = new String(device);
       System.out.println(" try ");
       System.out.println(" "+deviceName);
       File deviceFile = new FileSystemRoot(deviceName);
       boolean ok = deviceFile.exists();
       System.out.println(" "+ok);
       if (deviceFile != null && deviceFile.exists()) {
         rootsVector.addElement(deviceFile);
         System.out.println(" use "+deviceName);
       }
     }

     File[] roots = new File[rootsVector.size()];
     rootsVector.copyInto(roots);
     return roots;
   }
 } // class WindowsAltFileSystemView

 private class FileSystemRoot extends File {
   public FileSystemRoot(File f) {
     super(f, "");
   }

   public FileSystemRoot(String s) {
     super(s);
   }

   public boolean isDirectory() {
     return true;
   }
 } // class FileSystemRoot

} */
