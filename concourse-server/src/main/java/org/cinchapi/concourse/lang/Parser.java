/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Jeff Nelson, Cinchapi Software Collective
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cinchapi.concourse.lang;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.cinchapi.concourse.thrift.Operator;

import com.google.common.collect.Lists;

/**
 * The {@link Parser} is a tool that operates on various aspects of the
 * language.
 * 
 * @author jnelson
 */
public final class Parser {

    /**
     * Go through a list of symbols and group the expressions together in a
     * {@link Expression} object.
     * 
     * @param symbols
     * @return the expression
     */
    protected static List<Symbol> groupExpressions(List<Symbol> symbols) { // visible
                                                                           // for
                                                                           // testing
        try {
            List<Symbol> grouped = Lists.newArrayList();
            Iterator<Symbol> it = symbols.iterator();
            while (it.hasNext()) {
                Symbol symbol = it.next();
                if(symbol instanceof KeySymbol) {
                    // NOTE: We are assuming that the list of symbols is well
                    // formed, and, as such, the next elements will be an
                    // operator and one or more symbols. If this is not the
                    // case, this method will throw a ClassCastException
                    OperatorSymbol operator = (OperatorSymbol) it.next();
                    ValueSymbol value = (ValueSymbol) it.next();
                    Expression expression;
                    if(operator.getOperator() == Operator.BETWEEN) {
                        ValueSymbol value2 = (ValueSymbol) it.next();
                        expression = Expression.create((KeySymbol) symbol,
                                operator, value, value2);
                    }
                    else {
                        expression = Expression.create((KeySymbol) symbol,
                                operator, value);
                    }
                    grouped.add(expression);
                }
                else {
                    grouped.add(symbol);
                }
            }
            return grouped;
        }
        catch (ClassCastException e) {
            throw new SyntaxException(e.getMessage());
        }
    }

    /**
     * Convert a valid and well-formed list of {@link Symbol} objects into a
     * Queue in postfix notation.
     * <p>
     * NOTE: This method will group non-conjunctive symbols into
     * {@link Expression} objects.
     * </p>
     * 
     * @param symbols
     * @return the symbols in postfix notation
     */
    public static Queue<PostfixNotationSymbol> toPostfixNotation(
            List<Symbol> symbols) {
        Deque<Symbol> stack = new ArrayDeque<Symbol>();
        Queue<PostfixNotationSymbol> queue = new LinkedList<PostfixNotationSymbol>();
        symbols = groupExpressions(symbols);
        for (Symbol symbol : symbols) {
            if(symbol instanceof ConjunctionSymbol) {
                while (!stack.isEmpty()) {
                    Symbol top = stack.peek();
                    if(symbol == ConjunctionSymbol.OR
                            && (top == ConjunctionSymbol.OR || top == ConjunctionSymbol.AND)) {
                        queue.add((PostfixNotationSymbol) stack.pop());
                    }
                    else {
                        break;
                    }
                }
                stack.push(symbol);
            }
            else if(symbol == ParenthesisSymbol.LEFT) {
                stack.push(symbol);
            }
            else if(symbol == ParenthesisSymbol.RIGHT) {
                boolean foundLeftParen = false;
                while (!stack.isEmpty()) {
                    Symbol top = stack.peek();
                    if(top == ParenthesisSymbol.LEFT) {
                        foundLeftParen = true;
                        break;
                    }
                    else {
                        queue.add((PostfixNotationSymbol) stack.pop());
                    }
                }
                if(!foundLeftParen) {
                    throw new SyntaxException(MessageFormat.format(
                            "Syntax error in {0}: Mismatched parenthesis",
                            symbols));
                }
                else {
                    stack.pop();
                }
            }
            else {
                queue.add((PostfixNotationSymbol) symbol);
            }
        }
        while (!stack.isEmpty()) {
            Symbol top = stack.peek();
            if(top instanceof ParenthesisSymbol) {
                throw new SyntaxException(MessageFormat.format(
                        "Syntax error in {0}: Mismatched parenthesis", symbols));
            }
            else {
                queue.add((PostfixNotationSymbol) stack.pop());
            }
        }
        return queue;
    }

    private Parser() {/* noop */}

}
