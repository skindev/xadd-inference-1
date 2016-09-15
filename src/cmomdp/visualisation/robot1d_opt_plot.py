import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd

sns.set_style("white")
# sns.set_style("ticks")

fig = plt.figure()
ax = fig.add_subplot(111)

data = pd.read_csv('/Users/skin/repository/xadd-inference-1/src/cmomdp/domain/aaai2017/results/robot1d_irl_policy.csv', na_values='null')

plt.plot(data.location, data.argmax, lw=2)

plt.xlim([-20.1, 20.1])
plt.ylim([0., 55.0])

plt.axvspan(-20.0, 0.0, facecolor='r', alpha=0.05)
plt.axvspan(0.0, 10.0, facecolor='b', alpha=0.05)
plt.axvspan(10.0, 20.0, facecolor='r', alpha=0.05)

sns.despine()

ax.text(-12.5, 45., 'move', color='black')
ax.text(3.5, 45, 'stay', color='black')
ax.text(13.5, 45, 'move', color='black')

ax.set_ylabel(r'$w$', fontsize=20)
ax.set_xlabel(r'$location$', fontsize=20)

# plt.show()

plt.savefig('robot_opt.pdf', bbox_inches='tight')