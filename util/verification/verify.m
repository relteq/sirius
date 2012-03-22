aurora_fnam = 'I80_aurora.csv';
sirius_fnam = 'output_%s_0.txt';
sirius_conf = fullfile('conf', 'I80_sirius.xml');

fprintf('Reading %s\n', aurora_fnam);
aout = readAuroraOutput(aurora_fnam);
sout = readSiriusOutput(sirius_fnam, sirius_conf);

linktype = 'freeway';

%link id
aid = [aout.Links.id];
sid = sout.Links.id;
if numel(aid) ~= numel(sid) || any(aid ~= sid), error('links differ'); end
clear aid sid;

sizedens = min(size(aout.Density, 1), size(sout.density, 1));
sizeflow = min(size(aout.OutFlow, 1), size(sout.outflow, 1));

%lanes
alanes = [aout.Links.lanes];
slanes = sout.Links.lanes;

%density [veh/mile/lane]
adens = bsxfun(@rdivide, aout.Density(1:sizedens, :), alanes);
sdens = bsxfun(@rdivide, sout.density(1:sizedens, :), slanes .* sout.Links.length);

%flow [veh/hr/lane]
aflow = bsxfun(@rdivide, aout.OutFlow(1:sizeflow, :), alanes);
sflow = 3600 / sout.dt * bsxfun(@rdivide, sout.outflow(1:sizeflow, :), slanes);

fnsuff = '';
ind = strcmp(linktype, sout.Links.type);
if any(ind)
	adens = adens(:, ind);
	sdens = sdens(:, ind);
	aflow = aflow(:, ind);
	sflow = sflow(:, ind);
	fnsuff = ['-' linktype];
elseif ~isempty(linktype)
	warning('no %s links', linktype);
end
densdiff = abs(sdens - adens);
flowdiff = abs(sflow - aflow);
denserr = 100 * densdiff ./ adens;
flowerr = 100 * flowdiff ./ aflow;

scrsz = get(0,'ScreenSize');

figure('Position', [0, scrsz(2), 1000, 800], 'DefaultSurfaceEdgeColor', 'none');
subplot(2, 3, 1);
pcolor(adens);
title('Aurora Density, veh/mile/lane');
colorbar;
subplot(2, 3, 2);
pcolor(sdens);
title('Sirius Density, veh/mile/lane');
colorbar;
subplot(2, 3, 3);
pcolor(densdiff);
title('Absolute Density Error');
colorbar;
subplot(2, 3, 4);
pcolor(aflow);
title('Aurora Flow, veh/hr/lane');
colorbar;
subplot(2, 3, 5);
pcolor(sflow);
title('Sirius Flow, veh/hr/lane');
colorbar;
subplot(2, 3, 6);
pcolor(flowdiff);
title('Absolute Flow Error');
colorbar;
saveas(gcf, ['pcolor' fnsuff '.png']);

figure('Position', [0, scrsz(2), 720, 480]);
subplot(2, 2, 1);
plot(mean(denserr, 2));
xlim([0, size(denserr, 1)]);
title('Mean Density Error, %');
subplot(2, 2, 2);
plot(sqrt(var(denserr, 0, 2)));
xlim([0, size(denserr, 1)]);
title('Density Error Spread');
subplot(2, 2, 3);
plot(mean(flowerr, 2));
xlim([0, size(flowerr, 1)]);
title('Mean Flow Error, %');
subplot(2, 2, 4);
plot(sqrt(var(denserr, 0, 2)));
xlim([0, size(flowerr, 1)]);
title('Flow Error Spread');
saveas(gcf, ['errorstat' fnsuff '.eps'], 'psc2');

graphtitles = {'Aurora', 'Sirius'};

figure('Position', [0, scrsz(2), 1024, 640]);
subplot(2, 2, 1);
plot([adens(end, :); sdens(end, :)]');
title(sprintf('Density, veh/mile/lane, T = %d sec', sout.time(sizedens)));
legend(graphtitles{:});
xlim([1, size(adens, 2)]);
subplot(2, 2, 2);
plot(denserr(end, :));
title(sprintf('Density Error, %%, T = %d sec', sout.time(sizedens)));
xlim([1, size(denserr, 2)]);
ylimit = ylim; ylim([0, ylimit(2)]);
subplot(2, 2, 3);
plot([aflow(end, :); sflow(end, :)]');
title(sprintf('Flow, veh/hr/lane, T = %d sec', sout.time(sizeflow)));
legend(graphtitles{:});
xlim([1, size(aflow, 2)]);
subplot(2, 2, 4);
plot(flowerr(end, :));
title(sprintf('Flow Error, %%, T = %d sec', sout.time(sizeflow)));
xlim([1, size(flowerr, 2)]);
ylimit = ylim; ylim([0, ylimit(2)]);
saveas(gcf, ['compare' fnsuff '.eps'], 'psc2');
