import org.specs2.matcher.XmlMatchers.beEqualToIgnoringSpace
import org.specs2.mutable._
import play2.tools.xml._
import scala.xml.Utility._
import play2.tools.xml.DefaultImplicits._

class EXMLSpec extends Specification {
  case class Foo(id: Long, name: String, age: Int, amount: Float, isX: Boolean, opt: Option[Double], numbers: List[Int], map: Map[String, Short])

  implicit object FooXMLF extends XMLFormatter[Foo] {
    def read(x: xml.NodeSeq): Option[Foo] = {
      for( id <- EXML.fromXML[Long](x \ "id");
        name <- EXML.fromXML[String](x \ "name");
        age <- EXML.fromXML[Int](x \ "age");
        amount <- EXML.fromXML[Float](x \ "amount");
        isX <- EXML.fromXML[Boolean](x \ "isX");
        opt <- EXML.fromXML[Option[Double]](x \ "opt");
        numbers <- EXML.fromXML[List[Int]](x \ "numbers" \ "nb");
        map <- EXML.fromXML[Map[String, Short]](x \ "map" \ "item")
      ) yield(Foo(id, name, age, amount, isX, opt, numbers, map))
    }

    def write(f: Foo, base: xml.NodeSeq): xml.NodeSeq = {
      <foo>
        <id>{ f.id }</id>
        <name>{ f.name }</name>
        <age>{ f.age }</age>
        <amount>{ f.amount }</amount>
        <isX>{ f.isX }</isX>
        { EXML.toXML(f.opt, <opt/>) }
        <numbers>{ EXML.toXML(f.numbers, <nb/>) }</numbers>
        <map>{ EXML.toXML(f.map, <item/>) }</map>
      </foo>
    }
  }

  "EXML" should {
    "serialize XML" in {
        EXML.toXML(Foo(1234L, "albert", 23, 123.456F, true, None, List(123, 57), Map("alpha" -> 23.toShort, "beta" -> 87.toShort))) must beEqualToIgnoringSpace(
          <foo>
            <id>1234</id>
            <name>albert</name>
            <age>23</age>
            <amount>123.456</amount>
            <isX>true</isX>
            <opt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
            <numbers>
              <nb>123</nb>
              <nb>57</nb>
            </numbers>
            <map>
              <item><key>alpha</key><value>23</value></item>
              <item><key>beta</key><value>87</value></item>
            </map>
          </foo>
        )
    }

    "deserialize XML with option nil=true" in {
      EXML.fromXML[Foo](<foo>
            <id>1234</id>
            <name>albert</name>
            <age>23</age>
            <amount>123.456</amount>
            <isX>true</isX>
            <opt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true" />
            <numbers>
              <nb>123</nb>
              <nb>57</nb>
            </numbers>
            <map>
              <item><key>alpha</key><value>23</value></item>
              <item><key>beta</key><value>87</value></item>
            </map>
          </foo>) must equalTo(Some(Foo(1234L, "albert", 23, 123.456F, true, None, List(123, 57), Map("alpha" -> 23.toShort, "beta" -> 87.toShort))))
    }

    "deserialize XML" in {
      EXML.fromXML[Foo](<foo>
            <id>1234</id>
            <name>albert</name>
            <age>23</age>
            <amount>123.456</amount>
            <isX>true</isX>
            <numbers>
              <nb>123</nb>
              <nb>57</nb>
            </numbers>
            <map>
              <item><key>alpha</key><value>23</value></item>
              <item><key>beta</key><value>87</value></item>
            </map>
          </foo>) must equalTo(Some(Foo(1234L, "albert", 23, 123.456F, true, None, List(123, 57), Map("alpha" -> 23.toShort, "beta" -> 87.toShort))))
    }

    "deserialize XML to None if error" in {
      EXML.fromXML[Foo](<foo>
            <id>1234</id>
            <name>123</name>
            <age>fd</age>
            <amount>float</amount>
            <isX>true</isX>
          </foo>) must equalTo(None)
    }

    "deserialize Int accordingly to Some or None" in {
      EXML.fromXML[Int](<ab>123</ab>) must equalTo(Some(123))
      EXML.fromXML[Int](<ab>abc</ab>) must equalTo(None)
      EXML.fromXML[Int](<ab>12</ab> \\ "tag") must equalTo(None)
    }
    
    "deserialize Short accordingly to Some or None" in {
      EXML.fromXML[Short](<ab>123</ab>) must equalTo(Some(123))
      EXML.fromXML[Short](<ab>abc</ab>) must equalTo(None)
      EXML.fromXML[Short](<ab>12</ab> \\ "tag") must equalTo(None)
    }
    
    "deserialize Long accordingly to Some or None" in {
      EXML.fromXML[Long](<ab>123</ab>) must equalTo(Some(123))
      EXML.fromXML[Long](<ab>abc</ab>) must equalTo(None)
      EXML.fromXML[Long](<ab>12</ab> \\ "tag") must equalTo(None)
    }
    
    "deserialize Float accordingly to Some or None" in {
      EXML.fromXML[Float](<ab>123</ab>) must equalTo(Some(123))
      EXML.fromXML[Float](<ab>abc</ab>) must equalTo(None)
      EXML.fromXML[Float](<ab>12</ab> \\ "tag") must equalTo(None)
    }
    "deserialize Double accordingly to Some or None" in {
      EXML.fromXML[Double](<ab>123</ab>) must equalTo(Some(123))
      EXML.fromXML[Double](<ab>abc</ab>) must equalTo(None)
      EXML.fromXML[Double](<ab>12</ab> \\ "tag") must equalTo(None)
    }
    "deserialize Boolean accordingly to Some or None" in {
      EXML.fromXML[Boolean](<ab>true</ab>) must equalTo(Some(true))
      EXML.fromXML[Boolean](<ab>abc</ab>) must equalTo(None)
      EXML.fromXML[Boolean](<ab>12</ab> \\ "tag") must equalTo(None)
    }
  }
}
