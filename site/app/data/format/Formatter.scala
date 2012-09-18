
package data.format

import play.api.data.format.Formatter
import play.api.data.FormError
import play.api.data.format.Formats._

object Formats {

	import scala.util.control.Exception.allCatch
	import java.lang.{Double => JDouble}
	import java.lang.{Float => JFloat}

	implicit def doubleFormat = new Formatter[Double] {
		override val format = Some("format.numeric", Nil)

			def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Double] = {
				stringFormat.bind(key, data).right.flatMap { s =>
					val errorOrDouble = allCatch[Double].either(JDouble.parseDouble(s))
					errorOrDouble.left.map(e => Seq(FormError(key, "error.number", Nil)))
				}

			}

		def unbind(key: String, value: Double) = Map(key -> value.toString)

	}

	implicit def floatFormat = new Formatter[Float] {
		override val format = Some("format.numeric", Nil)

			def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Float] = {
				stringFormat.bind(key, data).right.flatMap { s =>
					val errorOrFloat = allCatch[Float].either(JFloat.parseFloat(s))
					errorOrFloat.left.map(e => Seq(FormError(key, "error.number", Nil)))
				}

			}

		def unbind(key: String, value: Float) = Map(key -> value.toString)

	}

}
