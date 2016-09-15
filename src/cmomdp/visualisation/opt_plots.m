
% Robot Policy Opt

plot(location,argmax, 'LineWidth', 2)
set(gca, 'Box', 'off');

ylim([0 55])

ylabel('$w_2$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
xlabel('location', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')

% SIR Policy Opt

plot(beta1, argmax, 'LineWidth', 2)
set(gca, 'Box', 'off');

ylim([-0.1 1.1])

ylabel('$\pi\left(\nu\right)$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
xlabel('$\beta$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')

% OE Policy Opt

plot(inventory, argmax, 'LineWidth', 2)
set(gca, 'Box', 'off');

ylim([0 1.1])

ylabel('$\pi\left(\theta\right)$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
xlabel('inventory', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')

% Time plot