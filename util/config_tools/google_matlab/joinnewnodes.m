function []=joinnewnodes(onelink,newnodesmod,outfile)

% extract indormation from onelink
Ponelink = decodepolyline(xmlreadsection(onelink,'AuroraRNM>DirectionsCache'));
clear onelink

% extract indormation from newnodes
N_newnodes = xmlreadsection(newnodesmod,'AuroraRNM>network>NodeList');
N_newnodes(~strcmp({N_newnodes.Name},'node'))=[];
Pnewnodes = NaN(length(N_newnodes),2);
for i=1:length(N_newnodes)
    N=getchild(N_newnodes(i),{'C','C'},{'position','point'});
    ind = strcmp({N.Attributes.Name},'x');
    x = str2num(N.Attributes(ind).Value);
    ind = strcmp({N.Attributes.Name},'y');
    y = str2num(N.Attributes(ind).Value);
    Pnewnodes(i,:) = [-y x];
end
clear N ind x y N_newnodes newnodesmod

% write to xml
allnodes = [Ponelink.from;flipud(Pnewnodes);Ponelink.to];
n = size(allnodes,1);
alllinks = [ (1:n-1)' (2:n)'];
writeXMLnetwork(allnodes,alllinks,outfile)

