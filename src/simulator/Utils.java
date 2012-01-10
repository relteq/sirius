package simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jaxb.Link;
import jaxb.Node;
import jaxb.Scenario;

import org.xml.sax.SAXException;

public class Utils {
	
	/////////////////////////////////////////////////////////////////////
	// static data
	/////////////////////////////////////////////////////////////////////
	
	private static final float EPSILON = (float) 1e-6;
	
	public static String configfilename;
	public static String outputfile_density;
	public static String outputfile_outflow;
	public static String outputfile_inflow;
	public static String schemafile = "data/schema/aurora.xsd";
	public static boolean freememory = true;
	public static Clock clock;
	public static _Scenario theScenario;
	public static float simdt;						// [hr]	
	public static int numVehicleTypes;
	public static OutputWriter outputwriter = null;
	
	/////////////////////////////////////////////////////////////////////
	// global link and node getters
	/////////////////////////////////////////////////////////////////////

	// NOTE: This only searches the top network
	public static _Link getLinkWithId(String id){
		for(Link link : theScenario.getNetwork().getLinkList().getLink()){
			if(link.getId().equals(id))
				return (_Link) link;
		}
		return null;
	}

	public static _Node getNodeWithId(String id){
		for(Node node : theScenario.getNetwork().getNodeList().getNode()){
			if(node.getId().equals(id))
				return (_Node) node;
		}
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	// math helpers
	/////////////////////////////////////////////////////////////////////
	
	public static Float sum(Float [] V){
		Float answ = 0f;
		for(int i=0;i<V.length;i++)
			answ += V[i];
		return answ;
	}
	
	public static Float [] times(Float [] V,float a){
		Float [] answ = new Float [V.length];
		for(int i=0;i<V.length;i++)
			answ[i] = a*V[i];
		return answ;
	}
	
	public static int ceil(float a){
		return (int) Math.ceil(a);
	}
	
	public static int floor(float a){
		return (int) Math.floor(a);
	}
	
	public static int round(float a){
		return Math.round(a);
	}
	
	public static boolean any (boolean [] x){
		for(int i=0;i<x.length;i++)
			if(x[i])
				return true;
		return false;
	}
	
	public static boolean all (boolean [] x){
		for(int i=0;i<x.length;i++)
			if(!x[i])
				return false;
		return true;
	}
	
	public static boolean[] not(boolean [] x){
		boolean [] y = x.clone();
		for(int i=0;i<y.length;i++)
			y[i] = !y[i];
		return y;
	}
	
	public static int count(boolean [] x){
		int s = 0;
		for(int i=0;i<x.length;i++)
			if(x[i])
				s++;
		return s;
	}
	
	public static ArrayList<Integer> find(boolean [] x){
		ArrayList<Integer> r = new ArrayList<Integer>();
		for(int i=0;i<x.length;i++)
			if(x[i])
				r.add(i);
		return r;
	}
	
	public static boolean isintegermultipleof(float A,float a){
		return Utils.equals( Utils.round(A/a) , A/a );
	}
	
	public static boolean equals(float a,float b){
		return Math.abs(a-b) < Utils.EPSILON;
	}
	
	public static boolean greaterthan(float a,float b){
		return a > b + Utils.EPSILON;
	}

	public boolean greaterorequalthan(float a,float b){
		return !lessthan(a,b);
	}
	
	public static boolean lessthan(float a,float b){
		return a < b - Utils.EPSILON;
	}

	public boolean lessorequalthan(float a,float b){
		return !greaterthan(a,b);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// load and save scenarios
	////////////////////////////////////////////////////////////////////////////

	public static _Scenario load(String filename){
        
		JAXBContext context;
		Unmarshaller u;
        _Scenario S = new _Scenario();
    	
    	// create unmarshaller
        try {
        	context = JAXBContext.newInstance("jaxb");
            u = context.createUnmarshaller();
        } catch( JAXBException je ) {
            je.printStackTrace();
            return S;
        }
        
        // schema assignment
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File schemaLocation = new File(Utils.schemafile);
        try{
        	Schema schema = factory.newSchema(schemaLocation);
        	u.setSchema(schema);
        } catch(SAXException e){
        	System.out.println("Schema not found. Skipping validation.");
        	u.setSchema(null);
        }
        
        // read and return
        try {
            u.setProperty("com.sun.xml.internal.bind.ObjectFactory",new _ObjectFactory());            
        	S = (_Scenario) u.unmarshal( new FileInputStream(filename) );
        } catch( JAXBException je ) {
            je.printStackTrace();
            return S;
        } catch (FileNotFoundException e) {
        	System.out.println("Configuration file not found.");
        	return S;
		}

        // copy data to static variables
        float dt = S.getNetwork().getDt().floatValue();
        Utils.theScenario = S;
        if(dt<0.1){
			System.out.println("Warning: Network dt given in hours. Changing to seconds.");
			dt *= 3600;
			S.getNetwork().setDt(new BigDecimal(dt));
        }
        Utils.simdt = dt / 3600f;
        Utils.numVehicleTypes = 1;
        if(S.getSettings().getVehicleTypes()!=null)
        	if(S.getSettings().getVehicleTypes().getVtype()!=null) 
        		Utils.numVehicleTypes = S.getSettings().getVehicleTypes().getVtype().size();
        		
        // initialize the scenario
        S.initialize();
        
        return S;

	}	
	
	public static void save(Scenario scenario,String filename){
        try {
        	JAXBContext context = JAXBContext.newInstance("jaxb");
        	Marshaller m = context.createMarshaller();
        	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        	m.marshal(scenario,new FileOutputStream(filename));
        } catch( JAXBException je ) {
            je.printStackTrace();
            return;
        } catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
        }
	}

	////////////////////////////////////////////////////////////////////////////
	// set input and output file names
	////////////////////////////////////////////////////////////////////////////
	
	public static void setconfigfilename(String name){
		if(!name.endsWith(".xml"))
			name += ".xml";
		Utils.configfilename = name;
	}
	
	public static void setoutputfilename(String name){
		if(name.endsWith(".csv"))
			name = name.substring(0,name.length()-4);
		Utils.outputfile_density = name + "_density.txt";
		Utils.outputfile_outflow = name + "_outflow.txt";
		Utils.outputfile_inflow = name + "_inflow.txt";
	}

}
