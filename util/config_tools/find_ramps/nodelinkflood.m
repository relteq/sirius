function [P]=nodelinkflood(scenario,offset,step,outputfile)
% load string of nodes and links from inputfile
% generate string of nodes on either side of the string
% generate links connecting all node combinations
% export

global gps2feet

% clean out the directions cache
disp('Mapping links to directions')
[~,dirused] = makeLink2DirectionsMap(scenario);

% decode directions
disp('Decoding directions')
polyline=decodepolyline(scenario.network.DirectionsCache.DirectionsCacheEntry(dirused));

% Plot polyline %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
figure
plot(polyline.points(1,1),polyline.points(1,2),'r.','MarkerSize',20),
hold on
for i=1:size(polyline.points,1)-1
    a1 = polyline.points(i,:);
    a2 = polyline.points(i+1,:);
    plot([a1(1) a2(1)],[a1(2) a2(2)],'o')
    plot([a1(1) a2(1)],[a1(2) a2(2)])
end

% Compute side points %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
P1 = [];
P2 = [];

% first
a1 = polyline.points(1,:);
a2 = polyline.points(2,:);
theta = atan2(a1(1)-a2(1),a2(2)-a1(2));
u = [cos(theta) sin(theta)]*offset/gps2feet;
aa = a1+u;
P1 = [P1;aa];
plot([a1(1),aa(1)],[a1(2) aa(2)])
plot(aa(1),aa(2),'g.','MarkerSize',14)
aa = a1-u;
P2 = [P2;aa];
plot([a1(1),aa(1)],[a1(2) aa(2)])
plot(aa(1),aa(2),'b.','MarkerSize',14)

% middle
for i=1:size(polyline.points,1)-2
    a1 = polyline.points(i,:);
    a2 = polyline.points(i+1,:);
    a3 = polyline.points(i+2,:);
    d12 = eucliddist(a1,a2);
    d23 = eucliddist(a2,a3);
    u1 = (a2-a1)/d12;
    u2 = (a3-a2)/d23;
    theta = atan2(-u1(1)-u2(1),u1(2)+u2(2));
    u = [cos(theta) sin(theta)]*offset/gps2feet;
    aa = a2+u;
    P1 = [P1;aa];
    plot([a2(1),aa(1)],[a2(2) aa(2)])
    plot(aa(1),aa(2),'g.','MarkerSize',14)
    aa = a1-u;
    P2 = [P2;aa];
    plot([a1(1),aa(1)],[a1(2) aa(2)])
    plot(aa(1),aa(2),'b.','MarkerSize',14)
end

% last
a2 = polyline.points(end-1,:);
a3 = polyline.points(end,:);
theta = atan2(a2(1)-a3(1),a3(2)-a2(2));
u = [cos(theta) sin(theta)]*offset/gps2feet;
aa = a3+u;
P1 = [P1;aa];
plot([a3(1),aa(1)],[a3(2) aa(2)])
plot(aa(1),aa(2),'g.','MarkerSize',14)
aa = a3-u;
P2 = [P2;aa];
plot([a3(1),aa(1)],[a3(2) aa(2)])
plot(aa(1),aa(2),'b.','MarkerSize',14)

axis equal

% Split side links %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
newP1 = P1(1,:);
for i=1:size(P1,1)-1
    a1 = P1(i,:);
    a2 = P1(i+1,:);
    dfeet = eucliddist(a1,a2)*gps2feet;
    numintervals = ceil(dfeet/step);
    intervalsize = dfeet/numintervals;
    a = a1;
    for j=1:numintervals
        a = a + (a2-a1)*intervalsize/dfeet;
        newP1 = [newP1;a];
    end
end

newP2 = P2(1,:);
for i=1:size(P2,1)-1
    a1 = P2(i,:);
    a2 = P2(i+1,:);
    dfeet = eucliddist(a1,a2)*gps2feet;
    numintervals = ceil(dfeet/step);
    intervalsize = dfeet/numintervals;
    a = a1;
    for j=1:numintervals
        a = a + (a2-a1)*intervalsize/dfeet;
        newP2 = [newP2;a];
    end
end

for i=1:size(newP1,1)
    plot(newP1(i,1),newP1(i,2),'k.','MarkerSize',14)  
end

for i=1:size(newP2,1)
    plot(newP2(i,1),newP2(i,2),'r.','MarkerSize',14)
end

% Generate all links %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
allnodes = [polyline.points(1,:);newP1;newP2;polyline.points(end,:)];
n = size(allnodes,1)-2;
alllinks = [ones(n,1) (2:n+1)' ; (2:n+1)' (n+2)*ones(n,1)];
          
% Write to XML %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
writeNodesAndLinksToNetwork(allnodes,alllinks,outputfile)



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
