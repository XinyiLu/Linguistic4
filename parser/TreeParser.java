package parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class TreeParser {
	
	class RuleConstituent{
		String label;
		double rho;
		
		public RuleConstituent(String l)
		{
			label=l;
			rho=0.0;
		}
		
		public RuleConstituent(String l,double prob){
			label=l;
			rho=prob;
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
	
	class CellConstituent{
		CellPointer leftPointer;
		CellPointer rightPointer;
		double mu;
		
		public CellConstituent(){
			leftPointer=null;
			rightPointer=null;
			mu=1.0;
		}
		
		public CellConstituent(CellPointer left,CellPointer right,double prob){
			leftPointer=left;
			rightPointer=right;
			mu=prob;
		}
	}
	
	class Cell{
		HashMap<String,CellConstituent> cellMap;
		
		public Cell(){
			cellMap=new HashMap<String,CellConstituent>();
		}
	}
	
	private HashMap<String,HashMap<String,HashSet<RuleConstituent>>> ruleSet;
	private HashMap<String,Integer> nodeCountMap;
	private final static String rootLabel="TOP";
	String[] lineArray;
	ArrayList<String> lineList;
	
	public TreeParser(){
		ruleSet=new HashMap<String,HashMap<String,HashSet<RuleConstituent>>>();
		nodeCountMap=new HashMap<String,Integer>();
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
		for(String leftLabel:ruleSet.keySet()){
			HashMap<String,HashSet<RuleConstituent>> subset=ruleSet.get(leftLabel);
			for(String rightLabel:subset.keySet()){
				HashSet<RuleConstituent> rules=subset.get(rightLabel);
				for(RuleConstituent rule:rules){
					rule.rho=rule.rho/(nodeCountMap.get(rule.label)*1.0);
				}
			}
		}
	}
	
	
	public void parseTrainingLineToMap(String line){
		String[] strs=line.split(" ");
		int count=Integer.parseInt(strs[0]);
		RuleConstituent component=new RuleConstituent(strs[1],count);
		String leftLabel=strs[3],rightLabel=(strs.length==4?null:strs[4]);
		
		if(!ruleSet.containsKey(leftLabel)){
			ruleSet.put(leftLabel,new HashMap<String,HashSet<RuleConstituent>>());
		}
		
		HashMap<String,HashSet<RuleConstituent>> subset=ruleSet.get(leftLabel);
		if(!subset.containsKey(rightLabel)){
			subset.put(rightLabel,new HashSet<RuleConstituent>());
		}
		
		subset.get(rightLabel).add(component);
		nodeCountMap.put(strs[1],(nodeCountMap.containsKey(strs[1])?nodeCountMap.get(strs[1]):0)+count);
	}

	public void parseTestFile(String fileName){
		try {
			BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"ISO-8859-1"));
			String line=null;
			lineList=new ArrayList<String>();
			while((line=reader.readLine())!=null){
				//System.out.println(parseLineToTree(line));
				lineList.add(line);
			}
			//close the buffered reader
			reader.close();
			
			lineArray=new String[lineList.size()];
			//create multiple arrays
			int processors=Runtime.getRuntime().availableProcessors();
			Thread[] threads=new Thread[processors];
			for(int i=0;i<processors;i++){
				threads[i]=new Thread(new ConcurrentParser(this,i,processors));
				threads[i].start();
			}
			
			for(int i=0;i<processors;i++){
				threads[i].join();
				int length=lineList.size()/processors;
				int offset=i*length,remain=(i==processors-1?lineList.size()%processors:0);
				for(int j=offset;j<length+offset+remain;j++){
					System.out.println(lineArray[j]);
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public String parseLineToTree(String line){
		String[] words=line.split(" ");
		if(words.length>25){
			return "*IGNORE*";
		}
		
		return expressTree(parseLineToChart(words));
	}
	
	public Cell[][] parseLineToChart(String[] words){
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
		Cell cell=chart[i][k];
		if(k==i+1){
			cell.cellMap.put(words[i],new CellConstituent());
		}
		
		for(int j=i+1;j<k;j++){
			Cell leftCell=chart[i][j],rightCell=chart[j][k];
			for(String leftLabel:leftCell.cellMap.keySet()){
				if(!ruleSet.containsKey(leftLabel))
					continue;
				HashMap<String,HashSet<RuleConstituent>> ruleSubMap=ruleSet.get(leftLabel);
				for(String rightLabel:ruleSubMap.keySet()){
					if(rightCell.cellMap.containsKey(rightLabel)){
						HashSet<RuleConstituent> rules=ruleSubMap.get(rightLabel);
						for(RuleConstituent rule:rules){
							double mu=rule.rho*leftCell.cellMap.get(leftLabel).mu*rightCell.cellMap.get(rightLabel).mu;
							CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,j),new CellPointer(rightLabel,j,k),mu);
							if(!(cell.cellMap.containsKey(rule.label)&&cell.cellMap.get(rule.label).mu>=mu)){
								cell.cellMap.put(rule.label,comp);
							}
						}
					}
				}
			}	
		}
		
		HashSet<String> searchSet=new HashSet<String>(cell.cellMap.keySet());
		while(!searchSet.isEmpty()){
			HashSet<String> addedSet=new HashSet<String>();
			for(String leftLabel:searchSet){
				HashMap<String,HashSet<RuleConstituent>> subset=ruleSet.get(leftLabel);
				if(subset==null||!subset.containsKey(null))
					continue;
				HashSet<RuleConstituent> ruleSubMap=subset.get(null);
				for(RuleConstituent rule:ruleSubMap){
					double mu=rule.rho*cell.cellMap.get(leftLabel).mu;
					CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,k),null,mu);
					if(!(cell.cellMap.containsKey(rule.label)&&cell.cellMap.get(rule.label).mu>=mu)){
						addedSet.add(rule.label);
						cell.cellMap.put(rule.label,comp);
					}
				}
				
			}
			searchSet=addedSet;
		}
		
	}
	
	
	public String expressTree(Cell[][] chart){
		CellPointer rootPointer=new CellPointer(rootLabel,0,chart.length-1);
		return expressTreeHelper(chart,rootPointer);
	}
	
	public String expressTreeHelper(Cell[][] chart,CellPointer pointer){
		String str=new String();
		if(pointer==null||(!chart[pointer.beginPos][pointer.endPos].cellMap.containsKey(pointer.label))){
			return str;
		}
		Cell cell=chart[pointer.beginPos][pointer.endPos];
		CellConstituent comp=cell.cellMap.get(pointer.label);
		/*if(pointer.label.contains("_")){
			str=expressTreeHelper(chart,comp.leftPointer)+expressTreeHelper(chart,comp.rightPointer);
			
		}else */if(comp.leftPointer==null&&comp.rightPointer==null){
			str=" "+pointer.label;
		}else{
			String label=pointer.label;
			/*if(pointer.label.endsWith("^")){
				label=label.substring(0,label.length()-1);
			}*/
			str="("+label+expressTreeHelper(chart,comp.leftPointer)+expressTreeHelper(chart,comp.rightPointer)+")";
		}
		return str;
	}
	
	public static void main(String[] args){
		TreeParser parser=new TreeParser();
		parser.readTrainingFile(args[0]);
		parser.parseTestFile(args[1]);
	}
}
