function data = readSiriusOutput(ofnam_templ, conf_path)
fprintf('Reading %s\n', conf_path);
scenario = xml_read(conf_path);

disp('Extracting link parameters');
if numel(scenario.NetworkList.network) > 1, error('Multiple networks'); end
network = scenario.NetworkList.network(1);
data = struct('Links', struct(), 'dt', network.ATTRIBUTE.dt);
links = network.LinkList.link;
for nam = {'lanes', 'length', 'id'}
	data.Links.(char(nam)) = zeros(1, numel(links));
	for iii = 1:numel(links)
		data.Links.(char(nam))(iii) = links(iii).ATTRIBUTE.(char(nam));
	end
end

for nam = {'density', 'inflow', 'outflow', 'time'}
	fnam = sprintf(ofnam_templ, char(nam));
	fprintf('Loading %s\n', fnam);
	data.(char(nam)) = load(fnam);
end
