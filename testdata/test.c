int countEven(int max) {
    int i = 1;           // s1
    int amount = 0;      // s2
    while (i <= max) {   // s3
        if (i % 2 == 0) {// s4
            amount++;    // s5
        }
        i++;             // s6
    }
    return amount;       // s7
}

int straight(int max) {
    int a = max;
    int b = 4;
    return max + a + b;
}

void wikiExample() {
    if (c1())
       f1();
    else
       f2();
    
    if (c2())
       f3();
    else
       f4();

}

void someInternetExample() {
    i = 0;
    n = 4;
    while (i < n - 1) {
        j = i + 1;
        while (j < n) {
            if (A[i] < A[j]) {
                swap(A[i], A[j]);
            }
            
        }
        i++;
    }
}

void elseifs() {
	int a;
	if (a < 2) {
	
	} else if (a < 4) {
	
	} else {
	
	}
}

void switchExample() {
	int a;
	switch (a) {
	case 1:
		break;
	case 2:
		break;
	case 3:
		break;
	}
}

void paperExample() {
	char c1;
	int c2;
	int c3;
	switch (c1) {
	case 'A': break;
	case 'B': if (c2); break;
	case 'C': if (c3); else; break;
	}
}

void doWhileTest() {
	do {
	
	} while (1);
}

