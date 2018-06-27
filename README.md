Small app to interact with local DynamoDB using scanamo library, and different aws client (sync/async)

## Dynamo DB
#### Intro to DynamoDB
https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html
https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.CoreComponents.html

#### Local DynamoDB Download:
https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html

#### Run DynamoDb locally
```
java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -sharedDb
```

#### Simple admin GUI
```
brew install node
npm install dynamodb-admin -g
export DYNAMO_ENDPOINT=http://localhost:8000
dynamodb-admin
```

## Scanamo library
https://www.scanamo.org/