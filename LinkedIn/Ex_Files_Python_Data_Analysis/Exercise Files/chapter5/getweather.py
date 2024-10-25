
import os
import urllib
import functools

import numpy as np
import pandas as pd

# if we don't have the stations file locally, download it
if not os.path.isfile('stations.txt'):
    urllib.request.urlretrieve('https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/ghcnd-stations.txt',
                               'stations.txt')
    
  
# FORMAT OF "ghcnd-stations.txt"
# ------------------------------
# Variable   Columns   Type
# ------------------------------
# ID            1-11   Character
# LATITUDE     13-20   Real
# LONGITUDE    22-30   Real
# ELEVATION    32-37   Real
# STATE        39-40   Character
# NAME         42-71   Character
# GSN FLAG     73-75   Character
# HCN/CRN FLAG 77-79   Character
# WMO ID       81-85   Character
# ------------------------------


# parse the stations.txt file as described in video 05_02
allstations = np.genfromtxt('stations.txt', delimiter=[11,9,10,7,3,31,4,4,6],
                                            usecols=[0,1,2,3,4,5,6,7,8],
                                            names=['id','latitude','longitude','elevation','state','name','gsn','hcn','wmo'],
                                            dtype=['U11','d','d','d','U3','U31','U4','U4','U6'],
                                            autostrip=True)

def getfile(station_name):  
    """Download the dly file for station_name, and return the local filename.
    
    If station_name is not in the list, find a station that _begins_
    with station_name, but give precedence to HCN and GSN stations."""
    
    # name of the local file
    station_file = f'{station_name}.dly'
    
    # if we don't have the file locally...
    if not os.path.isfile(station_file):
        # figure out the station id from the stations.txt file
        
        # start with all the station with names that begin as requested
        stations = allstations[np.char.find(allstations['name'], station_name) == 0]

        if len(stations) == 0:
            raise IOError("Station not available.")
        
        # we prefer GSN stations if available, then HCN, then we default to the first match
        if np.any(stations['gsn'] != ''):
            station = stations[stations['gsn'] != ''][0]
        elif np.any(stations['hcn'] != ''):
            station = stations[stations['hcn'] != ''][0]
        else:
            station = stations[0]
        
        print(f"Using {station}.")
        
        # compose the URL at which we expect to find the data
        url = f'https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/all/{station["id"]}.dly'
        
        print(f'Downloading {url}...')
        
        # download it to the local file
        urllib.request.urlretrieve(url, station_file)
        
    return station_file
    

# FORMAT OF "*.dly" files
# ------------------------------
# Variable   Columns   Type
# ------------------------------
# ID            1-11   Character
# YEAR         12-15   Integer
# MONTH        16-17   Integer
# ELEMENT      18-21   Character
# VALUE1       22-26   Integer
# MFLAG1       27-27   Character
# QFLAG1       28-28   Character
# SFLAG1       29-29   Character
# VALUE2       30-34   Integer
# MFLAG2       35-35   Character
# QFLAG2       36-36   Character
# SFLAG2       37-37   Character
#   .           .          .
#   .           .          .
#   .           .          .
# VALUE31    262-266   Integer
# MFLAG31    267-267   Character
# QFLAG31    268-268   Character
# SFLAG31    269-269   Character
# ------------------------------
    

# cache the results of this call so we do the work only once
@functools.lru_cache()
def getdata(station_name):
    """Make a pandas DataFrame with clean weather data for station_name.
    
    If station_name cannot be found, find a station that _begins_
    with station_name, but give precedence to HCN and GSN stations."""
    
    station_file = getfile(station_name)
    
    # load the fixed-width file following its definition in readme.txt
    # note that there are 31 sequences of values + flags, one of each day of the month
    # (some will be undefined for some months)
    w = np.genfromtxt(station_file,
                      delimiter=[11,4,2,4] + [5,1,1,1]*31,
                      # we will not use the daily flags, so this list becomes
                      # 0, 1, 2, 3, 4, 8, 12, 16, 20, 24...
                      usecols=[0,1,2,3] + list(range(4,4*32,4)),
                      # the names of the daily observations will be day1, day2, day3, ...
                      names=['id','year','month','element'] + [f'day{i}' for i in range(1,32)],
                      dtype=['U11','i','i','U4'] + ['d']*31,
                      autostrip=True)

    # convert the numpy record array to a pandas DataFrame, a more powerful object
    # for cleaning and restructuring data
    pw = pd.DataFrame(w)

    # "melt" the daily observations into one record per daily observation,
    # storing the column name in 'day'
    pw = pd.melt(pw, id_vars=['id','year','month','element'], var_name='day', value_name='value')
    
    # throw away null observations
    pw = pw[pw.value != -9999]

    # keep only min/max temperatures, precipitation, and snow
    pw = pw[pw.element.isin(['TMAX','TMIN','PRCP','SNOW'])]

    # convert 'day1', 'day2', etc. to the number of the day 
    pw['day'] = pw.day.apply(lambda x: int(x[3:]))
    
    # make a date out of year, month, day
    pw['date'] = pd.to_datetime(pw[['year','month','day']])

    # keep only data, element, and value
    pw = pw[['date','element','value']]

    # restructure the DataFrame so that different elements for the same day appear in the same row
    # (basically the opposite of melt)
    pw = pw.pivot(index='date', columns='element')['value']
    pw.columns.name = None    
    pw = pw[['TMIN','TMAX','PRCP','SNOW']]
    
    # last, convert temperatures to degrees
    pw['TMIN'] /= 10.0
    pw['TMAX'] /= 10.0
    
    # wow, all done
    return pw


def getyear(station_name, elements, year):
    """Make a NumPy record array of length 365, containing weather data
    at station_name for the list of requested elements (TMIN/TMAX/PRCP/SNOW),
    restricted to year.
    
    If station_name is not in the list, find a station that _begins_
    with station_name, but give precedence to HCN and GSN stations.
    """
        
    alldata = getdata(station_name)
    
    # select data by year, and get rid of the extra day in leap years
    # then pick out the "element" column
    yeardata = alldata[(alldata.index.year == year) & (alldata.index.dayofyear < 366)]
    
    # make an empty record array full of nans
    empty = np.full(365, np.nan, dtype=[(element, np.float64) for element in elements])
    
    for element in elements:    
        # fill it with values, using day of the year (1 to 365) as index
        empty[element][yeardata.index.dayofyear - 1] = yeardata[element].values
    
    return empty
