/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public class Types {

	public enum ScenarioElement 	{NULL, link, 
										   node,
										   controller,
										   sensor,
										   event,
										   signal };
										   
	public enum Link				{NULL, freeway,
										   HOV,
										   HOT,
										   onramp,
										   offramp,
										   freeway_connector,
										   street,
										   intersection_apporach,other };	
										   	
	public enum Node				{NULL, simple,
										   onramp,
										   offramp,
										   signalized_intersection,
										   unsignalized_intersection };
										   
	public enum Sensor				{NULL, static_point,
									   	   static_area,
									   	   moving_point };
	
	public enum DataSource			{NULL, PeMSDataClearinghouse,
										   CaltransDBX,
										   BHL };

	public enum Event				{NULL, fundamental_diagram,
										   link_demand_knob,
										   link_lanes, 
										   node_split_ratio,
										   control_toggle,
										   global_control_toggle,
										   global_demand_knob };
				
	public enum Controller 			{NULL, IRM_alinea,
										   IRM_time_of_day,
										   IRM_traffic_responsive,
										   CRM_swarm,
										   CRM_hero,
										   VSL_time_of_day,
										   SIG_pretimed,
										   SIG_actuated };
										   
	public enum QueueControl		{NULL, queue_override,
										   proportional,
										   proportional_integral  };
								 		   
	public enum Dynamics			{NULL, CTM,
										   region_tracking,
										   discrete_departure };
	
	public enum Mode 				{ normal, warmupFromZero , warmupFromIC };
}
