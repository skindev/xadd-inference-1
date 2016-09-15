

horizon_mat = reshape(horizon, 15, size(horizon,1)/15);
node_mat = reshape(nodes, 15, size(horizon,1)/15);
branches_mat = reshape(branches, 15, size(horizon,1)/15);
opt_mat = reshape(opt_time, 15, size(horizon,1)/15);
sdp_mat = reshape(sdp_time, 15, size(horizon,1)/15);

mean_sdp = mean(sdp_mat,2);
mean_opt = mean(opt_mat,2);

mean_node = mean(node_mat,2);
mean_branches = mean(branches_mat,2);

% Time Plot

plot(mean(horizon_mat, 2), mean_sdp, 'LineWidth', 2)
hold on
plot(mean(horizon_mat, 2), mean_opt, 'LineWidth', 2)
set(gca, 'Box', 'off');
hold off
legend({'SDP','Optimizer'},'FontSize',12, 'Location','northwest');

xlabel('horizon', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
ylabel('time (s)', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')


% Space Plot

plot(mean(horizon_mat, 2), mean_node, 'LineWidth', 2)
hold on
plot(mean(horizon_mat, 2), mean_branches, 'LineWidth', 2)
set(gca, 'Box', 'off');
hold off

legend({'Nodes','Branches'},'FontSize',12, 'Location','northwest');

xlabel('horizon', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
ylabel('count', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')


