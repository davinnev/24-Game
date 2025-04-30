// This class serves as the evaluator and validator of the answers submitted by players

public class ExpressionParser {

    private int position = 0;
    private String expression;

    private ExpressionParser(String expression) {
        this.expression = expression;
    }

    public static double evaluate(String expression) {
        ExpressionParser parser = new ExpressionParser(expression.replaceAll("\\s+", ""));
        double result = parser.parseExpression();
        // Check if entire expression was consumed
        if (parser.position < parser.expression.length()) {
            throw new IllegalArgumentException("Invalid expression: unexpected characters at position " + parser.position);
        }
        return result;
    }
    
    // This method parses addition and substraction
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
    
    // Thiss method parses multiplication and division
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
    
    // This method handles parentheses and numbers
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
            // Handle card values
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
            // Handle regular numbers
            while (position < expression.length() && (Character.isDigit(expression.charAt(position)) || expression.charAt(position) == '.')) {
                sb.append(expression.charAt(position));
                position++;
            }
        }
        // Convert the string number
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format at position " + position);
        }
    }
    
    // This method checks if the evaluated expression is equal to 24
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