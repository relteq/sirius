function [D] = readAuroraOutput(inputfile, npoints, scope)
if nargin < 2, npoints = Inf; end
if nargin < 3, scope = struct('timeon', true, 'time', [0 24]); end
    
[pathname,inputfile] = fileparts(inputfile);
name = [fullfile(pathname,inputfile) '.csv'];
fid = fopen(name);
if -1 == fid, error('ARG:User', 'File %s does not exist', name); end

D.dt = 1;
D.date = datevec(fgetl(fid));
linkinfo = struct;

delim = '\s*,\s*';
while true
	tline = fgetl(fid);
	if ~ischar(tline), break, end
	if isempty(tline), continue; end
	row = regexp(tline, delim, 'split');
	name = strrep(row{1}, ' ', '');
	switch row{1}
		case 'Vehicle Type'
			vt = struct('name', {}, 'weight', {});
			while true
				tline = fgetl(fid);
				if isempty(tline), break; end
				row = regexp(tline, delim, 'split');
				vt(end + 1).name = row{1};
				vt(end).weight = str2double(row{2});
			end
		case 'Entry Format'
			tline = fgetl(fid);
			row = regexp(tline, delim, 'split');
			entries = strrep(regexp(row{1}, ';', 'split'), '_Value', '');
			entries = strrep(entries, '_', '');
			entries = strrep(entries, '-', '');
			fdformat = regexp(row{3}, ':', 'split');
			D.vehicletypes = regexp(row{2}, ':', 'split');

			% order weights as vehicletypes
			D.vehicletypeweight = zeros(size(D.vehicletypes));
			for i = 1:numel(D.vehicletypes)
				D.vehicletypeweight(i) = vt(strcmp(D.vehicletypes(i), {vt.name})).weight;
			end
			clear vt i
		case 'Sampling Period'
			while true
				tline = fgetl(fid);
				if isempty(tline), break; end
				row = regexp(tline, delim, 'split');
				if strcmp('seconds', row{2}),
					D.dt = round(str2double(row{1}) * 100) / 100;
				end
			end
		case 'Description'
			D.description = {};
			while true
				tline = fgetl(fid);
				if isempty(tline), break; end
				row = regexp(tline, delim, 'split');
				D.description{end + 1} = row{1};
			end
		case 'Routes'
			routes = struct('name', {}, 'links', {});
			while 1
				tline = fgetl(fid);
				if isempty(tline), break; end
				row = regexp(tline, delim, 'split');
				routes(end + 1).name = row{1};
				routes(end).links = str2double(row(2:end));
			end
		case {'Link Length', 'Link Width'}
			linkinfo.(name) = str2double(row(2:end));
		case {'Link ID', 'Link Name'}
			linkinfo.(name) = row(2:end);
		case 'Link Type'
			row = row(2:end);
			linktype = NaN(size(row));
			linktype(strcmp('Freeway', row)) = 1;
			linktype(strcmp('On-Ramp', row)) = 2;
			linktype(strcmp('Off-Ramp', row)) = 3;
			linktype(strcmp('HOV', row)) = 4;
			linkinfo.(name) = linktype;
			clear linktype;
		case 'Source'
			linkinfo.(name) = strcmp('Yes', row(2:end));
		case 'Time Step'
			data = textscan(fid, '%f', 'whitespace', ',;:\n');
			nlinks = numel(linkinfo.LinkID);
			recordsize = 3 * numel(D.vehicletypes) + numel(fdformat) + numel(entries) - 4;
			data = reshape(data{1}, nlinks * recordsize + 1, []);
			time = D.dt / 3600 * data(1, :);
			if scope.timeon,
				ind = time >= scope.time(1) & time <= scope.time(2);
				time = time(ind)';
				data = data(2:end, ind);
			else
				time = time';
				data = data(2:end, :);
			end
			if (npoints > 0 && numel(time) > npoints)
				ind = false(numel(time), 1);
				ind(1:ceil(numel(time) / npoints):end) = true;
				time = time(ind);
				data = data(:, ind);
			end
			D.time = time;
			%(data, link, time) -> (time, link, data)
			data = permute(reshape(data, [recordsize, nlinks, numel(D.time)]), [3 2 1]);
			ind = 0;
			for iii = 1:numel(entries)
				entry = entries{iii};
				switch entry
					case {'Density', 'InFlow', 'OutFlow'}
						delta = numel(D.vehicletypes);
						D.(entry) = data(:, :, ind + (1:delta));
						ind = ind + delta;
					case 'FD'
						for key = fdformat
							ind = ind + 1;
							D.(entry) = data(:, :, ind);
						end
					otherwise
						ind = ind + 1;
						D.(entry) = data(:, :, ind);
				end
			end
			break;
		otherwise
			warning('Unknown title %s', row{1});
	end
end
fclose(fid);

D.Links = struct('id', linkinfo.LinkID, ...
	'name', linkinfo.LinkName, 'type', num2cell(linkinfo.LinkType), ...
	'length', num2cell(linkinfo.LinkLength), ...
	'lanes', num2cell(linkinfo.LinkWidth), ...
	'issource', num2cell(linkinfo.Source));
