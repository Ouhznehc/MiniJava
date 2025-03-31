/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 Copyright (c) 2017 Ivan Kochurkin (upgrade to Java 8)
 Copyright (c) 2021 Michał Lorek (upgrade to Java 11)
 Copyright (c) 2022 Michał Lorek (upgrade to Java 17)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

parser grammar MiniJavaParser;

options {
    tokenVocab = MiniJavaLexer;
}

compilationUnit : methodDeclaration* EOF;

methodDeclaration
    : (typeType | VOID) identifier formalParameters methodBody = block
    ;

variableDeclarator
    : identifier ('=' variableInitializer)?
    ;

variableInitializer
    : arrayInitializer
    | expression
    ;

arrayInitializer
    : '{' (variableInitializer (',' variableInitializer)* ','?)? '}'
    ;

formalParameters
    : '(' formalParameterList? ')'
    ;

formalParameterList
    : formalParameter (',' formalParameter)*
    ;

formalParameter
    : typeType identifier
    ;

literal
    : DECIMAL_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | BOOL_LITERAL
    | NULL_LITERAL
    ;

block
    : '{' blockStatement* '}'
    ;

blockStatement
    : localVariableDeclaration ';'
    | statement
    ;

localVariableDeclaration
    : VAR identifier '=' expression
    | typeType variableDeclarator
    ;

identifier
    : IDENTIFIER
    | MODULE
    | OPEN
    | REQUIRES
    | EXPORTS
    | OPENS
    | TO
    | USES
    | PROVIDES
    | WITH
    | TRANSITIVE
    | YIELD
    | SEALED
    | PERMITS
    | RECORD
    | VAR
    | ASSERT
    ;

statement
    : block
    | IF parExpression statement (ELSE statement)?
    | FOR '(' forControl ')' statement
    | WHILE parExpression statement
    | RETURN expression? ';'
    | BREAK ';'
    | CONTINUE ';'
    | SEMI
    | expression ';'
    ;

parExpression
    : '(' expression ')'
    ;

forControl
    : forInit? ';' expression? ';' forUpdate = expressionList?
    ;

forInit
    : localVariableDeclaration
    | expressionList
    ;

expressionList
    : expression (',' expression)*
    ;

expression
    : primary
    | expression '[' expression ']'
    | methodCall
    | expression postfix = ('++' | '--')
    | prefix = ('+' | '-' | '++' | '--' | '~' | 'not') expression
    | '(' typeType ')' expression
    | NEW creator
    | expression bop = ('*' | '/' | '%') expression
    | expression bop = ('+' | '-') expression
    | expression bop = ('<<' | '>>>' | '>>') expression
    | expression bop = ('<=' | '>=' | '>' | '<') expression
    | expression bop = ('==' | '!=') expression
    | expression bop = '&' expression
    | expression bop = '^' expression
    | expression bop = '|' expression
    | expression bop = 'and' expression
    | expression bop = 'or' expression
    | <assoc = right> expression bop = '?' expression ':' expression
    | <assoc = right> expression bop = (
        '=' | '+=' | '-=' | '*=' | '/=' | '&=' | '|=' | '^=' | '>>=' | '>>>=' | '<<=' | '%='
    ) expression
    ;

primary : '(' expression ')' | literal | identifier ;

methodCall
    : identifier arguments
    ;

creator
    : createdName arrayCreatorRest
    ;

createdName
    : primitiveType
    ;

arrayCreatorRest
    : ('[' ']')+ arrayInitializer
    | ('[' expression ']')+ ('[' ']')*
    ;

typeType
    : primitiveType ('[' ']')*
    ;

primitiveType
    : BOOLEAN
    | CHAR
    | INT
    | STRING
    ;

arguments
    : '(' expressionList? ')'
    ;