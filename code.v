
.define pi 3.1415927

.define e 2.7

.define i 1i

float $1

float $2

start:
    add   3 , $2, pi   # ignore this
    nop
    external 1
useless_label:
    jmp start