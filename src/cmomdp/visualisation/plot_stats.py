import matplotlib.pyplot as plt 
import pandas as pd 
import numpy as np 
import seaborn as sns 

import os
import sys

def plot_stats(csv_file_path):

	filename, file_extension = os.path.splitext(csv_file_path)

	data = pd.read_csv(csv_file_path, na_values='null')

	fig = plt.figure()

	plt.plot(data.horizon, data.sdp_time, color='blue', label='SDP')
	plt.plot(data.horizon, data.opt_time, color='green', label='Optimizer')
	plt.xlabel('Horizon')
	plt.ylabel('Time (seconds)')

	plt.legend()
	plt.show()

	fig.savefig(filename + '_time_fig.pdf')
	plt.close()

	fig2 = plt.figure()
	plt.plot(data.horizon, data.nodes, color='blue', label='Nodes')
	plt.plot(data.horizon, data.branches, color='green', label='Case Statements')

	plt.xlabel('Horizon')
	plt.ylabel('Number of Nodes/Case Statements')

	plt.legend()
	plt.show()

	fig2.savefig(filename + '_memory_fig.pdf')
	plt.close()


if __name__ == "__main__":
	stats_file = sys.argv[1]
	print(stats_file)

	plot_stats(stats_file)


