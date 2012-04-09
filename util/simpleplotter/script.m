clear
close all

fnam = fullfile('..', '..', 'output_0.xml');
path = {fullfile('..', 'verification', '')};
addpath(path{:});
fprintf('Reading %s\n', fnam);
sout = readSiriusOutput(fnam);
rmpath(path{:});

disp('Processing data');
slanes = [sout.Links.lanes];
% veh/mile/lane
density = bsxfun(@rdivide, sout.density, slanes .* [sout.Links.length]);
% veh/hr/lane
flow = 3600 * bsxfun(@rdivide, sout.outflow, slanes);
% mile/hr
speed = min(flow ./ density(2:end, :), sout.free_flow_speed(2:end, :));

disp('Plotting');
fprops = {'DefaultSurfaceEdgeColor', 'none'};
figure(fprops{:});
pcolor(density);
colorbar;
title('Density, veh/mile/lane');

figure(fprops{:});
pcolor(flow);
colorbar;
title('Flow, veh/hr/lane');

figure(fprops{:});
pcolor(speed);
colorbar;
title('Speed, mph');
