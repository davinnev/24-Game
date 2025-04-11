public class ExpressionParser {

    private int position = 0;
    private String expression;


    public static double evaluate(String expression) {
        ExpressionParser parser = new ExpressionParser(expression.replaceAll("\\s+", ""));
        double result = parser.parseExpression();
        
        // Check if entire expression was consumed
        if (parser.position < parser.expression.length()) {
            throw new IllegalArgumentException("Invalid expression: unexpected characters at position " + parser.position);
        }
        
        return result;
    }
    
    private ExpressionParser(String expression) {
        this.expression = expression;
    }
    
    // Expression -> Term [+ Term]* or [- Term]*
    private double parseExpression() {
        double left = parseTerm();
        
        while (position < expression.length()) {
            char op = expression.charAt(position);
            if (op == '+' || op == '-') {
                position++;
                double right = parseTerm();
                if (op == '+')
                    left += right;
                else
                    left -= right;
            } else {
                break;
            }
        }
        
        return left;
    }
    
    // Term -> Factor [* Factor]* or [/ Factor]*
    private double parseTerm() {
        double left = parseFactor();
        
        while (position < expression.length()) {
            char op = expression.charAt(position);
            if (op == '*' || op == '/') {
                position++;
                double right = parseFactor();
                if (op == '*')
                    left *= right;
                else {
                    if (right == 0)
                        throw new ArithmeticException("Division by zero");
                    left /= right;
                }
            } else {
                break;
            }
        }
        
        return left;
    }
    
    // Factor -> Number or (Expression)
    private double parseFactor() {
        char c = expression.charAt(position);
        
        // Handle parentheses
        if (c == '(') {
            position++;
            double result = parseExpression();
            
            if (position >= expression.length() || expression.charAt(position) != ')')
                throw new IllegalArgumentException("Missing closing parenthesis");
            
            position++; // Consume the ')'
            return result;
        }
        
        // Handle numbers or card values
        return parseNumber();
    }
    
    // Parse a number or card value
    private double parseNumber() {
        StringBuilder sb = new StringBuilder();
        
        // Handle negative numbers
        if (position < expression.length() && expression.charAt(position) == '-') {
            sb.append('-');
            position++;
        }
        
        // Handle card values and numbers
        if (position < expression.length()) {
            char c = expression.charAt(position);
            
            // Handle special card values
            if (c == 'A') {
                position++;
                return 1;
            } else if (c == 'J') {
                position++;
                return 11;
            } else if (c == 'Q') {
                position++;
                return 12;
            } else if (c == 'K') {
                position++;
                return 13;
            }
            
            // Handle regular numeric values
            while (position < expression.length() && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                sb.append(expression.charAt(position));
                position++;
            }
        }
        
        if (sb.length() == 0)
            throw new IllegalArgumentException("Expected number at position " + position);
        
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format at position " + position);
        }
    }
    
    public static boolean isCorrect24Solution(String expression) {
        try {
            double result = evaluate(expression);
            // Allow for small floating point errors
            return Math.abs(result - 24.0) < 0.0001;
        } catch (Exception e) {
            return false;
        }
    }
}