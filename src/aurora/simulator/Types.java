package aurora.simulator;

public class Types {

	public enum Element 			{NODE,LINK,NETWORK};
	public enum Controller 			{none,ALINEA}; //,TOD,TR,VSLTOD,SIMPLESIGNAL,pretimed,actuated,synchornized,swarm,hero,slave};
	public enum Link				{FW,HW,HOV,HOT,HV,ETC,OR,FR,IC,ST,LT,RT,D};
	public enum Node				{F,H,S,P,O,T}
	public enum Event				{FD}; //,demand,qlim,srm,wfm,scontrol,ncontrol,ccontrol,tcontrol,monitor};
	// public enum QueueControl		{queueoverride,proportional,pi}

}
