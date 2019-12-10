package listener.main;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable.Type;
import static listener.main.BytecodeGenListenerHelper.*;


public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		int id;
		int initVal;
		
		public VarInfo(Type type,  int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		public VarInfo(Type type, int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public String sigStr;
	}
	static public class MethodInfo {
		public String calledMethodName;
		public int callCount;
		
		public MethodInfo(String calledMethodName, int callCount) {
			this.calledMethodName = calledMethodName;
			this.callCount = callCount;
		}
	}
	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 
	private Map<String, MethodInfo> _msymtable = new HashMap<>(); //호출한 메소드.
	
	void putMethodCall(String currentMethodName, String calledMethodName) {
		if(_msymtable.containsKey(currentMethodName)) {
			_msymtable.get(currentMethodName).callCount++;
		}
		else {
			MethodInfo methodInfo = new MethodInfo(calledMethodName, 1);
			_msymtable.put(currentMethodName, methodInfo);
		}
	}
	
	//table에 넣을 변수들의 ID. 전역변수로 선언. table에 넣을때마다 ID를 ++해서 변수들끼리 서로 겹치지않게 해준다.
	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 0;
	
	SymbolTable(){
		initFunDecl();
		initFunTable();
	}
	
	void initFunDecl(){		// at each func decl
		_lsymtable.clear();
		_localVarID = 0;
		_labelID = 0;
		_tempVarID = 32;		
	}
	
	void putLocalVar(String varname, Type type){
		//<Fill here>
		VarInfo varInfo = new VarInfo(type, _localVarID);//변수정보를 담은 varInfo객체를 만든다.
		_localVarID++;									//한번 만들었으니까 +1해줘서 변수들끼리 겹치지 않게 해준다.
		_lsymtable.put(varname, varInfo);				//table안에 넣어줌.
	}
	
	void putGlobalVar(String varname, Type type){
		//<Fill here>
		VarInfo varInfo = new VarInfo(type, _globalVarID);
		_globalVarID++;
		_gsymtable.put(varname, varInfo);
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		VarInfo varInfo = new VarInfo(type,_localVarID,initVar);
		_localVarID++;
		_lsymtable.put(varname, varInfo);
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		VarInfo varInfo = new VarInfo(type,_globalVarID,initVar);
		_globalVarID++;
		_gsymtable.put(varname, varInfo);
	}
	
	void putParams(MiniCParser.ParamsContext params) {//파라미터도 local table에 저장한다.
		for(int i = 0; i < params.param().size(); i++) {
		//<Fill here>
			VarInfo varInfo = new VarInfo(Type.INT,_localVarID);
			_localVarID++;
			_lsymtable.put(getParamName(params.param().get(i)), varInfo);
			//_fsymtable.put
		}
	}
	
	private void initFunTable() {//함수의 정보를 넣은 table. _print가 인식되면 java/io/PrintStream/println(I)V로 출력함.
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";
		
		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}
	
	public String getFunSpecStr(String fname) {	//initFunTable에서 넣어준 함수정보를 함수이름으로 찾아서 리턴해줌.
		// <Fill here>
		String funSpecStr = _fsymtable.get(fname).sigStr;
		return funSpecStr;
	}

	public String getFunSpecStr(Fun_declContext ctx) {//initFunTable에서 넣어준 함수정보를 함수이름으로 찾아서 리턴해줌. 
		// <Fill here>	
		String funSpecStr = _fsymtable.get(getFunName(ctx)).sigStr;
		return funSpecStr;
	}
	
	public String putFunSpecStr(Fun_declContext ctx) {//만든 static 함수의 정보를 함수 table에 저장. 
		String fname = getFunName(ctx);
		String argtype = getParamTypesText(ctx.params());//함수 parameter타입
		String rtype = getTypeText(ctx.type_spec());//함수 return타입
		String res = "";
		
		// <Fill here>	
		res =  fname + "(" + argtype + ")" + rtype;
		
		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);
		
		return res;
	}

	String getVarId(String name){//변수의 이름으로 지역,전역변수 table을 검사해서 그 변수가 있으면 변수의 ID를 리턴.
		// <Fill here>	
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if(lvar != null) {
			return Integer.toString(lvar.id);
		}
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return Integer.toString(gvar.id);
		}
		return Type.ERROR+"";
	}
	
	Type getVarType(String name){//변수의 이름으로 지역,전역변수 table을 검사해서 그 변수가 있으면 변수의 type을 리턴.
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return lvar.type;
		}
		
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return gvar.type;
		}
		
		return Type.ERROR;	
	}
	String newLabel() {//loop문이나 condition문에서 사용하는 label을 리턴. 한번 리턴할때마다 ++해서 겹치지 않게 해준다.
		return "label" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {//변수의 이름으로 전역변수 table을 검사해서 그 변수가 있으면 변수의 ID를 리턴.-> getVarId이용
		// <Fill here>	
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {//변수의 이름으로 지역변수 table을 검사해서 그 변수가 있으면 변수의 ID를 리턴.-> getVarId이용
		String sname = "";
		//System.out.println(ctx.IDENT().toString());
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
	
}
