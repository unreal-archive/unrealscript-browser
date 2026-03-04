package org.unrealarchive.scriptbrowser.listeners;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class UnrealScriptErrorListener extends BaseErrorListener {

	public static final UnrealScriptErrorListener INSTANCE = new UnrealScriptErrorListener();

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
							RecognitionException e) {
		// no-op?
	}
}
