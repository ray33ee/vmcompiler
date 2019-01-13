
float $1

float $2

start:
    add   $1  , $2, 2.4 
    nop
    external 1
useless_label:
    jmp start