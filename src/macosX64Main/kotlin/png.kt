package org.jonnyzzz.kotlin.mpp.clipboard

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import org.jonnyzzz.png.*

private interface Deferred {
  fun defer(a: () -> Unit)
}

private fun <T> withDefer(action: Deferred.() -> T) : T {
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

fun readPng(data: ByteArray) = memScoped {
  withDefer {
    /* initialize stuff */
    val png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, null, null, null)
            ?: error("[read_png_file] png_create_read_struct failed")

    defer {

      //TODO: create pointer
      png_destroy_read_struct(png_ptr., null, null)
    }

    val info_ptr = png_create_info_struct(png_ptr) ?: error("[read_png_file] png_create_info_struct failed");

//    if (setjmp(png_jmpbuf(png_ptr)))
//      abort_("[read_png_file] Error during init_io");

    png_init_io(png_ptr, fp);
    png_set_sig_bytes(png_ptr, 8);

    png_read_info(png_ptr, info_ptr);

    width = png_get_image_width(png_ptr, info_ptr);
    height = png_get_image_height(png_ptr, info_ptr);
    color_type = png_get_color_type(png_ptr, info_ptr);
    bit_depth = png_get_bit_depth(png_ptr, info_ptr);

    number_of_passes = png_set_interlace_handling(png_ptr);
    png_read_update_info(png_ptr, info_ptr);


/* read file */
    if (setjmp(png_jmpbuf(png_ptr)))
      abort_("[read_png_file] Error during read_image");

    row_pointers = (png_bytep *) malloc (sizeof(png_bytep) * height);
    for (y= 0; y < height; y++)
    row_pointers[y] = (png_byte *) malloc (png_get_rowbytes(png_ptr, info_ptr));

    png_read_image(png_ptr, row_pointers);

    fclose(fp);
  }
}