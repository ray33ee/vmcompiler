
Enforce type safety at compile time!!! Implicitly convert between float and int. Promote when converting int f

Do we even need string support???

Don't use pointers to registers, use pointers to blocks of memory. Is this needed?

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

vev: 32 bit pointer to 2d vector object. A Mathematical vector, not a resizable array

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

label:              # A label. ofc - 0

int $1              # Designates register 1 as an integer

float $1            # Designates register 1 as a float

vec $1              # Designates register 1 as a 2D vector
ptr $1              # Designates register 1 as a pointer

jmp     label       # Unconditional jump to label - 1 - 0x0100
je      label       # Jumps if equal flag is set - 1 - 0x0101
jne     label       # Jumps if equal flag is not set - 1 - 0x0102
jl      label       # Jumps if sign bit is set - 1 - 0x0103
jle     label       # Jumps if sign bit or equal flag is set - 1 - 0x0104

store   $1, 100     # Puts into register 1 the value of 100 - 2 - 0x0240
store   $1, 1.33e3  # Puts into register 1 the value of 1.33e3 - 2 - 0x0241
store   $1, $2      # Copy the contents of register 2 into register 1 - 2 - 0x0242
store   $1, 1.3, 2.9# Copy the vector [1.3, 2.9] into reg1 - 3 - 0x0340

store   $1, &2      # Copy the address of reg 2 into reg 1 - 2 - 0x0243 

nop                 # No operation - 0 - 0x0000
halt                # Holds processor - 0 - 0x0001

external 3          # Puts in a call to the third external function. All external functions are implementation-specific. - 1 - 0x0105

elapsed $1          # Puts into #1 the time (in milliseconds) elapsed since last elapsed call - 1 - 0x0106

add $1, $2, $3      # Add the value in reg 2 to the value in reg 3, and store in reg 1 - 3 - 0x0300, 0x0301, 0x0302
sub $1, $2, $3      # Subtracts the value in reg 2 from the value in reg 3, and store in reg 1 - 3 - 0x0303, 0x0304, 0x0305, 0x0306, 0x0307
mul $1, $2, $3      # Multiply the value in reg 2 to the value in reg 3, and store in reg 1 - 3 - 0x0308, 0x0309, 0x030A

div $1, $2, $3      # Divide the value in reg 2 by the value in reg 3, and store in reg 1 - 3 - 0x030B, 0x030C, 0x030D, 0x030E, 0x030F

pow $1, $2, 1.414   # Puts into reg 1 reg2 ^ 1.414 - 3 - 0x0310
pow $1, $2, $3      # Puts into reg 1 reg2 ^ reg3 - 3 - 0x0311
pow $1, $2, 7       # Puts into reg 1 reg2 ^ 7 - 3 - 0x0312

sin $1, $2          # Into reg 1, put the sine of reg 2 - 2 - 0x0200
cos $1, $2          # Into reg 1, put the sine of reg 2 - 2 - 0x0201
tan $1, $2          # Into reg 1, put the sine of reg 2 - 2 - 0x0202

abs $1, $2          # Puts into reg 1 |reg2| - 2 - 0x0203, 0x0204

norm $1, $2         # Puts into reg 1 (reg2)/|reg2| - 2 - 0x0205, 0x0206

ln $1, $2           # Puts into reg 1 ln(reg2) - 2 - 0x0207
exp $1, $2          # Puts into reg 1 exp(reg2) - 2 - 0x0208

floor $1, $2        # Puts into reg 1 floor(reg2) - 2 - 0x0209
ceil $1, $2         # Puts into reg 1 ceil(reg2) - 2 - 0x020A

sqrt $1, $2         # Puts into reg 1 sqrt(reg2) - 2 - 0x020B

neg $1, $2          # Puts into reg 1 -reg2 - 2 - 0x20C

dec $1              # Decrement the value in register 1  - 1 - 0x0107
inc $1              # Increment the value in register 1 - 1 - 0x0108

cmp $1, 22          # Compares the value in reg 1 with 22, and sets flags - 2 - 0x0244
cmp $1, 2.0         # Compares the value in reg 1 with 2.0 and sets flags - 2 - 0x0245
cmp $1, $2          # Compares the values in reg 1 with the value in reg 2 and sets flags - 2 - 0x0246

random $1           # Puts a random number into reg 1 (between 0 and 1 for float, or 0 to MAX_INT for int) - 1 - 0x0109

peek $1             # Copies the value at the top of the stack into reg 1 - 1 - 0x010A

push $1             # Pushes the value in reg 1 onto stack - 1 - 0x010B
push 18             # Push the value 18 onto the stack - 1 - 0x010C
push 1.3            # Push the value 1.3 onto the stack - 1 - 0x010D

pop $1              # Pops the top of the stack into reg 1 - 1 - 0x010E

call                # Call function - 1 - 0x010F

ret                 # Return from function call - 0 - 0x0003



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
