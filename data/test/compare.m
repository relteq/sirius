clear
close all

thisfolder = fileparts(mfilename('fullpath'));
configfolder = [thisfolder '\..\config\'];
configfile = [configfolder 'scenario_2009_02_12.xml'];
out_aurora2 = [configfolder 'out_2.csv'];
out_auroralite = [configfolder 'outlite'];

%  run aurora2
system(['java -jar C:\Users\gomes\workspace\auroralite\data\test\aurora2.jar ' configfile ' ' out_aurora2]);

% load aurora2 output
S = topl_scenario;
S.load(configfile); , disp('loaded config')
save config
load config

% S.settings.timeInitial = 16.5;
% S.settings.timeMax = 18.5;
% S.settings.outdt = 5/3600;

S.readAuroraCSV(out_aurora2); , disp('loaded data')
save csv
load csv

onrampind = [S.network.LinkList.issource];


% plot aurora2
aurora2_veh = S.output.density;
aurora2_inflow = S.output.inflow;
aurora2_outflow = S.output.outflow;

% write aurora2
dlmwrite('..\config\aurora2_density.txt',aurora2_veh,'delimiter','\t')
dlmwrite('..\config\aurora2_inflow.txt',aurora2_inflow,'delimiter','\t')
dlmwrite('..\config\aurora2_outflow.txt',aurora2_outflow,'delimiter','\t')

% % run auroralite
% system(['java -jar C:\Users\gomes\workspace\auroralite\data\test\auroralite.jar ' configfile ' ' out_auroralite]);

% plot auroralite
auroralite_veh = load([out_auroralite '_density.txt']);
auroralite_inflow = load([out_auroralite '_inflow.txt']);
auroralite_outflow = load([out_auroralite '_outflow.txt']);

%  crop to same size
L = min([size(aurora2_inflow,1) size(auroralite_inflow,1)]);
time = [0:size(aurora2_veh,1)-1];
time = time(1:L);
aurora2_veh = aurora2_veh(1:L,:);
aurora2_inflow = aurora2_inflow(1:L,:);
aurora2_outflow = aurora2_outflow(1:L,:);
auroralite_veh = auroralite_veh(1:L,:);
auroralite_inflow = auroralite_inflow(1:L,:);
auroralite_outflow = auroralite_outflow(1:L,:);

% difference
delta_veh=aurora2_veh-auroralite_veh;
delta_inflow = aurora2_inflow-auroralite_inflow;
delta_outflow = aurora2_outflow-auroralite_outflow;

figure('Position',[140 255 560 420])
h=pcolor(aurora2_veh);
set(h,'EdgeAlpha',0)
clear h S

figure('Position',[717 256 560 420])
h=pcolor(auroralite_veh);
set(h,'EdgeAlpha',0)

figure('Position',[458 30 560 645])
subplot(311)
plot(aurora2_veh-auroralite_veh)
subplot(312)
plot(aurora2_inflow-auroralite_inflow)
subplot(313)
plot(aurora2_outflow-auroralite_outflow)

figure
subplot(211)
plot(aurora2_outflow(:,onrampind))
subplot(212)
plot(auroralite_outflow(:,onrampind))


sum(sum(abs(aurora2_veh)))-sum(sum(abs(auroralite_veh)))

%  write to excel 
s = 150;
e = min([s+255 L]);
time = time(s:e);
aurora2_veh = aurora2_veh(s:e,:);
aurora2_inflow = aurora2_inflow(s:e,:);
aurora2_outflow = aurora2_outflow(s:e,:);
auroralite_veh = auroralite_veh(s:e,:);
auroralite_inflow = auroralite_inflow(s:e,:);
auroralite_outflow = auroralite_outflow(s:e,:);
delta_veh = delta_veh(s:e,:);
delta_inflow = delta_inflow(s:e,:);
delta_outflow = delta_outflow(s:e,:);

filename = 'compare.xlsx';
system(['copy empty.xlsx ' filename])
xlswrite([time;aurora2_veh'],{},'',filename,'aurora2_veh');
xlswrite([time;auroralite_veh'],{},'',filename,'auroralite_veh');
xlswrite([time;aurora2_inflow'],{},'',filename,'aurora2_inflow');
xlswrite([time;auroralite_inflow'],{},'',filename,'auroralite_inflow');
xlswrite([time;aurora2_outflow'],{},'',filename,'aurora2_outflow');
xlswrite([time;auroralite_outflow'],{},'',filename,'auroralite_outflow');

filename = 'comparedelta.xlsx';
system(['copy emptydelta.xlsx ' filename])
xlswrite([time;abs(delta_veh')],{},'',filename,'delta_veh');
xlswrite([time;abs(delta_inflow')],{},'',filename,'delta_inflow');
xlswrite([time;abs(delta_outflow')],{},'',filename,'delta_outflow');

disp('done')
