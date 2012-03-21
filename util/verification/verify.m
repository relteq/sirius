aurora_fnam = 'I80_aurora.csv';
sirius_fnam = 'output_%s_0.txt';
sirius_conf = fullfile('conf', 'I80_sirius.xml');

fprintf('Reading %s\n', aurora_fnam);
aout = readAuroraOutput(aurora_fnam, 10000);
sout = readSiriusOutput(sirius_fnam, sirius_conf);

linktype = 'freeway';

%link id
aid = [aout.Links.id];
sid = sout.Links.id;
if numel(aid) ~= numel(sid) || any(aid ~= sid), error('links differ'); end

%lanes
alanes = [aout.Links.lanes];
slanes = sout.Links.lanes;

%density [veh/mile/lane]
adens = bsxfun(@rdivide, aout.Density, alanes);
sdens = bsxfun(@rdivide, sout.density(1:(end - 1), :), slanes .* sout.Links.length);

%flow [veh/hr/lane]
aflow = bsxfun(@rdivide, aout.OutFlow, alanes);
sflow = 3600 / sout.dt * bsxfun(@rdivide, sout.outflow, slanes);

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
saveas(gcf, ['pcolor' fnsuff '.png']);

tend = sout.time(end - 1);
adensend = adens(end, :);
sdensend = sdens(end, :);
aflowend = aflow(end, :);
sflowend = sflow(end, :);
denserr = 100 * abs(sdensend - adensend) ./ max(adensend, sdensend);
denserr(adensend == 0 & sdensend == 0) = 0;
flowerr = 100 * abs(sflowend - aflowend) ./ max(aflowend, sflowend);
flowerr(aflowend == 0 & sflowend == 0) = 0;
graphtitles = {'Aurora', 'Sirius'};

figure('Position', [0, scrsz(2), 1024, 640]);
subplot(2, 2, 1);
plot([adensend; sdensend]');
title(sprintf('Density, veh/mile/lane, T = %d sec', tend));
legend(graphtitles{:});
xlim([1, size(adens, 2)]);
subplot(2, 2, 2);
plot(denserr);
title(sprintf('Density Error, %%, T = %d sec', tend));
xlim([1, size(denserr, 2)]);
ylim([0, min(100, ceil(max(denserr)))]);
subplot(2, 2, 3);
plot([aflow(end, :); sflow(end, :)]');
title(sprintf('Flow, veh/hr/lane, T = %d sec', tend));
legend(graphtitles{:});
xlim([1, size(aflow, 2)]);
subplot(2, 2, 4);
plot(flowerr);
title(sprintf('Flow Error, %%, T = %d sec', tend));
xlim([1, size(flowerr, 2)]);
ylim([0, min(100, ceil(max(flowerr)))]);
saveas(gcf, ['compare' fnsuff '.eps'], 'psc2');
