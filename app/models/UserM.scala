package models

import anorm._
import anorm.RowParser
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB
import play.api.Logger

case class UserM(
  id: Pk[Int],
  first_name: String,
  last_name: String,
  email: String,
  deleted: Boolean
)


object UserM {

  val userParser: RowParser[UserM] = {
    import anorm.~
    get[Pk[Int]]("id") ~
    get[String]("first_name") ~
    get[String]("last_name") ~
    get[String]("email") ~ 
    get[Boolean]("deleted") map {
      case id ~ first_name ~ last_name ~ email ~ deleted =>
        UserM(
          id,
          first_name,
          last_name,
          email,
          deleted
        )
    } 
  }
  def getUser(id: Int) = Helper.getSingle[UserM](id, "users", "id", userParser)
  def getUsers = Helper.getAllSort[UserM]("users", "first_name", false, userParser)

  def getUserFullName(id: Int) = getUser(id).map(user => user.first_name + " " + user.last_name)

  def addUser(user: UserM) = DB.withConnection{
    implicit connection =>
    try{
      SQL("""INSERT INTO users VALUES ({id},{first_name},{last_name},{email},{deleted})""").on(
        "id" -> user.id,
        "first_name" -> user.first_name,
        "last_name" -> user.last_name,
        "email" -> user.email,
        "deleted" -> user.deleted
      ).executeUpdate() match {
        case 1 => None
        case _ => Some("Not added")

      }
    }
    catch{
      case e => {
        Logger.error(e.toString)
        Some(e.toString)
      }

    }
  }

  def deleteUser(id: Int) = Helper.softDelete("users", "deleted", "id", id)

  def getPrimaryResponderIncidents(user_id: Int) = DB.withConnection{
    implicit connection =>
    SQL(
      """
      SELECT * FROM incidents WHERE primary_responder = {user_id}
      """
    ).on("user_id" -> user_id).as(IncidentM.incidentParser *)
  }


  //Get incidents where user is part of the response team
  def getResponseTeamIncidents(user_id: Int) = DB.withConnection{
    implicit connection =>
    SQL(
      """
      SELECT incidents.* FROM incidents
      JOIN user_team_map
      ON incidents.respond_team_id = user_team_map.team_id
      WHERE user_team_map.user_id = {user_id}
      """
    ).on("user_id" -> user_id).as(IncidentM.incidentParser *)
  }

  //Gets all incidents user is subscribed to through their teams
  def getSubscriptions(user_id: Int) = DB.withConnection{
    implicit connection =>
    SQL(
      """
      SELECT incidents.* FROM incidents 
      JOIN incident_subscriptions
      ON incident_subscriptions.incident_id = incidents.id
      JOIN user_team_map
      ON user_team_map.team_id = incident_subscriptions.team_id
      WHERE user_team_map.user_id = {user_id}
      ORDER BY incidents.title
      """
    ).on(
      "user_id" -> user_id
    ).as(IncidentM.incidentParser *)
  }



}