package org.jonnyzzz.kotlin.mpp.clipboard

import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardItem
import platform.AppKit.NSPasteboardTypePNG
import platform.AppKit.NSPasteboardTypeString
import platform.Foundation.base64EncodedStringWithOptions
import platform.posix.exit


fun main() {
  println("Kotlin/Native clipboard app v0.0.1")

  val board = NSPasteboard.generalPasteboard

  val items = board.pasteboardItems?.filterIsInstance<NSPasteboardItem>() ?: listOf()
  println("There are ${items.size} item(s) in the Pasteboard:")
  for (item in items) {
    println("  ${item.types}")
  }

  val pngPasteboardType = NSPasteboardTypePNG
  val pngImages = items.filter { it.types.filterIsInstance<String>().contains(pngPasteboardType) }
  println("Found ${pngImages.size} PNG image(s)")

  val pngImage = pngImages.singleOrNull()
  if (pngImage == null) {
    println("ERR: One PNG image is required")
    return exit(1)
  }

  val data = pngImage.dataForType(pngPasteboardType)
  if (data == null) {
    println("ERR: The pasteboard object does not have $pngPasteboardType contents")
    return exit(1)
  }

  val base64Data = data.base64EncodedStringWithOptions(0UL)
  val result = "data:application/png;base64,$base64Data"

  println("Encoded PNG: ${result.take(40)}...")

  board.declareTypes(listOf(NSPasteboardTypeString), owner = null)
  board.setString(result, forType = NSPasteboardTypeString)
}


// presentation plan:
// - write code to support markdown image format ![]()
// - demo it live
// - add libpng (another branch) to generate preview and resize the image
// - use libpng to set transparent color ;)
// - add Kotlin-Fracrtals common code to generate fractals PNG urls



