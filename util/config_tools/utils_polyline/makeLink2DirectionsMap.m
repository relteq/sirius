function [link2dir,used]=makeLink2DirectionsMap(scenario)

numDirCache = length(scenario.network.DirectionsCache.DirectionsCacheEntry);

% extract node positions
for i=1:length(scenario.network.NodeList.node)   
    NodeLatLng(i,1)=scenario.network.NodeList.node(i).position.point.ATTRIBUTE.lat;
    NodeLatLng(i,2)=scenario.network.NodeList.node(i).position.point.ATTRIBUTE.lng;
    node_id(i)=scenario.network.NodeList.node(i).ATTRIBUTE.id;
end

% extract link begin and end nodes
for i=1:length(scenario.network.LinkList.link)   
    LinkNodeId(i,1) = scenario.network.LinkList.link(i).begin.ATTRIBUTE.node_id;
    LinkNodeId(i,2) = scenario.network.LinkList.link(i).xEnd.ATTRIBUTE.node_id;
end

% Extract directions cache from/to positions
for i=1:numDirCache
    x=scenario.network.DirectionsCache.DirectionsCacheEntry(i);
    DirFromLatLng(i,1) = x.From.ALatLng.ATTRIBUTE.lat;
    DirFromLatLng(i,2) = x.From.ALatLng.ATTRIBUTE.lng;
    DirToLatLng(i,1) = x.To.ALatLng.ATTRIBUTE.lat;
    DirToLatLng(i,2) = x.To.ALatLng.ATTRIBUTE.lng;
end

% keep direction if it goes from one node to another
used = false(1,numDirCache);
for i=1:length(scenario.network.LinkList.link)
    fromlatlng = NodeLatLng(LinkNodeId(i,1)==node_id,:);
    tolatlng = NodeLatLng(LinkNodeId(i,2)==node_id,:);
    ind = find(fromlatlng(1)==DirFromLatLng(:,1) & fromlatlng(2)==DirFromLatLng(:,2) & ...
          tolatlng(1)==DirToLatLng(:,1) & tolatlng(2)==DirToLatLng(:,2));
    if(length(ind)==1)
        link2dir(i) = ind;
        used(ind) = true;
    else
        link2dir(i) = nan;        
    end
end
