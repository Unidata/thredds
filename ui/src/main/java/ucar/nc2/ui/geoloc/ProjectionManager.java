/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ui.geoloc;

import java.awt.*;
import java.beans.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.*;
import ucar.nc2.util.ListenerManager;
import ucar.util.prefs.PreferencesExt;

import ucar.unidata.geoloc.*;

/**
 * Manages a modal dialogue box that allows the user to define
 * projections using subclasses of ucar.unidata.gis.ProjectionImpl.
 * <pre>
 *    1) It uses bean introspection on ProjectionImpl subclasses to dynamically
 *       configure edit fields for the projection's parameters. The subclass should
 *       define getXXX() and setXXX() methods for each parameter.
 *    2) A user-defined list of ProjectionImpl is maintained through a PersistentStore object.
 *    3) when the user selects a projection, a NewProjectionEvent is sent to any listeners
 *    4) currently the list of possible projection classes is maintained as a
 *       hard-wired list of class names. This should be rethunk.
 * </pre>
 *
 * @author John Caron
 * @version revived 9/20/2012
 */
public class ProjectionManager {

  // the current list of projection classes: be nice to do this dynamically
  // what about reading all classes in ucar.unidata.gis/projection ? (glenn doesnt like)
  private static String[] types = {
          "ucar.unidata.geoloc.projection.LatLonProjection",
          "ucar.unidata.geoloc.projection.AlbersEqualArea",
          "ucar.unidata.geoloc.projection.FlatEarth",
          "ucar.unidata.geoloc.projection.LambertAzimuthalEqualArea",
          "ucar.unidata.geoloc.projection.LambertConformal",
          "ucar.unidata.geoloc.projection.Mercator",
          "ucar.unidata.geoloc.projection.Orthographic",
          "ucar.unidata.geoloc.projection.RotatedLatLon",
          "ucar.unidata.geoloc.projection.Stereographic",
          "ucar.unidata.geoloc.projection.TransverseMercator",
          "ucar.unidata.geoloc.projection.UtmProjection",
          "ucar.unidata.geoloc.projection.VerticalPerspectiveView",
  };

  private ProjectionImpl current;
  private JFrame parent;
  private PreferencesExt store;
  private boolean eventsOK = false;

  // GUI stuff that needs class scope
  // private JTableProjection projTable;
  //private NPController npViewControl;
  //private NavigatedPanel mapViewPanel;
  //private PersistentDataDialog viewDialog;
  //private NewProjectionDialog newDialog = null;

  private Frame owner;
  private NewProjectionDialog dialog; // created in JFormDesigner
  private ListenerManager lm;

  // misc
  private boolean debug = false, debugBeans = false;

  /**
   * default constructor
   */
  public ProjectionManager(Frame owner, PreferencesExt store) {
    this.owner = owner;
    this.store = store;

    // manage NewProjectionListeners
    lm = new ListenerManager(
            "java.beans.PropertyChangeListener",
            "java.beans.PropertyChangeEvent",
            "propertyChange");


    /*
    // the actual list is a JTable subclass
    projTable = new JTableProjection(store);

    // empty table : populate it
    if (projTable.isEmpty()) {
      for (String aProjClassName : projClassName) {
        try {
          ProjectionClass pc = new ProjectionClass(aProjClassName);
          ProjectionImpl pi = pc.makeDefaultProjection();
          pi.setName("Default " + pi.getClassName());
          projTable.addProjection(pi);

        } catch (ClassNotFoundException ee) {
          System.err.println("ProjectionManager failed on " + aProjClassName + " " + ee);
        } catch (IntrospectionException ee) {
          System.err.println("ProjectionManager failed on " + aProjClassName + " " + ee);
        }
      }
    }

    // put it together in the viewDialog
    // viewDialog = new PersistentDataDialog(parent, false, "Projection", mapViewPanel, new JScrollPane(projTable), this);

    // default current and working projection
    if (null != (current = projTable.getSelected())) {
      setWorkingProjection(current);
      projTable.setCurrentProjection(current);
    }

    // listen for new working Projections from projTable
    projTable.addNewProjectionListener(new NewProjectionListener() {
      public void actionPerformed(NewProjectionEvent e) {
        if (e.getProjection() != null) {
          if (debug) System.out.println("NewProjectionListener from ProjTable " + e.getProjection());
          setWorkingProjection(e.getProjection());
        }
      }
    });  */

    eventsOK = true;
  }


  /* void makeUI() {  // LOOK replace with JFormDesigner

    // the map and associated toolbar
    npEditControl = new NPController();
    add(npEditControl, BorderLayout.CENTER);

    /*JToolBar navToolbar = mapEditPanel.getNavToolBar();
    navToolbar.setFloatable(false);
    JToolBar moveToolbar = mapEditPanel.getMoveToolBar();
    moveToolbar.setFloatable(false);
    //toolbar.remove("setReference");
    ucar.nc2.ui.widget.PopupMenu mapBeanMenu = MapBean.makeMapSelectButton();
    JPanel toolbar = new JPanel();
    toolbar.add( mapBeanMenu.getParentComponent());
    toolbar.add(navToolbar);
    toolbar.add(moveToolbar);    */

    ////////////////////////////////////////////////////////////////////////////////////////////

    /* the list of Projection classes is kept in a comboBox
    JPanel typePanel = new JPanel();
    JLabel typeLabel = new JLabel("Type: ");
    projClassCB = new JComboBox();
    // standard list of projection classes
    for (String aProjClassName : projClassName) {
      try {
        projClassCB.addItem(new ProjectionParamPanel(aProjClassName));
      } catch (ClassNotFoundException ee) {
        System.err.println("ProjectionManager failed on " + aProjClassName + " " + ee);
      } catch (IntrospectionException ee) {
        System.err.println("ProjectionManager failed on " + aProjClassName + " " + ee);
      }
    }
    typePanel.add(typeLabel);
    typePanel.add(projClassCB);

    /* the projection parameters
    Box mainBox = Box.createVerticalBox();
    mainPanel.add(mainBox, BorderLayout.EAST);
    //mainBox.setPreferredSize(new Dimension(350, 300));
    JPanel namePanel = new JPanel(); // the Projection name
    JLabel nameLabel = new JLabel("Name: ");
    nameTF = new JTextField(20);
    namePanel.add(nameLabel);
    namePanel.add(nameTF);

    // the Projection parameter area
    paramPanel = new JPanel();
    paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
    paramPanel.setBorder(new TitledBorder(standardBorder, "Projection Parameters", TitledBorder.CENTER, TitledBorder.ABOVE_TOP));

    // the Projection parameter area
    bbPanel = new JPanel();
    bbPanel.setLayout(new BoxLayout(bbPanel, BoxLayout.Y_AXIS));
    bbPanel.setBorder(new TitledBorder(standardBorder, "Bounding Box", TitledBorder.CENTER, TitledBorder.ABOVE_TOP));

    // the bottom button panel
    JPanel buttPanel = new JPanel();
    JButton acceptButton = new JButton("Save");
    JButton previewButton = new JButton("Preview");
    JButton cancelButton = new JButton("Cancel");
    buttPanel.add(acceptButton, null);
    buttPanel.add(previewButton, null);
    buttPanel.add(cancelButton, null);

    mainBox.add(namePanel);
    mainBox.add(typePanel);
    mainBox.add(paramPanel);
    mainBox.add(bbPanel);
    mainBox.add(Box.createGlue());
    mainBox.add(buttPanel);
    pack();

    //enable event listeners when we're done constructing the UI
    projClassCB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ProjectionClass selectClass = (ProjectionClass) projClassCB.getSelectedItem();
        setProjection(selectClass.makeDefaultProjection());
      }
    });

    acceptButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        accept();
      }
    });
    previewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        ProjectionClass projClass = findProjectionClass(editProjection);
        if (null != projClass) {
          setProjFromDialog(projClass, editProjection);
          setProjection(editProjection);
        }
      }
    });
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        NewProjectionDialog.this.setVisible(false);
      }
    });
  } */

  public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
    lm.addListener(l);
  }

  public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
    lm.removeListener(l);
  }

  public void setVisible() {
    if (null == dialog) init();
    dialog.setVisible(true);
  }

  public void storePersistentData() {
    // projTable.storePersistentData();
  }

  private void init() {
    dialog = new NewProjectionDialog(owner);
    ArrayList<Object> ps = new ArrayList<Object>();
    for (String p : types)
      try {
        ps.add(new ProjectionClass(p));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (IntrospectionException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    dialog.setProjectionTypes(ps);
  }


  /*
   * Get the currently selected object, of type getPersistentObjectClass()
   *
  public PersistentObject getSelection() {
    return (PersistentObject) current;
  }

  /
   * Set the currently selected thing: must be of type getPersistentObjectClass().
   * this does NOT fire a PropertyChangeEvent
   *
  public void setSelection(PersistentObject select) {
    setWorkingProjection((ProjectionImpl) select);
  }

  ////// PersistentDataManager methods
  public void accept() {
    ProjectionRect bb = mapViewPanel.getMapArea();
    current.setDefaultMapArea((ProjectionRect) bb.clone());
    lm.sendEvent(new java.beans.PropertyChangeEvent(this, "ProjectionImpl", null, current));
  }

  public boolean contains(String id) {
    return projTable.contains(id);
  }

  public void deleteSelected() {
    projTable.deleteSelected();
  }

  public void edit(boolean isNew) {
    if (null == newDialog)
      newDialog = new NewProjectionDialog(parent);

    if (isNew) {
      newDialog.setDefaultProjection();
    } else {
      newDialog.setProjection(current);
    }

    newDialog.setVisible(true);
  }


  public void saveProjection(ProjectionImpl proj) {
    //setVisible(true);  how to make this work? seperate Thread ?

    // force new name
    ProjectionImpl newProj = proj.constructCopy();
    newProj.setName("");
    setWorkingProjection(newProj);

    if (null == newDialog) // lazy instantiation
      newDialog = new NewProjectionDialog(parent);

    // start up edit Dialog
    newDialog.setProjection(current);
    newDialog.show();
  }

  private void setMap(ucar.nc2.ui.util.Renderer map) {
    if (null == newDialog) // lazy instantiation
      newDialog = new NewProjectionDialog(parent);

    if (map != null) {
      npViewControl.addRenderer(map);
      newDialog.setMap(map);
    }
    System.out.println("Projection Manager setMap " + map);
  }


  private void setWorkingProjection(ProjectionImpl proj) {
    if (proj == null)
      return;
    if (debug) System.out.println("ProjManager.setWorkingProjection " + proj);
    current = proj.constructCopy();
    if (debug) System.out.println("ProjManager.setWorkingProjection map area = " + current.getDefaultMapArea());

    npViewControl.setProjection(current);
    //viewDialog.setCurrent(current.getName());
  }

  //////////////////////////////////////////////////////////////////////////////
  // heres where projections get defined / edited
  private class NewProjectionDialog extends IndependentWindow {

    private ProjectionImpl editProjection;
    private ListenerManager lm;         // manage NewProjection events
    private String startingName = null;           // track Projection name

    // GUI stuff that needs class scope
    private JComboBox projClassCB;
    private JPanel paramPanel, bbPanel;
    private JTextField nameTF;
    private NPController npEditControl;
    private NavigatedPanel mapEditPanel;
    private Border standardBorder = new EtchedBorder();

    NewProjectionDialog(JFrame parent) {
      super("ProjectionManager", BAMutil.getImage("GDV"));
      makeUI();
      setLocation(100, 100);
    }

    void makeUI() {  // LOOK replace with JFormDesigner
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.setBorder(new LineBorder(Color.blue));
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      // the map and associated toolbar
      npEditControl = new NPController();
      mapEditPanel = npEditControl.getNavigatedPanel();  // here's where the map will be drawn
      mapEditPanel.setPreferredSize(new Dimension(250, 250));
      /*JToolBar navToolbar = mapEditPanel.getNavToolBar();
      navToolbar.setFloatable(false);
      JToolBar moveToolbar = mapEditPanel.getMoveToolBar();
      moveToolbar.setFloatable(false);
      //toolbar.remove("setReference");
      ucar.nc2.ui.widget.PopupMenu mapBeanMenu = MapBean.makeMapSelectButton();
      JPanel toolbar = new JPanel();
      toolbar.add( mapBeanMenu.getParentComponent());
      toolbar.add(navToolbar);
      toolbar.add(moveToolbar);

      JPanel mapSide = new JPanel();
      mapSide.setLayout(new BorderLayout());
      TitledBorder mapBorder = new TitledBorder(standardBorder, "Edit Projection", TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
      mapSide.setBorder(mapBorder);
      //mapSide.add(toolbar, BorderLayout.NORTH);
      mapSide.add(npEditControl, BorderLayout.CENTER);
      mainPanel.add(mapSide, BorderLayout.CENTER);

      mapEditPanel.addNewMapAreaListener(new NewMapAreaListener() {
        @Override
        public void actionPerformed(NewMapAreaEvent e) {
          ProjectionRect rect = mapEditPanel.getMapArea();
          System.out.printf("%s%n", rect.toString2());
        }
      });

      ////////////////////////////////////////////////////////////////////////////////////////////

      // the list of Projection classes is kept in a comboBox
      JPanel typePanel = new JPanel();
      JLabel typeLabel = new JLabel("Type: ");
      projClassCB = new JComboBox();
      // standard list of projection classes
      for (String aProjClassName : projClassName) {
        try {
          projClassCB.addItem(new ProjectionParamPanel(aProjClassName));
        } catch (ClassNotFoundException ee) {
          System.err.println("ProjectionManager failed on " + aProjClassName + " " + ee);
        } catch (IntrospectionException ee) {
          System.err.println("ProjectionManager failed on " + aProjClassName + " " + ee);
        }
      }
      typePanel.add(typeLabel);
      typePanel.add(projClassCB);

      // the projection parameters
      Box mainBox = Box.createVerticalBox();
      mainPanel.add(mainBox, BorderLayout.EAST);
      //mainBox.setPreferredSize(new Dimension(350, 300));
      JPanel namePanel = new JPanel(); // the Projection name
      JLabel nameLabel = new JLabel("Name: ");
      nameTF = new JTextField(20);
      namePanel.add(nameLabel);
      namePanel.add(nameTF);

      // the Projection parameter area
      paramPanel = new JPanel();
      paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
      paramPanel.setBorder(new TitledBorder(standardBorder, "Projection Parameters", TitledBorder.CENTER, TitledBorder.ABOVE_TOP));

      // the Projection parameter area
      bbPanel = new JPanel();
      bbPanel.setLayout(new BoxLayout(bbPanel, BoxLayout.Y_AXIS));
      bbPanel.setBorder(new TitledBorder(standardBorder, "Bounding Box", TitledBorder.CENTER, TitledBorder.ABOVE_TOP));

      // the bottom button panel
      JPanel buttPanel = new JPanel();
      JButton acceptButton = new JButton("Save");
      JButton previewButton = new JButton("Preview");
      JButton cancelButton = new JButton("Cancel");
      buttPanel.add(acceptButton, null);
      buttPanel.add(previewButton, null);
      buttPanel.add(cancelButton, null);

      mainBox.add(namePanel);
      mainBox.add(typePanel);
      mainBox.add(paramPanel);
      mainBox.add(bbPanel);
      mainBox.add(Box.createGlue());
      mainBox.add(buttPanel);
      pack();

      //enable event listeners when we're done constructing the UI
      projClassCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          ProjectionClass selectClass = (ProjectionClass) projClassCB.getSelectedItem();
          setProjection(selectClass.makeDefaultProjection());
        }
      });

      acceptButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          accept();
        }
      });
      previewButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          ProjectionClass projClass = findProjectionClass(editProjection);
          if (null != projClass) {
            setProjFromDialog(projClass, editProjection);
            setProjection(editProjection);
          }
        }
      });
      cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          NewProjectionDialog.this.setVisible(false);
        }
      });
    }

    public void setProjection(ProjectionImpl proj) {
      ProjectionClass pc = findProjectionClass(proj);
      if (pc == null) {
        System.out.println("Projection Manager: cant find Class for " + proj);
        return;
      }
      if (debug) System.out.println(" NPDialog set projection " + proj);

      //if (proj instanceof LambertConformal)
      //  proj.setDefaultMapArea(new ProjectionRect(-2013.441384640456,-34.18738379648357,71.21978635954429,2160.192796203516));

      setProjectionClass(pc, proj);
      npEditControl.setProjection(proj);
      startingName = proj.getName();
    }

    void setDefaultProjection() {
      // let the first projection class be the default
      ProjectionClass selectedClass = (ProjectionClass) projClassCB.getItemAt(0);
      if (selectedClass == null) {
        System.out.println("Projection Manager: no Default Projection available");
        return;
      }
      ProjectionImpl proj = selectedClass.makeDefaultProjection();
      setProjection(proj);
      startingName = "";
    }

    public void setMap(ucar.nc2.ui.util.Renderer map) {
      if (map != null)
        npEditControl.addRenderer(map);
    }

    // user has hit the "accept/save" button
    private void accept() {
      ProjectionClass projClass = findProjectionClass(editProjection);
      if (null == projClass) {
        System.out.println("Projection Manager accept: findProjectionClass failed" + editProjection);
        return;
      }
      setProjFromDialog(projClass, editProjection);

      editProjection.setDefaultMapArea(mapEditPanel.getMapArea());
      if (debug) System.out.println("Projection Manager accept bb =" + editProjection.getDefaultMapArea());
      ProjectionImpl newProj = editProjection.constructCopy(); // use a copy


     //if (!viewDialog.checkSaveOK(startingName, newProj.getName()))
      //  return;

      if (ProjectionManager.this.contains(newProj.getName()))
        projTable.replaceProjection(newProj);
      else
        projTable.addProjection(newProj);

      // set new projection to working projection and exit this Dialog
      setWorkingProjection(newProj);
      NewProjectionDialog.this.setVisible(false);
    }

    private ProjectionClass findProjectionClass(Projection proj) {
      Class want = proj.getClass();
      ComboBoxModel projClassList = projClassCB.getModel();
      for (int i = 0; i < projClassList.getSize(); i++) {
        ProjectionClass pc = (ProjectionClass) projClassList.getElementAt(i);
        if (want.equals(pc.projClass))
          return pc;
      }
      return null;
    }

    private void setProjectionClass(ProjectionClass pc, ProjectionImpl proj) {
      if ((null == proj) || (null == pc))
        return;

      if (debug) System.out.println("Projection setProjectionClass= " + proj);

      setFieldsWithClassParams(pc);
      editProjection = proj.constructCopy();
      putProjInDialog(pc, editProjection);

      if (debug) System.out.println("Projection setProjectionClass ok ");

      invalidate();    // force new layout
      validate();
    }

    // construct input fields based on Projection Class
    private void setFieldsWithClassParams(ProjectionClass projClass) {

      // set the projection in the JComboBox
      String want = projClass.toString();
      for (int i = 0; i < projClassCB.getItemCount(); i++) {
        ProjectionClass pc = (ProjectionClass) projClassCB.getItemAt(i);
        if (pc.toString().equals(want)) {
          projClassCB.setSelectedItem(pc);
          break;
        }
      }

      // set the parameter fields
      paramPanel.removeAll();
      paramPanel.setVisible(0 < projClass.paramList.size());

      for (int i = 0; i < projClass.paramList.size(); i++) {
        ProjectionParam pp = projClass.paramList.get(i);
        // construct the label
        JPanel thisPanel = new JPanel();
        thisPanel.add(new JLabel(pp.name + ": "));
        // text input field
        JTextField tf = new JTextField();
        pp.setTextField(tf);
        tf.setColumns(12);
        thisPanel.add(tf);
        paramPanel.add(thisPanel);
      }
    }

    // get values from this projection and put into Dialog fields
    private void putProjInDialog(ProjectionClass projClass, Projection proj) {
      nameTF.setText(proj.getName().trim());

      Object[] voidObjectArg = new Object[]{};

      for (int i = 0; i < projClass.paramList.size(); i++) {
        ProjectionParam pp = projClass.paramList.get(i);
        // fetch the value from the projection object
        Double value;
        try {
          if (debugBeans) System.out.println("Projection putProjInDialog invoke reader on " + pp);
          value = (Double) pp.reader.invoke(proj, voidObjectArg);
          if (debugBeans) System.out.println("Projection putProjInDialog value " + value);

        } catch (Exception ee) {
          System.err.println("ProjectionManager: putProjInDialog failed  invoking read " + pp.name + " class " + projClass);
          continue;
        }
        String valstr = ucar.unidata.util.Format.d(value, 5);
        pp.getTextField().setText(valstr);
      }
    }

    // set values from the Dialog fields into the projection
    private void setProjFromDialog(ProjectionClass projClass, ProjectionImpl proj) {
      proj.setName(nameTF.getText().trim());

      for (int i = 0; i < projClass.paramList.size(); i++) {
        ProjectionParam pp = projClass.paramList.get(i);
        // fetch the value from the projection object
        try {
          String valstr = pp.getTextField().getText();
          Double valdub = new Double(valstr);
          Object[] args = {valdub};
          if (debugBeans) System.out.println("Projection setProjFromDialog invoke writer on " + pp);
          pp.writer.invoke(proj, args);

        } catch (Exception ee) {
          System.err.println("ProjectionManager: setProjParams failed  invoking write " + pp.name + " class " + projClass);
        }
      }
    }

  } // end NewProjectionDialog   */

  // inner class ProjectionClass: parsed projection classes
  class ProjectionClass {
    Class projClass;
    String name;
    java.util.List<ProjectionParam> paramList = new ArrayList<ProjectionParam>();

    public String toString() {
      return name;
    }

    ProjectionClass(String className) throws ClassNotFoundException, IntrospectionException {
      projClass = Class.forName(className);

      // eliminate common properties with "stop class" for getBeanInfo()
      Class stopClass;
      try {
        stopClass = Class.forName("ucar.unidata.geoloc.ProjectionImpl");
      } catch (Exception ee) {
        System.err.println("constructParamInput failed ");
        stopClass = null;
      }

      // analyze current projection class as a bean; may throw IntrospectionException
      BeanInfo info = java.beans.Introspector.getBeanInfo(projClass, stopClass);

      // find read/write methods
      PropertyDescriptor[] props = info.getPropertyDescriptors();
      if (debugBeans) {
        System.out.print("Bean Properties for class " + projClass);
        if (props == null || props.length == 0) {
          System.out.println("none");
          return;
        }
        System.out.println("");
      }
      for (PropertyDescriptor pd : props) {
        Method reader = pd.getReadMethod();
        Method writer = pd.getWriteMethod();
        // only interested in read/write properties
        if (reader == null || writer == null)
          continue;
        ProjectionParam p = new ProjectionParam(pd.getName(), reader, writer, pd.getPropertyType());
        paramList.add(p);

        if (debugBeans) System.out.println("  -->" + p);
      }

      // get an instance of this class so we can call toClassName()
      Projection project;
      if (null == (project = makeDefaultProjection())) {
        name = "none";
        return;
      }

      Class[] voidClassArg = new Class[]{};
      Object[] voidObjectArg = new Object[]{};

      // invoke the toClassName method
      try {
        Method m = projClass.getMethod("getClassName", voidClassArg);
        name = (String) m.invoke(project, voidObjectArg);

      } catch (NoSuchMethodException ee) {
        System.err.println("ProjectionManager: class " + projClass + " does not have method getClassName()");
        throw new ClassNotFoundException();
      } catch (SecurityException ee) {
        System.err.println("ProjectionManager: class " + projClass + " got SecurityException on getClassName()" + ee);
        throw new ClassNotFoundException();
      } catch (Exception ee) {
        System.err.println("ProjectionManager: class " + projClass + " Exception when invoking getClassName()" + ee);
        throw new ClassNotFoundException();
      }
    }

    private ProjectionImpl makeDefaultProjection() {
      Class[] voidClassArg = new Class[]{};
      Object[] voidObjectArg = new Object[]{};

      // the default constructor
      try {
        Constructor c = projClass.getConstructor(voidClassArg);
        return (ProjectionImpl) c.newInstance(voidObjectArg);

      } catch (Exception ee) {
        System.err.println("ProjectionManager makeDefaultProjection failed to construct class " + projClass);
        System.err.println("   " + ee);
        return null;
      }
    }
  }  // end ProjectionClass inner class

  // inner class ProjectionParam: parameters for Projection Class
  class ProjectionParam {
    Method reader;
    Method writer;
    String name;
    Class paramType;
    JTextField tf;      // edit field component

    ProjectionParam(String name, Method reader, Method writer, Class paramType) {
      this.name = name;
      this.reader = reader;
      this.writer = writer;
      this.paramType = paramType;
    }

    public String toString() {
      return paramType.getName() + " " + name + " " + ((reader == null) ? "-" : "R") + ((writer == null) ? "-" : "W");
    }

    void setTextField(JTextField tf) {
      this.tf = tf;
    }

    JTextField getTextField() {
      return tf;
    }

  }


  // testing 1-2-3

  public static void main(String[] args) {
    ProjectionManager  d = new ProjectionManager( null, null);
    d.setVisible();
  }

}


