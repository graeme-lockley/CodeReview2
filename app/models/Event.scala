package models

import java.util.Date

import org.squeryl.PrimitiveTypeMode._
import play.api.libs.json.{JsValue, Json}
import ports.{DBEvent, Library}

case class Event(id: EventID, who: AuthorID, when: Date, name: EventName, whatString: String) {
  def publish(): Unit = Event.publish(this)

  def what() = Event.fromJson(name, whatString)

  def whatJson() = Json.parse(whatString)
}

object Event {
  def all() = inTransaction {
    DBEvent.all().map(x => Event(x.id, x.authorID, x.when, x.name, x.content))
  }

  var marshaller: Map[String, EventStateMarshaller] = Map()

  marshaller = marshaller
    .+(("CreateFeedback", new CreateFeedbackEventMarshaller()))
    .+(("CreateResponse", new CreateResponseEventMarshaller()))
    .+(("CloseFeedback", new CloseFeedbackEventMarshaller()))
    .+(("StartReview", new StartReviewEventMarshaller()))
    .+(("CompleteReview", new CompleteReviewEventMarshaller()))
    .+(("CancelReview", new CancelReviewEventMarshaller()))

  def apply(id: EventID, who: AuthorID, when: Date, name: EventName, what: EventState) = new Event(id, who, when, name, toJson(name, what).toString())

  def publish(event: Event) = inTransaction {
    Library.events.insert(DBEvent(event.id, event.who, event.when, event.name, event.whatString))
  }

  def fromJson(eventName: String, input: String): EventState = fromJson(eventName: String, Json.parse(input))

  def fromJson(eventName: String, json: JsValue): EventState = marshaller(eventName).fromJson(json)

  def toJson(eventName: String, eventState: EventState): JsValue = marshaller(eventName).toJson(eventState)
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

case class CloseFeedbackEvent(feedbackID: FeedbackID, authorID: AuthorID) extends EventState {
  def name() = "CloseFeedback"
}

case class CreateFeedbackEvent(comment: String, authorID: AuthorID, revisionEntryID: RevisionEntryID, lineNumber: LineNumberType, feedbackStatus: FeedbackStatus, feedbackID: FeedbackID) extends EventState {
  def name() = "CreateFeedback"
}

case class CreateResponseEvent(responseID: ResponseID, feedbackID: FeedbackID, authorID: AuthorID, comment: String) extends EventState {
  def name() = "CreateResponse"
}

case class StartReviewEvent(revisionID: RevisionID, authorID: AuthorID) extends EventState {
  def name() = "StartReview"
}

case class CompleteReviewEvent(revisionID: RevisionID, authorID: AuthorID) extends EventState {
  def name() = "CompleteReview"
}

case class CancelReviewEvent(revisionID: RevisionID, authorID: AuthorID) extends EventState {
  def name() = "CancelReview"
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

class CreateResponseEventMarshaller extends EventStateMarshaller {
  override def toJson(eventState: EventState): JsValue = {
    val createResponseEvent = eventState.asInstanceOf[CreateResponseEvent]
    Json.obj(
      "id" -> createResponseEvent.responseID,
      "feedbackID" -> createResponseEvent.feedbackID,
      "authorID" -> createResponseEvent.authorID,
      "comment" -> createResponseEvent.comment
    )
  }

  override def fromJson(json: JsValue): EventState =
    CreateResponseEvent(
      (json \ "responseID").as[Long],
      (json \ "feedbackID").as[Long],
      (json \ "authorID").as[Long],
      (json \ "comment").as[String]
    )
}

class StartReviewEventMarshaller extends EventStateMarshaller {
  override def toJson(eventState: EventState): JsValue = {
    val startReviewEvent = eventState.asInstanceOf[StartReviewEvent]
    Json.obj(
      "revisionID" -> startReviewEvent.revisionID,
      "authorID" -> startReviewEvent.authorID
    )
  }

  override def fromJson(json: JsValue): EventState =
    StartReviewEvent(
      (json \ "revisionID").as[Long],
      (json \ "authorID").as[Long]
    )
}

class CompleteReviewEventMarshaller extends EventStateMarshaller {
  override def toJson(eventState: EventState): JsValue = {
    val completeReviewEvent = eventState.asInstanceOf[CompleteReviewEvent]
    Json.obj(
      "revisionID" -> completeReviewEvent.revisionID,
      "authorID" -> completeReviewEvent.authorID
    )
  }

  override def fromJson(json: JsValue): EventState =
    CompleteReviewEvent(
      (json \ "revisionID").as[Long],
      (json \ "authorID").as[Long]
    )
}

class CancelReviewEventMarshaller extends EventStateMarshaller {
  override def toJson(eventState: EventState): JsValue = {
    val cancelReviewEvent = eventState.asInstanceOf[CancelReviewEvent]
    Json.obj(
      "revisionID" -> cancelReviewEvent.revisionID,
      "authorID" -> cancelReviewEvent.authorID
    )
  }

  override def fromJson(json: JsValue): EventState =
    CancelReviewEvent(
      (json \ "revisionID").as[Long],
      (json \ "authorID").as[Long]
    )
}
