clear
close all

filefolder = [pwd '\..\config\'];
configfile = [filefolder 'test_event.xml'];
outputprefix = 'out_';

scenario = xml_read(configfile);

if(length(length(scenario.NetworkList.network))~=1)
    error('simplot does not work for scenarios with multiple networks')
end

dt = round(2*scenario.NetworkList.network(1).ATTRIBUTE.dt)/2;

%  temp
outdt = dt;

% density in veh/mile
density = load([filefolder outputprefix 'density.txt']);
for i=1:length(scenario.NetworkList.network(1).LinkList.link)
    lgth = scenario.NetworkList.network(1).LinkList.link(i).ATTRIBUTE.length;
    density(:,i) = density(:,i)/lgth;
end

% flow in veh/hr
flow = load([filefolder outputprefix 'outflow.txt']);
flow = flow/outdt;

%  speed in mile/hr
speed = flow./density;

figure
h=pcolor(density);
set(h,'EdgeAlpha',0)

figure
h=pcolor(flow);
set(h,'EdgeAlpha',0)

figure
h=pcolor(speed);
set(h,'EdgeAlpha',0)

