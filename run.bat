if not exist tmp mkdir tmp
mvn clean install -Djmeter.path="%USERPROFILE%/scoop/apps/jmeter/current" && cd tmp && jmeter
