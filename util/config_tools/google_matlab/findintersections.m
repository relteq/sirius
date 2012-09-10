function [uniquenewintersection]=findintersections(P,filename,casedir,doplot)

storefile = [casedir 'store'];
if(~exist([storefile '.mat'],'file'))
    D = decodepolyline(xmlreadsection(filename,'AuroraRNM>DirectionsCache'));
    save([casedir 'store'])
else    
    load([casedir 'store'])
end
clear storefile

if(doplot)
    [ppt,op]=openppt('plots',0);
end

eps = 4e-5;
Dist = cell(length(D),1);
newintersection=[];
intersectionindex = NaN(length(D),1);
for i=1:length(D)
    i
    clear Pclosestsegment
    for dd=1:size(D(i).points,1)
        d = D(i).points(dd,:);
        [Dist{i}(dd),Pclosestsegment(dd)] = point2polydistance(P.points,d);
    end
    
    dbegin = eucliddist(P.from,D(i).from);
    dend = eucliddist(P.to,D(i).to);
    
    if(dbegin<dend)
        index = find(Dist{i}>eps,1,'first')-1;
    else
        index = find(Dist{i}>eps,1,'last')+1;
    end
    
    if(index>1)
        intersectionindex(i) = index;
    end
    
    if(~isnan(intersectionindex(i)))
        newintersection = [newintersection;D(i).points(intersectionindex(i),:)];
    end
    
    if(doplot)
        close
        f = figure('Position',[147    39   659   601],'Visible','off');
        subplot(211)
        plotpolyline(f,D(i).points,'b',2,'o','k');
        text(D(i).points(:,1),D(i).points(:,2),num2str((1:size(D(i).points,1))'))
        plotpolyline(f,P.points,'r',1,'.','g');
        if(~isnan(intersectionindex(i)))
            plot(D(i).points(intersectionindex(i),1),D(i).points(intersectionindex(i),2),'r.','MarkerSize',16)
        end
        set(gca,'ydir','reverse')
        set(gca,'xdir','reverse')
        subplot(212)
        plot(Dist{i})
        vline(intersectionindex(i))
        grid
        addslide(op,num2str(i),f)
    end
    
    
end
if(doplot)
    closeppt(ppt,op)
    close all
end

uniquenewintersection = unique(newintersection,'rows');

plotpolyline([],P.points,'r',1,'.','g');
plot(uniquenewintersection(:,1),uniquenewintersection(:,2),'co','MarkerSize',16)
