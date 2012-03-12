clear
close all

configfile = fullfile('..', 'config', 'test_event.xml');
fnformat = fullfile('..', '..', 'output_%s_0.txt');

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

disp('Plotting density');
figure;
h=pcolor(density);
set(h,'EdgeAlpha',0)

disp('Plotting flow');
figure;
h=pcolor(flow);
set(h,'EdgeAlpha',0)

disp('Plotting speed');
figure;
h=pcolor(speed);
set(h,'EdgeAlpha',0)

