
Enforce type safety at compile time!!! Implicitly convert between float and int. Promote when converting int f

Don't use pointers to registers, use pointers to blocks of memory. Is this needed?

==============
Control Flow
==============

For a single program, two bytecode files must be supplied. Each named 'loop' and 'setup', setup is called once at the start and loop is called continuously after.

Before each loop is executed, the interpreter must iterate through all live physics objects, and update. It must also check for collisions and handle appropriately.

==============
Flags
==============

NOTE: As well as the cmp instruction, all mathematical instructions that return real values will set the flags

equal: Set if opa == opb

sign: Set if opa < opb

==============
Types
==============

int: 32 bit integer, compatible with store, add, sub, mul, div, dec, inc, tofloat, cmp, peek push and pop.

float: 32 bit floating point number. SImilar implementation to int.

ptr: 32 bit address. Similar implementation to int.

vec: 32 bit pointer to 2d vector object. A Mathematical vector, not a resizable array

phys: 32 bit pointer to a physics object. This objects contains paramaters for velocity, speed, position, max speed, etc. 

==============
Instructions:
==============

Opcodes 0-255 take 0 arguments,
opcodes 256-511 take 1 argument,
Opcodes 512-767 take 2 arguments,
opcodes 768-1023 take 3 argument,
Opcodes 1024-1279 take 4 arguments.

Opcodes 512 - 575 are 2 argument math functions
Opcodes 768 - 831 are 3 argument math functions

==================
Signatures
==================

Each instruction is converted to a signature. THis signature is used in the lookup to find the corresponding opcode. 
For example, the following function

    add $3, $1, 44

(where $3 is an integer and $1 is an integer) has the signature

    add_vi_vi_li

add represents the instruction type. The first 'vi' says that the first argument is a variable (v) integer (i). THe second argument 
is also a variable integer. The third argument is a literal (l) integer (i). vi and li represent argument signatures which are always two characters.
The first character denotes whether the argument is a variable (v) or a literal. The second character represents the type integer (i), float (f), 
2dvector (v), or doesn't matter (*). The doesn't matter identifier, (*) is used where type safety is not needed.
