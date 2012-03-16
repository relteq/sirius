aurora_fnam = 'I80_aurora.csv';
sirius_fnam = 'output_%s_0.txt';
sirius_conf = fullfile('conf', 'I80_sirius.xml');

fprintf('Reading %s\n', aurora_fnam);
aout = readAuroraOutput(aurora_fnam);
sout = readSiriusOutput(sirius_fnam, sirius_conf);

%lanes
alanes = [aout.Links.lanes];
slanes = sout.Links.lanes;

%density [veh/mile/lane]
adens = bsxfun(@rdivide, aout.Density, alanes);
sdens = bsxfun(@rdivide, sout.density(1:(end - 1), :), slanes .* sout.Links.length);

%flow [veh/hr/lane]
aflow = bsxfun(@rdivide, aout.OutFlow, alanes);
sflow = 3600 / sout.dt * bsxfun(@rdivide, sout.outflow, slanes);

scrsz = get(0,'ScreenSize');
adjust = @(h) set(h, 'EdgeColor', 'none');

figure('Position', [0, scrsz(2), 1280, 1024]);
subplot(2, 2, 1);
adjust(pcolor(adens));
title('Aurora Density, veh/mile/lane');
colorbar;
subplot(2, 2, 2);
adjust(pcolor(sdens));
title('Sirius Density, veh/mile/lane');
colorbar;
subplot(2, 2, 3);
adjust(pcolor(aflow));
title('Aurora Flow, veh/hr/lane');
colorbar;
subplot(2, 2, 4);
adjust(pcolor(sflow));
title('Sirius Flow, veh/hr/lane');
colorbar;
saveas(gcf, 'pcolor.png');

graphtitles = {'Aurora', 'Sirius'};
figure;
subplot(2, 1, 1);
plot([adens(end, :); sdens(end, :)]');
title('Density, veh/mile/lane, T = 1 day');
legend(graphtitles{:});
xlim([1, size(adens, 2)]);

subplot(2, 1, 2);
plot([aflow(end, :); sflow(end, :)]');
title('Flow, veh/hr/lane, T = 1 day');
legend(graphtitles{:});
xlim([1, size(aflow, 2)]);
saveas(gcf, 'compare.eps', 'psc2');
