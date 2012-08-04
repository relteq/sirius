package com.relteq.sirius.calibrator;

public class FDParameters {

	// nominal values
	private static float nom_vf = 65;				// [mph]
	private static float nom_w = 15;				// [mph]
	private static float nom_q_max = 2000;			// [veh/hr/lane]

	private float vf;
	private float w;
	private float q_max;
	
	public FDParameters(){
		vf = FDParameters.nom_vf;
		w  = FDParameters.nom_w;
		q_max = FDParameters.nom_q_max;
	}
	
	public FDParameters(float vf,float w,float q_max){
		this.vf = vf;
		this.w  = w;
		this.q_max = q_max;
	}
	
	public void setFD(float vf,float w,float q_max){
	if(!Float.isNaN(vf))
		this.vf = vf;
	if(!Float.isNaN(w))
		this.w = w;
	if(!Float.isNaN(q_max))
		this.q_max = q_max;
	}
	
	public float getVf() {
		return vf;
	}
	
	public float getW() {
		return w;
	}
	
	public float getQ_max() {
		return q_max;
	}
	
	public float getRho_crit() {
		return q_max/vf;
	}
	
	public float getRho_jam() {
		return q_max*(1/vf+1/w);
	}
	
}
