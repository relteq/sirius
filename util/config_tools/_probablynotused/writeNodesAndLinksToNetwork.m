function [] = writeNodesAndLinksToNetwork(allnodes,alllinks,outputfile)
% write list of nodes and links to a network file


scenario.ATTRIBUTE.schemaVersion='1.0.20';
scenario.network.ATTRIBUTE.id=-1;
scenario.network.ATTRIBUTE.name='none';
scenario.network.ATTRIBUTE.ml_control='false';
scenario.network.ATTRIBUTE.q_control='false';
scenario.network.ATTRIBUTE.dt= 0;
scenario.network.description = 'none';
scenario.network.position.point.ATTRIBUTE.lat = 0;
scenario.network.position.point.ATTRIBUTE.lng = 0;
scenario.network.LinkList = [];
scenario.settings.units = 'US';

%%% NodeList
for i=1:size(allnodes,1)
    n.ATTRIBUTE.type = 'F';
    n.ATTRIBUTE.id    = num2str(i);
    n.ATTRIBUTE.name  = ['node ' num2str(i)];
    n.description  = ['node ' num2str(i)];
    n.postmile = 0;
    
    n.outputs = [];
    n.inputs = [];
    
    if(~isempty(alllinks))
        myoutputs = find(alllinks(:,1)==i);
        if(~isempty(myoutputs))
            for j=1:length(myoutputs)
                n.outputs.output(j).ATTRIBUTE.link_id = num2str(myoutputs(j));
            end
        end
        
        myinputs = find(alllinks(:,2)==i);
        if(~isempty(myinputs))
            for j=1:length(myinputs)
                n.inputs.input(j).ATTRIBUTE.link_id = num2str(myinputs(j));
            end
        end
    end
    n.position.point.ATTRIBUTE.lng = allnodes(i,2);
    n.position.point.ATTRIBUTE.lat = allnodes(i,1);
    n.position.point.ATTRIBUTE.elevation = 0;
    scenario.network.NodeList.node(i) = n;
    clear n
end

%%% LinkList
for i=1:size(alllinks,1)
    l.ATTRIBUTE.name = ['Link ' num2str(i)];
    l.ATTRIBUTE.lanes = '1';
    l.ATTRIBUTE.length = '1';
    l.ATTRIBUTE.type = 'FW';
    l.ATTRIBUTE.id = num2str(i);
    l.begin.ATTRIBUTE.node_id = num2str(alllinks(i,1));
    l.end.ATTRIBUTE.node_id = num2str(alllinks(i,2));
    l.fd.ATTRIBUTE.densityCritical =  '1';
    l.fd.ATTRIBUTE.flowMax =  '1';
    l.fd.ATTRIBUTE.densityJam =  '1';
    l.dynamics.ATTRIBUTE.type = 'CTM';
    l.qmax =  '0';
    scenario.network.LinkList.link(i) = l;
    clear l
end

xml_write(outputfile, scenario);