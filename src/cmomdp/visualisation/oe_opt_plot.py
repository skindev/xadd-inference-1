import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd

sns.set_style("white")
# sns.set_style("ticks")

fig = plt.figure()
ax = fig.add_subplot(111)

data = pd.read_csv('/Users/skin/repository/xadd-inference-1/src/cmomdp/domain/aaai2017/results/opt_execution_fraction_opt_max.csv', na_values='null')

plt.plot(data.inventory, data.argmax, lw=2)

# plt.xlim([-20.1, 20.1])
# plt.ylim([0., 55.0])

sns.despine()

ax.set_ylabel(r'$\theta$', fontsize=20)
ax.set_xlabel(r'$inventory$', fontsize=20)

# plt.show()

plt.savefig('oe_opt.pdf', bbox_inches='tight')