import matplotlib.pyplot as plt 
import pandas as pd 
import numpy as np 
import seaborn as sns 

import os
import sys

def plot_opt(csv_file_path, variable_name):

	filename, file_extension = os.path.splitext(csv_file_path)


	data = pd.read_csv(csv_file_path)

	fig = plt.figure()
	ax = fig.add_subplot(111)

	plt.plot(data[variable_name], data.argmax, color='blue')
	plt.xlabel(variable_name.title())
	plt.ylabel('w')
	
	ax.set_ylim([0.0, 55.0])

	plt.show()

	fig.savefig(filename + '_fig.pdf')
	plt.close()


if __name__ == "__main__":
	stats_file = sys.argv[1]
	print(stats_file)

	variable_name = sys.argv[2]

	plot_opt(stats_file, variable_name)


