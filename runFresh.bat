:: script to compile + run files
:: requires classpath, src, and output to be set in project settings
mkdir bin
rd /q /s "output" 2>nul
mkdir output

call mvn clean compile
call mvn test -f "e:\Code\wishlistgenerator\pom.xml" -q > output/TestResults.txt
call mvn package
call java -jar target/wishlistgenerator-1.0.1.jar > output/WishListScripted.txt
