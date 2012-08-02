function [disttoP,segmentindex]=point2polydistance(P,d)

if(isempty(P))
    disttoP = nan;
    segmentindex = nan;
    return
end
    
pointvector = ones(size(P,1),1)*d-P;
pointdistance = euclidnorm(pointvector);
segment = P(2:end,:)-P(1:end-1,:);
segmentlength = euclidnorm(segment);

verticaldistance = NaN*segmentlength;
for i=1:size(segment,1)
    verticaldistance(i) = abs(det([pointvector(i,:);segment(i,:)]))/segmentlength(i);
end

keep = find(verticaldistance<=min(pointdistance));

disttosegment = Inf*segmentlength;
iind = sum(segment(keep,:).*pointvector(keep,:),2)<0;
ind = keep(iind);
disttosegment(ind) = pointdistance(ind);
keep(iind) = [];

if(~isempty(keep))
    H = sqrt(segmentlength(keep).^2+pointdistance(keep+1).^2);
    iind = pointdistance(keep)<H;
    ind = keep(iind);
    disttosegment(ind) = verticaldistance(ind);
    keep(iind) = [];
end

if(~isempty(keep))
    disttosegment(keep) = pointdistance(keep+1);
end

[disttoP,segmentindex] = min(disttosegment);

