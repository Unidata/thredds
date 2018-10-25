---
title: ToolsUI
last_updated: 2018-10-18
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: toolsui_ref.html
---

## Running ToolsUI

### Download and run locally

You can download toolsUI.jar from either the NetCDF-Java downloads or documentation pages. 
For the workshop, simply download toolsUI from [here](https://artifacts.unidata.ucar.edu/repository/unidata-all/edu/ucar/toolsUI/5.0.0-WORKSHOP/toolsUI-5.0.0-WORKSHOP.jar)
You can then run the ToolsUI application using a command similar to:

~~~bash
java -Xmx1g -jar toolsUI.jar
~~~

Alternatively, on many operating systems, you can simply double-click the JAR file.

## Tabs

### Viewer

The Viewer tab reads a dataset and displays its metadata in a tabular format.

#### Testing dataset readability

The NetCDF-Java library can read data from a wide variety of [scientific data formats](ncj_file_types.html). To check that your dataset can be read by NetCDF-Java, try to open it in the Viewer tab. You can open it in two ways:

* Enter the URL or file path of the dataset in the \"dataset\" field.
  Then hit the \"Enter\" key.
* Click the folder icon and select the file in the `FileChooser` dialog that pops up.

If the dataset can be opened, you will see its dimensions, variables, and other details in the window.
For example:

{% include image.html file="netcdf-java/reference/toolsUI/viewer1.png" alt="Viewer tab" caption="" %}

#### Showing data values

Select a variable in the table, right-click to open its context menu, and choose \"NCdump Data\".
The \"NCDump Variable Data\" dialog will pop up.
Here you can print (i.e. \"dump\") its values to the screen.

Here - using [Fortran 90 array section syntax](http://www.adt.unipd.it/corsi/Bianco/www.pcc.qub.ac.uk/tec/courses/f90/stu-notes/F90_notesMIF_5.html#HEADING41){:target="_blank"} (start:end:stride) - you can print (i.e. \"dump\") all or part of the variable\'s values to the screen.
By default, the full extent of the variable\'s dimensions are shown, where start and end are inclusive and zero-based.
For example:

{% include image.html file="netcdf-java/reference/toolsUI/ncdump_variable_data1.png" alt="Full extent" caption="" %}

However, you can edit that section to dump just the values you want.
For example:

{% include image.html file="netcdf-java/reference/toolsUI/ncdump_variable_data2.png" alt="Sliced" caption="" %}

In general, it\'s a good idea to make the number of values dumped reasonably small, if possible.

### Writer

The Writer tab takes a dataset in any format that NetCDF-Java can understand and writes it out to NetCDF. Several NetCDF \"flavors\" are supported:

* netcdf3
* netcdf4
* netcdf4_classic
* netcdf3c
* netcdf3c64
* ncstream

Note: to write to NetCDF-4, you must have the [C library loaded](netcdf4_c_library.html).

{% include image.html file="netcdf-java/reference/toolsUI/writer1.png" alt="Writer tab" caption="" %}

### NCDump

The NCDump tab offers functionality similar to - ​albeit more limited than - ​the ncdump utility.
The string that you enter in the \"Command\" field should be of the form:

~~~bash
<filename> [-unsigned] [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]
~~~

{% include image.html file="netcdf-java/reference/toolsUI/ncdump1.png" alt="NCDump tab" caption="" %}

### CoordSys

The `CoordSys` tab displays the coordinate systems that NetCDF-Java identified in the dataset.


{% include image.html file="netcdf-java/reference/toolsUI/coordsys1.png" alt="CoordSys tab" caption="" %}

### FeatureTypes

The sub-tabs of `FeatureTypes` provide detailed information about the various scientific feature types that a CDM dataset can contain.

#### `FeatureTypes`  &rarr; `Grids`

The Grids sub-tab can be used to determine if a dataset contains grids or not.
Simply try to open the dataset, the same way you did in the Viewer tab.
If rows of metadata are displayed in the tables, the dataset is gridded; otherwise it’s not (or perhaps NetCDF-Java just doesn’t recognize it as such).

Once you\'ve opened the gridded data, you can click the Grid Viewer button {% include inline_image.html file="netcdf-java/reference/toolsUI/redrawButton.jpg" alt="Grid Viewer button" %} to display the \"Grid Viewer\" dialog.
Press the other Grid Viewer button {% include inline_image.html file="netcdf-java/reference/toolsUI/redrawButton.jpg" alt="Grid Viewer button" %} in the dialog to visualize your grid.

{% include image.html file="netcdf-java/reference/toolsUI/gridViewer1.png" alt="Grid Viewer" caption="" %}

### `FeatureTypes` &rarr; `PointFeature`

The `PointFeature` sub-tab can be used to determine if a dataset contains point features or not.
Simply try to open the dataset, the same way you did in the Viewer tab.
If a metadata row appears in the top-left table, the dataset is \"pointed\"; otherwise it’s not (or perhaps NetCDF-Java just doesn’t recognize it as such).
Note that the middle-left table will populate as soon as you choose a row in the top-left table.
Similarly, the bottom table will populate as soon as you choose a row in the middle-left table.

{% include image.html file="netcdf-java/reference/toolsUI/pointFeature1.png" alt="PointFeature sub-tab" caption="" %}

## THREDDS

The THREDDS tab acts as a client to THREDDS servers.
Simply input the URL of a catalog and the available datasets will be displayed, just as if you were navigating a THREDDS server with your browser.
The difference is that when you select a dataset in the left window and then click on the \"Open File\", \"Open CoordSys\", or \"Open dataset\" buttons, ToolsUI will automatically switch to the appropriate tab and show metadata about that dataset.

{% include image.html file="netcdf-java/reference/toolsUI/thredds1.png" alt="THREDDS tab" caption="" %}

## NcML

When you open a dataset in the NcML tab, an NcML document containing all its metadata will be generated.
From here, it’s easy to modify the dataset using the [powers of NcML](ncj_basic_ncml_tutorial.html) and write the document out to disk.

{% include image.html file="netcdf-java/reference/toolsUI/ncml1.png" alt="NcML tab" caption="" %}