function isValidReg(input = "") {
	let testRegex = /Inspired by .*?(\.[^A-Za-z0-9])/gm;
	return testRegex.test(input); 
}