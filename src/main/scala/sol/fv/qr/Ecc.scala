package sol.fv.qr

enum Ecc(val formatBits: Int):

  // Must be declared in ascending order of error protection
  // so that the implicit ordinal() and values() work properly

  case LOW extends Ecc    (1)
  /** The QR Code can tolerate about 15% erroneous codewords. */
  case MEDIUM extends Ecc    (0)
  /** The QR Code can tolerate about 25% erroneous codewords. */
  case QUARTILE extends Ecc    (3)
  /** The QR Code can tolerate about 30% erroneous codewords. */
  case HIGH extends Ecc    (2)


