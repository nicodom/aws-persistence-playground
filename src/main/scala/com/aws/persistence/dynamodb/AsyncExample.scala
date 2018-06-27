package com.aws.persistence.dynamodb

import scala.concurrent.Await

object AsyncExample extends App {
  import com.gu.scanamo._
  import com.gu.scanamo.syntax._

  val asyncClient = LocalDynamoDB.asyncClient()
  import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
  LocalDynamoDB.createTable(asyncClient)("muppets", true)('name -> S)

  case class Muppet(name: String, species: String)
  val muppets = Table[Muppet]("muppets")

  val operations = for {
    _ <- muppets.put(Muppet("Kermit", "Frog"))
    _ <- muppets.put(Muppet("Cookie Monster", "Monster"))
    _ <- muppets.put(Muppet("Miss Piggy", "Pig"))
    kermit <- muppets.get('name -> "Kermit")
  } yield kermit //Future[Option[Either[DynamoReadError, Muppet]]]

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  //  val interpreter = ScanamoInterpreters.future(asyncClient)
  //  val futureRes = operations.foldMap(interpreter)
  val futureRes = ScanamoAsync.exec(asyncClient)(operations)

  Await.result(futureRes, 5 seconds)

  System.out.println(futureRes)
}
