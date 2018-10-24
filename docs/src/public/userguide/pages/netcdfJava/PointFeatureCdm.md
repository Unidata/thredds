---
title: PointFeatures
last_updated: 2018-10-23
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: pointfeature_ref.html
---

## Overview

**Point Feature Datasets** (also known as **Discrete Sampling Geometry (DSG) datasets**) are collections of Point Features.
Point Feature Datasets contain one or more DsgFeatureCollections:

~~~java
public interface ucar.nc2.ft.FeatureDatasetPoint extends ucar.nc2.ft.FeatureDataset {
  List<DsgFeatureCollection> getPointFeatureCollectionList();
}
~~~

~~~java
public interface DsgFeatureCollection {
  String getName();
  ucar.nc2.constants.FeatureType getCollectionFeatureType();
  CalendarDateUnit getTimeUnit();
  String getAltUnits();
  CalendarDateRange getCalendarDateRange();
  ucar.unidata.geoloc.LatLonRect getBoundingBox();
  int size();
}
~~~

Point feature types are arrangements of collections of Point Features (a set of measurements at the same point in space and time), distinguished by the geometry and topology of the collections. 
The `Point Feature Types` that are implemented are:
1. Point feature : one or more parameters measured at one point in time and space.
2. Station time series feature : a time-series of data points all at the same location, with varying time.
3. Profile feature : a set of data points along a vertical line.
4. Station Profile feature : a time-series of profile features at a named location.
5. Trajectory feature : a set of data points along a 1D curve in time and space.
6. Trajectory Profile feature : a collection of profile features which originate along a trajectory.
   Also known as a Section.

Related documents:
* [CF Discrete Sampling Geometries Conventions](http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/cf-conventions.html#discrete-sampling-geometries){:target="_blank"}
* [CDM Nested Table encoding](cf_dsg_encoding_ref.html)
* {% include link_file.html file="netcdf-java/reference/uml/PointFeatureUML.svg" text="Complete Point Feature UML" %}

## Point Features

{% include image.html file="netcdf-java/reference/uml/PointFeatureUML.png" alt="Point Feature UML" caption="" %}

A `PointFeature` is a collection of data taken at a single time and a single place:

~~~java
public interface PointFeature {
  DsgFeatureCollection getFeatureCollection();

  ucar.unidata.geoloc.EarthLocation getLocation();

  double getObservationTime();
  CalendarDate getObservationTimeAsCalendarDate();
  double getNominalTime();
  CalendarDate getNominalTimeAsCalendarDate();
  CalendarDateUnit getTimeUnit();

  ucar.ma2.StructureData getDataAll() throws java.io.IOException;
  ucar.ma2.StructureData getFeatureData() throws java.io.IOException;
}
~~~

The time can be retrieved as a `CalendarDate` or as a double in units of `getFeatureCollection().getTimeUnit()`.
The actual time of the data sample is the observation time, and is always present.
Some observational systems bin data into standard intervals, in which case there is also a nominal time.
When the nominal time is not given in the data, it is usually set to the observational time. 
Conversion between time as a `double` and as a `CalendarDate` is done by `getFeatureCollection().getTimeUnits()`.

When a `PointFeature` is part of nested features, `getDataAll()` will return the data with all of the parent data in a single `StructureData` object, while `getFeatureData()` will return the data at just the innermost feature.
See examples below.

The `location` is returned as:

~~~java
public interface ucar.unidata.geoloc.EarthLocation {
  double getLatitude();
  double getLongitude();
  double getAltitude();
  ucar.unidata.geoloc.LatLonPoint getLatLon();
}
~~~

The `latitude` and `longitude` are required, while the `altitude` may be missing and if so, is set to `Double.NaN`.
The `altitude` `units` (if they exist) can be found from `getFeatureCollection().getAltUnits()`.

The actual data of the observation is contained in a [`ucar.ma2.StructureData`](arraystructures_ref.html), which has a collection of `StructureMembers` which describe the individual data members, along with many convenience routines for extracting the data.

### PointFeatureCollection

A `PointFeatureCollection` is a collection of `PointFeatures`:

~~~java
public interface PointFeatureCollection extends DsgFeatureCollection, Iterable<PointFeature> {
  PointFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;
}
~~~

A `PointFeatureCollection` inherits from `DsgFeatureCollection`, and so has a `name`, `featureType`, and `timeUnit`.
The `size`, `boundingBox`, and `dateRange` may not be known until after iterating through the collection, that is, actually reading the data.
You can force the discovery of these by calling `ucar.nc2.ft.point.DsgCollectionHelper.calcBounds()`.
A `PointFeatureCollection` implements `Iterable<PointFeature>`, so can be used in a foreach block:

~~~java
for (PointFeature pf : pointFeatureCollection) {
  ...
}
~~~

You may subset a `PointFeatureCollection` with a lat/lon bounding box, and/or a `dateRange`, for example:

~~~java
CalendarDateRange dateRange = CalendarDateRange.of(start, end);
LatLonRect horizSubset = new LatLonRect(40,-105,10.0,20.0);

// get all the points in that subset
PointFeatureCollection subset = original.subset(horizSubset, dateRange);
for (PointFeature pf : subset) {
    ...
}
~~~

## Profile Features

A `ProfileFeature` is a collection of PointFeatures along a vertical line.

~~~java
public interface ProfileFeature extends PointFeatureCollection, Iterable<PointFeature> {
  LatLonPoint getLatLon();
  CalendarDate getTime(); // nominal
  StructureData getFeatureData() throws IOException;
}
~~~

`ProfileFeature` extends `PointFeatureCollection` and `DsgFeatureCollection`, so has a `name`, `altUnit`, etc.
The iterator will return `PointFeatures` that all belong to the same profile, with the same lat/lon point and varying heights.
Each profile will have a nominal time, but there may also be a time associated with each point in the profile that varies.

Since a profile is a `PointFeatureCollection`, it implements `Iterable<PointFeature>`, so you get its data using:

~~~java
for (PointFeature pf : profile) {
  ...
}
~~~

### ProfileFeatureCollection

A collection of `ProfileFeatures` is a `ProfileFeatureCollection`:

~~~java
public interface ProfileFeatureCollection extends PointFeatureCC, Iterable<ProfileFeature> {
  ProfileFeatureCollection subset(LatLonRect boundingBox) throws IOException;
  ProfileFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;
}
~~~

To read all the data, iterate through each `ProfileFeature` in the collection, then through each `PointFeature` of the `ProfileFeature`:

~~~java
for (ProfileFeature profile : profileFeatureCollection) {
  StructureData profileData = profile.getFeatureData();
  for (PointFeature obs : profile) {
    StructureData obsData = obs.getFeatureData();
    ...
  }
}
~~~

Data associated with the entire profile will be found in `profile.getFeatureData()`, while the data along the z axis will be in `obs.getFeatureData()`.

You may subset a `ProfileFeatureCollection` with a lat/lon bounding box, getting back another `ProfileFeatureCollection`.
Typically this is a logical subset, and no data is read until you iterate over the subset:

~~~java
LatLonRect wantBB = new LatLonRect("-60,120,12,20");
ProfileFeatureCollection subset = profileFeatureCollection.subset(wantBB);

// get all the profiles in the specified bounding box
for (ProfileFeature profile : subset) {
  LatLonPoint profileLocation = profile.getLatlon();
  ...
}
~~~

## Station Time Series Features

A `StationTimeSeriesFeature` is a time series of `PointFeatures` at a single, named location called a `Station`:

~~~java
public interface StationTimeSeriesFeature extends StationFeature, PointFeatureCollection {
  String getName();
  String getDescription();
  String getWmoId();
  double getLatitude();
  double getLongitude();
  double getAltitude();
  LatLonPoint getLatLon();

  StructureData getFeatureData() throws IOException;

  StationTimeSeriesFeature subset(CalendarDateRange dateRange) throws IOException;
}
~~~

`StationTimeSeriesFeature` extends `PointFeatureCollection` and `DsgFeatureCollection`, so has a `name`, `altUnit`, `timeUnits`, etc.
It also extends `Station` and `EarthLocation` and so has a `description`, `lat`, `lon`, `altitude` and so on.

An iterator will return `PointFeatures` that all belong to the same station.
These may or may not be time-ordered.
One can also subset on `dateRange`:

~~~java
CalendarDateRange dateRange = CalendarDateRange.of(start, end);
PointFeatureCollection subset = stationTimeSeriesCollection.subset(dateRange);
for (PointFeature pointFeature : subset) {
  StructureData allData = pointFeature.getDataAll();
  ...
}
~~~

The example also shows getting a single `StructureData` that will include the data from both the station and the observation.

### StationTimeSeriesFeatureCollection

A `StationTimeSeriesFeatureCollection` is a collection of stations with time series data at each:

~~~java
public interface StationTimeSeriesFeatureCollection extends PointFeatureCC, Iterable<StationTimeSeriesFeature> {

  List<StationFeature> getStationFeatures() throws IOException;
  List<StationFeature> getStationFeatures( List<String> stnNames)  throws IOException;
  List<StationFeature> getStationFeatures( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  StationFeature findStationFeature(String name);
  StationTimeSeriesFeature getStationTimeSeriesFeature(StationFeature s) throws IOException;

  // subsetting
  StationTimeSeriesFeatureCollection subset(List<StationFeature> stations) throws IOException;
  StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;
  StationTimeSeriesFeatureCollection subset(List<StationFeature> stns, CalendarDateRange dateRange) throws IOException;
  StationTimeSeriesFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;

  PointFeatureCollection flatten(List<String> stations, CalendarDateRange dateRange, List<VariableSimpleIF> varList) throws IOException;
  PointFeatureCollection flatten(LatLonRect llbbox, CalendarDateRange dateRange) throws IOException;
}
~~~

A `StationTimeSeriesFeatureCollection` is a collection of stations, from which you can get the list of available stations, a bounding box, etc.
You may subset the station collection by passing in a list of station names or a lat/lon bounding box.
You may subset the timeseries collection by passing in a list of stations, a lat/lon bounding box, or a date range.
You may flatten the timeseries collection, making it into a collection of `PointFeatures`.
The flattening may include subsetting by lat/lon bounding box, and/or a dateRange.
Flattening can sometimes improve performance.

To access the data, get a `StationTimeSeriesFeature` for a specified Station, or iterate over all `StationTimeSeriesFeatures` in the collection:

~~~java
for (StationTimeSeriesFeature timeSeries : stationCollection) {
  StructureData stnData = timeSeries.getFeatureData();
  for (ucar.nc2.ft.PointFeature pointFeature : timeSeries) {
    StructureData obsData = pointFeature.getFeatureData();
    ...
  }
}
~~~

To get a time series at a particular station:

~~~java
Station stn = stationTimeSeriesCollection.getStation("FXOW");
StationTimeSeriesFeature timeSeries = stationTimeSeriesCollection.getStationFeature(stn);
for (ucar.nc2.ft.PointFeature pointFeature : timeSeries) {
  ...
}
~~~

To get all PointFeatures in a specific area and time range, it can help performance sometimes to flatten the `StationTimeSeriesCollection`, so that the points can be returned in the order they are stored, instead of sorting by Station.
One can still retrieve the associated station by calling `stationCollection.getStationFeature(pointFeature)`:

~~~java
LatLonRect bb = new LatLonRect( new LatLonPointImpl(40.0, -105.0),
                                new LatLonPointImpl(42.0, -100.0));
CalendarDateRange dateRange = CalendarDateRange.of(start, end);
PointFeatureCollection points = stnCollection.flatten(bb,dateRange);

for (PointFeature pointFeature : points) {
  StationFeature stationFeature = stnCollection.getStationFeature(pointFeature);
  String stationName = stationFeature.getName();
  ...
}
~~~

## Station Profile Features

A `StationProfileFeature` is a time series of `ProfileFeatures` at a single, named location.

~~~java
public interface StationProfileFeature extends StationFeature, PointFeatureCC, Iterable<ProfileFeature> {
  String getName();
  String getDescription();
  String getWmoId();
  double getLatitude();
  double getLongitude();
  double getAltitude();
  StructureData getFeatureData() throws IOException;

  List<CalendarDate> getTimes() throws IOException;
  ProfileFeature getProfileByDate(CalendarDate date) throws IOException;

  StationProfileFeature subset(CalendarDateRange dateRange) throws IOException;
}
~~~

A `StationProfileFeature` is a time series of profiles at a named location.
It extends `StationFeature`, and so has `name`, `description`, etc.
Each profile has a nominal time value, and you can get a list of these, or find a specific profile by time.

You can iterate over the `ProfileFeatures` in the collection, then through all `PointFeatures` of the `ProfileFeature`:

~~~java
for (ucar.nc2.ft.ProfileFeature profile : stationProfileFeature) {
  StructureData profileData = profile.getFeatureData();
  for (ucar.nc2.ft.PointFeature pointFeature : profile) {
    ...
  }
}
~~~

### StationProfileFeatureCollection

A `StationProfileFeatureCollection` is a collection of `StationProfileFeature`, i.e. multiple stations, each of which has time series of profiles.

~~~java
public interface StationProfileFeatureCollection extends PointFeatureCCC, Iterable<StationProfileFeature> {

  List<StationFeature> getStationFeatures() throws IOException;
  List<StationFeature> getStationFeatures( List<String> stnNames)  throws IOException;
  List<StationFeature> getStationFeatures( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  StationFeature findStationFeature(String name);
  StationProfileFeature getStationProfileFeature(StationFeature s) throws IOException;

  // subsetting
  StationProfileFeatureCollection subset(List<StationFeature> stations) throws IOException;
  StationProfileFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;
  StationProfileFeatureCollection subset(List<StationFeature> stns, CalendarDateRange dateRange) throws IOException;
  StationProfileFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException;
}
~~~

A `StationProfileFeatureCollection` looks much like a `StationTimeSeriesFeatureCollection`, except you have profiles instead of point features.

To run through all the data, iterate through each `StationProfileFeature` in the collection, then through each `ProfileFeature` in the `StationProfileFeature`, then through each `PointFeature` of the `ProfileFeatures`:

~~~java
for (StationProfileFeature stationProfile : stationProfileFeatureCollection) {
  StructureData stnData = stationProfile.getFeatureData();
  for (ProfileFeature profile : stationProfile) {
    StructureData profileData = profile.getFeatureData();
    for (PointFeature pointFeature : profile) {
      StructureData obsData = pointFeature.getFeatureData();
      ...
    }
  }
}
~~~

## Trajectory Features

A `TrajectoryFeature` is a connected collection of `PointFeatures` along a line in space and time.

~~~java
public interface TrajectoryFeature extends PointFeatureCollection, Iterable<PointFeature> {
  int size();
  CalendarDateRange getCalendarDateRange();
  ucar.unidata.geoloc.LatLonRect getBoundingBox();
  StructureData getFeatureData() throws IOException;
}
~~~

`TrajectoryFeature` extends `PointFeatureCollection` and `DsgFeatureCollection`, so has a `name`, `altUnit`, etc.
The iteration will return `PointFeatures` that all belong to the same trajectory.
Since a trajectory is a `PointFeatureCollection`, it implements `Iterable<PointFeature>`, so you get its data using:

~~~java 
for (PointFeature pf : trajectory) {
  ...
}
~~~

### TrajectoryFeatureCollection

A collection of `TrajectoryFeatures` is a `TrajectoryFeatureCollection`:

~~~java
public interface TrajectoryFeatureCollection extends PointFeatureCC, Iterable<TrajectoryFeature> {
  TrajectoryFeatureCollection subset(LatLonRect boundingBox) throws IOException;
}
~~~

To read all the data, iterate through each `TrajectoryFeature` in the collection, then through each `PointFeature`:

~~~java
for (TrajectoryFeature traj : trajectoryFeatureCollection) {
  StructureData trajData = traj.getFeatureData();
  for (PointFeature obs : traj) {
    StructureData obsData = obs.getFeatureData();
    ...
  }
}
~~~

Data associated with the entire trajectory will be found in `traj.getFeatureData()`, while the data along the trajectory will be in `obs.getFeatureData()`.

You may subset a `TrajectoryFeatureCollection` with a lat/lon bounding box, getting back another `TrajectoryFeatureCollection`.
Typically this is a logical subset, and no data is read until you iterate over the subset:

~~~java
LatLonRect wantBB = new LatLonRect("-60,120,12,20");
TrajectoryFeatureCollection subset = trajectoryFeatureCollection.subset(wantBB);

// get all the profiles in the specified bounding box
for (TrajectoryFeature traj : subset) {
  ...
}
~~~

## TrajectoryProfileFeature Features

A `TrajectoryProfileFeature` is a time series of profiles along a line is space and time, ie a trajectory.

~~~java
public interface TrajectoryProfileFeature extends PointFeatureCC, Iterable<ProfileFeature> {
  StructureData getFeatureData() throws IOException;
}
~~~

You can iterate over the `ProfileFeatures` in the collection, then through all `PointFeatures` of the `ProfileFeature`:

~~~java
for (ucar.nc2.ft.ProfileFeature profile : trajProfileFeature) {
  StructureData profileData = profile.getFeatureData();
  for (ucar.nc2.ft.PointFeature pointFeature : profile) {
    ...
  }
}
~~~

### TrajectoryProfileFeatureCollection

A `TrajectoryProfileFeatureCollection` is a collection of `TrajectoryProfileFeatures`, i.e. multiple trajectories, each of which has a time series of profiles.

~~~java
public interface TrajectoryProfileFeatureCollection extends PointFeatureCCC, Iterable<StationProfileFeature> {}
~~~

To run through all the data, iterate through each `TrajectoryProfileFeatureCollection` in the collection, then through each `ProfileFeature` in the `StationProfileFeature`, then through each `PointFeature` of the `ProfileFeatures`:

~~~java
for (TrajectoryProfileFeature trajProfile : trajProfileFeatureCollection) {
  StructureData trajData = trajProfile.getFeatureData();
  for (ProfileFeature profile : trajProfile) {
    StructureData profileData = profile.getFeatureData();
    for (PointFeature pointFeature : profile) {
      StructureData obsData = pointFeature.getFeatureData();
      ...
    }
  }
}
~~~