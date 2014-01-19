package controllers

import play.api._
import play.api.mvc._
import play2.tools.xml._
import play2.tools.xml.DefaultImplicits._

object Application extends Controller {
  case class Foo(id: Long, name: String, age: Option[Int])

  implicit object FooXMLF extends XMLFormatter[Foo] {
  	def read(x: xml.NodeSeq): Option[Foo] = {
    	for( 
        id <- EXML.fromXML[Long](x \ "id");
      	name <- EXML.fromXML[String](x \ "name");
        age <- EXML.fromXML[Option[Int]](x \ "age")
      ) yield(Foo(id, name, age))
    }

    def write(f: Foo, base: xml.NodeSeq): xml.NodeSeq = {
      <foo>
        <id>{ f.id }</id>
        <name>{ f.name }</name>
        { EXML.toXML(f.age, <age/>) }
      </foo>
    }
  }  

  def index = Action {
    Ok("coucou")
  }

  def foo = Action(parse.xml) { request =>
    EXML.fromXML[Foo](request.body).map { foo =>
      Ok(EXML.toXML(foo))
    }.getOrElse{
      BadRequest("Expecting Foo XML data")
    }
  }
  
}