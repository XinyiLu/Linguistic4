package parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class Parser {
	
	@SuppressWarnings("rawtypes")
	class RuleConstituent implements Comparable{
		ArrayList<String> childNodes;
		double rho;
		
		public RuleConstituent()
		{
			childNodes=new ArrayList<String>();
			rho=0.0;
		}

		@Override
		public int compareTo(Object o) {
			RuleConstituent rule=(RuleConstituent)o;
			if(rule.childNodes.size()==childNodes.size()){
				for(int i=0;i<childNodes.size();i++){
					if(!childNodes.get(i).equals(rule.childNodes.get(i))){
						return -1;
					}
				}
				return 0;
			}
			return -1;
		}
		
	}
	
	HashMap<String,HashSet<RuleConstituent>> ruleSet;
	
	public Parser(){
		ruleSet=new HashMap<String,HashSet<RuleConstituent>>();
	}
	
	public void readTrainingFile(String fileName){
		try {
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"ISO-8859-1"));
			String line=null;
			while((line=reader.readLine())!=null){
				parseTrainingLineToMap(line);
			}
			//close the buffered reader
			reader.close();
			updateRhoMap();
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void updateRhoMap(){
		for(String parentNode:ruleSet.keySet()){
			HashSet<RuleConstituent> childSet=ruleSet.get(parentNode);
			double totalCount=0;
			for(RuleConstituent comp:childSet){
				totalCount+=comp.rho;
			}
			
			for(RuleConstituent comp:childSet){
				comp.rho=comp.rho/totalCount;
			}
		}
	}
	
	
	public void parseTrainingLineToMap(String line){
		String[] strs=line.split(" ");
		assert(strs.length>=4);
		if(!ruleSet.containsKey(strs[1])){
			ruleSet.put(strs[1],new HashSet<RuleConstituent>());
		}
		
		RuleConstituent component=new RuleConstituent();
		for(int i=3;i<strs.length;i++){
			component.childNodes.add(strs[i]);
		}
		
		assert(!ruleSet.get(strs[1]).contains(component));
		component.rho=Integer.parseInt(strs[0]);		
		ruleSet.get(strs[1]).add(component);
	}
	
	
	public static void main(String[] args){
		Parser parser=new Parser();
		parser.readTrainingFile(args[0]);
		
		System.out.println("Finished");
	}
}
