package sol.fv.qr

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
}
