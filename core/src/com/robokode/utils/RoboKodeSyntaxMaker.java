package com.robokode.utils;

// RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

// Swing
import javax.swing.text.Segment;


/**
 * Adaptation de RSyntaxHighlighter pour notre syntaxe personnalisée
 */
public class RoboKodeSyntaxMaker extends AbstractTokenMaker {

	protected final String operators = "=><&!";
	protected final String separators = "()[]";
	protected final String separators2 = ".,;";			// Characters you don't want syntax highlighted but separate identifiers.
	protected final String shellVariables = "#-?$!*@_";	// Characters that are part of "$<char>" shell variables; e.g., "$_".

	private int currentTokenStart;
	private int currentTokenType;


	/**
	 * Constructor.
	 */
	public RoboKodeSyntaxMaker() {
		super();	// Initializes tokensToHighlight.
	}


	/**
	 * Checks the token to give it the exact ID it deserves before
	 * being passed up to the super method.
	 *
	 * @param segment <code>Segment</code> to get text from.
	 * @param start Start offset in <code>segment</code> of token.
	 * @param end End offset in <code>segment</code> of token.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which the token occurs.
	 */
	@Override
	public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {

		switch (tokenType) {
			// Since reserved words, functions, and data types are all passed into here
			// as "identifiers," we have to see what the token really is...
			case Token.IDENTIFIER:
				int value = wordsToHighlight.get(segment, start,end);
				if (value!=-1)
					tokenType = value;
				break;
			case Token.WHITESPACE:
			case Token.SEPARATOR:
			case Token.OPERATOR:
			case Token.LITERAL_NUMBER_DECIMAL_INT:
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
			case Token.LITERAL_CHAR:
			case Token.LITERAL_BACKQUOTE:
			case Token.COMMENT_EOL:
			case Token.PREPROCESSOR:
			case Token.VARIABLE:
				break;

			default:
				tokenType = Token.IDENTIFIER;
				break;

		}

		super.addToken(segment, start, end, tokenType, startOffset);

	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getLineCommentStartAndEnd(int languageIndex) {
	    // Le # est commentaire dans notre langage
		return new String[] { "#", null };
	}


	/**
	 * Returns whether tokens of the specified type should have "mark
	 * occurrences" enabled for the current programming language.
	 *
	 * @param type The token type.
	 * @return Whether tokens of this type should have "mark occurrences"
	 *         enabled.
	 */
	@Override
	public boolean getMarkOccurrencesOfTokenType(int type) {
		return type== Token.IDENTIFIER || type== Token.VARIABLE;
	}


	/**
	 * Returns the words to highlight
	 *
	 * @return A <code>TokenMap</code> containing the words to highlight for
	 *         UNIX shell scripts.
	 * @see AbstractTokenMaker#getWordsToHighlight
	 */
	@Override
	public TokenMap getWordsToHighlight() {

		TokenMap tokenMap = new TokenMap();

		// Définition des mots du langage "réservés"
		int reservedWord = Token.RESERVED_WORD;
		//
		tokenMap.put("tantque",				reservedWord);
		tokenMap.put("fintantque",				reservedWord);
		//
		tokenMap.put("si",				reservedWord);
		tokenMap.put("sinon",				reservedWord);
		tokenMap.put("finsi",				reservedWord);
		//
		tokenMap.put("boucleInfinie",				reservedWord);
		tokenMap.put("finboucle",				reservedWord);
		//
		tokenMap.put("pour",				reservedWord);
		tokenMap.put("finpour",				reservedWord);

		// Définition des fonctions du langage
		int function = Token.FUNCTION;
		tokenMap.put("deplacer",			function);
		tokenMap.put("attaquer",			function);
		tokenMap.put("detecterEnnemi",			function);
		tokenMap.put("estVivant",			function);
		tokenMap.put("recharger",			function);

		// Définition des suffixes des fonctions
		int suffix = Token.LITERAL_BOOLEAN;
		tokenMap.put("melee",			suffix);
		tokenMap.put("tirer",			suffix);

		// Définition des énumérés
        int nenum = Token.DATA_TYPE;
        tokenMap.put("HAUTDROIT",			nenum);
        tokenMap.put("HAUTGAUCHE",			nenum);
        tokenMap.put("BASGAUCHE",			nenum);
        tokenMap.put("BASDROIT",			nenum);

		return tokenMap;
	}


	/**
	 * Returns a list of tokens representing the given text.
	 *
	 * @param text The text to break into tokens.
	 * @param startTokenType The token with which to start tokenizing.
	 * @param startOffset The offset at which the line of tokens begins.
	 * @return A linked list of tokens representing <code>text</code>.
	 */
	@Override
	public Token getTokenList(Segment text, int startTokenType, final int startOffset) {

		resetTokenList();

		char[] array = text.array;
		int offset = text.offset;
		int count = text.count;
		int end = offset + count;

		// See, when we find a token, its starting position is always of the form:
		// 'startOffset + (currentTokenStart-offset)'; but since startOffset and
		// offset are constant, tokens' starting positions become:
		// 'newStartOffset+currentTokenStart' for one less subraction operation.
		int newStartOffset = startOffset - offset;

		currentTokenStart = offset;
		currentTokenType  = startTokenType;
		boolean backslash = false;

//beginning:
		for (int i=offset; i<end; i++) {

			char c = array[i];

			switch (currentTokenType) {

				case Token.NULL:

					currentTokenStart = i;	// Starting a new token here.

					switch (c) {

						case ' ':
						case '\t':
							currentTokenType = Token.WHITESPACE;
							break;

						case '`':
							if (backslash) { // Escaped back quote => call '`' an identifier..
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								backslash = false;
							}
							else {
								currentTokenType = Token.LITERAL_BACKQUOTE;
							}
							break;

						case '"':
							if (backslash) { // Escaped double quote => call '"' an identifier..
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								backslash = false;
							}
							else {
								currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							}
							break;

						case '#':
							backslash = false;
							currentTokenType = Token.COMMENT_EOL;
							break;

						default:
							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
								break;
							}
							else if (RSyntaxUtilities.isLetter(c) || c=='/' || c=='_') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}
							int indexOf = operators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.OPERATOR, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.SEPARATOR, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
							else {
								currentTokenType = Token.IDENTIFIER;
								break;
							}

					} // End of switch (c).

					break;

				case Token.WHITESPACE:

					switch (c) {

						case ' ':
						case '\t':
							break;	// Still whitespace.

						case '"': // Don't need to worry about backslashes as previous char is space.
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							backslash = false;
							break;

						case '#':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.COMMENT_EOL;
							break;

						default:	// Add the whitespace token and start anew.

							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;

							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
								break;
							}
							else if (RSyntaxUtilities.isLetter(c) || c=='/' || c=='_') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}
							int indexOf = operators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							else {
								currentTokenType = Token.IDENTIFIER;
							}

					} // End of switch (c).

					break;

				default: // Should never happen
				case Token.IDENTIFIER:

					switch (c) {

						case ' ':
						case '\t':
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;

						case '"': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							backslash = false;
							break;

						case '=': // Special case here; when you have "identifier=<value>" in shell, "identifier" is a variable.
							addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
							currentTokenType = Token.NULL;
							break;

						default:
							if (RSyntaxUtilities.isLetterOrDigit(c) || c=='/' || c=='_') {
								break;	// Still an identifier of some type.
							}
							int indexOf = operators.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							// Otherwise, we're still an identifier (?).

					} // End of switch (c).

					break;

				case Token.LITERAL_NUMBER_DECIMAL_INT:

					switch (c) {

						case ' ':
						case '\t':
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;

						case '"': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							backslash = false;
							break;

						default:

							if (RSyntaxUtilities.isDigit(c)) {
								break;	// Still a literal number.
							}
							int indexOf = operators.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}

							// Otherwise, remember this was a number and start over.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							i--;
							currentTokenType = Token.NULL;

					} // End of switch (c).

					break;

				case Token.VARIABLE:

					// Note that we first arrive here AFTER the '$' character.
					// First check if the variable name is enclosed in '{' and '}' characters.
//					if (c=='{') {
//						while (++i<end) {
//							if (array[i]=='}') {
//								addToken(text, currentTokenStart,i, Token.VARIABLE, newStartOffset+currentTokenStart);
//								currentTokenType = Token.NULL;
//								break;
//							}
//						} // End of while (++i<end).
//						if (i==end) { // Happens when '}' wasn't found...
//							addToken(text, currentTokenStart,end-1, Token.VARIABLE, newStartOffset+currentTokenStart);
//							currentTokenType = Token.NULL;
//						}
//						break;
//					} // End of if (i<end-1 && array[i+1]=='{').

					// If we didn't find the '{' character, find the end of the variable...
					while (i<end) {
						c = array[i];	// Not needed the first iteration, but can't think of a better way to do it...
						if (!RSyntaxUtilities.isLetterOrDigit(c) && shellVariables.indexOf(c)==-1 && c!='_') {
							addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
							i--;
							currentTokenType = Token.NULL;
							break;
						}
						i++;
					}

					// This only happens if we never found the end of the variable in the loop above.
					if (i==end) {
						addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
						currentTokenType = Token.NULL;
					}

					break;

				case Token.COMMENT_EOL:
					// If we got here, then the line != "#" only, so check for "#!".
					if (c=='!')
						currentTokenType = Token.PREPROCESSOR;
					i = end - 1;
					addToken(text, currentTokenStart,i, currentTokenType, newStartOffset+currentTokenStart);
					// We need to set token type to null so at the bottom we don't add one more token.
					currentTokenType = Token.NULL;

					break;

				case Token.LITERAL_CHAR:

                        backslash = false; // Need to set backslash to false here as a character was typed.
						// Otherwise, we're still an unclosed char literal...

						break;

				case Token.LITERAL_STRING_DOUBLE_QUOTE:

						switch (c) {

							case '"':
								if (!backslash) {
									addToken(text, currentTokenStart,i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset+currentTokenStart);
									currentTokenType = Token.NULL;
									// backslash is definitely false when we leave.
									break;
								}
								backslash = false;
								break;

							// Otherwise, we're still in an unclosed string...
							default:
								backslash = false; // Need to set backslash to false here as a character was typed.

						} // End of switch (c).

						break;

			} // End of switch (currentTokenType).

		} // End of for (int i=offset; i<end; i++).

		switch (currentTokenType) {

			// Remember what token type to begin the next line with.
			case Token.LITERAL_BACKQUOTE:
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
			case Token.LITERAL_CHAR:
						addToken(text, currentTokenStart,end-1, currentTokenType, newStartOffset+currentTokenStart);
						break;

			// Do nothing if everything was okay.
			case Token.NULL:
						addNullToken();
						break;

			// All other token types don't continue to the next line...
			default:
						addToken(text, currentTokenStart,end-1, currentTokenType, newStartOffset+currentTokenStart);
						addNullToken();

		}

		// Return the first token in our linked list.
		return firstToken;

	}
}