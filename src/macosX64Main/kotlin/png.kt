package org.jonnyzzz.kotlin.mpp.clipboard

import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import org.jonnyzzz.png.PNG_COLOR_TYPE_RGB
import org.jonnyzzz.png.PNG_COLOR_TYPE_RGBA
import org.jonnyzzz.png.PNG_LIBPNG_VER_STRING
import org.jonnyzzz.png.png_byteVar
import org.jonnyzzz.png.png_bytep
import org.jonnyzzz.png.png_bytepVar
import org.jonnyzzz.png.png_create_info_struct
import org.jonnyzzz.png.png_create_read_struct
import org.jonnyzzz.png.png_destroy_read_struct
import org.jonnyzzz.png.png_get_bit_depth
import org.jonnyzzz.png.png_get_color_type
import org.jonnyzzz.png.png_get_image_height
import org.jonnyzzz.png.png_get_image_width
import org.jonnyzzz.png.png_get_io_ptr
import org.jonnyzzz.png.png_get_rowbytes
import org.jonnyzzz.png.png_read_image
import org.jonnyzzz.png.png_read_info
import org.jonnyzzz.png.png_read_update_info
import org.jonnyzzz.png.png_set_interlace_handling
import org.jonnyzzz.png.png_set_read_fn
import org.jonnyzzz.png.png_structp
import org.jonnyzzz.png.png_structpVar
import org.jonnyzzz.png.png_structrp
import platform.posix.size_t
import kotlin.math.max
import kotlin.math.pow

private interface Deferred {
  fun defer(a: () -> Unit)
}

private inline fun <T> withDefer(action: Deferred.() -> T): T {
  val actions = mutableListOf<() -> Unit>()
  val def = object : Deferred {
    override fun defer(a: () -> Unit) {
      actions += a
    }
  }
  try {
    return def.action()
  } finally {
    actions.reversed().forEach { it() }
  }
}

class PngIOSource(val data: ByteArray) {
  private val ref = StableRef.create(this)
  private var off = 0

  fun readNext(size: size_t, to: png_bytep) {
    var toWrite = size
    var i = 0
    while(toWrite-- > 0U) {
      to[i++] = data[off++].toUByte()
    }
  }

  fun install(png_ptr: png_structrp) {
    png_set_read_fn(png_ptr, ref.asCPointer(), readDataFunction)
  }

  companion object {
    private val readDataFunction = staticCFunction<png_structp?, png_bytep?, size_t, Unit> { png_ptr, target, size ->
      val io_ptr = png_get_io_ptr(png_ptr) ?: return@staticCFunction
      val reader = io_ptr.asStableRef<PngIOSource>().get()
      reader.readNext(size, target ?: return@staticCFunction)
    }
  }
}


fun readPng(data: ByteArray): Image = withDefer {
  /* initialize stuff */
  val png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, null, null, null)
          ?: error("[read_png_file] png_create_read_struct failed")

  defer {
    memScoped {
      val ref = alloc<png_structpVar> {
        value = png_ptr
      }
      png_destroy_read_struct(ref.ptr, null, null)
    }
  }

  val info_ptr = png_create_info_struct(png_ptr) ?: error("[read_png_file] png_create_info_struct failed");

  PngIOSource(data).install(png_ptr)
  png_read_info(png_ptr, info_ptr)

  val width = png_get_image_width(png_ptr, info_ptr)
  val height = png_get_image_height(png_ptr, info_ptr)
  val (color_type, pixelSize) = when(val type = png_get_color_type(png_ptr, info_ptr).toInt()) {
    PNG_COLOR_TYPE_RGB -> "rgb" to 3
    PNG_COLOR_TYPE_RGBA -> "rgba" to 4
    else -> "UnknownType($type)" to 0
  }
  val bit_depth = png_get_bit_depth(png_ptr, info_ptr)

  val number_of_passes = png_set_interlace_handling(png_ptr);
  png_read_update_info(png_ptr, info_ptr)

  println("png-info: ${width}x$height, color-type=$color_type, bits=$bit_depth, phases=$number_of_passes")

  val image = Image(width.toInt(), height.toInt())
  memScoped {
    val rowSize = png_get_rowbytes(png_ptr, info_ptr).toInt()
    println("rowSize=$rowSize")

    val rows = allocArray<png_bytepVar>(height.toLong()) {
      value = allocArray(rowSize)
    }

    png_read_image(png_ptr, rows)

    for(y in 0 until height.toInt()) {
      val row = rows[y]!!
      for(x in 0 until width.toInt()) {
        val r = row[pixelSize * x].toUByte()
        val g = row[pixelSize * x + 1].toUByte()
        val b = row[pixelSize * x + 2].toUByte()

        image.setPixel(x, y, Color(r,g,b))
      }
    }
  }

  return image
}
