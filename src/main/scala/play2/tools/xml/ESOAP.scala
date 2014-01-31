package play2.tools.xml

import scala.xml.{Attribute, NamespaceBinding}

object ESOAP extends ESOAP

trait ESOAP {
    def toSOAP[T](t: T, ns: NamespaceBinding, base: xml.Elem)
    	(implicit r: XMLWriter[T]): xml.NodeSeq = {
    		DefaultImplicits.SoapEnvelopeWriter[T](r).write(SoapEnvelope(t)(ns), base)
    }
    def toSOAP[T](t: T)(implicit r: XMLWriter[T], ns: NamespaceBinding): xml.NodeSeq = {
    	toSOAP(t, ns, <envelope/>)
    }
    def toSOAP[T](t: T, ns: NamespaceBinding)(implicit r: XMLWriter[T]): xml.NodeSeq = {
    	toSOAP(t, ns, <envelope/>)
    }

    def fromSOAP[T](x: xml.NodeSeq)(implicit r: XMLReader[T]): Option[T] = {
    	DefaultImplicits.SoapEnvelopeReader[T](r).read(x) match {
	    	case Some(SoapEnvelope(t)) => Some(t)
	    	case None => None
	    }  
	}

	val SoapNS = xml.NamespaceBinding("soapenv", "http://schemas.xmlsoap.org/soap/envelope/", xml.TopScope)
}


case class SoapEnvelope[T](t: T)(implicit _namespace: NamespaceBinding = ESOAP.SoapNS) {
	def namespace = _namespace
}

case class SoapFault[T](
	faultcode: String,
	faultstring: String,
	faultactor: Option[String] = None,
	detail: Option[T] = Option.empty[T]
)

object SoapFault {
		// Found an invalid namespace for the SOAP Envelope element
	val FAULTCODE_VERSION_MISMATCH = "SOAP-ENV:VersionMismatch" 
	// An immediate child element of the Header element, with the mustUnderstand attribute set to "1", was not understood
	val FAULTCODE_MUST_UNDERSTAND = "SOAP-ENV:MustUnderstand"
	// The message was incorrectly formed or contained incorrect information
	val FAULTCODE_CLIENT = "SOAP-ENV:Client"
	// There was a problem with the server so the message could not proceed
	val FAULTCODE_SERVER = "SOAP-ENV:Server"
}


object DefaultImplicits extends DefaultImplicits

trait DefaultImplicits extends DefaultSOAPFormatters with BasicReaders with SpecialReaders with BasicWriters with SpecialWriters

trait DefaultSOAPFormatters {
	
    implicit def SoapEnvelopeReader[T](implicit fmt: XMLReader[T]) = new XMLReader[SoapEnvelope[T]] {
        def read(x: xml.NodeSeq): Option[SoapEnvelope[T]] = {
        	x.collectFirst{ case x:xml.Elem if(x.label == "Envelope") => x}.flatMap { env =>
        		(env \ "Body").headOption.flatMap{ body =>
        			fmt.read(body).map{ t =>
        				SoapEnvelope(t)(env.scope)
        			}
				}
        	}
        }
    }

    implicit def SoapEnvelopeWriter[T](implicit fmt: XMLWriter[T]) = new XMLWriter[SoapEnvelope[T]] {
        def write(st: SoapEnvelope[T], base: xml.NodeSeq) = {
            val env = <soapenv:Envelope>
						<soapenv:Header/>
						<soapenv:Body>
					    { 
					        EXML.toXML(st.t)
					    }
						</soapenv:Body>
					</soapenv:Envelope>
            env.copy(scope = st.namespace)
        }
    }    

    implicit def SoapFaultReader[T](implicit fmt: XMLReader[T], strR:XMLReader[String]) = new XMLReader[SoapFault[T]] {
        def read(x: xml.NodeSeq): Option[SoapFault[T]] = {
        	val envelope = (x \\ "Fault")
                envelope.headOption.flatMap[SoapFault[T]]( {elt => 
                  ( 
                    strR.read(elt \ "faultcode"), strR.read(elt \ "faultstring"), strR.read(elt \ "faultactor"), fmt.read(elt \ "detail") 
                  ) match {
                    case (None,_,_,_) => {println("Code part missing in SOAP Fault"); None} 
                    case (_,None,_,_) => {println("Message part missing in SOAP Fault"); None}
                    case (Some(code),Some(msg), actor, detail) => {Some(SoapFault(code, msg, actor, detail))}
                    case _ => None
                  }
                })
        }
    }

    implicit def SoapFaultWriter[T](implicit fmt: XMLWriter[T]) = new XMLWriter[SoapFault[T]] {
        def write(fault: SoapFault[T], base: xml.NodeSeq) = {
			<soapenv:Fault>
				<faultcode>{ fault.faultcode }</faultcode>
				<faultstring>{ fault.faultstring }</faultstring>
				{ if (fault.faultactor.isDefined)
				  <faultactor>{ fault.faultactor.get }</faultactor> }
				{ if (fault.detail.isDefined)
				  <detail>{ EXML.toXML(fault.detail.get) }</detail> }
			</soapenv:Fault>
    	}
    }   
}
