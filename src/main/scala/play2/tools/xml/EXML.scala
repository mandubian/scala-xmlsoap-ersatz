package play2.tools.xml

import scala.xml.{Attribute, NamespaceBinding}
import scala.collection._

trait XMLReader[T] {
	// No error management for the time being... maybe later
    def read(x: xml.NodeSeq): Option[T]
}

trait XMLWriter[-T] {
    def write(t: T, base: xml.NodeSeq): xml.NodeSeq
}

trait XMLFormatter[T] extends XMLReader[T] with XMLWriter[T]

object EXML extends EXML

trait EXML {
    def toXML[T](t: T, base: xml.NodeSeq = xml.NodeSeq.Empty)(implicit w: XMLWriter[T]): xml.NodeSeq = w.write(t, base)
    def fromXML[T](x: xml.NodeSeq)(implicit r: XMLReader[T]): Option[T] = r.read(x)   
}

object BasicReaders extends BasicReaders

trait BasicReaders {
        import scala.util.control.Exception._

	implicit object StringReader extends XMLReader[String] {
		def read(x: xml.NodeSeq): Option[String] = if(x.isEmpty) None else Some(x.text)
	}

	implicit object IntReader extends XMLReader[Int] {
		def read(x: xml.NodeSeq): Option[Int] = if(x.isEmpty) None else catching(classOf[NumberFormatException]) opt x.text.toInt
	}

	implicit object LongReader extends XMLReader[Long] {
		def read(x: xml.NodeSeq): Option[Long] =  if(x.isEmpty) None else catching(classOf[NumberFormatException]) opt x.text.toLong
	}

	implicit object ShortReader extends XMLReader[Short] {
		def read(x: xml.NodeSeq): Option[Short] =  if(x.isEmpty) None else catching(classOf[NumberFormatException]) opt x.text.toShort
	}

	implicit object FloatReader extends XMLReader[Float] {
		def read(x: xml.NodeSeq): Option[Float] =  if(x.isEmpty) None else catching(classOf[NumberFormatException]) opt x.text.toFloat
	}

	implicit object DoubleReader extends XMLReader[Double] {
		def read(x: xml.NodeSeq): Option[Double] =  if(x.isEmpty) None else catching(classOf[NumberFormatException]) opt x.text.toDouble
	}

	implicit object BooleanReader extends XMLReader[Boolean] {
		def read(x: xml.NodeSeq): Option[Boolean] =  if(x.isEmpty) None else catching(classOf[NumberFormatException]) opt x.text.toBoolean
	}
}

object SpecialReaders extends SpecialReaders 

trait SpecialReaders {
	implicit def OptionReader[T](implicit r: XMLReader[T]) = new XMLReader[Option[T]] {
		def read(x: xml.NodeSeq): Option[Option[T]] = {
			x.collectFirst{ case e: xml.Elem => e }.map{ e => 
				if(e.attributes.exists{ a => a.key == "nil" && a.value.text == "true" }) None
				else r.read(e)
			}.orElse(Some(None))
		}
	}

	implicit def traversableReader[F[_], A](implicit bf: generic.CanBuildFrom[F[_], A, F[A]], r: XMLReader[A]) = new XMLReader[F[A]] {
		def read(x: xml.NodeSeq): Option[F[A]] = {
			val builder = bf()
          	x.foreach{ n => r.read(n).foreach{ builder += _ } }
			Some(builder.result)
		}
	}

	implicit def mapReader[K, V](implicit rk: XMLReader[K], rv: XMLReader[V]): XMLReader[collection.immutable.Map[K,V]] = new XMLReader[collection.immutable.Map[K,V]] {
		def read(x: xml.NodeSeq): Option[collection.immutable.Map[K, V]] = {
			Some(x.collect{ case e: xml.Elem => 
				for(k <- EXML.fromXML[K](e \ "key"); 
					v <- EXML.fromXML[V](e \ "value")
				) yield( k -> v ) 
			}.filter(_.isDefined).map(_.get).toMap[K,V])
		}
	}
}

object BasicWriters extends BasicWriters

trait BasicWriters {
	implicit object StringWriter extends XMLWriter[String] {
		def write(s: String, base: xml.NodeSeq): xml.NodeSeq = base.collectFirst{ case e: xml.Elem => e.copy(child = xml.Text(s)) }.getOrElse(xml.Text(s))
	}

	implicit object IntWriter extends XMLWriter[Int] {
		def write(s: Int, base: xml.NodeSeq): xml.NodeSeq = StringWriter.write(s.toString, base)
	}

	implicit object LongWriter extends XMLWriter[Long] {
		def write(s: Long, base: xml.NodeSeq): xml.NodeSeq = StringWriter.write(s.toString, base)
	}

	implicit object FloatWriter extends XMLWriter[Float] {
		def write(s: Float, base: xml.NodeSeq): xml.NodeSeq = StringWriter.write(s.toString, base)
	}

	implicit object ShortWriter extends XMLWriter[Short] {
		def write(s: Short, base: xml.NodeSeq): xml.NodeSeq = StringWriter.write(s.toString, base)
	}

	implicit object DoubleWriter extends XMLWriter[Double] {
		def write(s: Double, base: xml.NodeSeq): xml.NodeSeq = StringWriter.write(s.toString, base)
	}

	implicit object BooleanWriter extends XMLWriter[Boolean] {
		def write(s: Boolean, base: xml.NodeSeq): xml.NodeSeq = StringWriter.write(s.toString, base)
	}
}

object SpecialWriters extends SpecialWriters 

trait SpecialWriters {

	val xsiNS = xml.NamespaceBinding("xsi", "http://www.w3.org/2001/XMLSchema-instance", xml.TopScope)

	implicit def optionWriter[T](implicit w: XMLWriter[T]) = new XMLWriter[Option[T]] {
		def write(t: Option[T], base: xml.NodeSeq) = {
			t match {
		    	case None => base.collectFirst{ case e: xml.Elem => e.copy(scope = xsiNS) % Attribute("xsi", "nil", "true", xml.Null) }.getOrElse(xml.NodeSeq.Empty)
		    	case Some(t) => w.write(t, base)
	    	}
	    }
    }

    implicit def traversableWriter[T](implicit w: XMLWriter[T]) = new XMLWriter[Traversable[T]] {
    	def write(t: Traversable[T], base: xml.NodeSeq) = {
			t.foldLeft(xml.NodeSeq.Empty)( (acc, n) => acc ++ w.write(n, base) )
	    }
    }

    implicit def mapWriter[K, V](implicit kw: XMLWriter[K], vw: XMLWriter[V]) = new XMLWriter[Map[K, V]] {
    	def write(m: Map[K, V], base: xml.NodeSeq) = {
    		m.foldLeft(xml.NodeSeq.Empty){ (acc, n) => 
    			base.collectFirst{ case e:xml.Elem => 
    				e.copy( child = kw.write(n._1, <key/>) ++ vw.write(n._2, <value/>) ) 
    			}.map( acc ++ _ ).getOrElse(acc)
    		}
    	}
    }
}
