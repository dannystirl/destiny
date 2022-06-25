:: script to compile + run files
:: requires classpath, src, and output to be set in project settings
mkdir bin
javac -d bin/ -cp "." src/main/java/destiny/*.java
rd /q /s "output" 2>nul
mkdir output

:: java -cp bin WishlistGenerator %1 > output\WishListScripted.txt

:: the above only works when using a workspace, not a maven folder
:: the below works when using a maven folder (-q hides info lines)

::mvn test -f "e:\Code\wishlistgenerator\pom.xml" -q > output/TestResults.txt
::mvn compile exec:java -e -q > output/WishListScripted.txt