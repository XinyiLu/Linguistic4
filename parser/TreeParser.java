package parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
	
	private HashMap<String,HashMap<String,HashSet<RuleConstituent>>> ruleSet;
	private HashMap<String,Integer> nodeCountMap;
	private final static String rootLabel="TOP";
	
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
		assert(strs.length>3&&strs.length<6);
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
			while((line=reader.readLine())!=null){
				System.out.println(parseLineToTree(line));
			}
			//close the buffered reader
			reader.close();
			updateRhoMap();
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	public String parseLineToTree(String line){
		String[] words=line.split(" ");
		if(words.length>25){
			return "*IGNORE*";
		}
		
		Cell[][] chart=parseLineToChart(words);
		return expressTree(chart);
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
		assert(i<k);
		Cell cell=chart[i][k];
		if(k==i+1){
			assert(!cell.cellMap.containsKey(words[i]));
			cell.cellMap.put(words[i],new CellConstituent());
		}
		
		for(int j=i+1;j<k;j++){
			Cell leftCell=chart[i][j],rightCell=chart[j][k];
			for(String leftLabel:leftCell.cellMap.keySet()){
				HashMap<String,HashSet<RuleConstituent>> ruleSubMap=ruleSet.get(leftLabel);
				if(ruleSubMap==null)
					continue;
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
						if(!cell.cellMap.containsKey(rule.label)){
							addedSet.add(rule.label);
						}
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
		if(pointer.label.contains("_")){
			assert(pointer.label.equals(comp.childPointers[0].label+"_"+comp.childPointers[1].label));
			str=expressTreeHelper(chart,comp.childPointers[0])+expressTreeHelper(chart,comp.childPointers[1]);
			
		}else if(comp.childPointers[0]==null&&comp.childPointers[1]==null){
			str=" "+pointer.label;
		}else{
			String label=pointer.label;
			if(pointer.label.endsWith("^")){
				label=label.substring(0,label.length()-1);
			}
			str="("+label+expressTreeHelper(chart,comp.childPointers[0])+expressTreeHelper(chart,comp.childPointers[1])+")";
		}
		return str;
	}
	
	public static void main(String[] args){
		TreeParser parser=new TreeParser();
		parser.readTrainingFile(args[0]);
		//parser.printRuleMap();
		parser.parseTestFile(args[1]);
		//System.out.println("Finished");
	}
}
