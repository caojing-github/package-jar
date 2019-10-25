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