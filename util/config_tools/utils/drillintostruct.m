function X = drillintostruct(A,CorA,names)

if(isempty(names))
    X = A;
    return    
end

curr = A;
for i = 1:length(names)
    next = getchild(curr,CorA(i),names(i));
    if(isempty(next))
        X = [];
        return;
    else
        curr = next;
    end
end
X = curr;