import org.specs2.mutable._
import play2.tools.xml._
import scala.xml.Utility._
import play2.tools.xml.DefaultImplicits._
import scala.xml.{Attribute, NamespaceBinding}

class ESOAPSpec extends Specification {
  implicit val ns = NamespaceBinding(
    prefix = "test", 
    uri = "http://test.com/",
    parent = ESOAP.SoapNS
  )
  case class Foo(id: Long, name: String, age: Int, amount: Float, isX: Boolean, opt: Option[Double], numbers: List[Int], map: Map[String, Short])

  implicit object FooXMLF extends XMLFormatter[Foo] {
    def read(x: xml.NodeSeq): Option[Foo] = {
      for( foo <- (x \ "foo").headOption;
        id <- EXML.fromXML[Long](foo \ "id");
        name <- EXML.fromXML[String](foo \ "name");
        age <- EXML.fromXML[Int](foo \ "age");
        amount <- EXML.fromXML[Float](foo \ "amount");
        isX <- EXML.fromXML[Boolean](foo \ "isX");
        opt <- EXML.fromXML[Option[Double]](foo \ "opt");
        numbers <- EXML.fromXML[List[Int]](foo \ "numbers" \ "nb");
        map <- EXML.fromXML[Map[String, Short]](foo \ "map" \ "item")
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

  "ESOAP" should {
    "serialize SOAP" in {
        ESOAP.toSOAP(Foo(1234L, "albert", 23, 123.456F, true, None, List(123, 57), Map("alpha" -> 23.toShort, "beta" -> 87.toShort))) must beEqualToIgnoringSpace(
          <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:test="http://test.com">
            <soapenv:Header/>
            <soapenv:Body>
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
            </soapenv:Body>
          </soapenv:Envelope>
        )
    }

    "deserialize SOAP" in {
      ESOAP.fromSOAP[Foo](
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:test="http://test.com">
          <soapenv:Header/>
          <soapenv:Body>
          <foo>
            <id>1234</id>
            <name>albert</name>
            <age>23</age>
            <amount>123.456</amount>
            <isX>true</isX>
            <opt>987.654</opt>
            <numbers>
              <nb>123</nb>
              <nb>57</nb>
            </numbers>
            <map>
              <item><key>alpha</key><value>23</value></item>
              <item><key>beta</key><value>87</value></item>
            </map>
          </foo>
          </soapenv:Body>
        </soapenv:Envelope>) must equalTo(Some(Foo(1234L, "albert", 23, 123.456F, true, Some(987.654), List(123, 57), Map("alpha" -> 23.toShort, "beta" -> 87.toShort))))
    }

    "deserialize SOAP to None if error" in {
      ESOAP.fromSOAP[Foo](<foo>
            <id>1234</id>
            <name>123</name>
            <age>fd</age>
            <amount>float</amount>
            <isX>true</isX>
          </foo>) must equalTo(None)
    }

    "deserialize SOAP fault that it previously generated" in {
      val savon = ESOAP.toSOAP(
        SoapFault(
          faultcode = SoapFault.FAULTCODE_SERVER,
          faultstring = "Super error",
          faultactor = "http://uriToError.com",
          detail = "erreur"
          )
        )
      val fault = ESOAP.fromSOAP[SoapFault[String]](savon)
      fault must beSome
      fault.get.faultcode must equalTo(SoapFault.FAULTCODE_SERVER)
      fault.get.faultstring must equalTo("Super error")
    }

    "return None if faultcode parameter is missing" in {
      val soapMessage = <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://interfacedepot.acsia.gip_info_retraite.fr/types/">
          <soapenv:Body>
            <soapenv:Fault>
              <soapenv:faultstring xml:lang="?">2</soapenv:faultstring>
              <soapenv:faultactor>3</soapenv:faultactor>
              <detail>
                Message
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
      val res = ESOAP.fromSOAP[SoapFault[String]](soapMessage)
      res must beNone
    }
    "return None if faultstring parameter is missing" in {
      val soapMessage = <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://interfacedepot.acsia.gip_info_retraite.fr/types/">
          <soapenv:Body>
            <soapenv:Fault>
              <soapenv:faultcode>1</soapenv:faultcode>
              <soapenv:faultactor>3</soapenv:faultactor>
              <detail>
                Message
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
      val res = ESOAP.fromSOAP[SoapFault[String]](soapMessage)
      res must beNone
    }
    "return None if faultactor parameter is missing" in {
      val soapMessage = <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://interfacedepot.acsia.gip_info_retraite.fr/types/">
          <soapenv:Body>
            <soapenv:Fault>
              <soapenv:faultcode>1</soapenv:faultcode>
              <soapenv:faultstring xml:lang="?">2</soapenv:faultstring>
              <detail>
                Message
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
      val res = ESOAP.fromSOAP[SoapFault[String]](soapMessage)
      res must beNone
    }
    "return None if detail parameter is missing" in {
      val soapMessage = <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://interfacedepot.acsia.gip_info_retraite.fr/types/">
          <soapenv:Body>
            <soapenv:Fault>
              <soapenv:faultcode>1</soapenv:faultcode>
              <soapenv:faultstring xml:lang="?">2</soapenv:faultstring>
              <soapenv:faultactor>3</soapenv:faultactor>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>
      val res = ESOAP.fromSOAP[SoapFault[String]](soapMessage)
      res must beNone
    }

    "deserialize SOAP fault generated by third party" in {
      case class ComplexObject(param1 : String, param2: String)
      implicit object ComplexObjectReader extends XMLReader[ComplexObject] {
        def read(x : xml.NodeSeq) : Option[ComplexObject] = {
          for( msg <- (x \ "message").headOption; 
               param1 <- EXML.fromXML[String](msg \ "param1"); 
               param2 <- EXML.fromXML[String](msg \ "param2")  
               )
            yield(ComplexObject(param1, param2))
        }        
      }

      val soapMessage = <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://interfacedepot.acsia.gip_info_retraite.fr/types/">
          <soapenv:Body>
            <soapenv:Fault>
              <soapenv:faultcode>1</soapenv:faultcode>
              <soapenv:faultstring xml:lang="?">2</soapenv:faultstring>
              <soapenv:faultactor>3</soapenv:faultactor>
              <detail>
                <message>
                  <param1>Textual informations</param1>
                  <param2>More informations</param2>
                </message>
              </detail>
            </soapenv:Fault>
          </soapenv:Body>
        </soapenv:Envelope>

      val fault = ESOAP.fromSOAP[SoapFault[ComplexObject]](soapMessage)
      fault must beSome
      fault.get.faultcode must equalTo("1")
      fault.get.detail.param1 must equalTo("Textual informations")
      fault.get.detail.param2 must equalTo("More informations")
    }
  }
}
