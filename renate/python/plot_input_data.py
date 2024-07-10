import geopandas as gpd 
import pandas as pd 

from pathlib import Path 

import matplotlib.pyplot as plt

dsn = Path("input")

home_locs = pd.read_csv(dsn.joinpath("districts_home_locations.csv"))

print(home_locs.head())

