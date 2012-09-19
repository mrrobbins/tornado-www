
package data

import data.format.Formats._
import play.api.data.Forms._

object Forms {
  val doubleDecimal = of[Double]
  val decimal = of[Float]
}
