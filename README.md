# 遇到的一些问题

### 在使用Kotlin进行IO操作时,使用BufferedReader类时，使用以下语法会无法正确读入数据，并且总是无法结束循环
```
while ({line = reader.readLine();line}()!=null)
while (({line = reader.readLine();line})!=null)
```
### 使用如下写法就不会出现问题
```
while (true){
            line = reader.readLine()?:break
}
```
