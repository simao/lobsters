import java.sql.Connection

import io.simao.db.FeedDatabase
import io.simao.lobster.FeedItem
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite

class FeedDatabaseTest extends FunSuite with MockFactory {
  val defaultStr = "jdbc:sqlite::memory:"
  val db =  FeedDatabase(defaultStr)

  db.setupTables()

  test("rejects items already saved in the db") {
    val item = FeedItem("g0", "Title 1", "link 1", "clink 1", DateTime.now(), List())

    val items = List(item)

    db.save(item)

    assert(db.rejectSaved(items) === List())
  }

  test("checks if a feed is saved") {
    val item = FeedItem("g1", "Title 1", "link 1", "clink 1", DateTime.now(), List())

    assert(db.isSaved(item) === false)

    db.save(item)

    assert(db.isSaved(item) === true)
  }

  test("calls the block with a db connection") {
    val f = mockFunction[FeedDatabase, Int]

    f.expects(*).returning(22)

    assert(FeedDatabase.withConnection(defaultStr)(f) === 22)
  }
}
