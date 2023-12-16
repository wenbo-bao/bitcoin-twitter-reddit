To make jar file: 

```
sbt assembly
```

To run the file
```
spark-submit --master yarn --class TextSentiment target/scala-2.12/TextSentiment-assembly-0.1.jar  --deploy-mode cluster
```

Note: the input file name is given in scala code, if filepath changes, please change it in the `textSentiment.scala`
