#================================================================
# A Makefile for the fodt (first-order decision theory) package
# and subpackages.
#
# Author: Scott Sanner
# $Revision: 1.1 $
#================================================================

# environment settings:
JAVAC   = javac -classpath ../../../..
JAVADOC = javadoc
JAVA    = java -classpath ../../../..

# project specifics:
CLASSES      = \
	Yylex.class sym.class parser.class ParseException.class

# list of subpackages:
PACKAGES     = parser

# Explain to make how compiling Java code works...
.SUFFIXES :     .class .java
.java.class :
	$(JAVAC) $<

# some other useful macros we can derive...
SRCS = $(CLASSES:.class=.java)
DOCS = $(CLASSES:.class=.html)

# Typical make targets:
all: $(PACKAGES) $(CLASSES) 

docs: ALWAYS_BUILD
	-mkdir docs; \
	$(JAVADOC) -author -d docs $(SRCS) $(PACKAGES)

parser: ALWAYS_BUILD
	-$(JAVA) jlex.Main fopc.lex; \
	mv fopc.lex.java Yylex.java; \
	$(JAVA) java_cup.Main -expect 10 fopc.cup;

clean:
	-rm *.class *~

wc: ALWAYS_BUILD
	wc `find . -name "*.java"`

# Packages:


# This is a dummy target that can be used to ensure that a target's
# dependencies are never satisfied.  This is useful because by default, make
# treats the presence of a directory with the same name as the target as an
# up-to-date target.
ALWAYS_BUILD:
