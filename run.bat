if not exist tmp mkdir tmp
mvn clean install -Djmeter.path=C:/scoop/apps/jmeter/current && cd tmp && jmeter
