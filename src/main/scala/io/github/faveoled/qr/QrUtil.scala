package io.github.faveoled.qr

object QrUtil {

  def printQr(qr: QrCode) = {
    println(buildQrTermString(qr))
  }

  def buildQrTermString(qr: QrCode): String = {
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

  def toSvgString(qr: QrCode, border: Int, lightColor: String, darkColor: String) = {
    if (border < 0) {
      throw new IllegalArgumentException("Border must be non-negative")
    }
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
