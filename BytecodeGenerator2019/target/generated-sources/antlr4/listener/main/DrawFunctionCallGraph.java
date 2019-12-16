package listener.main;

import java.util.HashMap;
import java.util.Map;

import listener.main.SymbolTable.VarInfo;

public class DrawFunctionCallGraph {

	static StringBuffer graph = new StringBuffer();//출력할 그림. 콘솔출력.
	private static Map<String, String> functionGraphs = new HashMap<>();//각 함수마다의 그래프를 그려서 보관.
	static int depth = 0;//현재 함수 깊이. 
	static boolean currentFlowIsMain = false;//main들어가면 true로.
	String currentGraphName;//현재 그리고 있는 함수명
	
	static void addGraph(String currentGraphName, StringBuffer graph) {
		functionGraphs.put(currentGraphName, graph.toString());//그린 그래프 map에 저장.
	}
	static void drawNewFunction(String fname) {
		
		graph.append("---------------\n");
		graph.append("|     "+fname+"     |\n");
		graph.append("---------------\n");
		graph.append("       |\n");
		graph.append("       |\n");
		graph.append("       |\n");
	}
	static void drawCallState() {
		graph.append("        ---------------->\n");
	}
	static void drawReturnState() {//리턴문 나올때 그래프그리기. 리턴 == 함수끝 이므로 StringBuffer초기화.
		graph.append("       <----------------\n");
		graph.append("       |\n");
		graph.append("       |\n");
		graph.append("       |\n");
	}
	static StringBuffer getSPACE(int depth) {
		StringBuffer SPACE = new StringBuffer();
		for(int i = 0; i < depth; i++) {
			SPACE.append("                  "); 
		}
		return SPACE;
	}
	static void appendGraph(String nextGraph) {
		int lineCount = nextGraph.split("\n").length;//그래프의 줄수
		String callString = "        ---------------->";
		String returnString = "       <----------------";
		
		for(int i = 0; i < lineCount; i++) {
			if(nextGraph.split("\n")[i].equals(returnString)) {//returnstate일때,
				//graph.append(" ");
				depth--;
			}
			graph.append(getSPACE(depth));
			graph.append(nextGraph.split("\n")[i]);
			graph.append("\n");
			
			if(nextGraph.split("\n")[i].equals(callString)) {//함수안에서 함수를 중첩해서 call하는 경우.
				
				String calleeFunction = "";
				for(int j = i+1; j < lineCount; j++) {
					calleeFunction += nextGraph.split("\n")[j] + "\n";
				}
				
				depth++;
				lineCount -= calleeFunction.split("\n").length;
				appendGraph(calleeFunction);
				
			}
		}
		
	}
	static String getGraphFromMap(String calleeFunName) {//함수 이름주고 그래프그림 리턴.
		return functionGraphs.get(calleeFunName);
	}
	static void clearGraph() {//그래프 초기화하기.
		graph.setLength(0);
	}
	static void printGraph() {
		System.out.println(functionGraphs.get("main"));
	}
}
