package visitor;
import java.util.*;

public class GraphColoring{

	HashMap<String, HashMap<String, HashSet<String>>> globalInterferenceMap;
	HashMap<String, HashMap<String, HashSet<String>>> globalInterferenceMapCopy;
	HashMap<String, HashMap<String, String>> globalRegisterMap;
	HashSet<String> argRegs;
	HashSet<String> sRegs;
	HashSet<String> tRegs;
	Stack<String> spillStack;
	HashMap<String, Stack<String>> globalSpillStack;
	Stack<String> coloredStack;
	HashMap<String, Integer> spillCosts;
	HashMap<String, HashMap<String, ArrayList<String>>> procStats;
	int maxRegs;

	public GraphColoring(HashMap<String, HashMap<String, HashSet<String>>> interferenceMap, HashMap<String, Integer> spillCosts,
						 HashMap<String, HashMap<String, ArrayList<String>>> procStats){
		this.globalInterferenceMap = interferenceMap;
		this.globalInterferenceMapCopy = trueCopy(globalInterferenceMap);
		this.globalRegisterMap = new HashMap<String, HashMap<String, String>>();
		this.globalSpillStack = new HashMap<String, Stack<String>>();
		this.spillCosts = spillCosts;
		this.procStats = procStats;

		List<String> regs = Arrays.asList("a0", "a1", "a2", "a3");
		this.argRegs = new HashSet<String>(regs);
		regs = Arrays.asList("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7");
		this.sRegs = new HashSet<String>(regs);
		regs = Arrays.asList("t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9");
		this.tRegs = new HashSet<String>(regs);
		this.maxRegs = tRegs.size() + sRegs.size();
	}

	// tool function to make a copy of the interferenceMap
	public HashMap<String, HashMap<String, HashSet<String>>> trueCopy(HashMap<String, HashMap<String, HashSet<String>>> original){
		HashMap<String, HashMap<String, HashSet<String>>> copy = new HashMap<String, HashMap<String, HashSet<String>>>();
		for(String firstLevel : original.keySet()){
			copy.put(firstLevel, new HashMap<String, HashSet<String>>());
			for(String secondLevel : original.get(firstLevel).keySet()){
				copy.get(firstLevel).put(secondLevel, new HashSet<String>());
				for(String thirdLevel : original.get(firstLevel).get(secondLevel)){
					copy.get(firstLevel).get(secondLevel).add(thirdLevel);
				}
			}
		}
		return copy;
	} 

	// Calculates and returns the registerMap
	public HashMap<String, HashMap<String, String>> getRegisterMap(){
		String tempToRemove;

		for(String method : globalInterferenceMap.keySet()){
			coloredStack = new Stack<String>();
			spillStack = new Stack<String>();
			globalRegisterMap.put(method, new HashMap<String, String>());
			globalSpillStack.put(method, spillStack);

			// special treatment for "this"
			if(globalInterferenceMapCopy.get(method).containsKey("TEMP 0")){
				globalRegisterMap.get(method).put("TEMP 0", "s0");
				sRegs.remove("s0");
				removeTemp("TEMP 0", globalInterferenceMap.get(method));
			}

			// also for function arguments
			if(!method.equals("MAIN")){
				// System.out.println(method+" has "+Integer.parseInt(procStats.get(method).get("argsNo").get(0))+" arguments");
				for(int i=1; i<Integer.parseInt(procStats.get(method).get("argsNo").get(0)); i++){
					if(i > 3){
						globalRegisterMap.get(method).put("TEMP "+i, "PASSARG "+(i-3));
						continue;							
					}
					globalRegisterMap.get(method).put("TEMP "+i, "s"+i);
					sRegs.remove("s"+i);
					removeTemp("TEMP "+i, globalInterferenceMap.get(method));
					// System.out.println("TEMP "+i+": "+globalRegisterMap.get(method).get("TEMP "+i));
				}
				// try{System.in.read();}
				// catch(Exception e){}
			}

			// System.out.println("This should not contain arg temps: "+globalInterferenceMap.get(method));
			// System.out.println(method+"'s regMap before loop: "+globalRegisterMap.get(method));

			// try{System.in.read();}
			// catch(Exception e){}

			// Chaitin's Algorithm
			while( !globalInterferenceMap.get(method).isEmpty() ){
				while( ( tempToRemove = notInterferingRegs(globalInterferenceMap.get(method)) ) != null){
					removeTemp(tempToRemove, globalInterferenceMap.get(method));
				}
				if(globalInterferenceMap.get(method).isEmpty()){
					break;
				}
				else{
					tempToRemove = getTempToRemove( globalInterferenceMap.get(method) );
					spillTemp(tempToRemove, globalInterferenceMap.get(method));
					while( ( tempToRemove = notInterferingRegs(globalInterferenceMap.get(method)) ) != null){
						removeTemp(tempToRemove, globalInterferenceMap.get(method));
					}
				}
			}

			// System.out.println("final coloredStack: "+coloredStack);
			// System.out.println("final spillStack: "+spillStack);
			
			// Coloring of the registers
			for(String temp : coloredStack){
				if(globalRegisterMap.get(method).get(temp)!=null){
					continue;
				}
				HashSet<String> availableRegs = new HashSet<String>(tRegs);

				for(String neighbor : globalInterferenceMapCopy.get(method).get(temp)){
					String reg = globalRegisterMap.get(method).get(neighbor);
					if( reg != null ){
						availableRegs.remove(reg);
						if(availableRegs.isEmpty())
							availableRegs.addAll(sRegs);
					}
				}
				globalRegisterMap.get(method).put(temp, (String) availableRegs.toArray()[0]);
			}

			// System.out.println("-------------------------------\n"+method+"'s regMap final: "+globalRegisterMap.get(method)+"\n");
			// System.out.println("-------------------------------\n"+method+" spillMap: "+globalSpillStack.get(method)+"\n");

			// try{System.in.read();}
			// catch(Exception e){}
		}

		return globalRegisterMap;
	}

	public HashMap<String, Stack<String>> getSpillStack(){
		return globalSpillStack;
	}

	public String getTempToRemove(HashMap<String, HashSet<String>> methodInterferenceMap){
		int spillCost;
		int currentDegree;
		int currentCriterion;
		int criterion = 99999999;
		String tempToRemove = null;

		// the selection is based on the heuristic that Chaitin proposed
		// minimum: spillcost / currentdegree
		for(String temp : methodInterferenceMap.keySet()){
			currentDegree = methodInterferenceMap.get(temp).size();
			spillCost = spillCosts.get(temp);
			currentCriterion = spillCost/currentDegree;
			if(currentCriterion < criterion){
				criterion = currentCriterion;
				tempToRemove = temp;
			}
		}
		return tempToRemove;
	}

	// returns a temp that has a degree less than our maxRegs therefore -> colorable
	public String notInterferingRegs(HashMap<String, HashSet<String>> methodInterferenceMap){
		for(String temp : methodInterferenceMap.keySet()){
			if(methodInterferenceMap.get(temp).size() < maxRegs)
				return temp;
		}
		return null;
	}

	// removing temp that is not colorable and putting it in the spillStack
	public void spillTemp(String temp, HashMap<String, HashSet<String>> methodInterferenceMap){
		methodInterferenceMap.remove(temp);
		spillStack.push(temp);

		for(String temp2 : methodInterferenceMap.keySet()){
			for(String conflictTemp : methodInterferenceMap.get(temp2)){
				methodInterferenceMap.remove(temp);
			}
		}
	}

	// removing temp that is colorable
	public void removeTemp(String temp, HashMap<String, HashSet<String>> methodInterferenceMap){
		methodInterferenceMap.remove(temp);
		coloredStack.push(temp);
		
		for(String temp2 : methodInterferenceMap.keySet()){
			methodInterferenceMap.get(temp2).remove(temp);
		}
	}
}