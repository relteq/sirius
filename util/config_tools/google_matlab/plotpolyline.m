function [fig]=plotpolyline(fig,P,linecolor,linewidth,marker,markercolor)

if(isempty(fig))
    fig = figure;
end

set(0,'CurrentFigure',fig)
if(~iscell(P))
    P = {P};
end

for i=1:length(P)
    X = P{i};
    for j=1:size(X,1)-1
        hold on
        if(strcmp(linecolor,'rand'))
            c = rand(1,3);
        else
            c = linecolor;
        end
        plot([X(j,1) X(j+1,1)],[X(j,2) X(j+1,2)],'Color',c,'LineWidth',linewidth)
        plot([X(j,1) X(j+1,1)],[X(j,2) X(j+1,2)],[markercolor marker],'MarkerSize',10)
    end
end