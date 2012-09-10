function [O]=getchild(I,g,str)

O=[];
for i=1:length(str)
    
    switch g{i}
        case 'C'
            ind = strcmp({I.Children.Name},str{i});
        case 'A'
            ind = strcmp({I.Attributes.Name},str{i});
    end
    if(~any(ind))
        return
    end
    
    switch g{i}
        case 'C'
            I = I.Children(ind);
        case 'A'
            I = I.Attributes(ind);
    end
    
    
end
O = I;
