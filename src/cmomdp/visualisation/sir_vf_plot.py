from mpl_toolkits.mplot3d import Axes3D
from matplotlib import cm
from matplotlib.ticker import LinearLocator, FormatStrFormatter
import matplotlib.mlab as mlab
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import pandas as pd

sns.set_style("white")

data = pd.read_table('/Users/skin/repository/xadd-inference-1/src/cmomdp/domain/aaai2017/results/sir_value_function_data.txt', names=['x', 'y', 'z'])
xv, yv = np.meshgrid(data.x.values, data.y.values)
zv = mlab.griddata(data.x.values, data.y.values, data.z.values, xv, yv, interp='linear')

fig = plt.figure()
ax = fig.add_subplot(111, projection='3d')

ax.plot_surface(xv, yv, zv, cmap=cm.magma, linewidth=0, rstride=1,cstride=1, antialiased=False)

ax.set_xlim(0.0, 1.0)
ax.set_ylim(0.0, 1.0)

ax.xaxis.labelpad = 15
ax.yaxis.labelpad = 15

ax.set_ylabel(r'$\nu$', fontsize=20)
ax.set_xlabel(r'$\beta$', fontsize=20)

plt.savefig('sir_vf.pdf')