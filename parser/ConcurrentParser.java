package parser;

//class for the concurrent job
public class ConcurrentParser implements Runnable {

	private int category;
	private TreeParser parser;
	private int processors;
	
	//category represents which section of the testing file this thread is in charge of
	//processors represent how many sections in total
	public ConcurrentParser(TreeParser tparser,int id,int pno){
		category=id;
		parser=tparser;
		processors=pno;
	}
	
	@Override
	public void run() {
		int length=parser.lineList.size()/processors;
		//get the offset of the section this thread is running on
		//if this is the last thread, then it should also run the remaining lines if the number
		//of lines could not be divided by the number of processors
		int offset=category*length,remain=(category==processors-1?parser.lineList.size()%processors:0);
		for(int i=offset;i<length+offset+remain;i++){
			String treeStr=parser.parseLineToTree(parser.lineList.get(i));
			parser.lineArray[i]=treeStr;
		}
		
	}
}
