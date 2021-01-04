

import daisy.lang._
import Real._


object Mul {

  def Mul8 (x0: Real, x1: Real, x2: Real, x3: Real, x4: Real, x5: Real, x6: Real, x7: Real) = {
    require(1 <= x0 && x0 <= 2 && 1 <= x1 && x1 <= 2 && 1 <= x2 && x2 <= 2 && 1 <= x3 && x3 <= 2 && 1 <= x4 && x4 <= 2 && 1 <= x5 && x5 <= 2 && 1 <= x6 && x6 <= 2 && 1 <= x7 && x7 <= 2 &&)
		(((((((x0 * x1) * x2) * x3) * x4) * x5) * x6) * x7)
  }

}
