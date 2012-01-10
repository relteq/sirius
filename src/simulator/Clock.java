package simulator;

public class Clock {
	private float t;
	private float dt;
	private float maxt;
	private int currentstep;
	
	public Clock(float to,float tf,float dt){
		this.t = to;
		this.dt = dt;
		this.maxt = tf-to;
		this.currentstep = 0;
	}

	public float getT() {
		return t;
	}

	public int getCurrentstep() {
		return currentstep;
	}
	
	public void advance(){
		t += dt;
		currentstep++;
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
