package parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import parser.Parser.Cell.CellConstituent;


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
	
	@SuppressWarnings("rawtypes")
	class CellConstituent implements Comparable{
		String[] rhoList;
		double mu;
		
		public CellConstituent(){
			rhoList=new String[]{null,null};
			mu=1.0;
		}
		
		public CellConstituent(String left,String right,double prob){
			rhoList=new String[]{left,right};
			mu=prob;
		}
		@Override
		public int compareTo(Object arg0) {
			CellConstituent comp=(CellConstituent)arg0;
			if(rhoList.length==comp.rhoList.length){
				for(int i=0;i<rhoList.length;i++){
					if(!rhoList[i].equals(comp.rhoList[i])){
						return -1;
					}
				}
				return 0;
			}
			return -1;
		}
		
	}
	
	class Cell{
		HashMap<String,CellConstituent> cellMap;
		
		public Cell(){
			cellMap=new HashMap<String,CellConstituent>();
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
		assert(strs.length>3&&strs.length<6);
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
	
	public void printRuleMap(){
		for(String parentNode:ruleSet.keySet()){
			System.out.println("--------------------------");
			System.out.println(parentNode);
			for(RuleConstituent comp:ruleSet.get(parentNode)){
				for(String child:comp.childNodes){
					System.out.print(child+"\t");
				}
				System.out.println(comp.rho);
			}
			System.out.println("--------------------------");
		}
	}

	public Cell[][] parseLineToChart(String line){
		String[] words=line.split(" ");
		int length=words.length;
		Cell[][] chart=initiateChart(length+1);
		for(int l=1;l<=words.length;l++){
			for(int s=0;s<=length-l;s++){
				fillCell(chart,words,s,s+l);
			}
		}
		return chart;
	}
	
	public Cell[][] initiateChart(int length){
		Cell[][] chart=new Cell[length][length];
		for(int i=0;i<length;i++){
			for(int j=0;j<length;j++){
				chart[i][j]=new Cell();
			}
		}
	}
	
	
	public void fillCell(Cell[][] chart,String[] words,int i,int k){
		assert(i<k);
		Cell cell=chart[i][k];
		if(k==i+1){
			assert(!cell.cellMap.containsKey(words[i]));
			cell.cellMap.put(words[i],new CellConstituent());
		}
		
		
	}
	
	public static void main(String[] args){
		Parser parser=new Parser();
		parser.readTrainingFile(args[0]);
		//parser.printRuleMap();
		System.out.println("Finished");
	}
}
