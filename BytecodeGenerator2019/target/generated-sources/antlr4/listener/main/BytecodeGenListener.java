package listener.main;

import java.util.Hashtable;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.sun.org.apache.xerces.internal.parsers.SecurityConfiguration;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.ProgramContext;
import generated.MiniCParser.StmtContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

import static listener.main.BytecodeGenListenerHelper.*;
import static listener.main.SymbolTable.*;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();
	
	int tab = 0;
	int label = 0;
	
	// program	: decl+
	
	//fun_decl	: type_spec IDENT '(' params ')' compound_stmt ;
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) { //함수정보와 파라미터 정보를 symbolTable에 저장.
		symbolTable.initFunDecl();
		
		String fname = getFunName(ctx);//함수이름.
		ParamsContext params;
		
		if (fname.equals("main")) {
			symbolTable.putLocalVar("args", Type.INTARRAY);
		} else {
			symbolTable.putFunSpecStr(ctx);
			params = (MiniCParser.ParamsContext) ctx.getChild(3);
			symbolTable.putParams(params);
		}
		
		setCurrentMethodName(fname);//현재 메소드
	}
		
	
	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		
		if (isArrayDecl(ctx)) {
			symbolTable.putGlobalVar(varName, Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) {
			symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
		}
		else  { // simple decl
			symbolTable.putGlobalVar(varName, Type.INT);
		}
	}

	
	@Override
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {			
		if (isArrayDecl(ctx)) {
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) {
			symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));	
		}
		else  { // simple decl
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
		}	
	}

	//program	: decl+			
	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		String classProlog = getFunProlog();//JVM 첫부분. class public Text... 
		
		String fun_decl = "", var_decl = "";
		
		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(isFunDecl(ctx, i))
				fun_decl += newTexts.get(ctx.decl(i).fun_decl());
			else
				var_decl += newTexts.get(ctx.decl(i).var_decl());
		}
		
		newTexts.put(ctx, classProlog + var_decl + fun_decl);
		
		System.out.println(newTexts.get(ctx));
		
	}	
	
	
	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1)
		{
			if(ctx.var_decl() != null)				//var_decl
				decl += newTexts.get(ctx.var_decl());
			else							//fun_decl
				decl += newTexts.get(ctx.fun_decl());
		}
		newTexts.put(ctx, decl);
	}
	
	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)
		{
			if(ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());
			else if(ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());
			// <(0) Fill here!>		
			else if(ctx.if_stmt() != null)			// if_stmt
				stmt += newTexts.get(ctx.if_stmt());
			
			else if(ctx.while_stmt() != null)		// while_stmt
				stmt += newTexts.get(ctx.while_stmt());
			
			else if(ctx.return_stmt() != null)		// return_stmt
				stmt += newTexts.get(ctx.return_stmt());
		}
		newTexts.put(ctx, stmt);
	}
	
	// expr_stmt	: expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() == 2)
		{
			stmt += newTexts.get(ctx.expr());	// expr
		}
		newTexts.put(ctx, stmt);
	}
	
	
	// while_stmt	: WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
			// <(1) Fill here!>
		String stmt = "";
		String condExpr = newTexts.get(ctx.expr());
		String thenStmt = "";
		if(ctx.stmt() != null) {
			thenStmt = newTexts.get(ctx.stmt());
		}
		String lend = symbolTable.newLabel();		//while첫부분
		String lendElse = symbolTable.newLabel();	//while이 끝나고 바깥부분
		
		stmt += lend + ": "	+"\n"				//while문 첫부분
				+ condExpr +"\n"					//while문 처음 조건문 부터 검사.
				+ "ifeq " + lendElse + "\n"	//조건이 거짓이면 lendElse로.
				+ thenStmt +"\n"				//조건이 참일시
				+ "goto " + lend + "\n"
				+ lendElse + ": "+"\n";			//while 바깥으로 점프
				
		newTexts.put(ctx, stmt);
	}
	
	//fun_decl	: type_spec IDENT '(' params ')' compound_stmt ;
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
			// <(2) Fill here!>
		String stmt = "";
		stmt += funcHeader(ctx,getFunName(ctx));
		stmt += newTexts.get(ctx.compound_stmt());
		stmt += ".end method\n";
		newTexts.put(ctx, stmt);
	}
	

	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"	
				+ "\t" + ".limit stack "	+ getStackSize(ctx) + "\n"
				+ "\t" + ".limit locals " 	+ getLocalVarSize(ctx) + "\n";
				 	
	}
	
	
	
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		String varDecl = "";
		
		if (isDeclWithInit(ctx)) {
			varDecl += "putfield " + varName + "\n";  
			// v. initialization => Later! skip now..: 
		}
		newTexts.put(ctx, varDecl);
	}
	
	//local_decl	: type_spec IDENT ';'
	//				| type_spec IDENT '=' LITERAL ';'	
	//				| type_spec IDENT '[' LITERAL ']' ';'	
	
	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";
		
		if (isDeclWithInit(ctx)) {
			//symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));
			//(안 그러면 같은 변수가 테이블에 두 번 들어감. 이것도 당장 문제는 없음. 왜냐하면 같은 변수에 대해 데이터를 덮어쓰므로, 결국 나중 것만 의미있으므로)
			String vId = symbolTable.getVarId(ctx);
			varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
					+ "istore_" + vId + "\n"; 			
		}
		//System.out.println(varDecl);
		newTexts.put(ctx, varDecl);
	}

	
	// compound_stmt	: '{' local_decl* stmt* '}'
	
	// local_decl		: type_spec IDENT ';'
	//					| type_spec IDENT '=' LITERAL ';'	
	//					| type_spec IDENT '[' LITERAL ']' ';'	
	
	//stmt		: expr_stmt			
	//			| compound_stmt			
	//			| if_stmt			
	//			| while_stmt			
	//			| return_stmt	
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		// <(3) Fill here>
		String stmt = "";
		
		for(int i = 0; i < ctx.local_decl().size(); i++) {
			stmt += newTexts.get(ctx.local_decl(i));
		}
		for(int i = 0; i < ctx.stmt().size(); i++) {
			stmt += newTexts.get(ctx.stmt(i));
		}
		//System.out.println(stmt);
		newTexts.put(ctx, stmt);
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		String condExpr= newTexts.get(ctx.expr());//if(x==3)에서 x==3에 해당.
		String thenStmt = newTexts.get(ctx.stmt(0));
		
		String lend = symbolTable.newLabel();
		String lendElse = symbolTable.newLabel();
		
		//System.out.println("thenStmt :" + thenStmt);
		if(noElse(ctx)) {		
			stmt += condExpr
					+ "ifeq " + lend + "\n"//참(1)이면 thenStmt로. 아니면 lend로 점프
					+ thenStmt + "\n"
					+ lend + ": " + "\n";	
		}
		else {//condExpr이 참이면 스택에 1넣는다.
			String elseStmt = newTexts.get(ctx.stmt(1));
			stmt += condExpr + "\n"
					+ "ifeq " + lendElse + "\n"
					+ thenStmt + "\n"
					+ "goto " + lend + "\n"
					+ lendElse + ": " + "\n" + elseStmt + "\n"
					+ lend + ": " + "\n";	
			//System.out.println("lendElse : "+ lendElse +"\nElseStmt : "+ elseStmt);
		}
		
		newTexts.put(ctx, stmt);
	}
	
	
	// return_stmt	: RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
			// <(4) Fill here>
		String stmt = "";
		if(ctx.getChildCount() == 2) {
			stmt += "return\n";
		}
		else if(ctx.getChildCount() == 3) {//exitExpr부분 참고.바로밑.
			stmt += newTexts.get(ctx.expr());
			stmt += "ireturn\n";
		}
		newTexts.put(ctx, stmt);
	}

	
	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";

		if(ctx.getChildCount() <= 0) {
			newTexts.put(ctx, ""); 
			return;
		}		
		
		if(ctx.getChildCount() == 1) { // IDENT | LITERAL
			if(ctx.IDENT() != null) {
				String idName = ctx.IDENT().getText();
				if(symbolTable.getVarType(idName) == Type.INT) {
					expr += "iload_" + symbolTable.getVarId(idName) + " \n";
				}
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
				} else if (ctx.LITERAL() != null) {
					String literalStr = ctx.LITERAL().getText();
					expr += "ldc " + literalStr + " \n";
				}
			} else if(ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);	//newTexts.get(ctx)가 null인 경우 handleUnaryExpr안에서 null인지 판단해야함.		
		}
		else if(ctx.getChildCount() == 3) {	 
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));
				
			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				expr = newTexts.get(ctx.expr(0))
						+ "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";
				
			} else { 											// binary operation
				expr = handleBinExpr(ctx, expr);
				
			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else { // expr
				// Arrays: TODO  
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
		}
		newTexts.put(ctx, expr);
	}


	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		String l1 = symbolTable.newLabel();
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		//if(expr.equals("null")) {
			expr = "";	//expr이 null이면 ""로 초기화해줌.
		//}
		//if(ctx.expr(0))
			String vId = symbolTable.getVarId(ctx.expr(0).getText()); //unaryExpr의 결과를 저장할 id를 가져온다. (istore_vId)
			
		expr += newTexts.get(ctx.expr(0));
		switch(ctx.getChild(0).getText()) {
		case "-":								//int sum = -10;과 같은건 안됨. g4파일에 var_decl보면 LITERAL만 받기때문. 
			expr += "           ineg \n"; 		//MiniC는  int sum; sum = -5050; 으로 사용
			break;
		case "--":
			expr += "ldc 1" + "\n"				//스택에 1넣고
					+ "isub" + "\n"				//빼준 뒤 
					+ "istore_" + vId + "\n";	//그 값을 다시 스택에 저장.
			break;
		case "++":
			expr += "ldc 1" + "\n"				//스택에 1넣고
					+ "iadd" + "\n"				//더한뒤 
					+ "istore_" + vId + "\n";   //그 값을 다시 스택에 저장.
		
			break;
		case "!":
			expr += "ifeq " + l2 + "\n"
					+ l1 + ": " + "\n"+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": " + "\n"+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		}
		return expr;
	}

/*	expr	: 	| expr '*' expr				 
				| expr '/' expr				 
				| expr '%' expr				 
				| expr '+' expr				 
				| expr '-' expr				 
				| expr EQ expr		==		
				| expr NE expr		!=	 
				| expr LE expr		<=		 
				| expr '<' expr				 
				| expr GE expr		>=	 
				| expr '>' expr				 				 
				| expr AND expr		&&		 
				| expr OR expr		||	
				*/
	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {//binary Expression
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		expr += newTexts.get(ctx.expr(0));
		expr += newTexts.get(ctx.expr(1));
		
		switch (ctx.getChild(1).getText()) {
			case "*":
				expr += "imul \n"; break;
			case "/":
				expr += "idiv \n"; break;
			case "%":
				expr += "irem \n"; break;
			case "+":		// expr(0) expr(1) iadd
				expr += "iadd \n"; break;
			case "-":
				expr += "isub \n"; break;
				
			case "==":
				expr += "isub " + "\n"					//스택에 있는 두 값을 빼서
						+ "ifeq "+ l2 + "\n"			//ifeq : 스택에 있는값과 0을 비교후 같으면 l2로 점프. 같으면 뒤에서 스택에 1넣음. 다르면 0넣음
						+ "ldc 0" + "\n"				//다르면 0넣음
						+ "goto " + lend + "\n"			//lend:0넣고 if_stmt나 while같은곳으로 goto
						+ l2 + ": "+ "\n" + "ldc 1" + "\n"	//같으면 1넣고 lend로
						+ lend + ": " + "\n";					//lend 내용(if 나 while문)
				break;
			case "!=":
				expr += "isub " + "\n"
						+ "ifne "+ l2 + "\n"			//ifne : 스택에 있는값과 0을 비교후 다르면 l2로 점프
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": "+ "\n" + "ldc 1" + "\n"
						+ lend + ": "+ "\n";
				break;
			case "<="://위에서 ifne만 바꿔보자 LE
				// <(5) Fill here>
				expr += "isub " + "\n"
						+ "ifle "+ l2 + "\n"			//ifle : 스택에 있는값이 0보다 작거나 같으면 l2로 점프
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": "+ "\n" + "ldc 1" + "\n"
						+ lend + ": "+ "\n";
				break;
			case "<":
				// <(6) Fill here>
				expr += "isub " + "\n"
						+ "iflt "+ l2 + "\n"			//iflt : 스택에 있는값이 0보다 작으면 l2로 점프
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n" + "ldc 1" + "\n"
						+ lend + ": "+ "\n";
				break;

			case ">=":
				// <(7) Fill here>
				expr += "isub " + "\n"
						+ "ifge "+ l2 + "\n"			//ifge : 스택에 있는값이 0보다 크거나 같으면 l2로 점프
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n"+ "ldc 1" + "\n"
						+ lend + ": "+ "\n";
				break;

			case ">":
				// <(8) Fill here>
				expr += "isub " + "\n"
						+ "ifgt "+ l2 + "\n"			//ifgt : 스택에 있는값이 0보다 크면 l2로 점프
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n" + "ldc 1" + "\n"
						+ lend + ": "+ "\n";
				break;

			case "and":
				expr +=  "ifne "+ lend + "\n"			//첫번째 조건 결과가 참이 아니면
						+ "pop" + "\n" + "ldc 0" + "\n"	//스택에 거짓임을 뜻하는 0을넣고
						+ lend + ": "+ "\n"; 			//보내주기
				break;
			case "or":
				// <(9) Fill here>
				expr +=  "ifeq "+ lend + "\n"			//첫번째 조건 결과가 참이면 뒤에거 볼 필요도 없이
						+ "pop" + "\n" + "ldc 1" + "\n"	//스택에 참임을 뜻하는 1을 넣고
						+ lend + ": "+ "\n"; 			//보내주기.
				break;

		}
		return expr;
	}
	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {//함수처리.
		String fname = getFunName(ctx);		

		if (fname.equals("_print")) {		// System.out.println	
			expr = "getstatic java/lang/System/out Ljava/io/PrintStream; "
			  		+ newTexts.get(ctx.args()) 
			  		+ "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
		} else {	
			expr = newTexts.get(ctx.args()) 
					+ "invokestatic " + getCurrentClassName()+ "/" + symbolTable.getFunSpecStr(fname) + "\n";
			
			expr += "현재 메소드 : " + getCurrentMethodName() +"\n" + "호출된 함수 : " + fname + "\n";
			symbolTable.putMethodCall(getCurrentMethodName(), fname);
		}	
		
		return expr;
			
	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {//args처리.

		String argsStr = "\n";
		
		for (int i=0; i < ctx.expr().size() ; i++) {
			argsStr += newTexts.get(ctx.expr(i)) ; 
		}		
		newTexts.put(ctx, argsStr);
	}

}
