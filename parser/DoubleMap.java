package parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

public class DoubleMap {
	
	class RuleSetMap{
		HashMap<String,HashSet<RuleConstituent>> parentMap;
		HashMap<String,HashSet<String>> leftMap;
		HashMap<String,HashSet<String>> rightMap;
		
		public RuleSetMap(){
			parentMap=new HashMap<String,HashSet<RuleConstituent>>();
			leftMap=new HashMap<String,HashSet<String>>();
			rightMap=new HashMap<String,HashSet<String>>();
		}
		
		public HashSet<RuleConstituent> searchByParent(String pLabel){
			return parentMap.get(pLabel);
		}
		
		public HashSet<RuleConstituent> searchByChild(String lLabel,String rLabel,int index){
			HashSet<RuleConstituent> resultSet=new HashSet<RuleConstituent>();
			switch(index){
			case SEARCH_BY_BOTH:
				HashSet<String> leftParent=leftMap.get(lLabel);
				HashSet<String> rightParent=rightMap.get(rLabel);
				if(leftParent==null||rightParent==null){
					break;
				}
				for(String right:rightParent){
					if(leftParent.contains(right)){
						resultSet.addAll(parentMap.get(right));
					}
				}
				break;
			case SEARCH_BY_LEFT:
				HashSet<String> leftParents=leftMap.get(lLabel);
				if(leftParents==null)
					break;
				for(String pLabel:leftParents){
					resultSet.addAll(parentMap.get(pLabel));
				}
				break;
			case SEARCH_BY_RIGHT:
				HashSet<String> rightParents=rightMap.get(rLabel);
				if(rightParents==null)
					break;
				for(String pLabel:rightParents){
					resultSet.addAll(parentMap.get(pLabel));
				}
				break;
			default:
				break;
			}
			
			return resultSet;
		}

	}
	
	class RuleConstituent{
		String parentLabel;
		String[] childLabels;
		double rho;
		
		public RuleConstituent(String label)
		{
			parentLabel=label;
			childLabels=new String[]{null,null};
			rho=0.0;
		}
		
		public RuleConstituent(String label,String left,String right,double prob){
			parentLabel=label;
			childLabels=new String[]{left,right};
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
	
	private RuleSetMap ruleSet;
	private final static String rootLabel="TOP";
	private final static int SEARCH_BY_LEFT=1;
	private final static int SEARCH_BY_RIGHT=2;
	private final static int SEARCH_BY_BOTH=0;
	
	public DoubleMap(){
		ruleSet=new RuleSetMap();
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
		HashMap<String, HashSet<RuleConstituent>> parentMap=ruleSet.parentMap;
		for(String parentNode:parentMap.keySet()){
			HashSet<RuleConstituent> childSet=parentMap.get(parentNode);
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
		
		String leftLabel=strs[3];
		String rightLabel=(strs.length==4?null:strs[4]);
		String parentLabel=strs[1];
		RuleConstituent component=new RuleConstituent(parentLabel,leftLabel,rightLabel,Integer.parseInt(strs[0]));
		if(!ruleSet.parentMap.containsKey(parentLabel)){
			ruleSet.parentMap.put(parentLabel,new HashSet<RuleConstituent>());
		}
		ruleSet.parentMap.get(parentLabel).add(component);
		if(!ruleSet.leftMap.containsKey(leftLabel)){
			ruleSet.leftMap.put(leftLabel,new HashSet<String>());
		}
		ruleSet.leftMap.get(leftLabel).add(parentLabel);
		
		if(!ruleSet.rightMap.containsKey(rightLabel)){
			ruleSet.rightMap.put(rightLabel,new HashSet<String>());
		}
		ruleSet.rightMap.get(rightLabel).add(parentLabel);
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
		Cell cell=chart[i][k];
		if(k==i+1){
			cell.cellMap.put(words[i],new CellConstituent());
		}
		HashMap<String,HashSet<RuleConstituent>> parentMap=ruleSet.parentMap;
		for(int j=i+1;j<k;j++){
			Cell leftCell=chart[i][j],rightCell=chart[j][k];
			/*for(String parentNode:parentMap.keySet()){
				HashSet<RuleConstituent> childSet=parentMap.get(parentNode);
				for(RuleConstituent rule:childSet){
					if(leftCell.cellMap.containsKey(rule.childLabels[0])&&rightCell.cellMap.containsKey(rule.childLabels[1])){
						String leftLabel=rule.childLabels[0],rightLabel=rule.childLabels[1];
						double mu=rule.rho*leftCell.cellMap.get(leftLabel).mu*rightCell.cellMap.get(rightLabel).mu;
						CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,j),new CellPointer(rightLabel,j,k),mu);
						if(!(cell.cellMap.containsKey(parentNode)&&cell.cellMap.get(parentNode).mu>=mu)){
							cell.cellMap.put(parentNode,comp);
						}
					}
				}
			}*/
			
			for(String rightLabel:rightCell.cellMap.keySet()){
				HashSet<RuleConstituent> rightRuleSet=ruleSet.searchByChild(null, rightLabel, SEARCH_BY_RIGHT);
				for(RuleConstituent rule:rightRuleSet){
					if(leftCell.cellMap.containsKey(rule.childLabels[0])){
						//find the rule
						String leftLabel=rule.childLabels[0];
						double mu=rule.rho*leftCell.cellMap.get(leftLabel).mu*rightCell.cellMap.get(rightLabel).mu;
						CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,j),new CellPointer(rightLabel,j,k),mu);
						if(!(cell.cellMap.containsKey(rule.parentLabel)&&cell.cellMap.get(rule.parentLabel).mu>=mu)){
							cell.cellMap.put(rule.parentLabel,comp);
						}
					}
				}
			}
		}
		
		boolean check=true;
		while(check){
			int prevSize=cell.cellMap.size();
			for(String parentNode:parentMap.keySet()){
				HashSet<RuleConstituent> childSet=parentMap.get(parentNode);
				for(RuleConstituent rule:childSet){
					if(rule.childLabels[1]==null&&cell.cellMap.containsKey(rule.childLabels[0])){
						String leftLabel=rule.childLabels[0];
						double mu=rule.rho*cell.cellMap.get(leftLabel).mu;
						CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,k),null,mu);
						if(!(cell.cellMap.containsKey(parentNode)&&cell.cellMap.get(parentNode).mu>=mu)){
							cell.cellMap.put(parentNode,comp);
						}
					}
				}
			}
			check=(prevSize==cell.cellMap.size()?false:true);
		}
		
		/*for(String leftLabel:cell.cellMap.keySet()){
			HashSet<RuleConstituent> rules=ruleSet.searchByChild(leftLabel, null, SEARCH_BY_BOTH);
			for(RuleConstituent rule:rules){
				double mu=rule.rho*cell.cellMap.get(leftLabel).mu;
				CellConstituent comp=new CellConstituent(new CellPointer(leftLabel,i,k),null,mu);
				if(!(cell.cellMap.containsKey(rule.parentLabel)&&cell.cellMap.get(rule.parentLabel).mu>=mu)){
					cell.cellMap.put(rule.parentLabel,comp);
				}
			}
		}*/
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
		DoubleMap parser=new DoubleMap();
		parser.readTrainingFile(args[0]);
		//parser.printRuleMap();
		parser.parseTestFile(args[1]);
		//System.out.println("Finished");
	}
}
