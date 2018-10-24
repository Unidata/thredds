## Modify existing metadata

### Rename the variable `lflx` to `LavaFlow`:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="nc/cldc.mean.nc">
  <variable name="LavaFlow" orgName="lflx" />
  ...
</netcdf>
~~~

### Remove the variable `LavaFlow` from the dataset.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="nc/cldc.mean.nc">
  <remove name="LavaFlow" type="variable" />
</netcdf>
~~~

### Remove the attribute `unwantedVariableAttribute` from the variable `LavaFlow`.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="nc/cldc.mean.nc">
  <variable name="LavaFlow">
    <remove name="unwantedVariableAttribute" type="attribute" />
  </variable>
</netcdf>
~~~

### Remove the global attribute unwantedGlobalAttribute from the dataset.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="nc/cldc.mean.nc">
  <remove name="unwantedAttribute" type="attribute" />
</netcdf>
~~~

### Rename a nested variable in a group:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="grid_1_2d.h5">
  <group name="HDFEOS">
    <group name="GRIDS">
      <group name="GeoGrid">
        <group name="Data_Fields">
          <variable name="temp" orgName="temperature" />
        </group>
      </group>
    </group>
  </group>
</netcdf>
~~~

### Override the values in a Variable:

~~~xml
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="myFile.nc">
  <variable name="longitude">
    <values start="-67.95" incr="1.05" />
  </variable>
</netcdf>
~~~

* The longitude variable already exists, and all its metadata is used; its better not to re-specify it so that its clear exactly what the NcML does.
* The number of points is taken from the size of the variable
* The variables\' values are replaced with the values `-67.95, -66.90, -65.85, ..., start*incr*(npts-1)`

## Copy only some data into a new netCDF file

1. First, use ToolsUI NcML tab to read in existing file or remote dataset.
2. Modify the NcML to add the `<explicit />` element and remove the dimensions, variables, and attributes not wanted (leave just the ones that you want), eg:
   ~~~xml
   <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="dods://thredds.ucar.edu/thredds/dodsC/grib/NCEP/NAM/CONUS_12km/conduit/Best">
     <explicit />
     <dimension name="reftime" length="56" />
     ...
     <attribute name="Originating_or_generating_Subcenter" value="0" />
     ...
     <variable name="reftime_ISO" shape="reftime" type="String">
       <attribute name="units" value="ISO8601" />
     </variable>
     ...
   </netcdf>
   ~~~
3. Use the ToolsUI NcML **Write Netcdf File** button to copy just those variables to a new netCDF file, or
4. Save the NcML file and use the command line copy. One can generate the NcML from a script and automate the process.

## Logical Views

The original variable has extraneous dimensions `latitude` and `longitude`:

~~~xml
<dimension name="time" length="143" />
<dimension name="pressure" length="63" />
<dimension name="latitude" length="1" />
<dimension name="longitude" length="1" />

<variable name="temperature" shape="time pressure latitude longitude" type="float">
  <attribute name="long_name" value="Sea Temperature" />
  <attribute name="units" value="Celsius" />
</variable>
~~~

Here is the NcML to remove them:

~~~xml
<variable name="temperature">
  <logicalReduce dimNames="latitude longitude" />
</variable>
~~~

## Union Aggregation

Create **union** of datasets `cldc.mean.nc` and `lflx.mean.nc` located in the directory `nc` relative to where the NcML file is found:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <attribute name="title" type="string" value="Union cldc and lflx"/>
 <aggregation type="union">
   <netcdf location="nc/cldc.mean.nc"/>
   <netcdf location="nc/lflx.mean.nc"/>
  </aggregation>
</netcdf>
~~~

Create **union** of all datasets ending in `mean.nc` located in the directory `nc` reletive to where the NcML file is found:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <attribute name="title" type="string" value="Union all mean.nc files"/>
  <aggregation type="union">
    <scan location="file:src/test/data/ncml/nc/" suffix="mean.nc"/> 
  </aggregation>
</netcdf>
~~~

See [Dataset URLs](ncj_dataset_urls.html) for more information on the location attribute.

## JoinNew Aggregation

### Name each dataset in a netcdf element and assign coordinate values there:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <variable name="time" type="int">
   <attribute name="units" value="months since 2000-6-16 6:00"/>
 </variable>
 <aggregation  dimName="time" type="joinNew">
   <variableAgg name="T"/>
   <netcdf location="file:src/test/data/ncml/nc/time0.nc" coordValue="0"/>
   <netcdf location="file:src/test/data/ncml/nc/time1.nc" coordValue="10"/>
   <netcdf location="file:src/test/data/ncml/nc/time2.nc" coordValue="99"/>
 </aggregation>
</netcdf>
~~~

#### Assign coordinate values in the coordinate variable:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <variable name="time" type="int">
   <attribute name="units" value="months since 2000-6-16 6:00"/>
   <values>0 10 99</values>
 </variable>
 <aggregation  dimName="time" type="joinNew">
   <variableAgg name="T"/>
   <netcdf location="file:src/test/data/ncml/nc/time0.nc"/>
   <netcdf location="file:src/test/data/ncml/nc/time1.nc"/>
   <netcdf location="file:src/test/data/ncml/nc/time2.nc"/>
 </aggregation>
</netcdf>
~~~

### Assign coordinate values to unknown number of datasets.

You dont have to know the number of files found in the scan, but they must be evenly spaced, and they must be in alphabetic order.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <variable name="time" type="int" shape="time">
   <attribute name="long_name" type="string" value="time coordinate" />
   <attribute name="units" type="string" value="days since 2001-8-31 00:00:00 UTC" />
   <values start="0" increment="1" />
 </variable>
 <aggregation  dimName="time" type="joinNew">
   <variableAgg name="T"/>
   <scan location="src/test/data/ncml/nc/" suffix="Dir.nc"/>
 </aggregation>
</netcdf>
~~~

### Scan directory, assign date coordinate value from filename.

The date coordinate must be derivable from the filename, using the dateFormatMark attribute.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <aggregation dimName="time" type="joinNew">
    <variableAgg name="T"/>
    <scan location="/data/goes/" suffix=".gini" dateFormatMark="SUPER-NATIONAL_1km_SFC-T_#yyyyMMdd_HHmm" />
  </aggregation>
</netcdf> 
~~~

## JoinExisting Aggregation

### Name each dataset in a netcdf element and read coordinate values from the files.

A coordinate variable named time must exist in each file.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <aggregation dimName="time" type="joinExisting">
   <netcdf location="file:src/test/data/ncml/nc/jan.nc"/>
   <netcdf location="file:src/test/data/ncml/nc/feb.nc"/>
 </aggregation>   
</netcdf>
~~~

### Name each dataset in a netcdf element and assign coordinate values explicitly.

Overrides existing coordinate variable, if any. 

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <variable name="time">
   <attribute name="long_name" value="climatological time"/>
   <attribute name="units" value="days since 000-00-00 0:00"/>
   <attribute name="_CoordinateAxisType" value="Time" />
 </variable>
 <aggregation dimName="time" type="joinExisting">
   <netcdf location="file:src/test/data/ncml/nc/jan.nc"
      coordValue="0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30"/>
   <netcdf location="file:src/test/data/ncml/nc/feb.nc"
      coordValue="31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58"/>
 </aggregation>
</netcdf>
~~~

### Scan directory, assign date coordinate value from filename.

Each file must have exactly one time slice.
The date coordinate must be derivable from the filename, using the `dateFormatMark` attribute.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <aggregation dimName="time" type="joinExisting">
    <scan dateFormatMark="CG#yyyyDDD_HHmmss" location="src/test/data/ncml/nc/cg/" suffix=".nc" subdirs="false" />
  </aggregation>
</netcdf>

<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <aggregation dimName="time" type="joinExisting" timeUnitsChange="true"> 
  <netcdf location="20060925_0600.nc" ncoords="2"/>
  <netcdf location="20060925_1200.nc" ncoords="2"/>
  <netcdf location="20060925_1800.nc" ncoords="2"/>
  <netcdf location="20060926_0000.nc" ncoords="2"/>
 </aggregation>
</netcdf>
~~~

### Name each dataset in a netcdf element and read coordinate values from the files, whose units change.

Add the `timeUnitsChange` attribute.
Also works for `Fmrc`.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <aggregation dimName="time" type="joinExisting" timeUnitsChange="true"> 
  <netcdf location="20060925_0600.nc" ncoords="2"/>
  <netcdf location="20060925_1200.nc" ncoords="2"/>
  <netcdf location="20060925_1800.nc" ncoords="2"/>
  <netcdf location="20060926_0000.nc" ncoords="2"/>
 </aggregation>
</netcdf>
~~~

## Change Global Attributes to Variables

The value(s) of the attribute in each file are placed into the named `Variable`.
In the following, the Variable is given a different name (`times`) than the global attribute (`time_coverage_end`):

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <aggregation dimName="time" type="joinExisting" recheckEvery="4 sec">
    <promoteGlobalAttribute name="times" orgName="time_coverage_end" />
    <scan dateFormatMark="CG#yyyyDDD_HHmmss" location="src/test/data/ncml/nc/cg/" suffix=".nc" subdirs="false" />
  </aggregation>
</netcdf>
~~~

Each file has a global attribute of type String:

`:time_coverage_end = "2006-06-07T13:00:00Z"`;

In the aggregation dataset, a String variable is created using the aggregation dimension:

~~~bash
  String times(time=3);
  data:
   "2006-06-07T12:00:00Z",   "2006-06-07T13:00:00Z",   "2006-06-07T14:00:00Z"
~~~

Only can be used on OuterDimension Aggregations: `JoinExisting`, `JoinNew`, `Fmrc`