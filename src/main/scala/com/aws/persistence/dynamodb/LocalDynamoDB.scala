package com.aws.persistence.dynamodb

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model._

import scala.collection.JavaConverters._

object LocalDynamoDB {
  private val url = "http://localhost:8000"

  def client(): AmazonDynamoDB =
    AmazonDynamoDBClient
      .builder()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "credentials")))
      .withEndpointConfiguration(new EndpointConfiguration(url, ""))
      .build()

  def asyncClient(): AmazonDynamoDBAsync =
    AmazonDynamoDBAsyncClient
      .asyncBuilder()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "credentials")))
      .withEndpointConfiguration(new EndpointConfiguration(url, ""))
      .build()

  def createTable(client: AmazonDynamoDB)(tableName: String, dropIfExists: Boolean)(attributes: (Symbol, ScalarAttributeType)*) = {

    def create = client.createTable(
      attributeDefinitions(attributes),
      tableName,
      keySchema(attributes),
      arbitraryThroughputThatIsIgnoredByDynamoDBLocal
    )

    def delete = client.deleteTable(tableName)

    val tableNames = client.listTables()

    (tableNames.getTableNames.contains(tableName), dropIfExists) match {
      case (true, true) => {
        delete
        create
      }
      case (false, _)   => create
      case (true, _)    =>
    }
  }

  def withTable[T](client: AmazonDynamoDB)(tableName: String)(attributeDefinitions: (Symbol, ScalarAttributeType)*)(
    thunk: => T
  ): T = {
    createTable(client)(tableName, true)(attributeDefinitions: _*)
    val res = try {
      thunk
    } finally {
      client.deleteTable(tableName)
      ()
    }
    res
  }

  def usingTable[T](client: AmazonDynamoDB)(tableName: String)(attributeDefinitions: (Symbol, ScalarAttributeType)*)(
    thunk: => T
  ): Unit = {
    withTable(client)(tableName)(attributeDefinitions: _*)(thunk)
    ()
  }

  def createTableWithSecondaryIndex(client: AmazonDynamoDB)(tableName: String, secondaryIndexName: String, dropIfExists: Boolean)(
    primaryIndexAttributes: (Symbol, ScalarAttributeType)*)(secondaryIndexAttributes: (Symbol, ScalarAttributeType)*) = {

    def create = client.createTable(
      new CreateTableRequest()
        .withTableName(tableName)
        .withAttributeDefinitions(attributeDefinitions(
          primaryIndexAttributes.toList ++ (secondaryIndexAttributes.toList diff primaryIndexAttributes.toList)))
        .withKeySchema(keySchema(primaryIndexAttributes))
        .withProvisionedThroughput(arbitraryThroughputThatIsIgnoredByDynamoDBLocal)
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName(secondaryIndexName)
            .withKeySchema(keySchema(secondaryIndexAttributes))
            .withProvisionedThroughput(arbitraryThroughputThatIsIgnoredByDynamoDBLocal)
            .withProjection(new Projection().withProjectionType(ProjectionType.ALL))))

    def delete = client.deleteTable(tableName)

    val tableNames = client.listTables()

    (tableNames.getTableNames.contains(tableName), dropIfExists) match {
      case (true, true) => {
        delete
        create
      }
      case (false, _)   => create
      case (true, _)    =>
    }
  }

  def withTableWithSecondaryIndex[T](client: AmazonDynamoDB)(tableName: String, secondaryIndexName: String)(
    primaryIndexAttributes: (Symbol, ScalarAttributeType)*)(secondaryIndexAttributes: (Symbol, ScalarAttributeType)*)(
                                      thunk: => T
                                    ): T = {
    client.createTable(
      new CreateTableRequest()
        .withTableName(tableName)
        .withAttributeDefinitions(attributeDefinitions(
          primaryIndexAttributes.toList ++ (secondaryIndexAttributes.toList diff primaryIndexAttributes.toList)))
        .withKeySchema(keySchema(primaryIndexAttributes))
        .withProvisionedThroughput(arbitraryThroughputThatIsIgnoredByDynamoDBLocal)
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName(secondaryIndexName)
            .withKeySchema(keySchema(secondaryIndexAttributes))
            .withProvisionedThroughput(arbitraryThroughputThatIsIgnoredByDynamoDBLocal)
            .withProjection(new Projection().withProjectionType(ProjectionType.ALL)))
    )
    val res = try {
      thunk
    } finally {
      client.deleteTable(tableName)
      ()
    }
    res
  }

  private def keySchema(attributes: Seq[(Symbol, ScalarAttributeType)]) = {
    val hashKeyWithType :: rangeKeyWithType = attributes.toList
    val keySchemas = hashKeyWithType._1 -> KeyType.HASH :: rangeKeyWithType.map(_._1 -> KeyType.RANGE)
    keySchemas.map { case (symbol, keyType) => new KeySchemaElement(symbol.name, keyType) }.asJava
  }

  private def attributeDefinitions(attributes: Seq[(Symbol, ScalarAttributeType)]) = {
    attributes.map { case (symbol, attributeType) => new AttributeDefinition(symbol.name, attributeType) }.asJava
  }

  private val arbitraryThroughputThatIsIgnoredByDynamoDBLocal = new ProvisionedThroughput(1L, 1L)
}
