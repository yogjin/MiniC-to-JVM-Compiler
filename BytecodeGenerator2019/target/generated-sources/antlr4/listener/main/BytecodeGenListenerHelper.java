package listener.main;

import java.util.Hashtable;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable;
import listener.main.SymbolTable.VarInfo;

public class BytecodeGenListenerHelper {
	
	// <boolean functions>
	
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}
	
	// type_spec IDENT '[' ']'
	static boolean isArrayParamDecl(ParamContext param) {//이번에는 array포함안함.
		return param.getChildCount() == 4;
	}
	
	// global vars
	static int initVal(Var_declContext ctx) {//선언할때 LITERAL의 값을 리턴
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	// <local vars>
	// local_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static int initVal(Local_declContext ctx) {//선언할때 LITERAL의 값을 리턴
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6;
	}
	
	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	
	/*type_spec	: VOID				
				| INT
				
	  fun_decl	: type_spec IDENT '(' params ')' compound_stmt ;
	*/
	static boolean isVoidF(Fun_declContext ctx) {//함수 타입이 void인 경우.
			// <Fill in>
		return ctx.type_spec().getText().equals("VOID");
	}
	//return_stmt	: RETURN ';'			
	//				| RETURN expr ';'
	static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {//int리턴하면 자식개수 3
		return ctx.getChildCount() ==3;
	}


	static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {// void -> childCount == 2
		return ctx.getChildCount() == 2;
	}
	
	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}
	static String getLocalVarSize(Fun_declContext ctx) {
		return "32";
	}
	static String getTypeText(Type_specContext typespec) {
			// <Fill in>
		String typeText = "";
		if(typespec.getText().equals("int")) {//int의 경우 I로 바꿔줌. -> JVM에서 인식가능하게
			typeText = "I";
		}
//		else if(typespec.getText() == "char") {//char을 추가하는경우.. MiniC에는 char없음.
//			...추가.
//		}
		return typeText;
	}

	// param		: type_spec IDENT		
	//				| type_spec IDENT '[' ']'	;
	static String getParamName(ParamContext param) {//파라미터 이름 리턴
		// <Fill in>
		return param.IDENT().toString();
	}
	
	static String getParamTypesText(ParamsContext params) {//무슨 타입인지 String으로 반환.-> getTypeText -> int인 경우 I로 리턴받음. func(II)I
		String typeText = "";
		
		for(int i = 0; i < params.param().size(); i++) {
			MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext)  params.param(i).getChild(0);
			typeText += getTypeText(typespec); // + ";";
		}
		return typeText;
	}
	
	//local_decl	: type_spec IDENT ';'
	//				| type_spec IDENT '=' LITERAL ';'	
	//				| type_spec IDENT '[' LITERAL ']' ';'	;
	static String getLocalVarName(Local_declContext local_decl) {//지역변수이름 리턴
		// <Fill in>
		return local_decl.IDENT().toString();		
	}
	
	//fun_decl	: type_spec IDENT '(' params ')' compound_stmt ;
	static String getFunName(Fun_declContext ctx) {//함수이름 리턴
		// <Fill in>
		return ctx.IDENT().toString();
	}
	
	static String getFunName(ExprContext ctx) {//함수이름 리턴
		// <Fill in>
		
		return ctx.IDENT().toString();
	}
	
	static boolean noElse(If_stmtContext ctx) {//else가 없는 if문인지 판별
		return ctx.getChildCount() <= 5;
	}
	
	static String getFunProlog() {	//JVM 처음 시작부분. Test class로 생성. 생성자부분.
		String Prolog = ".class public Test\n"
				+ ".super java/lang/Object\n"
				+ "; strandard initializer\n"
				+ ".method public <init>()V\n"
				+ "aload_0\n"
				+ "invokenonvirtual java/lang/Object/<init>()V\n"
				+ "return\n"
				+ ".end method\n";
		return Prolog;
	}
	
	static String getCurrentClassName() {
		return "Test";
	}
}
