// $Id: FileManager.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.ui;

import ucar.util.prefs.PreferencesExt;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.lang.reflect.Method;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.filechooser.*;


/**
 * Cover for JFileChooser.
 *
 * <pre>
 *
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

 * @author John Caron
 * @version $Id: FileManager.java 50 2006-07-12 16:30:06Z caron $
 */

public class FileManager {
  private static final String BOUNDS = "Bounds";
  private static final String DEFAULT_DIR = "DefaultDir";
  private static final String DEFAULT_FILTER = "DefaultFilter";

  // JNLP workaround
  private static final Object[] noArgs = {};
  private static final Class[] noArgTypes = {};
  private static Method listRootsMethod = null;
  private static boolean listRootsMethodChecked = false;

  // regular
  private PreferencesExt prefs;
  private JFrame parent;
  private IndependentDialog w;
  private ucar.util.prefs.ui.ComboBox dirComboBox;
  private javax.swing.JFileChooser chooser = null;
  private ArrayList defaultDirs = new ArrayList();

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
    this.parent = parent;
    this.prefs = prefs;

    // where to start ?
    if (defDir != null)
      defaultDirs.add( defDir);
    else {
      String dirName = (prefs != null) ? prefs.get(DEFAULT_DIR, ".") : ".";
      defaultDirs.add( dirName);
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
        if (test) System.out.println("FileManager 1 = "+defaultDirectory);
        chooser = new javax.swing.JFileChooser( defaultDirectory); // new WindowsAltFileSystemView());
      } else
        chooser = new javax.swing.JFileChooser( defaultDirectory);

    } catch (SecurityException se) {
      System.out.println("FileManager SecurityException "+ se);
      readOk = false;
      JOptionPane.showMessageDialog( null, "Sorry, this Applet does not have disk read permission.");
    }

    chooser.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (debug) System.out.println("**** chooser event="+e.getActionCommand()+"\n  "+e);
        //if (debug) System.out.println("  curr directory="+chooser.getCurrentDirectory());
        //if (debug) System.out.println("  selected file="+chooser.getSelectedFile());

        if (e.getActionCommand().equals("ApproveSelection"))
          selectedFile = true;
        w.hide();
      }
    });

    // set filters
    if (filters != null) {
      for (int i = 0; i < filters.length; i++) {
        FileFilter filter = filters[i];
        chooser.addChoosableFileFilter(filter);
      }
    }

    // saved file filter
    if (prefs != null) {
      String wantFilter = prefs.get(DEFAULT_FILTER, null);
      if (wantFilter != null) {
        FileFilter[] currFilters = chooser.getChoosableFileFilters();
        for (int i = 0; i < currFilters.length; i++) {
          FileFilter fileFilter = currFilters[i];
          if (fileFilter.getDescription().equals(wantFilter))
            chooser.setFileFilter( fileFilter);
        }
      }
    }

    // buttcons
    AbstractAction usedirAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        String item = (String) dirComboBox.getSelectedItem();
        // System.out.println(" cb =  "+item);
        if (item != null)
          chooser.setCurrentDirectory( new File(item));
      }
    };
    BAMutil.setActionProperties( usedirAction, "FingerDown", "use this directory", false, 'U', -1);

    AbstractAction savedirAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        File currDir = chooser.getCurrentDirectory();
        //System.out.println("  curr directory="+currDir);
        if (currDir != null)
          dirComboBox.addItem( currDir.getPath());
      }
    };
    BAMutil.setActionProperties( savedirAction, "FingerUp", "save current directory", false, 'S', -1);

    // put together the UI
    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    BAMutil.addActionToContainer(buttPanel, usedirAction);
    BAMutil.addActionToContainer(buttPanel, savedirAction);

    JPanel dirPanel = new JPanel( new BorderLayout());
    dirComboBox = new ucar.util.prefs.ui.ComboBox( prefs);
    dirComboBox.setEditable( true);
    dirPanel.add( new JLabel(" Directories: "),BorderLayout.WEST);
    dirPanel.add( dirComboBox, BorderLayout.CENTER);
    dirPanel.add( buttPanel, BorderLayout.EAST);

    JPanel main = new JPanel( new BorderLayout());
    main.add( dirPanel, BorderLayout.NORTH);
    main.add( chooser, BorderLayout.CENTER);

    //w = new IndependentWindow("FileChooser", BAMutil.getImage("FileChooser"), main);
    w = new IndependentDialog(parent, true, "FileChooser", main);
    if (null != prefs) {
      Rectangle b = (Rectangle) prefs.getObject( BOUNDS);
      if (b != null)
        w.setBounds( b);
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

  /**
   * Allow user to select file, then return the filename, in canonical form,
   * always using '/', never '\'
   * @return chosen filename in canonical form, or null if nothing chosen.
   */
  public String chooseFilename() {
    if (!readOk) return null;
    selectedFile = false;
    w.show(); // modal, so blocks; listener calls hide(), which unblocks.

    if (selectedFile) {
      File file = chooser.getSelectedFile();
      if (file == null) return null;
      try {
        if (debug) System.out.println("  selected file= "+file);
        return file.getCanonicalPath().replace('\\','/');
      } catch (IOException ioe) {} // return null
    }
    if (debug) System.out.println("  return null");
    return null;
  }

  public String chooseFilename( String defaultFilename) {
    chooser.setSelectedFile( new File(defaultFilename));
    return chooseFilename();
  }


  public String getCurrentDirectory() {
    return chooser.getCurrentDirectory().getPath();
  }

  public void setCurrentDirectory(String dirName) {
    File dir = new File( dirName);
    chooser.setCurrentDirectory(dir);
  }

  private File findDefaultDirectory(ArrayList tryDefaultDirectories) {
    boolean readOK = true;
    for (int i=0; i<tryDefaultDirectories.size(); i++) {
      try {
        String dirName = (String) tryDefaultDirectories.get(i);
        if (debug) System.out.print("FileManager try "+ dirName);
        File dir = new File(dirName);
        if (dir.exists()) {
          if (debug) System.out.println(" = ok ");
          return dir;
        } else {
          if (debug) System.out.println(" = no ");
          continue;
        }
      } catch (SecurityException se) {
         if (debug) System.out.println("SecurityException in FileManager: "+ se);
         readOK = false;
      }
    }

    if (!readOK)
      JOptionPane.showMessageDialog( null, "Sorry, this Applet does not have disk read permission.");
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
      return  file.isDirectory() || name.endsWith(file_extension);
    }

    public String getDescription() {return desc;}
  }

  public static class NetcdfExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return  file.isDirectory() || name.endsWith(".nc") || name.endsWith(".cdf");
    }

    public String getDescription() {return "netcdf";}
  }

  public static class HDF5ExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return  file.isDirectory() || name.endsWith(".h5") || name.endsWith(".hdf");
    }

    public String getDescription() {return "hdf5";}
  }

  public static class XMLExtFilter extends FileFilter {

    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      return  file.isDirectory() || name.endsWith(".xml");
    }

    public String getDescription() {return "xml";}
  }

  public static void main(String args[]) throws IOException {

    JFrame frame = new JFrame("Test");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    final FileManager fm = new FileManager( frame);
    final JFileChooser fc = fm.chooser;
    fc.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("**** fm event="+e.getActionCommand());
        System.out.println("  curr directory="+fc.getCurrentDirectory());
        System.out.println("  selected file="+fc.getSelectedFile());
      }
    });

    JButton butt = new JButton("accept");
    butt.addActionListener( new AbstractAction() {
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


/* Change History:
   $Log: FileManager.java,v $
   Revision 1.13  2005/08/08 19:38:59  caron
   minor

   Revision 1.12  2005/07/27 23:29:14  caron
   minor

   Revision 1.11  2005/01/21 00:51:31  caron
   no message

   Revision 1.10  2004/12/15 00:11:46  caron
   2.2.05

   Revision 1.9  2004/12/14 15:41:01  caron
   *** empty log message ***

   Revision 1.8  2004/12/07 02:43:19  caron
   *** empty log message ***

   Revision 1.7  2004/12/01 05:54:23  caron
   improve FileChooser

   Revision 1.6  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.5  2004/09/24 03:26:33  caron
   merge nj22

   Revision 1.4  2004/06/12 02:06:35  caron
   add XMLExtFilter

   Revision 1.3  2003/05/29 23:03:27  john
   minor

   Revision 1.2  2003/03/17 20:12:20  john
   add chooseFilename

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.2  2002/04/29 22:26:56  caron
   minor

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources
*/
