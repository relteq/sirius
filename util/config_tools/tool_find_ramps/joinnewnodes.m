function []=joinnewnodes(onelink,newnodesmod,outfile)

% extract indormation from onelink
Ponelink = decodepolyline(xmlreadsection(onelink,'scenario>network>DirectionsCache'));
clear onelink

% extract indormation from newnodes
N_newnodes = xmlreadsection(newnodesmod,'scenario>network>NodeList');
N_newnodes(~strcmp({N_newnodes.Name},'node'))=[];
Pnewnodes = NaN(length(N_newnodes),2);
for i=1:length(N_newnodes)
    N=getchild(N_newnodes(i),{'C','C'},{'position','point'});
    ind = strcmp({N.Attributes.Name},'lat');
    lat = str2double(N.Attributes(ind).Value);
    ind = strcmp({N.Attributes.Name},'lng');
    lng = str2double(N.Attributes(ind).Value);
    Pnewnodes(i,:) = [lat lng];
end
clear N ind lng lat N_newnodes newnodesmod

% write to xml
allnodes = [Ponelink.from;flipud(Pnewnodes);Ponelink.to];
n = size(allnodes,1);
alllinks = [ (1:n-1)' (2:n)'];
writeNodesAndLinksToNetwork(allnodes,alllinks,outfile)

