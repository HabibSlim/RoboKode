package com.robokode.interpreter;

import com.robokode.utils.Direction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
** Interpréteur adapté de Jasic (Bob Nystrom) permettant d'analyser et d'exécuter un script RoboKode
 */
public class Interpreter {

    // Liste contenant l'ensemble des erreurs syntaxiques repérées à l'analyse
    private ArrayList<String[]> errMsg;

    // Booléen signalant la présence d'erreurs dans le code
    private boolean flagErrors;

    /** Interactions avec le contrôleur **/
    // Booléen signalant la nécessité de pauser l'exécution du script
    private boolean pauseFlag;
    private enum PauseState {
        /** Actions admises **/
        DEPLACEMENT, MELEE, TIRER, DETECTERENNEMI, ESTVIVANT, RECHARGER,

        /** Erreurs à l'exécution **/
        RUNTIME_DIV0, RUNTIME_UNDEF, RUNTIME_LOOP
        // énuméré décrivant la raison pour laquelle le script est pausé
    }
    private PauseState pauseState;
    private Direction dirJ, dirVar;
    private int boolVal;

    // Liste des statements du programme parsé
    private List<Statement> instructions;

    // Limite d'instructions à exécuter (hard codée)
    private static final int INSTR_LIMIT = 500;

    // Compteur du nombre d'instructions exécutées
    private int nbInstr = 0;

   // Tokenizing (lexing) -----------------------------------------------------
    
    /**
     * This function takes a script as a string of characters and chunks it into
     * a sequence of tokens. Each token is a meaningful unit of program, like a
     * variable name, a number, a string, or an operator.
     */
    private static List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<Token>();
        
        String token = "";
        TokenizeState state = TokenizeState.DEFAULT;
        
        // Many tokens are a single character, like operators and ().
        String charTokens = "\n=+-*/<>()!";
        TokenType[] tokenTypes = { TokenType.LINE, TokenType.EQUALS,
            TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR,
            TokenType.OPERATOR, TokenType.OPERATOR, TokenType.OPERATOR,
            TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN, TokenType.OPERATOR
        };
        
        // Scan through the code one character at a time, building up the list
        // of tokens.
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            switch (state) {
            case DEFAULT:
                if (charTokens.indexOf(c) != -1) {
                    tokens.add(new Token(Character.toString(c),
                        tokenTypes[charTokens.indexOf(c)]));
                } else if (Character.isLetter(c)) {
                    token += c;
                    state = TokenizeState.WORD;
                } else if (Character.isDigit(c)) {
                    token += c;
                    state = TokenizeState.NUMBER;
                } else if (c == '"') {
                    state = TokenizeState.STRING;
                } else if (c == '#') {
                    state = TokenizeState.COMMENT;
                }
                break;
                
            case WORD:
                if (Character.isLetterOrDigit(c)) {
                    token += c;
                } else if (c == ':') {
                    tokens.add(new Token(token, TokenType.LABEL));
                    token = "";
                    state = TokenizeState.DEFAULT;
                } else {
                    tokens.add(new Token(token, TokenType.WORD));
                    token = "";
                    state = TokenizeState.DEFAULT;
                    i--; // Reprocess this character in the default state.
                }
                break;
                
            case NUMBER:
                // HACK: Negative numbers and floating points aren't supported.
                // To get a negative number, just do 0 - <your number>.
                // To get a floating point, divide.
                if (Character.isDigit(c)) {
                    token += c;
                } else {
                    tokens.add(new Token(token, TokenType.NUMBER));
                    token = "";
                    state = TokenizeState.DEFAULT;
                    i--; // Reprocess this character in the default state.
                }
                break;
                
            case STRING:
                if (c == '"') {
                    tokens.add(new Token(token, TokenType.STRING));
                    token = "";
                    state = TokenizeState.DEFAULT;
                } else {
                    token += c;
                }
                break;
                
            case COMMENT:
                if (c == '\n') {
                    state = TokenizeState.DEFAULT;
                }
                break;
            }
        }
        
        // HACK: Silently ignore any in-progress token when we run out of
        // characters. This means that, for example, if a script has a string
        // that's missing the closing ", it will just ditch it.
        return tokens;
    }

    // Token data --------------------------------------------------------------

    /**
     * This defines the different kinds of tokens or meaningful chunks of code
     * that the parser knows how to consume. These let us distinguish, for
     * example, between a string "foo" and a variable named "foo".
     * 
     * HACK: A typical tokenizer would actually have unique token types for
     * each keyword (print, goto, etc.) so that the parser doesn't have to look
     * at the names, but Jasic is a little more crude.
     */
    private enum TokenType {
        WORD("Un mot (variable/instruction)"),
        NUMBER("Un nombre"),
        STRING("Une chaîne de caractères"),
        LABEL("Un label"),
        LINE("Saut de ligne"),
        EQUALS("Signe égal '='"),
        OPERATOR("Opérateur. ex : + - x "),
        LEFT_PAREN("Parenthèse gauche '('"),
        RIGHT_PAREN ("Parenthèse droite ')'"),
        EOF ("Fin du fichier");

        TokenType(String texte) {
            this.texteDesc = texte;
        }

        private String texteDesc;

        @Override
        public String toString() {
            return texteDesc;
        }
    }
    
    /**
     * This is a single meaningful chunk of code. It is created by the tokenizer
     * and consumed by the parser.
     */
    private static class Token {
        public Token(String text, TokenType type) {
            this.text = text;
            this.type = type;
        }
        
        public final String text;
        public final TokenType type;
    }
    
    /**
     * This defines the different states the tokenizer can be in while it's
     * scanning through the source code. Tokenizers are state machines, which
     * means the only data they need to store is where they are in the source
     * code and this one "state" or mode value.
     * 
     * One of the main differences between tokenizing and parsing is this
     * regularity. Because the tokenizer stores only this one state value, it
     * can't handle nesting (which would require also storing a number to
     * identify how deeply nested you are). The parser is able to handle that.
     */
    private enum TokenizeState {
        DEFAULT, WORD, NUMBER, STRING, COMMENT
    }

    // Parsing -----------------------------------------------------------------

    /**
     * Etat du parseur : définit dans quel bloc d'instruction on est situé
     */
    private enum ParserState {
        SI, TANT_QUE, DEFAULT, POUR
    }

    /**
     * This defines the Jasic parser. The parser takes in a sequence of tokens
     * and generates an abstract syntax tree. This is the nested data structure
     * that represents the series of statements, and the expressions (which can
     * nest arbitrarily deeply) that they evaluate. In technical terms, what we
     * have is a recursive descent parser, the simplest kind to hand-write.
     *
     * As a side-effect, this phase also stores off the line numbers for each
     * label in the program. It's a bit gross, but it works.
     */
    private class Parser {
        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            position = 0;
        }

        /**
         * The top-level function to start parsing. This will keep consuming
         * tokens and routing to the other parse functions for the different
         * grammar syntax until we run out of code to parse.
         * 
         * @param  labels   A map of label names to statement indexes. The
         *                  parser will fill this in as it scans the code.
         * @return          The list of parsed statements.
         */
        public List<Statement> parse(Map<String, Integer> labels) {
            List<Statement> statements = new ArrayList<Statement>();

            // On utilise une pile : ParserPair(Etat => Statement)
            statStacks = new Stack<ParserPair>();

            // On initialise l'état du parser à défault
            statStacks.push(new ParserPair(ParserState.DEFAULT, null));

            while (true) {
                // Ignore empty lines.
                while (match(TokenType.LINE));
                
                if (match(TokenType.LABEL)) {
                    // Mark the index of the statement after the label.
                    labels.put(last(1).text, statements.size());
                } else if (match(TokenType.WORD, TokenType.EQUALS)) {
                    String name = last(2).text;
                    enumVars.add(name);
                    Expression value = expression();
                    statements.add(new AssignStatement(name, value));
                } else if (match("print")) {
                    statements.add(new PrintStatement(expression()));
                } else if (match("input")) {
                    statements.add(new InputStatement(
                        consume(TokenType.WORD).text));
                } else if (match("goto")) {
                    statements.add(new GotoStatement(
                            consume(TokenType.WORD).text));

                /**   Instruction DEPLACER
                 * ===================================  **/
                } else if (match("deplacer")) {
                    // On teste la présence de parenthèses
                    consume(TokenType.LEFT_PAREN);

                    DeplacerStatement dS;

                    // Cas d'une direction passée par variable
                    if (enumVars.contains(get(0).text)) {
                        // On crée un DeplacerStatement avec le nom de la variable
                        dS = new DeplacerStatement(get(0).text);
                    } else {
                        // Cas d'une direction écrite à la main
                        // On vérifie que la chaîne suivante est un énuméré valide
                        Direction dir = Direction.strToEnum(get(0).text);
                        if (dir == null) {
                            addError("Cette direction est invalide", get(0).text, false);
                        }

                        // On créé un DeplacerStatement avec la direction ainsi créée
                        dS = new DeplacerStatement(dir);
                    }

                    // On ajoute ce statement à la liste
                    statements.add(dS);

                    // On consume la direction dans les 2 cas
                    consume(get(0).text);

                    // On teste la présence de parenthèses fermantes
                    consume(TokenType.RIGHT_PAREN);

                /**   Instruction ATTAQUER
                 * ===================================  **/
                } else if (match("attaquer")) {
                    match(".");
                    if (match("tirer")) {
                        // On teste la présence de parenthèses
                        consume(TokenType.LEFT_PAREN);

                        // Cas d'une direction passée par variable
                        if (enumVars.contains(get(0).text)) {
                            // On créé un TirerStatement avec le nom de la variable
                            TirerStatement tS = new TirerStatement(get(0).text);
                            // On ajoute ce statement à la liste
                            statements.add(tS);
                        } else {
                            // Cas d'une direction écrite à la main
                            // On vérifie que la chaîne suivante est un énuméré valide
                            Direction dir = Direction.strToEnum(get(0).text);
                            if (dir == null) {
                                addError("Cette direction est invalide", get(0).text, true);
                            }

                            // On créé un TirerStatement avec la direction ainsi créée
                            TirerStatement tS = new TirerStatement(dir);

                            // On ajoute ce statement à la liste
                            statements.add(tS);
                        }
                        // On consume la direction dans les 2 cas
                        consume(get(0).text);

                        // On teste la présence de parenthèses fermantes
                        consume(TokenType.RIGHT_PAREN);
                    } else if (match("melee")) {
                        // On teste la présence de parenthèses
                        consume(TokenType.LEFT_PAREN);

                        // On vérifie que la chaîne suivante est un énuméré valide
                        Direction dir = Direction.strToEnum(get(0).text);

                        // Si la direction est invalide, on ajoute une erreur
                        if (dir == null)
                            addError("Cette direction est invalide", get(0).text, true);

                        // On ajoute une nouvelle instruction déplacer
                        MeleeStatement mS = new MeleeStatement(dir);
                        statements.add(mS);

                        // On consume l'énuméré
                        consume(get(0).text);

                        // On teste la présence de parenthèses fermantes
                        consume(TokenType.RIGHT_PAREN);
                    }
                /** Instruction POUR
                 * ================================ */
                } else if (match("pour")) {
                    consume(TokenType.LEFT_PAREN);
                    // On récupère l'initialisation
                    match(TokenType.WORD, TokenType.EQUALS);
                    String name = last(2).text;
                    Expression value = expression();
                    AssignStatement initialisation = new AssignStatement(name, value);

                    // On enregistre la condition
                    Expression condition = expression();

                    // On récupère l'itération
                    match(TokenType.WORD, TokenType.EQUALS);
                    name = last(2).text;
                    value = expression();
                    AssignStatement iteration = new AssignStatement(name, value);

                    consume(TokenType.RIGHT_PAREN);

                    // On crée un nouveau label l_pour
                    String labelN = makeID();
                    labels.put(labelN, statements.size());

                    // On crée un nouveau statement pour
                    PourStatement pour = new PourStatement(initialisation, condition, iteration, labelN);

                    // On met à jour l'état du programme
                    ParserPair newPour = new ParserPair(ParserState.POUR, pour);
                    statStacks.push(newPour);

                    // On ajoute le statement nouvellement créé à la liste des statements
                    statements.add(pour);

                } else if (match("finpour")) {
                    if (statStacks.peek().getState() == ParserState.POUR) {
                        // On récupère l'instruction depuis la pile, sans dépiler
                        PourStatement pour = (PourStatement) statStacks.peek().getStatement();

                        // On ajoute un GotoStatement vers le pour
                        GotoStatement safeJump = new GotoStatement(pour.getPour());
                        statements.add(safeJump);

                        // On crée un label finPour, et on l'ajoute au pour
                        String labelN = makeID();
                        labels.put(labelN, statements.size());
                        pour.setFinPour(labelN);

                        // On dépile l'instruction
                        statStacks.pop();
                    } else {
                        // Il y a erreur
                        addError("Instruction inattendue", "finPour", true);
                    }
                /**   Instruction TANTQUE
                 * ===================================  **/
                } else if (match("tantque")) {
                    // On enregistre l'expression parenthésée
                    Expression condition = expression();

                    // On crée un nouveau label pour tantQue
                    String labelN = makeID();   labels.put(labelN, statements.size());

                    // On crée un nouveau statement tantque, et on l'ajoute à la pile
                    TantQueStatement tQ = new TantQueStatement(condition, labelN);

                    ParserPair newTq = new ParserPair(ParserState.TANT_QUE, tQ);
                    statStacks.push(newTq);

                    // On ajoute le statement nouvellement créé à la liste des statements
                    statements.add(tQ);

                } else if (match("fintantque")) {
                    // Si l'état du parser est bien le TANT_QUE
                    if (statStacks.peek().getState() == ParserState.TANT_QUE) {
                        // On récupère l'instruction depuis la pile, sans dépiler
                        TantQueStatement tQ = (TantQueStatement) statStacks.peek().getStatement();

                        // On ajoute un GotoStatement vers le tantque
                        GotoStatement safeJump = new GotoStatement(tQ.getTantQue());
                        statements.add(safeJump);

                        // On crée un label finTantQue, et on l'ajoute au tQ
                        String labelN = makeID();
                        labels.put(labelN, statements.size());
                        tQ.setFinTantQue(labelN);

                        // On dépile l'instruction
                        statStacks.pop();
                    } else {
                        // Il y a erreur
                        addError("Instruction inattendue", "fintantque", true);
                    }

                /**   Instruction SI\ELSE\FINSI
                 * ===================================  **/
                } else if (match("si")) {
                    // On enregistre l'expression entre parenthèses
                    Expression condition = expression();

                    // On crée un nouveau statement si (non complété), et on l'ajoute à la pile
                    IfThenStatement ifT = new IfThenStatement(condition);
                    ParserPair newIf = new ParserPair(ParserState.SI, ifT);
                    statStacks.push(newIf);

                    // On ajoute le statement nouvellement créé à la liste des statements
                    statements.add(ifT);

                } else if (match("sinon")) {
                    // Si l'état du parser est bien le SI on ajoute le else au SI courant
                    if (statStacks.peek().getState() == ParserState.SI) {
                        IfThenStatement ifT = (IfThenStatement) statStacks.peek().getStatement();

                        // On commence par ajouter un jump non conditionnel vers le finsi
                        GotoStatement safeJump = new GotoStatement();
                        statements.add(safeJump); ifT.setSafeJump(safeJump);

                        // On crée le label "jump" du else et on l'ajoute
                        String labelN = makeID();
                        labels.put(labelN, statements.size());

                        // On ajoute le label créé au si enregistré
                        // => sans l'enlever de la pile puisqu'on est pas sorti
                        ifT.setElse(labelN);
                    } else {
                        // Il y a erreur
                        addError("Instruction inattendue","else", true);
                    }
                } else if (match("finsi")) {
                    // Si l'état du parser est bien le SI on ferme l'instruction précédemment créée
                    if (statStacks.peek().getState() == ParserState.SI) {
                        // On récupère l'instruction depuis la pile, sans dépiler
                        IfThenStatement ifT = (IfThenStatement) statStacks.peek().getStatement();

                        // On crée le label "jump" du finsi
                        String labelN = makeID();
                        labels.put(labelN, statements.size());

                        // Si il y a eu un else, on relie le safeJump du else avec le finsi
                        if (ifT.hasElse()) ifT.getsafeJump().setLabel(labelN);

                        // On ajoute le label créé au si enregistré
                        ifT.setFinSi(labelN);

                        // On dépile l'instruction
                        statStacks.pop();
                    } else {
                        // Il y a erreur
                        addError("Instruction inattendue", "finsi", true);
                    }

                } else if (match("recharger")) {
                    // On crée un statement "recharger"
                    RechargerStatement rStatement = new RechargerStatement();
                    statements.add(rStatement);

                    consume(TokenType.LEFT_PAREN);
                    consume(TokenType.RIGHT_PAREN);
                // Unexpected token : on vérifie si on atteint la fin du fichier
                } else if (match(TokenType.EOF)) {
                    break;
                // Sinon on jette une exception et on passe à la ligne suivante
                } else {
                    addError("Instruction inattendue", get(0).text, true);
                    skipLine();
                }
            }
            
            return statements;
        }
        
        // The following functions each represent one grammatical part of the
        // language. If this parsed English, these functions would be named like
        // noun() and verb().
        
        /**
         * Parses a single expression. Recursive descent parsers start with the
         * lowest-precedent term and moves towards higher precedence. For Jasic,
         * binary operators (+, -, etc.) are the lowest.
         * 
         * @return The parsed expression.
         */
        private Expression expression() {
            if (match("detecterEnnemi")) {
                consume(TokenType.LEFT_PAREN);
                consume(TokenType.RIGHT_PAREN);

                return new DetecterEnnemi();

                // On enregistre la variable assignée
                // enumVars.add("");
            } else if (match("estVivant")) {
                consume(TokenType.LEFT_PAREN);
                consume(TokenType.RIGHT_PAREN);

                return new EstVivant();

            } else {
                return operator();
            }
        }
        
        /**
         * Parses a series of binary operator expressions into a single
         * expression. In Jasic, all operators have the same predecence and
         * associate left-to-right. That means it will interpret:
         *    1 + 2 * 3 - 4 / 5
         * like:
         *    ((((1 + 2) * 3) - 4) / 5)
         * 
         * It works by building the expression tree one at a time. So, given
         * this code: 1 + 2 * 3, this will:
         * 
         * 1. Parse (1) as an atomic expression.
         * 2. See the (+) and start a new operator expression.
         * 3. Parse (2) as an atomic expression.
         * 4. Build a (1 + 2) expression and replace (1) with it.
         * 5. See the (*) and start a new operator expression.
         * 6. Parse (3) as an atomic expression.
         * 7. Build a ((1 + 2) * 3) expression and replace (1 + 2) with it.
         * 8. Return the last expression built.
         * 
         * @return The parsed expression.
         */
        private Expression operator() {
            Expression expression = atomic();

            // Si l'expression lue est valide, on construit l'opérateur
            if (expression != null) {
                // Keep building operator expressions as long as we have operators.
                while (match(TokenType.OPERATOR) ||
                        match(TokenType.EQUALS)) {
                    char operator = last(1).text.charAt(0);
                    Expression right = atomic();
                    expression = new OperatorExpression(expression, operator, right);
                }
            }
            
            return expression;
        }
        
        /**
         * Parses an "atomic" expression. This is the highest level of
         * precedence and contains single literal tokens like 123 and "foo", as
         * well as parenthesized expressions.
         * 
         * @return The parsed expression.
         */
        private Expression atomic() {

            /** Test préalable sur les énumérés du language **/
            if (match("BASGAUCHE")) {
                return new DirectionValue(Direction.BG);
            } else if (match("BASDROIT")) {
                return new DirectionValue(Direction.BD);
            } else if (match("HAUTDROIT")) {
                return new DirectionValue(Direction.HD);
            } else if (match("HAUTGAUCHE")) {
                return new DirectionValue(Direction.HG);

            /** Test généraux sur les contenus possibles **/
            } else if (match(TokenType.WORD)) {
                // A word is a reference to a variable.
                return new VariableExpression(last(1).text);
            } else if (match(TokenType.NUMBER)) {
                return new NumberValue(Double.parseDouble(last(1).text));
            } else if (match(TokenType.STRING)) {
                return new StringValue(last(1).text);
            } else if (match(TokenType.LEFT_PAREN)) {
                // The contents of a parenthesized expression can be any
                // expression. This lets us "restart" the precedence cascade
                // so that you can have a lower precedence expression inside
                // the parentheses.
                Expression expression = expression();
                consume(TokenType.RIGHT_PAREN);
                return expression;
            }
            addError("Erreur",  getParserStateError(), false);
            skipLine(); // On passe à la ligne d'instructions suivante
            return null; // On renvoie NULL comme valeur par défaut
        }
        
        // The following functions are the core low-level operations that the
        // grammar parser is built in terms of. They match and consume tokens in
        // the token stream.

        /**
         *  Passe à la ligne d'instruction suivante ou atteint la fin du fichier
         *
         */
        private void skipLine() {
            while ((!match(TokenType.EOF)) && (!match(TokenType.LINE))) {
                position++;
            }
        }

        /**
         * Consumes the next two tokens if they are the given type (in order).
         * Consumes no tokens if either check fais.
         * 
         * @param  type1 Expected type of the next token.
         * @param  type2 Expected type of the subsequent token.
         * @return       True if tokens were consumed.
         */
        private boolean match(TokenType type1, TokenType type2) {
            if (get(0).type != type1) return false;
            if (get(1).type != type2) return false;
            position += 2;
            return true;
        }
        
        /**
         * Consumes the next token if it's the given type.
         * 
         * @param  type  Expected type of the next token.
         * @return       True if the token was consumed.
         */
        private boolean match(TokenType type) {
            if (get(0).type != type) return false;
            position++;
            return true;
        }
        
        /**
         * Consumes the next token if it's a word token with the given name.
         * 
         * @param  name  Expected name of the next word token.
         * @return       True if the token was consumed.
         */
        private boolean match(String name) {
            if (get(0).type != TokenType.WORD) return false;
            if (!get(0).text.equals(name)) return false;
            position++;
            return true;
        }
        
        /**
         * Consumes the next token if it's the given type. If not, throws an
         * exception. This is for cases where the parser demands a token of a
         * certain type in a certain position, for example a matching ) after
         * an opening (.
         * 
         * @param  type  Expected type of the next token.
         * @return       The consumed token.
         */
        private Token consume(TokenType type) {
            if (get(0).type != type) addError("On attendait plutôt", type+"", false);
            return tokens.get(position++);
        }
        
        /**
         * Consumes the next token if it's a word with the given name. If not,
         * throws an exception.
         * 
         * @param  name  Expected name of the next word token.
         * @return       The consumed token.
         */
        private Token consume(String name) {
            if (!match(name)) addError("On attendait plutôt", name, false);
            return last(1);
        }

        /**
         * Gets a previously consumed token, indexing backwards. last(1) will
         * be the token just consumed, last(2) the one before that, etc.
         * 
         * @param  offset How far back in the token stream to look.
         * @return        The consumed token.
         */
        private Token last(int offset) {
            return tokens.get(position - offset);
        }
        
        /**
         * Gets an unconsumed token, indexing forward. get(0) will be the next
         * token to be consumed, get(1) the one after that, etc.
         * 
         * @param  offset How far forward in the token stream to look.
         * @return        The yet-to-be-consumed token.
         */
        private Token get(int offset) {
            if (position + offset >= tokens.size()) {
                return new Token("", TokenType.EOF);
            }
            return tokens.get(position + offset);
        }

        private Stack<ParserPair> statStacks; // Pile des états du parser
        private final List<Token> tokens;
        public int position;

        // Génère un message plus avancé à propos d'une erreur du parser
        public String getParserStateError() {
            if (statStacks.peek().getState() != ParserState.DEFAULT)
                return statStacks.peek().getState().toString()+" non fermé";
            else
                return "instruction non fermée/mal ouverte";
        }
    }
    
    // Abstract syntax tree (AST) ----------------------------------------------

    // These classes define the syntax tree data structures. This is how code is
    // represented internally in a way that's easy for the interpreter to
    // understand.
    //
    // HACK: Unlike most real compilers or interpreters, the logic to execute
    // the code is baked directly into these classes. Typically, it would be
    // separated out so that the AST us just a static data structure.

    /**
     * Base interface for a Jasic statement. The different supported statement
     * types like "print" and "goto" implement this.
     */
    public interface Statement {
        /**
         * Statements implement this to actually perform whatever behavior the
         * statement causes. "print" statements will display text here, "goto"
         * statements will change the current statement, etc.
         */
        void execute();
    }

    /**
     * Base interface for an expression. An expression is like a statement
     * except that it also returns a value when executed. Expressions do not
     * appear at the top level in Jasic programs, but are used in many
     * statements. For example, the value printed by a "print" statement is an
     * expression. Unlike statements, expressions can nest.
     */
    public interface Expression {
        /**
         * Expression classes implement this to evaluate the expression and
         * return the value.
         * 
         * @return The value of the calculated expression.
         */
        Value evaluate();
    }
    
    /**
     * A "print" statement evaluates an expression, converts the result to a
     * string, and displays it to the user.
     */
    public class PrintStatement implements Statement {
        public PrintStatement(Expression expression) {
            this.expression = expression;
        }
        
        public void execute() {
            System.out.println(expression.evaluate().toString());
        }

        private final Expression expression;
    }
    
    /**
     * An "input" statement reads input from the user and stores it in a
     * variable.
     */
    public class InputStatement implements Statement {
        public InputStatement(String name) {
            this.name = name;
        }
        
        public void execute() {
            try {
                String input = lineIn.readLine();
                
                // Store it as a number if possible, otherwise use a string.
                try {
                    double value = Double.parseDouble(input);
                    variables.put(name, new NumberValue(value));
                } catch (NumberFormatException e) {
                    variables.put(name, new StringValue(input));
                }
            } catch (IOException e1) {
                // HACK: Just ignore the problem.
            }
        }

        private final String name;
    }

    /**
     * An assignment statement evaluates an expression and stores the result in
     * a variable.
     */
    public class AssignStatement implements Statement {
        public AssignStatement(String name, Expression value) {
            this.name = name;
            this.value = value;
        }
        
        public void execute() {
            variables.put(name, value.evaluate());
        }

        private final String name;
        private final Expression value;
    }
    
    /**
     * A "goto" statement jumps execution to another place in the program.
     */
    public class GotoStatement implements Statement {
        // Constructeur vide ajouté pour le else
        public GotoStatement() {  }

        public GotoStatement(String label) {
            this.label = label;
        }
        
        public void execute() {
            if (labels.containsKey(label)) {
                currentStatement = labels.get(label).intValue();
            } else {
                addError("Label non reconnu", label, true);
            }
        }

        public void setLabel(String label) {
            this.label = label;
        }

        private String label;
    }
    
    /**
     * An if then statement jumps execution to another place in the program, but
     * only if an expression evaluates to something other than 0.
     */
    public class IfThenStatement implements Statement {
        public IfThenStatement(Expression condition) {
            this.condition = condition;
        }
        
        public void execute() {
            // Si le label vers lequel jump est enregistré, alors bingo
            if (labels.containsKey(l_finSi)) {
                double value = condition.evaluate().toNumber();
                String jumpDir;

                // Si ce qui est évalué est faux :
                if (value == 0) {
                    // On saute directement à l'else si il est défini, sinon au finsi
                    if (l_else != null) {
                        jumpDir = l_else;
                    } else {
                        jumpDir = l_finSi;
                    }

                    // On saute à la condition choisie
                    currentStatement = labels.get(jumpDir).intValue();
                }

            } else {
                addError("Label non reconnu", l_finSi, true);
            }
        }

        /**   Getters   **/
        // Renvoie le label finsi
        public String getFinSi() { return this.l_finSi; }

        // Renvoie le safeJump
        public GotoStatement getsafeJump() { return this.safeJump; }

        // Renvoie true si le si possède un else
        public boolean hasElse() { return (l_else != null); }

        /**   Setters   **/
        // Définit le label finsi
        public void setFinSi(String label) {
            this.l_finSi = label;

            // Si le safeJump a été initialisé, on modifie son label
            if (this.safeJump != null)
                safeJump.setLabel(label);
        }

        // Définit le label else
        public void setElse(String label) {
            this.l_else = label;
        };

        // Définit le safe jump
        public void setSafeJump(GotoStatement safeJump) {
            this.safeJump = safeJump;
        }

        /**   Attributs de classe   **/

        // Condition principale du si
        private final Expression condition;

        // Labels finsi et else
        private String l_else, l_finSi;

        // Jump pour faire si => else ||
        private GotoStatement safeJump;
    }

    /**
     * PourStatement
     */
    // DEFAUT = jamais rentré
    // ITER = deja rentré 1 fois au moins
    private enum PourState {
        DEFAUT, ITER
    }

    public class PourStatement implements Statement {
        public PourStatement(Statement initialisation, Expression condition, Statement iteration, String label) {
            this.initialisation = initialisation;
            this.condition = condition;
            this.iteration = iteration;
            this.l_pour = label;
        }

        public void execute() {
            // Si le label vers lequel jump est enregistré, alors bingo
            if (labels.containsKey(l_finPour)) {
                if (etat == PourState.DEFAUT)
                    initialisation.execute();
                else
                    iteration.execute();

                // On évalue la condition
                double value = condition.evaluate().toNumber();

                // Si ce qui est évalué est faux
                if (value == 0) {
                    // On saute au fin tantQue
                    currentStatement = labels.get(l_finPour).intValue();
                } else if (etat == PourState.DEFAUT)
                    // Si on était dans l'état défaut on passe dans l'état iteration
                    etat = PourState.ITER;
            } else {
                addError("Label non reconnu", l_finPour, true);
            }
        }

        public String getPour() {
            return this.l_pour;
        }

        public void setFinPour(String finPour) {
            this.l_finPour = finPour;
        }

        // Condition et affectation du pour
        private Expression condition;
        private Statement initialisation, iteration;

        // Label finPour et pour
        private String l_finPour, l_pour;

        private PourState etat = PourState.DEFAUT;
    }
    /**
     * TirerStatement
     */
    public class TirerStatement implements Statement {
        public TirerStatement(Direction dir) {
            this.dir = dir;
        }

        public TirerStatement(String var) {
            this.var = var;
        }

        public void execute() {
            // On initialise pauseFlag à true
            pauseFlag = true;

            // On signale la raison de l'interruption
            pauseState = PauseState.TIRER;

            // Cas avec direction passée en string
            if (dir != null) {
                // On signale la direction dans laquelle le mec veut bouger
                dirJ = dir;
            } // Cas avec direction passée en variable
            else if (variables.containsKey(var)) {
                if (!(variables.get(var) instanceof DirectionValue)) {
                    // => Par défaut, la direction sera HAUTDROIT
                    dir = Direction.HD;
                } else {
                    // On signale la direction dans laquelle le mec veut bouger
                    dirJ = ((DirectionValue)variables.get(var)).getDirection();
                }
            } else {
                addError("Variable invalide : ", var, true);
            }
        }

        // Direction du tir
        private Direction dir;

        // Variable contenant la direction
        private String var;
    }

    /**
     * MeleeStatement
     */
    public class MeleeStatement implements Statement {
        public MeleeStatement(Direction dir) {
            this.dir = dir;
        }

        public void execute() {
            // On initialise pauseFlag à true
            pauseFlag = true;

            // On signale la raison de l'interruption
            pauseState = PauseState.MELEE;

            // On signale la direction dans laquelle le mec veut attaquer
            dirJ = dir;
        }

        // Direction du tir
        private Direction dir;
    }

    /**
     * TantQueStatement
     */
    public class TantQueStatement implements Statement {
        public TantQueStatement(Expression condition, String label) {
            this.l_tantQue = label;
            this.condition = condition;
        }

        public void execute() {
            // Si le label vers lequel jump est enregistré, alors bingo
            if (labels.containsKey(l_finTantQue)) {
                double value = condition.evaluate().toNumber();

                // Si ce qui est évalué est faux :
                if (value == 0) {
                    // On saute au fin tantQue
                    currentStatement = labels.get(l_finTantQue).intValue();
                }
                // Sinon on ne fait rien
            } else {
                addError("Label non reconnu", l_finTantQue, true);
            }
        }

        // Renvoie le label tant que
        public String getTantQue() { return this.l_tantQue; }

        // Définit le finTantQue
        public void setFinTantQue(String label) { this.l_finTantQue = label; }

        // Condition du tantque
        private final Expression condition;

        // Labels tantque et fintantque
        private String l_tantQue, l_finTantQue;
    }

    /**
     * DeplacerStatement
     */
    public class DeplacerStatement implements Statement {
        public DeplacerStatement(Direction dir) {
            this.dir = dir;
        }

        public DeplacerStatement(String var) {
            this.var = var;
        }

        public void execute() {
            // On initialise pauseFlag à true
            pauseFlag = true;

            // On signale la raison de l'interruption
            pauseState = PauseState.DEPLACEMENT;

            // Cas avec direction passée en string
            if (dir != null) {
                // On signale la direction dans laquelle le mec veut bouger
                dirJ = dir;
            } // Cas avec direction passée en variable
            else if (variables.containsKey(var)) {
                if (!(variables.get(var) instanceof DirectionValue)) {
                    // => Par défaut, la direction sera HAUTDROIT
                    dir = Direction.HD;
                } else {
                    // On signale la direction dans laquelle le mec veut bouger
                    dirJ = ((DirectionValue)variables.get(var)).getDirection();
                }
            } else {
                addError("Variable invalide : ", var, true);
            }
        }

        // Direction du déplacement
        private Direction dir;

        // Nom de la variable contenant la direction
        private String var;
    }

    /**
     * RechargerStatement
     */
    public class RechargerStatement implements Statement {
        public RechargerStatement() {
        }

        public void execute() {
            // On initialise pauseFlag à true
            pauseFlag = true;

            // On signale la raison de l'interruption
            pauseState = PauseState.RECHARGER;
        }
    }

    /**
     * DirectionValue
     */
    public class DirectionValue implements Value {
        public DirectionValue(Direction dir) {
            this.direction = dir;
        }

        public Value evaluate() { return this; }

        public Direction getDirection() { return direction; }

        @Override
        public double toNumber() {
            if (direction == null)
                return 0;

            switch (direction) {
                case HD:
                    return 1;
                case HG:
                    return 2;
                case BD:
                    return 3;
                case BG:
                default:
                    return 4;
            }
        }

        private Direction direction;
    }

    /**
     * DetecterEnnemi
     */
    public class DetecterEnnemi implements Expression {
        // Retourne la direction de l'ennemi s'il y en a une, sinon retourne 0
        public Value evaluate() {
            if (valued) {
                valued = false;

                return new DirectionValue(dirVar);
            } else {
                // On initialise pauseFlag à true
                pauseFlag = true;

                // On signale la raison de l'interruption
                pauseState = PauseState.DETECTERENNEMI;

                valued = true;

                // HACK : pour poursuivre l'exécution par défaut, on renvoie une direction arbitraire
                return new DirectionValue(Direction.HD);
            }
        }

        // Booléen déterminant si une valeur a été attribuée à cette expression
        private boolean valued = false;
    }

    /**
     * EstVivant
     */
    public class EstVivant implements Expression {
        // Retourne true si l'ennemi du joueur est vivant, faux sinon
        public Value evaluate() {
            if (valued) {
                valued = false;

                return new NumberValue(boolVal);
            } else {
                // On initialise pauseFlag à true
                pauseFlag = true;

                // On signale la raison de l'interruption
                pauseState = PauseState.ESTVIVANT;

                valued = true;

                // HACK : pour poursuivre l'exécution par défaut, on renvoie une valeur quelconque
                return new NumberValue(0);
            }
        }

        // Booléen déterminant si une valeur a été attribuée à cette expression
        private boolean valued = false;
    }

    /**
     * A variable expression evaluates to the current value stored in that
     * variable.
     */
    public class VariableExpression implements Expression {
        public VariableExpression(String name) {
            this.name = name;
        }
        
        public Value evaluate() {
            if (variables.containsKey(name)) {
                return variables.get(name);
            }

            // HACK : Par défaut, une variable non initialisée vaut zéro
            return new NumberValue(0);
        }
        
        private final String name;
    }
    
    /**
     * An operator expression evaluates two expressions and then performs some
     * arithmetic operation on the results.
     */
    public class OperatorExpression implements Expression {
        public OperatorExpression(Expression left, char operator,
                                  Expression right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        public Value evaluate() {
            Value leftVal = left.evaluate();
            Value rightVal = right.evaluate();
            
            switch (operator) {
            case '=':
                // Coerce to the left argument's type, then compare.
                if (leftVal instanceof NumberValue) {
                    return new NumberValue((leftVal.toNumber() ==
                            rightVal.toNumber()) ? 1 : 0);
                // Si la variable est du type DirectionValue
                } else if (leftVal instanceof DirectionValue) {
                    return new NumberValue((leftVal.toNumber() ==
                            rightVal.toNumber()) ? 1 : 0);
                } else {
                    return new NumberValue(leftVal.toString().equals(
                                           rightVal.toString()) ? 1 : 0);
                }
            case '!':
                if (leftVal instanceof NumberValue) {
                    return new NumberValue((leftVal.toNumber() ==
                            rightVal.toNumber()) ? 0 : 1);
                // Si la variable est du type DirectionValue
                } else if (leftVal instanceof DirectionValue) {
                    return new NumberValue((leftVal.toNumber() ==
                            rightVal.toNumber()) ? 0 : 1);
                } else {
                    return new NumberValue(leftVal.toString().equals(
                            rightVal.toString()) ? 0 : 1);
                }
            case '+':
                // Addition if the left argument is a number, otherwise do
                // string concatenation.
                if (leftVal instanceof NumberValue) {
                    return new NumberValue(leftVal.toNumber() +
                                           rightVal.toNumber());
                } else {
                    return new StringValue(leftVal.toString() +
                            rightVal.toString());
                }
            case '-':
                return new NumberValue(leftVal.toNumber() -
                        rightVal.toNumber());
            case '*':
                return new NumberValue(leftVal.toNumber() *
                        rightVal.toNumber());
            case '/':
                // Si le nombre est égal à zéro, on lisse le résultat par 0 pour éviter l'erreur
                if (rightVal.toNumber() == 0.0) return new NumberValue(0);
                return new NumberValue(leftVal.toNumber() / rightVal.toNumber());
            case '<':
                // Coerce to the left argument's type, then compare.
                if (leftVal instanceof NumberValue) {
                    return new NumberValue((leftVal.toNumber() <
                                            rightVal.toNumber()) ? 1 : 0);
                } else {
                    return new NumberValue((leftVal.toString().compareTo(
                                           rightVal.toString()) < 0) ? 1 : 0);
                }
            case '>':
                // Coerce to the left argument's type, then compare.
                if (leftVal instanceof NumberValue) {
                    return new NumberValue((leftVal.toNumber() >
                                            rightVal.toNumber()) ? 1 : 0);
                } else {
                    return new NumberValue((leftVal.toString().compareTo(
                            rightVal.toString()) > 0) ? 1 : 0);
                }
            }
            addError("Opérateur non reconnu", operator+"", true);
            return null; // On renvoie null par défaut pour continuer l'analyse syntaxique
        }
        
        private final Expression left;
        private final char operator;
        private final Expression right;
    }
    
    // Value types -------------------------------------------------------------
    
    /**
     * This is the base interface for a value. Values are the data that the
     * interpreter processes. They are what gets stored in variables, printed,
     * and operated on.
     * 
     * There is an implementation of this interface for each of the different
     * primitive types (really just double and string) that Jasic supports.
     * Wrapping them in a single Value interface lets Jasic be dynamically-typed
     * and convert between different representations as needed.
     * 
     * Note that Value extends Expression. This is a bit of a hack, but it lets
     * us use values (which are typically only ever seen by the interpreter and
     * not the parser) as both runtime values, and as object representing
     * literals in code.
     */
    public interface Value extends Expression {
        /**
         * Value types override this to convert themselves to a string
         * representation.
         */
        String toString();
        
        /**
         * Value types override this to convert themselves to a numeric
         * representation.
         */
        double toNumber();
    }
    
    /**
     * A numeric value. Jasic uses doubles internally for all numbers.
     */
    public class NumberValue implements Value {
        public NumberValue(double value) {
            this.value = value;
        }
        
        @Override public String toString() { return Double.toString(value); }
        public double toNumber() { return value; }
        public Value evaluate() { return this; }

        private final double value;
    }
    
    /**
     * A string value.
     */
    public class StringValue implements Value {
        public StringValue(String value) {
            this.value = value;
        }
        
        @Override public String toString() { return value; }
        public double toNumber() { return Double.parseDouble(value); }
        public Value evaluate() { return this; }

        private final String value;
    }

    // Interpreter -------------------------------------------------------------
    
    /**
     * Constructs a new Jasic instance. The instance stores the global state of
     * the interpreter such as the values of all of the variables and the
     * current statement.
     */
    public Interpreter() {
        // On initialise la liste des erreurs
        errMsg = new ArrayList<String[]>();

        // On initialise les flags d'états
        flagErrors = false; pauseFlag = false;

        variables = new HashMap<String, Value>();
        labels = new HashMap<String, Integer>();
        enumVars = new ArrayList<>();
        
        InputStreamReader converter = new InputStreamReader(System.in);
        lineIn = new BufferedReader(converter);
    }

    /**
     * This is where the magic happens. This runs the code through the parsing
     * pipeline to generate the AST. Then it executes each statement. It keeps
     * track of the current line in a member variable that the statement objects
     * have access to. This lets "goto" and "if then" do flow control by simply
     * setting the index of the current statement.
     *
     * In an interpreter that didn't mix the interpretation logic in with the
     * AST node classes, this would be doing a lot more work.
     * 
     * @param source A string containing the source code of a .jas script to
     *               interpret.
     */
    public InterpreterMessage interpret(String source) {
        InterpreterMessage msg;

        // HACK : ajout d'un saut de ligne à la fin de la source pour éviter une erreur
        source += "\n\n";

        // Tokenize.
        List<Token> tokens = tokenize(source);
        
        // Parse.
        Parser parser = new Parser(tokens);
        instructions = parser.parse(labels);

        // Si aucune erreur n'a été détectée, on interprète toutes les instructions
        if (!flagErrors) {
            // On reset le counter de l'instruction courante, et on lance l'interprétation
            currentStatement = 0;
            msg = execute();
        // Sinon on affiche toutes les erreurs détectées
        } else {
            msg = new InterpreterMessage(InterpreterMessage.TypeMessage.SYNTAX_ERR);
            msg.errorList = errMsg;
        }

        return msg;
    }

    /** Démarre l'interprétation du code **/
    private InterpreterMessage execute() {
        // Par défaut, on renvoie le message fin du programme
        InterpreterMessage msg = new InterpreterMessage(InterpreterMessage.TypeMessage.END_PRG);

        // On interprète tant qu'on arrive pas à la dernière ligne de code
        while (currentStatement < instructions.size()) {
            int thisStatement = currentStatement;
            currentStatement++;
            instructions.get(thisStatement).execute(); // On exécute après au cas où l'instruction modifie la ligne

            // On incrémente le compteur d'instructions
            nbInstr++;

            // Si on dépasse le nombre INSTR_LIMIT, on envoie un message approprié au contrôleur
            // et on sort de la boucle
            if (nbInstr > INSTR_LIMIT) {
                msg = new InterpreterMessage(InterpreterMessage.TypeMessage.LOOP_LIMIT);
                break;
            } else {
                // Si l'interprétation doit être pausée, on sort de la boucle et on envoie un message correspondant
                if (pauseFlag) {
                    switch (pauseState) {
                        // Si on s'est arrêté à cause d'une divison par zéro => afficher
                        // ....
                        case DEPLACEMENT:
                            // Si on s'arrête à cause d'un déplacement joueur, on l'effectue
                            msg = new InterpreterMessage(InterpreterMessage.TypeMessage.DEPLACEMENT);
                            msg.dirInst = dirJ; // => suppose que dirJ a été initialisé !
                        break;
                        case TIRER:
                            // Si on s'arrête à cause d'un tir joueur, on l'effectue
                            msg = new InterpreterMessage(InterpreterMessage.TypeMessage.TIRER);
                            msg.dirInst = dirJ; // => suppose que dirJ a été initialisé !
                        break;
                        case MELEE:
                            // Si on s'arrête à cause d'une attaque au CAC, on l'effectue
                            msg = new InterpreterMessage(InterpreterMessage.TypeMessage.MELEE);
                            msg.dirInst = dirJ; // => suppose que dirJ a été initialisé !
                        break;
                        case DETECTERENNEMI:
                            // Si le joueur demande la position de l'ennemi, on la renvoie
                            msg = new InterpreterMessage(InterpreterMessage.TypeMessage.DETECTERENNEMI);
                            // Dans ce cas, on retourne à la même instruction pour qu'elle soit exécutée avec une valeur
                            currentStatement = thisStatement;
                        break;
                        case ESTVIVANT:
                            // On donne au joueur l'information sur le statut vivant de son ennemi
                            msg = new InterpreterMessage(InterpreterMessage.TypeMessage.ESTVIVANT);
                            // Dans ce cas, on retourne à la même instruction pour qu'elle soit exécutée avec une valeur
                            currentStatement = thisStatement;
                        break;
                        case RECHARGER:
                            // Le joueur prend le temps de recharger ses munitions
                            msg = new InterpreterMessage(InterpreterMessage.TypeMessage.RECHARGER);
                        break;
                    }

                    // On reset les paramètres de la pause, par sécurité
                    pauseState = null; dirJ = null;

                    break;
                }
            }
        }

        return msg;
    }

    /** Redémarre l'interpréteur là où on l'a arrêté **/
    public InterpreterMessage restart() {
        // On repasse le flag à false
        pauseFlag = false;

        // On continue l'interprétation (on reprend à la ligne currentStatement)
        InterpreterMessage msg = execute();

        return msg;
    }

    /** Assigne une valeur aux variables ouvertes de l'interpréteur **/
    public void setDirVar(Direction dir) {
        dirVar = dir;
    }

    public void setBoolVar(int bool) {
        boolVal = bool;
    }

    private final Map<String, Value> variables;
    private final ArrayList<String> enumVars;
    private final Map<String, Integer> labels;
    private final BufferedReader lineIn;

    private int currentStatement;
    
    // Utility stuff -----------------------------------------------------------

    /**
     *  Ajoute une erreur au log d'erreurs errMsg
     *  wrongToken est à true si l'erreur est dûe à un faux token
     */
    public void addError(String error, String errToken, boolean wrongToken) {
        // On passe le flag d'erreurs à vrai
        this.flagErrors = true;
        errMsg.add(new String[]{error, errToken, wrongToken ? "w" : "r"});
    }

    /**
     * Classe paire utile pour la partie parseur, nous permettant de créer une association State=>Statement
     */
    public class ParserPair {

        private final ParserState left;
        private final Statement right;

        public ParserPair(ParserState left, Statement right) {
            this.left = left;
            this.right = right;
        }

        public ParserState getState() { return left; }
        public Statement getStatement() { return right; }
    }

    /**
     * Méthode générant un identifiant pour les labels de retour (différencier un finsi d'un autre pour les nommer)
     */
    private long idCounter = 0;

    public String makeID()
    {
        return String.valueOf(idCounter++);
    }
}
