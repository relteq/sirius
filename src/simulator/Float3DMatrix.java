package simulator;

import java.util.StringTokenizer;

public class Float3DMatrix {
	
	public int n1;		// number of rows
	public int n2;		// number of columns
	public int n3;		// number of slices
    private Float [][][] data;
    public boolean isvector;		// 1-D matrices are converted to column vectors
    public boolean isempty;			// true if there is no data;
    public Integer length = null;	// defined if isvector
    
	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
    
    public Float3DMatrix(int n1,int n2,int n3,Float val) {
    	this.n1 = n1;
    	this.n2 = n2;
    	this.n3 = n3;
    	data = new Float[n1][n2][n3];
    	for(int i=0;i<n1;i++)
        	for(int j=0;j<n2;j++)
            	for(int k=0;k<n3;k++)
            		data[i][j][k] = val;
    	this.isempty = n1==0 && n2==0 && n3==0;
    }
    
    public Float3DMatrix(String str,boolean vectorize) {
    	//data = new ArrayList<ArrayList<ArrayList<Float>>>();
    	int numtokens,i,j,k;
		boolean issquare = true;
    	n1 = 0;
    	n2 = 0;
    	n3 = 0;
    	
    	if ((str.isEmpty()) || (str.equals("\n")) || (str.equals("\r\n"))){
    		isempty = true;
			return;
    	}
    	
    	str.replaceAll("\\s","");
    	
    	// populate data
		StringTokenizer slicesX = new StringTokenizer(str, ";");
		n1 = slicesX.countTokens();
		i=0;
		while (slicesX.hasMoreTokens() && issquare) {
			String sliceX = slicesX.nextToken();
			StringTokenizer slicesXY = new StringTokenizer(sliceX, ",");
			
			// evaluate n2, check squareness
			numtokens = slicesXY.countTokens();
			if(n2==0) // first time here
				n2 = numtokens;
			else{
				if(n2!=numtokens){
					issquare = false;
					break;
				}
			}
			
			j=0;
			while (slicesXY.hasMoreTokens() && issquare) {
				String sliceXY = slicesXY.nextToken();
				StringTokenizer slicesXYZ = new StringTokenizer(sliceXY,":");
				
				// evaluate n3, check squareness
				numtokens = slicesXYZ.countTokens();
				if(n3==0){ // first time here
					n3 = numtokens;
					data = new Float[n1][n2][n3];
				}
				else{
					if(n3!=numtokens){
						issquare = false;
						break;
					}
				}
				
				k=0;
				while (slicesXYZ.hasMoreTokens() && issquare) {
					Float value = Float.parseFloat(slicesXYZ.nextToken());
					if(value>=0)
						data[i][j][k] = value;
					else
						data[i][j][k] = Float.NaN;
					k++;
				}
				j++;
			}
			i++;
		}
		
		if(!issquare){
			System.out.println("Data is not square.");
			data = null;
	    	this.isempty = true;
			return;
		}
		
		if(vectorize){
			makevector();
			length = n3;
		}

    	this.isempty = n1==0 && n2==0 && n3==0;
		
    }
     
	private void makevector(){
    	
    	// is it 1D?
    	int b1 = n1>1 ? 1 : 0;
    	int b2 = n2>1 ? 1 : 0;
    	int b3 = n3>1 ? 1 : 0;
    	isvector = b1+b2+b3<=1;

    	if(!isvector)
    		return;

    	if(n3>1)
    		return;
    	
    	// reshape
    	if(n2>1){
    		Float[][][] olddata = data.clone();
    		n3 = n2;
    		n2 = 1;
    		n1 = 1;
    		data = new Float[1][1][n3];
    		for(int k=0;k<n3;k++)
    			data[1][1][k] = olddata[1][k][1];
    		olddata = null;
    		return;
    	}

    	if(n1>1){
    		Float[][][] olddata = data.clone();
    		n3 = n1;
    		n2 = 1;
    		n1 = 1;
    		data = new Float[1][1][n3];
    		for(int k=0;k<n3;k++)
    			data[1][1][k] = olddata[k][1][1];
    		olddata = null;
    	}	
    }
    
	/////////////////////////////////////////////////////////////////////
	// get data
	/////////////////////////////////////////////////////////////////////  
	
    public Float get(int i,int j,int k){
    	if(isempty)
    		return Float.NaN;
    	else
    		return data[i][j][k];
    }
    
    public Float get(int k){
    	if(isempty)
    		return Float.NaN;
    	if(isvector)
    		return data[0][0][k];
    	else
    		return null;
    }

    public Float[] get(int i,int j){
    	if(isempty || isvector)
    		return null;
    	else
    		return data[i][j];
    }
    
    public Float [] getvector(){
    	if(isempty)
    		return null;
    	if(isvector)
    		return data[0][0];
    	else
    		return null;
    }

	/////////////////////////////////////////////////////////////////////
	// change data
	/////////////////////////////////////////////////////////////////////  
    
    public void set(int i,int j,int k,Float f){
    	data[i][j][k] = f;
    }
    
    public void multiplyscalar(float value){
    	int i,j,k;
    	for(i=0;i<n1;i++)
    		for(j=0;j<n2;j++)
    			for(k=0;k<n3;k++)
    				data[i][j][k] *= value;	
    }
    
    public void addscalar(float value){
    	int i,j,k;
    	for(i=0;i<n1;i++)
    		for(j=0;j<n2;j++)
    			for(k=0;k<n3;k++)
    				data[i][j][k] += value;	
    }
    
    public void copydata(Float3DMatrix in){
    	if(in.n1!=n1 || in.n2!=n2 || in.n3!=n3)
    		return;
    	int i,j,k;
    	for(i=0;i<n1;i++)
    		for(j=0;j<n2;j++)
    			for(k=0;k<n3;k++)
    				data[i][j][k] = in.data[i][j][k];	  
    }
   
    public void normalizeSplitRatioMatrix(){
    	
    	int i,j,k;
		boolean hasNaN;
		int countNaN;
		int idxNegative;
		float sum;
    	
    	for(i=0;i<n1;i++)
    		for(k=0;k<Utils.numVehicleTypes;k++){
				hasNaN = false;
				countNaN = 0;
				idxNegative = -1;
				sum = 0.0f;
				for (j = 0; j < n2; j++)
					if (data[i][j][k].isNaN()) {
						countNaN++;
						idxNegative = j;
						if (countNaN > 1)
							hasNaN = true;
					}
					else
						sum += data[i][j][k];
				
				if (countNaN==1) {
					data[i][idxNegative][k] = Math.max(0f, (1-sum));
					sum += data[i][idxNegative][k];
				}
				
				if ((!hasNaN) && (sum==0.0)) {	
					data[i][0][k] = 1f;
					//for (j=0; j<n2; j++)			
					//	data[i][j][k] = 1/((float) n2);
					continue;
				}
				
				if ((!hasNaN) && (sum<1.0)) {
					for (j=0;j<n2;j++)
						data[i][j][k] = (float) (1/sum) * data[i][j][k];
					continue;
				}
				
				if (sum >= 1.0)
					for (j=0; j<n2; j++)
						if (data[i][j][k].isNaN())
							data[i][j][k] = 0f;
						else
							data[i][j][k] = (float) (1/sum) * data[i][j][k];
    			
    			
    		}
    }
    
	/////////////////////////////////////////////////////////////////////
	// check data
	/////////////////////////////////////////////////////////////////////  
    
    public boolean hasNaN(){
    	if(isempty)
    		return false;
    	int i,j,k;
    	for(i=0;i<n1;i++)
    		for(j=0;j<n2;j++)
    			for(k=0;k<n3;k++)
    				if(data[i][j][k].isNaN())
    					return true;
    	return false;
    }

    
    
    
}
