获取可执行jar包  
```shell script
mvn package
```
* 在jar包当前目录执行默认获取dev所有token  
* 示例：  
```shell script
java -jar getToken.jar
```  
* 获取test环境token 
```shell script
java -jar getToken.jar test
```
* jar包目录会生成 包含所有token的文件`token.txt` 

部署在172.16.71.3 
只启动  
```shell script
nohup java -jar http.jar --server.port=9998 > http.log 2>&1 &
``` 
启动并打开日志  
```shell script
nohup java -jar http.jar --server.port=9998 > http.log 2>&1 & tail -f http.log
``` 

杀死http.jar包进程命令  
```shell script
jps -l | grep http.jar | awk '{print $2}' | xargs kill
```