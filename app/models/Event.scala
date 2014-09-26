package models

import java.util.Date

import org.squeryl.PrimitiveTypeMode._
import play.api.libs.json.{JsValue, Json}
import ports.{DBEvent, Library}

case class Event(id: EventID, who: AuthorID, when: Date, name: EventName, whatString: String) {
  def publish(): Unit = Event.publish(this)

  def what() = Event.fromJson(name, whatString)
}

object Event {
  var marshaller: Map[String, EventStateMarshaller] = Map()

  marshaller = marshaller
    .+(("CreateFeedback", new CreateFeedbackEventMarshaller()))
    .+(("CloseFeedback", new CloseFeedbackEventMarshaller()))

  def apply(id: EventID, who: AuthorID, when: Date, name: EventName, what: EventState) = new Event(id, who, when, name, toJson(name, what).toString())

  def publish(event: Event) = inTransaction {
    Library.events.insert(DBEvent(event.id, event.who, event.when, event.name, event.whatString))
  }

  def fromJson(eventName: String, input: String) = fromJson(eventName: String, Json.parse(input))
  def fromJson(eventName: String, json: JsValue) = marshaller(eventName).fromJson(json)

  def toJson(eventName: String, eventState: EventState) =  marshaller(eventName).toJson(eventState)
}

trait EventStateMarshaller {
  def toJson(eventState: EventState): JsValue

  def fromJson(json: JsValue): EventState
}

trait EventState {
  val authorID: AuthorID

  def publish() = Event(0, authorID, new Date(), name(), this).publish()

  def name(): String
}

case class CreateFeedbackEvent(comment: String, authorID: AuthorID, revisionEntryID: RevisionEntryID, lineNumber: LineNumberType, feedbackStatus: FeedbackStatus, feedbackID: FeedbackID) extends EventState {
  def name() = "CreateFeedback"
}

case class CloseFeedbackEvent(feedbackID: FeedbackID, authorID: AuthorID) extends EventState {
  def name() = "CloseFeedback"
}

class CreateFeedbackEventMarshaller extends EventStateMarshaller {
  override def toJson(eventState: EventState): JsValue = {
    val createFeedbackEvent = eventState.asInstanceOf[CreateFeedbackEvent]
    Json.obj(
      "comment" -> createFeedbackEvent.comment,
      "authorID" -> createFeedbackEvent.authorID,
      "revisionEntryID" -> createFeedbackEvent.revisionEntryID,
      "lineNumber" -> createFeedbackEvent.lineNumber,
      "status" -> (createFeedbackEvent.feedbackStatus match {
        case Open() => "open"
        case Closed() => "closed"
      }),
      "feedbackID" -> createFeedbackEvent.feedbackID
    )
  }

  override def fromJson(json: JsValue): EventState =
    CreateFeedbackEvent(
      (json \ "comment").as[String],
      (json \ "authorID").as[Long],
      (json \ "revisionEntryID").as[Long],
      (json \ "lineNumber").as[Long],
      (json \ "status").as[String] match {
        case "open" => Open()
        case "closed" => Closed()
      },
      (json \ "feedbackID").as[Long]
    )
}

class CloseFeedbackEventMarshaller extends EventStateMarshaller {
  override def toJson(eventState: EventState): JsValue = {
    val closeFeedbackEvent = eventState.asInstanceOf[CloseFeedbackEvent]
    Json.obj(
      "feedbackID" -> closeFeedbackEvent.feedbackID,
      "authorID" -> closeFeedbackEvent.authorID
    )
  }

  override def fromJson(json: JsValue): EventState =
    CloseFeedbackEvent((json \ "feedbackID").as[Long], (json \ "authorID").as[Long])
}


