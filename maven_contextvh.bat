start mvn -pl "contextvh" clean install -DskipTests=true -Dmaven.javadoc.skip=true -Dcobertura.skip -B -V
copy /Y "%~dp0\contextvh\target\*jar-with-depend*.jar" "%~dp0\..\TI2806\src\main\goal\nl\tudelft\ti2806\"
