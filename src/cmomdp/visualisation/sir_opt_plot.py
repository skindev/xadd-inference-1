import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd

sns.set_style("white")
# sns.set_style("ticks")

fig = plt.figure()
ax = fig.add_subplot(111)
ax.margins(0.05)

data = pd.read_csv('/Users/skin/repository/xadd-inference-1/src/cmomdp/domain/aaai2017/results/sir_infection_vaccination_opt.csv', na_values='null')

plt.plot(data.beta, data.argmax, lw=2)

plt.xlim([0, 1.0])
plt.ylim([-0.1, 1.1])

plt.axvspan(0.0, 0.25, facecolor='g', alpha=0.05)
plt.axvspan(0.25, 1.1, facecolor='b', alpha=0.05)

sns.despine()

ax.text(0.6, 0.5, '$R_0 > 1$', color='black')
ax.text(0.1, 0.5, '$R_0 < 1$', color='black')

ax.set_ylabel(r'$\nu$', fontsize=20)
ax.set_xlabel(r'$\beta$', fontsize=20)

# plt.show()

plt.savefig('sir_opt.pdf', bbox_inches='tight')