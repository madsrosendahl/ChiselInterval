This repository is a supplement to the article

# **Static Analysis for Hardware Design**

_Mads Rosendahl and Maja H. Kirkeby

Roskilde University
Computer Science
Denmark_
madsr@ruc.dk, kirkebym@acm.org

Abstract. Implementing algorithms in hardware can be a substantial
engineering challenge. Hardware accelerators for some algorithms may be
a way to achieve better time and energy efficiency of the computational
problems. We explore some possible applications of static analysis in the
design phase of construction hardware design for algorithms targeting
field-programmable gate arrays (FPGA).
Drawing inspiration from Alan Mycroft’s 2007 invited talk on static analysis
and subsequent articles discussing the connection between hardware
evolution, language design, and static analysis, we explore the usage of
static analysis as a tool to facilitate the realization of hardware accelerators
for algorithms. We examine methodologies for analyzing communication
and data flows within the hardware design, thereby enhancing
our understanding of these aspects in the pursuit of efficient FPGA-based
algorithm implementations.

-----------------

This repository contains implementations of the semantics and analysis of a subset of the hardware description language Chisel.
The implementation is written in Java 19+ and is converted from an earlier version in Scala.
It uses the newer features with records, pattern matching in switch statements and lamnda expressions.
It is an experiment to examine how easy it is to express semantics and analysis in java.
The current version is quite similar to how it could be written in ML or Scala.

The directory 'in' contains examples of programs in textual form. The Parser converts into abstract syntax using 
data types (records) in the file AbsSyn. The PrettyPrinter can revert abstract syntax back to textual form.
The Interpreter contains the state transition function and state iterator interpretation and a collecting interpretation.
