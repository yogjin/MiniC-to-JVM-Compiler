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
	private Map<String, MethodInfo> _msymtable = new HashMap<>(); //ȣ���� �޼ҵ�.
	
	void putMethodCall(String currentMethodName, String calledMethodName) {
		if(_msymtable.containsKey(currentMethodName)) {
			_msymtable.get(currentMethodName).callCount++;
		}
		else {
			MethodInfo methodInfo = new MethodInfo(calledMethodName, 1);
			_msymtable.put(currentMethodName, methodInfo);
		}
	}
	
	//table�� ���� �������� ID. ���������� ����. table�� ���������� ID�� ++�ؼ� �����鳢�� ���� ��ġ���ʰ� ���ش�.
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
		VarInfo varInfo = new VarInfo(type, _localVarID);//���������� ���� varInfo��ü�� �����.
		_localVarID++;									//�ѹ� ��������ϱ� +1���༭ �����鳢�� ��ġ�� �ʰ� ���ش�.
		_lsymtable.put(varname, varInfo);				//table�ȿ� �־���.
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
	
	void putParams(MiniCParser.ParamsContext params) {//�Ķ���͵� local table�� �����Ѵ�.
		for(int i = 0; i < params.param().size(); i++) {
		//<Fill here>
			VarInfo varInfo = new VarInfo(Type.INT,_localVarID);
			_localVarID++;
			_lsymtable.put(getParamName(params.param().get(i)), varInfo);
			//_fsymtable.put
		}
	}
	
	private void initFunTable() {//�Լ��� ������ ���� table. _print�� �νĵǸ� java/io/PrintStream/println(I)V�� �����.
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";
		
		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}
	
	public String getFunSpecStr(String fname) {	//initFunTable���� �־��� �Լ������� �Լ��̸����� ã�Ƽ� ��������.
		// <Fill here>
		String funSpecStr = _fsymtable.get(fname).sigStr;
		return funSpecStr;
	}

	public String getFunSpecStr(Fun_declContext ctx) {//initFunTable���� �־��� �Լ������� �Լ��̸����� ã�Ƽ� ��������. 
		// <Fill here>	
		String funSpecStr = _fsymtable.get(getFunName(ctx)).sigStr;
		return funSpecStr;
	}
	
	public String putFunSpecStr(Fun_declContext ctx) {//���� static �Լ��� ������ �Լ� table�� ����. 
		String fname = getFunName(ctx);
		String argtype = getParamTypesText(ctx.params());//�Լ� parameterŸ��
		String rtype = getTypeText(ctx.type_spec());//�Լ� returnŸ��
		String res = "";
		
		// <Fill here>	
		res =  fname + "(" + argtype + ")" + rtype;
		
		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);
		
		return res;
	}

	String getVarId(String name){//������ �̸����� ����,�������� table�� �˻��ؼ� �� ������ ������ ������ ID�� ����.
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
	
	Type getVarType(String name){//������ �̸����� ����,�������� table�� �˻��ؼ� �� ������ ������ ������ type�� ����.
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
	String newLabel() {//loop���̳� condition������ ����ϴ� label�� ����. �ѹ� �����Ҷ����� ++�ؼ� ��ġ�� �ʰ� ���ش�.
		return "label" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {//������ �̸����� �������� table�� �˻��ؼ� �� ������ ������ ������ ID�� ����.-> getVarId�̿�
		// <Fill here>	
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {//������ �̸����� �������� table�� �˻��ؼ� �� ������ ������ ������ ID�� ����.-> getVarId�̿�
		String sname = "";
		//System.out.println(ctx.IDENT().toString());
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
	
}
