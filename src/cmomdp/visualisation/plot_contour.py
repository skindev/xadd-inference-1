import pandas as pd
from mpl_toolkits.mplot3d import Axes3D
from matplotlib import cm
from matplotlib import ticker
#from matplotlib.ticker import LinearLocator, FormatStrFormatter, ScalarFormatter
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns

import os
import sys

def plot_contour(data_file_path, x_label, y_label):

       	filename, file_extension = os.path.splitext(data_file_path)

       	# load the data into a pandas dataframe
       	data = pd.read_table(data_file_path, names=[x_label, y_label, 'z_val'])

       	fig = plt.figure()

       	ax = fig.add_subplot(111, projection='3d')
       	surf = ax.plot_trisurf(data[x_label], data[y_label], data.z_val, cmap=cm.jet, linewidth=1)

       	# fig.colorbar(surf)
        niceMathTextForm = ticker.ScalarFormatter(useMathText=True, useOffset=False)
        niceMathTextForm.set_scientific(True)        
        ax.w_zaxis.set_major_formatter(niceMathTextForm)

        ax.set_xlabel(x_label.title())
        ax.set_ylabel(y_label.title())

        ax.view_init(elev=30, azim=35)
        
       	plt.show()
		
		# print("saving pdf")
       	fig.savefig(filename + '_fig.pdf')
       	plt.close()

if __name__ == "__main__":
       	data_file_path = sys.argv[1]
       	x_label = sys.argv[2]
       	y_label = sys.argv[3]

       	print(data_file_path)

       	plot_contour(data_file_path, x_label, y_label)