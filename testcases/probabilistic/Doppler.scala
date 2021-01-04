

import daisy.lang._
import Real._


object Doppler {

  def doppler1(u: Real, v: Real, T: Real): Real = {
    require(-100.0 <= u && u <= 100 && 20 <= v && v <= 20000 && -30 <= T && T <= 50)// && u + T <= 100)

 	((-(3314e-1 + (6e-1 * T)) * v) / (((3314e-1 + (6e-1 * T)) + u) * ((3314e-1 + (6e-1 * T)) + u)))

  }


  def doppler2(u: Real, v: Real, T: Real): Real = {
    require(-125.0 <= u && u <= 125 && 15 <= v && v <= 25000 && -40 <= T && T <= 60)// && u + T <= 100)

 	((-(3314e-1 + (6e-1 * T)) * v) / (((3314e-1 + (6e-1 * T)) + u) * ((3314e-1 + (6e-1 * T)) + u)))

  }


  def doppler3(u: Real, v: Real, T: Real): Real = {
    require(-30.0 <= u && u <= 120 && 320 <= v && v <= 20300 && -50 <= T && T <= 30)// && u + T <= 100)

	((-(3314e-1 + (6e-1 * T)) * v) / (((3314e-1 + (6e-1 * T)) + u) * ((3314e-1 + (6e-1 * T)) + u)))

  }


}
