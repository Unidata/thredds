---
title: CF DSG Encoding Table
last_updated: 2018-10-22
sidebar: netcdfJavaTutorial_sidebar
toc: true
permalink: cf_dsg_encoding_ref.html
---

## point

### single (point)

N/A

### multidim (point)

~~~bash
data(obs)
time,lat,lon,z(obs)
~~~

### ragged (point)

N/A

### Nested Table (point)

~~~bash
Table {
  lat, lon, z, time;
  data1, data2, ...
} obs(sample);
~~~

##  timeSeries

### single (timeSeries)

~~~bash
data(time)
time(time)
scalar lat,lon,stn_alt,stn_id
~~~

### multidim (timeSeries)

~~~bash
data(station,obs)
time(station,obs)|time(time)
lat,lon,stn_alt,stn_id(station)
~~~

### ragged (timeSeries)

~~~bash
data(obs)
time(obs)
lat,lon,stn_alt,stn_id(station)
row_size(station) | stn_index(obs)
~~~

### Nested Table (timeSeries)

~~~bash
Table {
  lat, lon, z;
  Table {
    time;
    data1, data2, ...
  } obs(*);
} station(station);
~~~

## trajectory

### single (trajectory)

~~~bash
data(obs)
time,lat,lon,z(obs)
scalar traj_id
~~~

### multidim (trajectory)

~~~bash
data(traj,obs)
time,lat,lon,z(traj,obs)|time(obs)
traj_id(traj)
~~~

### ragged (trajectory)

~~~bash
data(obs)
time,lat,lon,z(obs)
row_size(traj) | traj_index(obs)
traj_id(traj)
~~~

### Nested Table (trajectory)

~~~bash
Table {
  Table {
    time, lat, lon, z;
    data1, data2, ...
  } obs(*);
} traj(traj);
~~~

## profile

### single (profile)

~~~bash
data(z)
time(z) | time
scalar lat,lon
z(z)
scalar profile_id
~~~

### multidim (profile)

~~~bash
data(profile,z)
time(profile,z) | time(profile)
lat,lon(profile)
alt(profile,z) | z(z)
profile_id(profile)
~~~

### ragged (profile)

~~~bash
data(obs)
time(profile) | time(obs)
lat,lon(profile)
z(obs)
row_size(profile) | profile_index(obs)
~~~

### Nested Table (profile)

~~~bash
profile_id(profile)
Table {
  lat, lon, time;
  Table {
    z;
    data1, data2, ...
  } obs(*);
} profile(profile);
~~~

## seriesProfile

### single (seriesProfile)

~~~bash
data(profile, z)
time(profile, z)|time(profile)
lat,lon,stn_alt,stn_id
alt(profile,z) | z(z)
~~~

### multidim (seriesProfile)

~~~bash
data(station, profile, z)
time(sta,prof,z)|time(sta,prof)|time(prof)
lat,lon,stn_alt,stn_id(station)
alt(sta,prof,z)|alt(prof,z)|z(z)
~~~

### ragged (seriesProfile)

~~~bash
data(obs)
time(profile) | time(obs)
lat,lon,stn_alt,stn_id(station)
z(obs)
station_index(profile),row_size(profile)
~~~

### Nested Table (seriesProfile)

~~~bash
Table {
  lat, lon, alt;
  Table {
    time;
    Table {
      z;
      data1, data2, ...
    } obs(*);
  } profile(*);
} station(station);
~~~

## trajProfile

### single (trajProfile)

~~~bash
data(profile, z)
time(profile, z)|time(profile)
lat,lon(profile)
alt( profile,z) | z(z)
scalar traj_id
~~~

### multidim (trajProfile)

~~~bash
data(traj, profile, z)
time(traj,prof,z)|time(traj,prof)
lat,lon(traj, profile)
alt(traj,profile,z) | z(z)
traj_id(traj)
~~~

### ragged (trajProfile)

~~~bash
data(obs)
time(profile) | time(obs)
lat,lon(profile)
z(obs) | z(z)
traj_index(profile),row_size(profile)
traj_id(traj)
~~~

### Nested Table (trajProfile)

~~~bash
Table {
  Table {
    lat, lon, time;
    Table {
      z;
      data1, data2, ...
    } obs(*);
  } profile(*);
} traj(traj);
~~~