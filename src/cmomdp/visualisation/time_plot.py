import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd

# sns.set_style("white")
# sns.set_style("ticks")
# sns.despine()

sns.set_style("white", {'axes.grid' : False})

data = pd.read_csv('/Users/skin/repository/xadd-inference-1/src/cmomdp/domain/aaai2017/results/20160913/robot1d_irl_stats.csv', na_values='null')

mean_data = data.groupby(['horizon'], as_index=True).mean()

time_data = mean_data[['sdp_time', 'opt_time']]
time_data.columns = ['SDP', 'optimizer']

# sns.set_style("white")
time_data.plot(lw=2, grid=False)
sns.despine()

plt.ylabel(r'$time (s)$', fontsize=20)
plt.xlabel(r'$horizon$', fontsize=20)

# plt.show()

plt.savefig('time_plot.pdf', bbox_inches='tight')