function handles = traffic_plot(configfile, fnformat)
% TRAFFIC_PLOT plot the simulator output
% TRAFFIC_PLOT(config_file, filename_format)

if nargin < 1, configfile = '../config/test_event.xml'; end
if nargin < 2, fnformat = '../../output_%s_0.txt'; end

fprintf('Reading %s\n', configfile);
scenario = xml_read(configfile);

if(length(length(scenario.NetworkList.network))~=1)
    error('simplot does not work for scenarios with multiple networks')
end

dt = round(2*scenario.NetworkList.network(1).ATTRIBUTE.dt)/2;

%  temp
outdt = dt;

disp('Normalizing density');
% density in veh/mile
density = load(sprintf(fnformat, 'density'));
for i=1:length(scenario.NetworkList.network(1).LinkList.link)
    lgth = scenario.NetworkList.network(1).LinkList.link(i).ATTRIBUTE.length;
    density(:,i) = density(:,i)/lgth;
end

disp('Normalizing flow');
% flow in veh/hr
flow = load(sprintf(fnformat, 'outflow'));
flow = flow/outdt;

disp('Computing speed');
%  speed in mile/hr
speed = flow./density(1:(end - 1),:);

handles = zeros(1, 3);

disp('Plotting density');
handles(1) = figure;
h=pcolor(density);
set(h,'EdgeAlpha',0)

disp('Plotting flow');
handles(2) = figure;
h=pcolor(flow);
set(h,'EdgeAlpha',0)

disp('Plotting speed');
handles(3) = figure;
h=pcolor(speed);
set(h,'EdgeAlpha',0)

