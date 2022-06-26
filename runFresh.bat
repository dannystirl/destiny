:: script to compile + run files
:: requires classpath, src, and output to be set in project settings
mkdir bin
rd /q /s "output" 2>nul
mkdir output

call mvn clean compile
:: dont' actually need this since package calls test
:: call mvn test -f "e:\Code\wishlistgenerator\pom.xml" -q > output/TestResults.txt
call mvn package
call java -jar target/wishlistgenerator-1.1.0.jar > output/WishListScripted.txt
