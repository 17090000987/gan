test=0
echo project_name:$Project_Name
echo project_path:$Project_Path
echo project_ver:$Project_Ver
export LD_LIBRARY_PATH=$Project_Path/libs
if [ $test == 1 ]; then
 java -Xmx1024m -Xms1024m -Xmn512m -Xss1m -XX:ParallelGCThreads=2 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -jar -Dspring.config.location=./application.yml $Project_Name-$Project_Ver*.jar
else
 nohup java -Xmx4g -Xms4g -Xmn3g -XX:ParallelGCThreads=2 -XX:+UseConcMarkSweepGC -jar -Dspring.config.location=./application.yml $Project_Name-$Project_Ver*.jar  >>console.log 2>&1 &
fi

