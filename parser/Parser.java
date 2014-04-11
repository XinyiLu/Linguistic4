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
		ArrayList<String> childLabels;
		double rho;
		
		public RuleConstituent()
		{
			childLabels=new ArrayList<String>();
			rho=0.0;
		}

		@Override
		public int compareTo(Object o) {
			RuleConstituent rule=(RuleConstituent)o;
			if(rule.childLabels.size()==childLabels.size()){
				for(int i=0;i<childLabels.size();i++){
					if(!childLabels.get(i).equals(rule.childLabels.get(i))){
						return -1;
					}
				}
				return 0;
			}
			return -1;
		}
		
	}
	
	class CellPointer{
		int beginPos;
		int endPos;
		String label;
		
		public CellPointer(String word,int i,int k){
			label=word;
			beginPos=i;
			endPos=k;
		}
	}
	
	@SuppressWarnings("rawtypes")
	class CellConstituent implements Comparable{
		CellPointer[] childPointers;
		double mu;
		
		public CellConstituent(){
			childPointers=new CellPointer[]{null,null};
			mu=1.0;
		}
		
		public CellConstituent(CellPointer left,CellPointer right,double prob){
			childPointers=new CellPointer[]{left,right};
			mu=prob;
		}
		@Override
		public int compareTo(Object arg0) {
			CellConstituent comp=(CellConstituent)arg0;
			if(childPointers.length==comp.childPointers.length){
				for(int i=0;i<childPointers.length;i++){
					if(!childPointers[i].equals(comp.childPointers[i])){
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
			component.childLabels.add(strs[i]);
		}
		if(component.childLabels.size()==1){
			component.childLabels.add(null);
		}
		
		assert(component.childLabels.size()==2&&!ruleSet.get(strs[1]).contains(component));
		component.rho=Integer.parseInt(strs[0]);		
		ruleSet.get(strs[1]).add(component);
	}
	
	public void printRuleMap(){
		for(String parentNode:ruleSet.keySet()){
			System.out.println("--------------------------");
			System.out.println(parentNode);
			for(RuleConstituent comp:ruleSet.get(parentNode)){
				for(String child:comp.childLabels){
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
		return chart;
	}
	
	
	public void fillCell(Cell[][] chart,String[] words,int i,int k){
		assert(i<k);
		Cell cell=chart[i][k];
		if(k==i+1){
			assert(!cell.cellMap.containsKey(words[i]));
			cell.cellMap.put(words[i],new CellConstituent());
		}
		
		for(int j=i+1;j<k;j++){
			Cell leftCell=chart[i][j],rightCell=chart[j][k];
			for(String parentNode:ruleSet.keySet()){
				HashSet<RuleConstituent> childSet=ruleSet.get(parentNode);
				for(RuleConstituent rule:childSet){
					assert(rule.childLabels.size()==2);
					if(rightCell.cellMap.containsKey(rule.childLabels.get(1))&&leftCell.cellMap.containsKey(rule.childLabels.get(0))){
						String leftLabel=rule.childLabels.get(0),rightLabel=rule.childLabels.get(1);
						double mu=rule.rho*leftCell.cellMap.get(leftLabel).mu*rightCell.cellMap.get(rightLabel).mu;
						CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,j),new CellPointer(rightLabel,j,k),mu);
						if(!(cell.cellMap.containsKey(parentNode)&&cell.cellMap.get(parentNode).mu>=mu)){
							cell.cellMap.put(parentNode,comp);
						}
					}
				}
			}
		}
		
		for(String parentNode:ruleSet.keySet()){
			HashSet<RuleConstituent> childSet=ruleSet.get(parentNode);
			for(RuleConstituent rule:childSet){
				if(rule.childLabels.get(1)==null&&cell.cellMap.containsKey(rule.childLabels.get(0))){
					String leftLabel=rule.childLabels.get(0);
					double mu=rule.rho*cell.cellMap.get(leftLabel).mu;
					CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,k),null,mu);
					if(!(cell.cellMap.containsKey(parentNode)&&cell.cellMap.get(parentNode).mu>=mu)){
						cell.cellMap.put(parentNode,comp);
					}
				}
			}
		}
		
		
	}
	
	public static void main(String[] args){
		Parser parser=new Parser();
		parser.readTrainingFile(args[0]);
		//parser.printRuleMap();
		System.out.println("Finished");
	}
}
