package io.simao.db

import java.sql.{Connection, DriverManager}

import io.simao.lobster.FeedItem
import org.joda.time.DateTime
import io.simao.util.KestrelObj._

object FeedDatabase {
  def apply(jdbcString: String) = {
    val connection = DriverManager.getConnection(jdbcString)
    new FeedDatabase(connection)
  }

  def withConnection[T](jdbcString: String)(f: FeedDatabase ⇒ T) = {
    val connection = DriverManager.getConnection(jdbcString)
    val db = new FeedDatabase(connection)
    try f(db)
    finally connection.close()
  }
}

class FeedDatabase(connection: Connection) {
  def save(item: FeedItem): FeedItem = {
    item.tap { i ⇒
      val statement = connection.createStatement()
      val now = DateTime.now().toString
      statement.executeUpdate(s"insert into saved_feed values('${item.guid}', '$now')")
    }
  }

  def isSaved(item: FeedItem): Boolean = {
    val stm = connection.createStatement()
    val rs = stm.executeQuery(s"select guid from saved_feed where guid = '${item.guid}'")
    rs.next()
  }

  def setupTables(drop: Boolean = false): Connection = {
    connection.tap { c ⇒
      val statement = c.createStatement()
      if (drop)
        statement.executeUpdate("drop table if exists saved_feed")
      statement.executeUpdate("create table if not exists saved_feed (guid string, updated_at string)")
    }
  }
}
