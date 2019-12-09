int add(int x, int y) {
	int z ;
	z = x+y;
	if(z<100){
		z = 1000;
	}
	return z;
}

void main () {
	int t = 33;  
	_print(add(1,t));
	
	return;
}
