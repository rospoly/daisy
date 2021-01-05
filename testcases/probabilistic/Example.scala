

import daisy.lang._
import Real._


object Example {

  def Example1 (x: Real, y: Real) = {
    require(0 <= x && x <= 10 && 0 <= y && y <= 10)
	(x*y)+x
  }

}
