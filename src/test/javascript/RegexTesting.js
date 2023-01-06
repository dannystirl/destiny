function isValidReg(input = "") {
	let testRegex = /pv[pe]|m.?kb|controller|gambit/gm;
	return testRegex.test(input); 
}