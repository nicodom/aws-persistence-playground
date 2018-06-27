package com.aws.persistence.dynamodb


object SyncExample extends App {
  import com.gu.scanamo._
  import com.gu.scanamo.syntax._

  val client = LocalDynamoDB.client()
  import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
  LocalDynamoDB.createTable(client)("muppets", true)('id -> N)

  case class Muppet(id: Long, name: String, species: String)
  val muppets = Table[Muppet]("muppets")

  val operations = for {
    _ <- muppets.put(Muppet(1, "Kermit", "Frog"))
    _ <- muppets.put(Muppet(2, "Cookie Monster", "Monster"))
    kermit <- muppets.get('id -> 1)
  } yield kermit

  val batchOperations = for {
    _ <- muppets.putAll(Set(
      Muppet(3, "Miss Piggy", "Pig"),
      Muppet(4, "Domenico", "Human")))
    dom <- muppets.get('id -> 4)
  } yield dom //Option[Either[DynamoReadError, Muppet]]

  //The Either[DynamoReadError, Muppet] is here in case the returned JSON can’t be deserialised into a Farmer
  //The surrounding Option indicates the possibility that there is no matching record found in DynamoDB.

//  val interpreter = ScanamoInterpreters.id(client)
//  val result = operations.foldMap(interpreter)
  val result = Scanamo.exec(client)(operations)
  val result2 = Scanamo.exec(client)(batchOperations)

  System.out.println(result)
  System.out.println(result2)

  case class Transport(mode: String, line: String, colour: String)
  val transport = Table[Transport]("transport")
  val colourIndex = transport.index("colour-index")

  LocalDynamoDB.createTableWithSecondaryIndex(client)("transport", "colour-index", true)('mode -> S, 'line -> S)('colour -> S)
  val operations2 = for {
    _ <- transport.putAll(Set(
      Transport("Underground", "Circle", "Yellow"),
      Transport("Underground", "Metropolitan", "Maroon"),
      Transport("Underground", "Central", "Red")))
    maroonLine <- colourIndex.query('colour -> "Maroon")
  } yield maroonLine.toList

  val result3 = Scanamo.exec(client)(operations2)

  System.out.println(result3)
}