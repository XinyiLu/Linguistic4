package parser;


public class ConcurrentParser implements Runnable {

	private int category;
	private TreeParser parser;
	private int processors;
	public ConcurrentParser(TreeParser tparser,int id,int pno){
		category=id;
		parser=tparser;
		processors=pno;
	}
	
	@Override
	public void run() {
		int length=parser.lineList.size()/processors;
		int offset=category*length,remain=(category==processors-1?parser.lineList.size()%processors:0);
		for(int i=offset;i<length+offset+remain;i++){
			String treeStr=parser.parseLineToTree(parser.lineList.get(i));
			parser.lineArray[i]=treeStr;
		}
		
	}
}
