function [pos]=position_along_polyline(p,polyline)
% returns the poisition of a point "p" percentage down the length of the
% polyline

pos = nan;

allpoints = polyline.points;
numpoints = size(allpoints,1);

% compute length of each segment
for i=1:numpoints-1
    segmentlength(i) = sqrt(sum((allpoints(i+1,:)-allpoints(i,:)).^2));
end
cumlength = [0 cumsum(segmentlength)]/sum(segmentlength);

% find which segment p belongs to
mysegment = find(p>cumlength(1:end-1) & p<cumlength(2:end));

% relative position of p within the segment,
% absolute position of p
if(~isempty(mysegment))
    relpos = (p-cumlength(mysegment))/(cumlength(mysegment+1)-cumlength(mysegment));
    pos = allpoints(mysegment,:) + relpos * (allpoints(mysegment+1,:)-allpoints(mysegment,:));
end
