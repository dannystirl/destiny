function isValidReg(input = "") {
	let testRegex = /(\d{2}-){2}\d{4}/gm;
	return testRegex.test(input); 
}