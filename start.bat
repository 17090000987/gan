start /b java -Xmx2g -Xms2g -Xmn1g -XX:ParallelGCThreads=6 -XX:+UseConcMarkSweepGC -jar gan-2.0.0.jar  -Dspring.config.location=./application.yml

