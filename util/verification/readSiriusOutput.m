function data = readSiriusOutput(fnam)
data = struct;
javapath = {fullfile('..', '..', 'sirius.jar')};
javaaddpath(javapath{:});
import com.relteq.sirius.simulator.*;
out = OutputReader.Read(fnam);
links = out.getLinks().toArray();
nlinks = numel(links);
data.Links = struct('id', cell(1, nlinks), 'length', 0, 'lanes', 0, 'type', '');
for iii = 1:nlinks
	data.Links(iii).id = char(links(iii).getId());
	data.Links(iii).length = double(links(iii).getLength());
	data.Links(iii).lanes = double(links(iii).getLanes());
	data.Links(iii).type = char(links(iii).getType());
end

java2m = @(arr) cell2mat(arr.toArray().cell);
data.time = java2m(out.t);

nvehtypes = out.scenario.getNumVehicleTypes();
if nvehtypes <= 0, nvehtypes = 1; end
data.density = permute(reshape(java2m(out.d), nvehtypes, nlinks, []), [3 2 1]);
data.outflow = permute(reshape(java2m(out.f), nvehtypes, nlinks, []), [3 2 1]);

clear out;
javarmpath(javapath{:});