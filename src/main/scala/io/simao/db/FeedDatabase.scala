package io.simao.db

import java.sql.{Connection, DriverManager}

import io.simao.lobster.FeedItem
import org.joda.time.DateTime

object FeedDatabase {
  def apply(jdbcString: String) = {
    val connection = DriverManager.getConnection(jdbcString)
    new FeedDatabase(connection)
  }

  def withConnection[T](jdbcString: String)(f: FeedDatabase ⇒ T) = {
    val connection = DriverManager.getConnection(jdbcString)
    val db = new FeedDatabase(connection)
    try f(db)
    finally {
      println("CLOSINg")
      connection.close()
    }
  }
}

class FeedDatabase(connection: Connection) {
  def rejectSaved(items: Seq[FeedItem]): Seq[FeedItem] = {
    assert(items.size <= 30)

    val ids = items.map(i ⇒ s"'${i.guid}'").mkString(",")
    val stm = connection.createStatement()
    val rs = stm.executeQuery(s"select guid from saved_feed where guid IN ($ids)")
    var savedIds = Set[String]()

    while(rs.next())
      savedIds = savedIds + rs.getString("guid")

    items.filterNot(i ⇒ savedIds.contains(i.guid))
  }

  def save(item: FeedItem): FeedItem = {
    val statement = connection.createStatement()
    val now = DateTime.now().toString
    statement.executeUpdate(s"insert into saved_feed values('${item.guid}', '$now')")
    item
  }

  def isSaved(item: FeedItem): Boolean = {
    val stm = connection.createStatement()
    val rs = stm.executeQuery(s"select guid from saved_feed where guid = '${item.guid}'")
    println("saved?")
    rs.next()
  }

  def setupTables(): Connection = {
    val statement = connection.createStatement()
    // statement.executeUpdate("drop table if exists saved_feed")
    statement.executeUpdate("create table if not exists saved_feed (guid string, updated_at string)")
    connection
  }
}
