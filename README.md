# Minibase

Special case: when a string and an integer is compared, it will throw a comparsion error, since the PDF only indicated
that two string are compared in lexcial order, but not mentioned about between string and integer.(e.g. throw exception
for Q(x)-: R(x,y,z),
z="test string" ,where R is (int,int,int) ) 

Special case: if condition contains unseen Variable, it will throw a variable not found error. (e.g. throw exception 