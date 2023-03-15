function isValidReg(input = "") {
	let testRegex = /--[^\s]*/gm;
	return testRegex.test(input); 
}