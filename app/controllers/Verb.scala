package controllers

import play.api.mvc.Call

case class Verb(name: String, endPoint: Call)

object Verbs {
  def builder(): VerbsBuilder = new VerbsBuilder();
}

class VerbsBuilder {
  var verbs: List[Verb] = List()

  def add(name: String, endPoint: Call) = {
    verbs = verbs.+:(Verb(name, endPoint))
    this
  }

  def addIf(cond: Boolean, name: String, endPoint: Call) = {
    if (cond) add(name, endPoint) else this
  }

  def create: List[Verb] = verbs
}