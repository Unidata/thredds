---
title: Client Catalogs in ToolsUI
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_via_toolsUI.html
---

The NetCDF Tools User Interface (a.k.a. ToolsUI) can read and display THREDDS catalogs.
You can start it from the command line, or launch it from [webstart](http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/webstart/netCDFtools.jnlp).

Use the THREDDS Tab, and click on the ![fileOpen](../../../images/tds/tutorial/client_catalogs/fileIcon.jpg){:height="12px" width="12px"} button to navigate to a local catalog file, or enter in the URL of a remote catalog, as below (_note that this is an XML document, not an HTML page!_).
The catalog will be displayed in a tree widget on the left, and the selected dataset will be shown on the right:

{% include image.html file="tds/tutorial/client_catalogs/TUIthreddsTab.png" alt="ToolsUI" caption="" %}

Once you get your catalog working in a TDS, you can enter the TDS URL directly, and view the datasets with the `Open` buttons.
