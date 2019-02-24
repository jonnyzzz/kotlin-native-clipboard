package org.jonnyzzz.kotlin.mpp.clipboard

import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class Color(val r: UByte, val g: UByte, val b: UByte) {
  val L: Double
    get() {
      //https://stackoverflow.com/questions/17615963/standard-rgb-to-grayscale-conversion
      val l: Double = (0.0
              +
              0.2126 * r.toInt().toDouble() / 256.0
              +
              0.7152 * g.toInt().toDouble() / 256.0
              +
              0.0722 * b.toInt().toDouble() / 256.0
              )

      return when {
        l <= 0.0031308 -> 12.92 * l
        else -> 1.055 * l.pow(1.0 / 2.4) - 0.055
      }
    }

  companion object {
    val ZERO = Color(0U, 0U, 0U)
  }
}


class Image(val width: Int,
            val height: Int
) {
  private val buff = Array(height) { Array(width) { Color.ZERO } }

  fun getPixel(x: Int, y: Int) = buff[y][x]

  fun setPixel(x: Int, y: Int, p: Color) {
    buff[y][x] = p
  }

  fun forEachPixel(ƒ: (Int, Int, Color) -> Unit) {
    for (y in 0 until height) {
      for (x in 0 until width) {
        ƒ(x, y, getPixel(x, y))
      }
    }
  }
}

data class LStat(val avd: Double,
                 val min: Double,
                 val max: Double,
                 val sigma : Double,
                 val diff: Double = max - min)

fun Image.LStat(): LStat {
  var agv = 0.0
  var min = 1.0
  var max = 0.0
  var count = 0

  forEachPixel { _, _, it ->
    val L = it.L
    agv += L
    min = min(min, L)
    max = max(max, L)
    count++
  }

  agv /= count
  var d = 0.0
  forEachPixel { _, _, it ->
    d += (it.L - agv).pow(2)
  }

  d /= count - 1
  d = d.pow(0.5)

  return LStat(agv, min, max, d)
}


fun imageToASCII(image: Image) = buildString {
  //https://www.lifewire.com/aspect-ratio-table-common-fonts-3467385
  val consoleAspectRation = 0.43

  val cW = 70
  val cH = (cW * image.height / image.width * consoleAspectRation).roundToInt()

  println("Rendering image to ${cW}x${cH}...")

  val xPerDot = image.width / cW
  val yPerDot = image.height / cH
  println("Averaging ${xPerDot}x${yPerDot} pixes per one char")


  val L = image.LStat()
  println("averageL = $L")

  if (L.diff <= 1E-2) {
    appendln("-- empty image")
    return@buildString
  }

  for (aY in 0 until cH) {
    for (aX in 0 until cW) {

      var c = 0.0
      var count = 0
      for (x in (0 until xPerDot).map { it + aX * xPerDot }.filter { it < image.width }) {
        for (y in (0 until yPerDot).map { it + aY * yPerDot }.filter { it < image.height }) {
          c = max(c, (image.getPixel(x, y).L - L.avd).absoluteValue)
          count++
        }
      }

      /// L is from min..max => L / diff from 0 .. 1
      /// max | p - L | from  0 .. diff
      /// =>
      /// max | p - L | / diff  ==> 0 .. 1
      val q = c / L.diff
      val ch = when {
        q >= 0.8 -> 'X'
        q >= 0.6 -> 'I'
        q >= 0.4 -> '='
        q >= 0.2 -> '-'
        q >= 0.1 -> '.'
        else -> ' '
      }

      append(ch)
    }
    appendln()
  }
}

