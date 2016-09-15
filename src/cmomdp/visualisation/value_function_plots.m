

% SIR Value Function
h = trisurf(delaunay(x,y),x,y,z)
set(h,'LineStyle','none')
colormap jet

xlim([0 1])
ylim([0 1])

ylabel('$\pi\left(\nu\right)$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
xlabel('$\beta$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')

view([120 30])


% Robot Value Function

h = trisurf(delaunay(x,y),x,y,z)
set(h,'LineStyle','none')
colormap jet

xlim([-20 20])
ylim([0 50.0])

ylabel('$w_2$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
xlabel('location', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')

view([210 30])

% OE Value Function and OE Value Functino Deriv

h = trisurf(delaunay(x,y),x,y,z)
set(h,'LineStyle','none')
colormap jet

xlim([0 1000])
ylim([0 1.0])

ylabel('$\pi\left(\theta\right)$', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')
xlabel('inventory', 'interpreter', 'latex', 'FontSize',20, 'FontWeight', 'bold')

view([115 30])


