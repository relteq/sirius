package aurora.simulator;

public class Clock {
	protected float t;
	protected float to;
	protected float dt;
	protected float maxt;
	protected int currentstep;
	
	public Clock(float to,float tf,float dt){
		this.t = to;
		this.to = to;
		this.dt = dt;
		this.maxt = tf;
		this.currentstep = 0;
	}

	public float getT() {
		return t;
	}

	public int getCurrentstep() {
		return currentstep;
	}
	
	protected void advance(){
		currentstep++;
		t = to + currentstep*dt;
	}
	
	public boolean expired(){
		return t>maxt;
	}

	public boolean istimetosample(int samplesteps){	
		if(currentstep==1)
			return true;
		return currentstep % samplesteps == 0;
	}
}
