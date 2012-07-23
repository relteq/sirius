function []=plot_csv(configfile,outprefix)

fnformat = sprintf('%s_%%s_0.txt',outprefix);

fprintf('Reading %s\n', configfile);
scenario = xml_read(configfile);

if(length(length(scenario.NetworkList.network))~=1)
    error('simplot does not work for scenarios with multiple networks')
end

dt = round(2*scenario.NetworkList.network(1).ATTRIBUTE.dt)/2;

%  temp
outdt = dt/3600;
clear dt

% density in veh/mile
density = load(sprintf(fnformat, 'density'));
for i=1:length(scenario.NetworkList.network(1).LinkList.link)
    lgth = scenario.NetworkList.network(1).LinkList.link(i).ATTRIBUTE.length;
    density(:,i) = density(:,i)/lgth;
end

% flow in veh/hr
flow = load(sprintf(fnformat, 'outflow'));
flow = flow/outdt;

% speed in mile/hr
speed = flow./density(1:(end - 1),:);

figure
plot(speed)
title('Speed in [length]/hr')

figure
plot(flow)
title('Flow in veh/hr')

figure
plot(density)
title('Density in veh/[length]')


figure
set(pcolor(speed), 'EdgeAlpha', 0);
colorbar;
title('Speed in [length]/hr')

figure
set(pcolor(flow), 'EdgeAlpha', 0);
colorbar;
title('Flow in veh/hr')

figure
set(pcolor(density), 'EdgeAlpha', 0);
colorbar;
title('Density in veh/[length]')

