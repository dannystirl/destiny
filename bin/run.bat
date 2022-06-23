:: script to compile + run files
:: requires classpath, src, and output to be set in project settings
mkdir bin
javac -d bin/ -cp "." src/*.java
rd /q /s "output" 2>nul
mkdir output

java -cp bin WishlistGenerator %1 > output\WishListScripted.txt