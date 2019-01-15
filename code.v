


.define x $1

.define y $2

.define ans $3

.define i $4

.define geo $5

.define width 100

.define r 1.1

int x

int y

int ans

int i

float geo

    mul ans, y, width

    add ans, ans, x  # ans now contains y * width + x

    store i, 100

    store geo, r

for_start:

    dec i

    mul geo, geo, r

    jne for_start