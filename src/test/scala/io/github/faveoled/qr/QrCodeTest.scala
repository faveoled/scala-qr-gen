package io.github.faveoled.qr

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
    val svg = QrUtil.toSvgString(qr, 4, "#FFFFFF", "#000000")
    val expected = readString(Paths.get("src/test/resources/expected1.svg"))
    assert(expected == svg)
  }

  test("term string") {
    val text = "Hello, world!" // User-supplied Unicode text
    val qr = QrCode.encodeText(text, Ecc.LOW) // Make the QR Code symbol
    val str = QrUtil.buildQrTermString(qr)
    val expected = readString(Paths.get("src/test/resources/expected2.txt"))
    assert(expected == str)
  }

  test("variety 1") {
    val text = "DOLLAR-AMOUNT:$39.87 PERCENTAGE:100.00% OPERATIONS:+-*/" // User-supplied Unicode text
    val qr = QrCode.encodeText(text, Ecc.HIGH) // Make the QR Code symbol
    val actual = QrUtil.buildQrTermString(qr)
    val expected = readString(Paths.get("src/test/resources/expected_variety1.txt"))
    assert(expected == actual)
  }

  test("variety 2") {
    val text = "こんにちwa、世界！ αβγδ" // User-supplied Unicode text
    val qr = QrCode.encodeText(text, Ecc.QUARTILE) // Make the QR Code symbol
    val actual = QrUtil.buildQrTermString(qr)
    val expected = readString(Paths.get("src/test/resources/expected_variety2.txt"))
    assert(expected == actual)
  }


}
