package sol.fv.qr

import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, Path}
import java.util.Objects
import scala.io.Source
import java.nio.charset.StandardCharsets

class QrCodeTest extends AnyFunSuite {

  def readString(path: Path): String = {
    String(Files.readAllBytes(path), StandardCharsets.UTF_8)
  }

  test("basic demo") {
    val text = "Hello, world!" // User-supplied Unicode text
    val qr = QrCode.encodeText(text, Ecc.LOW) // Make the QR Code symbol
    val svg = toSvgString(qr, 4, "#FFFFFF", "#000000")
    val expected = readString(Paths.get("src/test/resources/expected1.svg"))
    assert(expected == svg)
  }

  test("term string") {
    val text = "Hello, world!" // User-supplied Unicode text
    val qr = QrCode.encodeText(text, Ecc.LOW) // Make the QR Code symbol
    val str = buildQrTermString(qr)
    val expected = readString(Paths.get("src/test/resources/expected2.txt"))
    assert(expected == str)
  }

  test("variety 1") {
    val text = "DOLLAR-AMOUNT:$39.87 PERCENTAGE:100.00% OPERATIONS:+-*/" // User-supplied Unicode text
    val qr = QrCode.encodeText(text, Ecc.HIGH) // Make the QR Code symbol
    val actual = buildQrTermString(qr)
    val expected = readString(Paths.get("src/test/resources/expected_variety1.txt"))
    assert(expected == actual)
  }

  test("variety 2") {
    val text = "こんにちwa、世界！ αβγδ" // User-supplied Unicode text
    val qr = QrCode.encodeText(text, Ecc.QUARTILE) // Make the QR Code symbol
    val actual = buildQrTermString(qr)
    val expected = readString(Paths.get("src/test/resources/expected_variety2.txt"))
    assert(expected == actual)
  }

  private def printQr(qr: QrCode) = {
    println(buildQrTermString(qr))
  }

  private def buildQrTermString(qr: QrCode): String = {
    val sb = new StringBuilder()
    val border = 1
    for (y <- -border until (qr.size + border)) {
      for (x <- -border until (qr.size + border)) {
        val cell = if qr.getModule(x, y) then "██" else "  "
        sb.append(cell)
      }
      sb.append(System.getProperty("line.separator"))
    }
    sb.toString()
  }


  private def toSvgString(qr: QrCode, border: Int, lightColor: String, darkColor: String) = {
    Objects.requireNonNull(qr)
    Objects.requireNonNull(lightColor)
    Objects.requireNonNull(darkColor)
    if (border < 0) throw new IllegalArgumentException("Border must be non-negative")
    val brd = border
    val sb = new StringBuilder().append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n").append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n").append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" viewBox=\"0 0 %1$d %1$d\" stroke=\"none\">\n", qr.size + brd * 2)).append("\t<rect width=\"100%\" height=\"100%\" fill=\"" + lightColor + "\"/>\n").append("\t<path d=\"")
    var y = 0
    while (y < qr.size) {
      var x = 0
      while (x < qr.size) {
        if (qr.getModule(x, y)) {
          if (x != 0 || y != 0) sb.append(" ")
          sb.append(String.format("M%d,%dh1v1h-1z", x + brd, y + brd))
        }
        x += 1
      }
      y += 1
    }
    sb.append("\" fill=\"" + darkColor + "\"/>\n").append("</svg>\n")
    sb.toString
  }
}
