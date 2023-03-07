function isValidReg(input = "") {
	let testRegex = /[^\x00-\x7F]/gm;
	return testRegex.test(input); 
}