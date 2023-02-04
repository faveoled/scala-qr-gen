package sol.fv.qr

import sol.fv.qr.QrCode.getNumDataCodewords
import sol.fv.qr.QrCode.getNumRawDataModules
import sol.fv.qr.QrCode.reedSolomonComputeDivisor
import sol.fv.qr.QrCode.reedSolomonComputeRemainder
import sol.fv.qr.QrCode.getNumRawDataModules
import sol.fv.qr.QrCode.getBit
import sol.fv.qr.QrCode.MIN_VERSION
import sol.fv.qr.QrCode.MAX_VERSION
import sol.fv.qr.QrCode.PENALTY_N1
import sol.fv.qr.QrCode.PENALTY_N2
import sol.fv.qr.QrCode.PENALTY_N3
import sol.fv.qr.QrCode.getBit
import sol.fv.qr.QrCode.PENALTY_N4
import sol.fv.qr.QrCode.ECC_CODEWORDS_PER_BLOCK
import sol.fv.qr.QrCode.NUM_ERROR_CORRECTION_BLOCKS

import java.util
import java.util.Arrays
import java.util.Objects
import scala.util.control.Breaks.{break, breakable}
object QrCode {


  /** The minimum version number  (1) supported in the QR Code Model 2 standard. */
  val MIN_VERSION = 1

  /** The maximum version number (40) supported in the QR Code Model 2 standard. */
  val MAX_VERSION = 40


  // For use in getPenaltyScore(), when evaluating which mask is best.
  val PENALTY_N1 = 3
  val PENALTY_N2 = 3
  val PENALTY_N3 = 40
  val PENALTY_N4 = 10

  val ECC_CODEWORDS_PER_BLOCK: Array[Array[Int]] = Array(
    // Version: (note that index 0 is for padding, and is set to an illegal value)
    //0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40    Error correction level
    Array(-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30), // Low
    Array(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28), // Medium
    Array(-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30), // Quartile
    Array(-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30)) // High


  val NUM_ERROR_CORRECTION_BLOCKS: Array[Array[Int]] = Array(
    // Version: (note that index 0 is for padding, and is set to an illegal value)
    //0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40    Error correction level
    Array(-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25), // Low
    Array(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49), // Medium
    Array(-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68), // Quartile
    Array(-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81)) // High


  /*---- Static factory functions (high level) ----*/

  /**
   * Returns a QR Code representing the specified Unicode text string at the specified error correction level.
   * As a conservative upper bound, this function is guaranteed to succeed for strings that have 738 or fewer
   * Unicode code points (not UTF-16 code units) if the low error correction level is used. The smallest possible
   * QR Code version is automatically chosen for the output. The ECC level of the result may be higher than the
   * ecl argument if it can be done without increasing the version.
   *
   * @param text the text to be encoded (not {@code null}), which can be any Unicode string
   * @param ecl  the error correction level to use (not {@code null}) (boostable)
   * @return a QR Code (not {@code null}) representing the text
   * @throws NullPointerException if the text or error correction level is {@code null}
   * @throws DataTooLongException if the text fails to fit in the
   *                              largest version QR Code at the ECL, which means it is too long
   */
  def encodeText(text: CharSequence, ecl: Ecc): QrCode = {
    Objects.requireNonNull(text)
    Objects.requireNonNull(ecl)
    val segs = QrSegment.makeSegments(text)
    encodeSegments(segs, ecl)
  }


  /**
   * Returns a QR Code representing the specified binary data at the specified error correction level.
   * This function always encodes using the binary segment mode, not any text mode. The maximum number of
   * bytes allowed is 2953. The smallest possible QR Code version is automatically chosen for the output.
   * The ECC level of the result may be higher than the ecl argument if it can be done without increasing the version.
   *
   * @param data the binary data to encode (not {@code null})
   * @param ecl  the error correction level to use (not {@code null}) (boostable)
   * @return a QR Code (not {@code null}) representing the data
   * @throws NullPointerException if the data or error correction level is {@code null}
   * @throws DataTooLongException if the data fails to fit in the
   *                              largest version QR Code at the ECL, which means it is too long
   */
  def encodeBinary(data: Array[Byte], ecl: Ecc): QrCode = {
    Objects.requireNonNull(data)
    Objects.requireNonNull(ecl)
    val seg = QrSegment.makeBytes(data)
    encodeSegments(List(seg), ecl)
  }


  /**
   * Returns a QR Code representing the specified segments at the specified error correction
   * level. The smallest possible QR Code version is automatically chosen for the output. The ECC level
   * of the result may be higher than the ecl argument if it can be done without increasing the version.
   * <p>This function allows the user to create a custom sequence of segments that switches
   * between modes (such as alphanumeric and byte) to encode text in less space.
   * This is a mid-level API; the high-level API is {@link # encodeText ( CharSequence, Ecc )}
   * and {@link # encodeBinary ( byte [ ], Ecc )}.</p>
   *
   * @param segs the segments to encode
   * @param ecl  the error correction level to use (not {@code null}) (boostable)
   * @return a QR Code (not {@code null}) representing the segments
   * @throws NullPointerException if the list of segments, any segment, or the error correction level is {@code null}
   * @throws DataTooLongException if the segments fail to fit in the
   *                              largest version QR Code at the ECL, which means they are too long
   */
  def encodeSegments(segs: List[QrSegment], ecl: Ecc): QrCode =
    encodeSegments(segs, ecl, MIN_VERSION, MAX_VERSION, -1, true)


  /**
   * Returns a QR Code representing the specified segments with the specified encoding parameters.
   * The smallest possible QR Code version within the specified range is automatically
   * chosen for the output. Iff boostEcl is {@code true}, then the ECC level of the
   * result may be higher than the ecl argument if it can be done without increasing
   * the version. The mask number is either between 0 to 7 (inclusive) to force that
   * mask, or &#x2212;1 to automatically choose an appropriate mask (which may be slow).
   * <p>This function allows the user to create a custom sequence of segments that switches
   * between modes (such as alphanumeric and byte) to encode text in less space.
   * This is a mid-level API; the high-level API is {@link # encodeText ( CharSequence, Ecc )}
   * and {@link # encodeBinary ( byte [ ], Ecc )}.</p>
   *
   * @param segs       the segments to encode
   * @param ecl        the error correction level to use (not {@code null}) (boostable)
   * @param minVersion the minimum allowed version of the QR Code (at least 1)
   * @param maxVersion the maximum allowed version of the QR Code (at most 40)
   * @param mask       the mask number to use (between 0 and 7 (inclusive)), or &#x2212;1 for automatic mask
   * @param boostEcl   increases the ECC level as long as it doesn't increase the version number
   * @return a QR Code (not {@code null}) representing the segments
   * @throws NullPointerException     if the list of segments, any segment, or the error correction level is {@code null}
   * @throws IllegalArgumentException if 1 &#x2264; minVersion &#x2264; maxVersion &#x2264; 40
   *                                  or &#x2212;1 &#x2264; mask &#x2264; 7 is violated
   * @throws DataTooLongException     if the segments fail to fit in
   *                                  the maxVersion QR Code at the ECL, which means they are too long
   */
  def encodeSegments(segs: List[QrSegment], eclArg: Ecc, minVersion: Int, maxVersion: Int, mask: Int, boostEcl: Boolean): QrCode = {
    var ecl = eclArg
    Objects.requireNonNull(segs)
    Objects.requireNonNull(ecl)
    if (!(MIN_VERSION <= minVersion && minVersion <= maxVersion && maxVersion <= MAX_VERSION) || mask < -1 || mask > 7)
      throw new IllegalArgumentException("Invalid value")
    // Find the minimal version number to use
    var version = 0
    var dataUsedBits = 0
    version = minVersion
    breakable {
      while (true) {
        val dataCapacityBits = getNumDataCodewords(version, ecl) * 8 // Number of data bits available

        dataUsedBits = QrSegment.getTotalBits(segs, version)
        if (dataUsedBits != -1 && dataUsedBits <= dataCapacityBits) {
          break
          // This version number is found to be suitable
        }
        if (version >= maxVersion) { // All versions in the range could not fit the given data
          var msg = "Segment too long"
          if (dataUsedBits != -1) msg = String.format("Data length = %d bits, Max capacity = %d bits", dataUsedBits, dataCapacityBits)
          throw new DataTooLongException(msg)
        }

        version += 1
      }
    }
    assert(dataUsedBits != -1)
    // Increase the error correction level while the data still fits in the current version number
    for (newEcl <- Ecc.values) { // From low to high
      if (boostEcl && dataUsedBits <= getNumDataCodewords(version, newEcl) * 8)  {
        ecl = newEcl
      }
    }
    // Concatenate all segments to create the data bit string
    val bb = new BitBuffer()
    for (seg <- segs) {
      bb.appendBits(seg.mode.modeBits, 4)
      bb.appendBits(seg.numChars, seg.mode.numCharCountBits(version))
      bb.appendData(seg.data)
    }
    assert(bb.bitLength() == dataUsedBits)
    // Add terminator and pad up to a byte if applicable
    val dataCapacityBits = getNumDataCodewords(version, ecl) * 8
    assert(bb.bitLength() <= dataCapacityBits)
    bb.appendBits(0, Math.min(4, dataCapacityBits - bb.bitLength()))
    bb.appendBits(0, (8 - bb.bitLength() % 8) % 8)
    assert(bb.bitLength() % 8 == 0)
    // Pad with alternating bytes until data capacity is reached
    var padByte = 0xEC
    while (bb.bitLength() < dataCapacityBits) {
      bb.appendBits(padByte, 8)
      padByte ^= 0xEC ^ 0x11
    }
    // Pack bits into bytes in big endian
    val dataCodewords = new Array[Byte](bb.bitLength() / 8)
    var i = 0
    while (i < bb.bitLength()) {
      dataCodewords(i >>> 3) = (dataCodewords(i >>> 3).toByte | (bb.getBit(i) << (7 - (i & 7))).toByte).toByte
      i += 1
    }
    // Create the QR Code object
    new QrCode(version, ecl, dataCodewords, mask)
  }


  // Returns the number of data bits that can be stored in a QR Code of the given version number, after
  // all function modules are excluded. This includes remainder bits, so it might not be a multiple of 8.
  // The result is in the range [208, 29648]. This could be implemented as a 40-entry lookup table.
  private def getNumRawDataModules(ver: Int) = {
    if (ver < MIN_VERSION || ver > MAX_VERSION) {
      throw new IllegalArgumentException("Version number out of range")
    }
    val size = ver * 4 + 17
    var result = size * size // Number of modules in the whole QR Code square

    result -= 8 * 8 * 3 // Subtract the three finders with separators

    result -= 15 * 2 + 1 // Subtract the format information and dark module

    result -= (size - 16) * 2 // Subtract the timing patterns (excluding finders)

    // The five lines above are equivalent to: int result = (16 * ver + 128) * ver + 64;
    if (ver >= 2) {
      val numAlign = ver / 7 + 2
      result -= (numAlign - 1) * (numAlign - 1) * 25 // Subtract alignment patterns not overlapping with timing patterns

      result -= (numAlign - 2) * 2 * 20 // Subtract alignment patterns that overlap with timing patterns

      // The two lines above are equivalent to: result -= (25 * numAlign - 10) * numAlign - 55;
      if (ver >= 7) result -= 6 * 3 * 2 // Subtract version information
    }
    assert(208 <= result && result <= 29648)
    result
  }


  // Returns a Reed-Solomon ECC generator polynomial for the given degree. This could be
  // implemented as a lookup table over all possible parameter values, instead of as an algorithm.
  private def reedSolomonComputeDivisor(degree: Int) = {
    if (degree < 1 || degree > 255) {
      throw new IllegalArgumentException("Degree out of range")
    }
    // Polynomial coefficients are stored from highest to lowest power, excluding the leading term which is always 1.
    // For example the polynomial x^3 + 255x^2 + 8x + 93 is stored as the uint8 array {255, 8, 93}.
    val result = new Array[Byte](degree)
    result(degree - 1) = 1 // Start off with the monomial x^0

    // Compute the product polynomial (x - r^0) * (x - r^1) * (x - r^2) * ... * (x - r^{degree-1}),
    // and drop the highest monomial term which is always 1x^degree.
    // Note that r = 0x02, which is a generator element of this field GF(2^8/0x11D).
    var root = 1
    for (i <- 0 until degree) {
      // Multiply the current product by (x - r^i)
      for (j <- 0 until result.length) {
        result(j) = reedSolomonMultiply(result(j) & 0xFF, root).asInstanceOf[Byte]
        if (j + 1 < result.length) {
          result(j) = (result(j) ^ result(j + 1)).toByte
        }
      }
      root = reedSolomonMultiply(root, 0x02)
    }
    result
  }


  // Returns the Reed-Solomon error correction codeword for the given data and divisor polynomials.
  private def reedSolomonComputeRemainder(data: Array[Byte], divisor: Array[Byte]) = {
    Objects.requireNonNull(data)
    Objects.requireNonNull(divisor)
    val result = new Array[Byte](divisor.length)
    for (b <- data) { // Polynomial division
      val factor = (b ^ result(0)) & 0xFF
      System.arraycopy(result, 1, result, 0, result.length - 1)
      result(result.length - 1) = 0
      for (i <- 0 until result.length) {
        result(i) = (result(i) ^ reedSolomonMultiply(divisor(i) & 0xFF, factor)).toByte
      }
    }
    result
  }


  // Returns the product of the two given field elements modulo GF(2^8/0x11D). The arguments and result
  // are unsigned 8-bit integers. This could be implemented as a lookup table of 256*256 entries of uint8.
  private def reedSolomonMultiply(x: Int, y: Int) = {
    assert(x >> 8 == 0 && y >> 8 == 0)
    // Russian peasant multiplication
    var z = 0
    for (i <- 7 to 0 by -1) {
      z = (z << 1) ^ ((z >>> 7) * 0x11D)
      z ^= ((y >>> i) & 1) * x
    }
    assert(z >>> 8 == 0)
    z
  }


  // Returns the number of 8-bit data (i.e. not error correction) codewords contained in any
  // QR Code of the given version number and error correction level, with remainder bits discarded.
  // This stateless pure function could be implemented as a (40*4)-cell lookup table.
  def getNumDataCodewords(ver: Int, ecl: Ecc): Int =
    getNumRawDataModules(ver) / 8
      - ECC_CODEWORDS_PER_BLOCK(ecl.ordinal)(ver)
      * NUM_ERROR_CORRECTION_BLOCKS(ecl.ordinal)(ver)

  // Returns true iff the i'th bit of x is set to 1.
  def getBit(x: Int, i: Int): Boolean = ((x >>> i) & 1) != 0
}

class QrCode {


  /** The version number of this QR Code, which is between 1 and 40 (inclusive).
   * This determines the size of this barcode. */
  var version = 0

  /** The width and height of this QR Code, measured in modules, between
   * 21 and 177 (inclusive). This is equal to version &#xD7; 4 + 17. */
  var size = 0

  /** The error correction level used in this QR Code, which is not {@code null}. */
  var errorCorrectionLevel: Ecc = null

  /** The index of the mask pattern used in this QR Code, which is between 0 and 7 (inclusive).
   * <p>Even if a QR Code is created with automatic masking requested (mask =
   * &#x2212;1), the resulting object still has a mask value between 0 and 7. */
  var mask = 0

  // Private grids of modules/pixels, with dimensions of size*size:

  // The modules of this QR Code (false = light, true = dark).
  // Immutable after constructor finishes. Accessed through getModule().
  private var modules: Array[Array[Boolean]] = null

  // Indicates function modules that are not subjected to masking. Discarded when constructor finishes.
  private var isFunction: Array[Array[Boolean]] = null

  /**
   * Constructs a QR Code with the specified version number,
   * error correction level, data codeword bytes, and mask number.
   * <p>This is a low-level API that most users should not use directly. A mid-level
   * API is the {@link # encodeSegments ( List, Ecc, int, int, int, boolean )} function.</p>
   *
   * @param ver           the version number to use, which must be in the range 1 to 40 (inclusive)
   * @param ecl           the error correction level to use
   * @param dataCodewords the bytes representing segments to encode (without ECC)
   * @param msk           the mask pattern to use, which is either &#x2212;1 for automatic choice or from 0 to 7 for fixed choice
   * @throws NullPointerException     if the byte array or error correction level is {@code null}
   * @throws IllegalArgumentException if the version or mask value is out of range,
   *                                  or if the data is the wrong length for the specified version and error correction level
   */
  def this(ver: Int, ecl: Ecc, dataCodewords: Array[Byte], mskArg: Int) = {
    this()
    var msk = mskArg
    // Check arguments and initialize fields
    if (ver < MIN_VERSION || ver > MAX_VERSION) {
      throw new IllegalArgumentException("Version value out of range")
    }
    if (msk < -1 || msk > 7) {
      throw new IllegalArgumentException("Mask value out of range")
    }
    version = ver
    size = ver * 4 + 17
    errorCorrectionLevel = Objects.requireNonNull(ecl)
    Objects.requireNonNull(dataCodewords)
    modules = Array.ofDim[Boolean](size, size) // Initially all light
    isFunction = Array.ofDim[Boolean](size, size)

    // Compute ECC, draw modules, do masking
    drawFunctionPatterns()
    val allCodewords = addEccAndInterleave(dataCodewords)
    drawCodewords(allCodewords)

    // Do masking
    if (msk == -1) { // Automatically choose best mask
      var minPenalty = Integer.MAX_VALUE
      for (i <- 0 until 8) {
        applyMask(i)
        drawFormatBits(i)
        val penalty = getPenaltyScore
        if (penalty < minPenalty) {
          msk = i
          minPenalty = penalty
        }
        applyMask(i) // Undoes the mask due to XOR
      }
    }
    assert(0 <= msk && msk <= 7)
    mask = msk
    applyMask(msk) // Apply the final choice of mask

    drawFormatBits(msk) // Overwrite old format bits
  }


  /**
   * Returns the color of the module (pixel) at the specified coordinates, which is {@code false}
   * for light or {@code true} for dark. The top left corner has the coordinates (x=0, y=0).
   * If the specified coordinates are out of bounds, then {@code false} (light) is returned.
   *
   * @param x the x coordinate, where 0 is the left edge and size&#x2212;1 is the right edge
   * @param y the y coordinate, where 0 is the top edge and size&#x2212;1 is the bottom edge
   * @return {@code true} if the coordinates are in bounds and the module
   *         at that location is dark, or {@code false} (light) otherwise
   */
  def getModule(x: Int, y: Int): Boolean =
    0 <= x && x < size && 0 <= y && y < size && modules(y)(x)


  // Reads this object's version field, and draws and marks all function modules.
  private def drawFunctionPatterns(): Unit = {
    // Draw horizontal and vertical timing patterns
    var i = 0
    while (i < size) {
      setFunctionModule(6, i, i % 2 == 0)
      setFunctionModule(i, 6, i % 2 == 0)

      i += 1
    }
    // Draw 3 finder patterns (all corners except bottom right; overwrites some timing modules)
    drawFinderPattern(3, 3)
    drawFinderPattern(size - 4, 3)
    drawFinderPattern(3, size - 4)
    // Draw numerous alignment patterns
    val alignPatPos = getAlignmentPatternPositions()
    val numAlign = alignPatPos.length
    for (i <- 0 until numAlign) {
      for (j <- 0 until numAlign) {
        // Don't draw on the three finder corners
        if (!(i == 0 && j == 0 || i == 0 && j == numAlign - 1 || i == numAlign - 1 && j == 0)) drawAlignmentPattern(alignPatPos(i), alignPatPos(j))
      }
    }
    // Draw configuration data
    drawFormatBits(0) // Dummy mask value; overwritten later in the constructor

    drawVersion()
  }


  // Draws two copies of the format bits (with its own error correction code)
  // based on the given mask and this object's error correction level field.
  private def drawFormatBits(msk: Int): Unit = {
    // Calculate error correction code and pack bits
    val data = errorCorrectionLevel.formatBits << 3 | msk // errCorrLvl is uint2, mask is uint3

    var rem = data
    for (i <- 0 until 10) {
      rem = (rem << 1) ^ ((rem >>> 9) * 0x537)
    }
    val bits = (data << 10 | rem) ^ 0x5412 // uint15

    assert(bits >>> 15 == 0)
    // Draw first copy
    for (i <- 0 to 5) {
      setFunctionModule(8, i, getBit(bits, i))
    }
    setFunctionModule(8, 7, getBit(bits, 6))
    setFunctionModule(8, 8, getBit(bits, 7))
    setFunctionModule(7, 8, getBit(bits, 8))
    for (i <- 9 until 15) {
      setFunctionModule(14 - i, 8, getBit(bits, i))
    }
    // Draw second copy
    for (i <- 0 until 8) {
      setFunctionModule(size - 1 - i, 8, getBit(bits, i))
    }
    for (i <- 8 until 15) {
      setFunctionModule(8, size - 15 + i, getBit(bits, i))
    }
    setFunctionModule(8, size - 8, true) // Always dark

  }


  // Draws two copies of the version bits (with its own error correction code),
  // based on this object's version field, iff 7 <= version <= 40.
  private def drawVersion(): Unit = {
    if (version < 7) return
    // Calculate error correction code and pack bits
    var rem = version // version is uint6, in the range [7, 40]

    for (i <- 0 until 12) {
      rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25)
    }
    val bits = version << 12 | rem // uint18

    assert(bits >>> 18 == 0)
    // Draw two copies
    for (i <- 0 until 18) {
      val bit = getBit(bits, i)
      val a = size - 11 + i % 3
      val b = i / 3
      setFunctionModule(a, b, bit)
      setFunctionModule(b, a, bit)
    }
  }


  // Draws a 9*9 finder pattern including the border separator,
  // with the center module at (x, y). Modules can be out of bounds.
  private def drawFinderPattern(x: Int, y: Int): Unit = {
    for (dy <- -4 to 4) {
      for (dx <- -4 to 4) {
        val dist = Math.max(Math.abs(dx), Math.abs(dy)) // Chebyshev/infinity norm

        val xx = x + dx
        val yy = y + dy
        if (0 <= xx && xx < size && 0 <= yy && yy < size) {
          setFunctionModule(xx, yy, dist != 2 && dist != 4)
        }
      }
    }
  }


  // Draws a 5*5 alignment pattern, with the center module
  // at (x, y). All modules must be in bounds.
  private def drawAlignmentPattern(x: Int, y: Int): Unit = {
    for (dy <- -2 to 2) {
      for (dx <- -2 to 2) {
        setFunctionModule(x + dx, y + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1)
      }
    }
  }


  // Sets the color of a module and marks it as a function module.
  // Only used by the constructor. Coordinates must be in bounds.
  private def setFunctionModule(x: Int, y: Int, isDark: Boolean): Unit = {
    modules(y)(x) = isDark
    isFunction(y)(x) = true
  }


  // Returns a new byte string representing the given data with the appropriate error correction
  // codewords appended to it, based on this object's version and error correction level.
  private def addEccAndInterleave(data: Array[Byte]): Array[Byte] = {
    Objects.requireNonNull(data)
    if (data.length != getNumDataCodewords(version, errorCorrectionLevel)) {
      throw new IllegalArgumentException
    }
    // Calculate parameter numbers
    val numBlocks = NUM_ERROR_CORRECTION_BLOCKS(errorCorrectionLevel.ordinal)(version)
    val blockEccLen = ECC_CODEWORDS_PER_BLOCK(errorCorrectionLevel.ordinal)(version)
    val rawCodewords = getNumRawDataModules(version) / 8
    val numShortBlocks = numBlocks - rawCodewords % numBlocks
    val shortBlockLen = rawCodewords / numBlocks
    // Split data into blocks and append ECC to each block
    val blocks = new Array[Array[Byte]](numBlocks)
    val rsDiv = reedSolomonComputeDivisor(blockEccLen)
    var i = 0
    var k = 0
    while (i < numBlocks) {
      val dat = Arrays.copyOfRange(
        data,
        k,
        k + shortBlockLen - blockEccLen + (if (i < numShortBlocks) 0 else 1)
      )
      k += dat.length
      val block = Arrays.copyOf(dat, shortBlockLen + 1)
      val ecc = reedSolomonComputeRemainder(dat, rsDiv)
      System.arraycopy(ecc, 0, block, block.length - blockEccLen, ecc.length)
      blocks(i) = block

      i += 1
    }
    // Interleave (not concatenate) the bytes from every block into a single sequence
    val result = new Array[Byte](rawCodewords)

    i = 0
    k = 0
    while (i < blocks(0).length) {
      for (j <- blocks.indices) {
        // Skip the padding byte in short blocks
        if (i != shortBlockLen - blockEccLen || j >= numShortBlocks) {
          result(k) = blocks(j)(i)
          k += 1
        }
      }
      i += 1
    }
    result
  }


  // Draws the given sequence of 8-bit codewords (data and error correction) onto the entire
  // data area of this QR Code. Function modules need to be marked off before this is called.
  private def drawCodewords(data: Array[Byte]): Unit = {
    Objects.requireNonNull(data)
    if (data.length != getNumRawDataModules(version) / 8) {
      throw new IllegalArgumentException
    }
    var i = 0 // Bit index into the data

    // Do the funny zigzag scan
    var right = size - 1
    while (right >= 1) { // Index of right column in each column pair
      if (right == 6) right = 5
      var vert = 0
      while (vert < size) { // Vertical counter
        for (j <- 0 until 2) {
          val x = right - j // Actual x coordinate

          val upward = ((right + 1) & 2) == 0
          val y = if (upward) size - 1 - vert
          else vert // Actual y coordinate

          if (!isFunction(y)(x) && i < data.length * 8) {
            modules(y)(x) = getBit(data(i >>> 3), 7 - (i & 7))
            i += 1
          }
        }

        vert += 1
      }

      right -= 2
    }
    assert(i == data.length * 8)
  }


  // XORs the codeword modules in this QR Code with the given mask pattern.
  // The function modules must be marked and the codeword bits must be drawn
  // before masking. Due to the arithmetic of XOR, calling applyMask() with
  // the same mask value a second time will undo the mask. A final well-formed
  // QR Code needs exactly one (not zero, two, etc.) mask applied.
  private def applyMask(msk: Int): Unit = {
    if (msk < 0 || msk > 7) {
      throw new IllegalArgumentException("Mask value out of range")
    }
    var y = 0
    while (y < size) {
      var x = 0
      while (x < size) {
        val invert = msk match {
          case 0 =>
            (x + y) % 2 == 0
          case 1 =>
            y % 2 == 0
          case 2 =>
            x % 3 == 0
          case 3 =>
            (x + y) % 3 == 0
          case 4 =>
            (x / 3 + y / 2) % 2 == 0
          case 5 =>
            x * y % 2 + x * y % 3 == 0
          case 6 =>
            (x * y % 2 + x * y % 3) % 2 == 0
          case 7 =>
            ((x + y) % 2 + x * y % 3) % 2 == 0
          case _ =>
            throw new AssertionError
        }
        modules(y)(x) ^= invert & !isFunction(y)(x)

        x += 1
      }

      y += 1
    }
  }


  // Calculates and returns the penalty score based on state of this QR Code's current modules.
  // This is used by the automatic mask choice algorithm to find the mask pattern that yields the lowest score.
  private def getPenaltyScore = {
    var result = 0
    // Adjacent modules in row having same color, and finder-like patterns
    var y = 0
    while (y < size) {
      var runColor = false
      var runX = 0
      val runHistory = new Array[Int](7)
      var x = 0
      while (x < size) {
        if (modules(y)(x) == runColor) {
          runX += 1
          if (runX == 5) result += PENALTY_N1
          else if (runX > 5) result += 1
        }
        else {
          finderPenaltyAddHistory(runX, runHistory)
          if (!runColor) result += finderPenaltyCountPatterns(runHistory) * PENALTY_N3
          runColor = modules(y)(x)
          runX = 1
        }

        x += 1
      }
      result += finderPenaltyTerminateAndCount(runColor, runX, runHistory) * PENALTY_N3

      y += 1
    }
    // Adjacent modules in column having same color, and finder-like patterns
    var x = 0
    while (x < size) {
      var runColor = false
      var runY = 0
      val runHistory = new Array[Int](7)
      var y = 0
      while (y < size) {
        if (modules(y)(x) == runColor) {
          runY += 1
          if (runY == 5) result += PENALTY_N1
          else if (runY > 5) result += 1
        }
        else {
          finderPenaltyAddHistory(runY, runHistory)
          if (!runColor) result += finderPenaltyCountPatterns(runHistory) * PENALTY_N3
          runColor = modules(y)(x)
          runY = 1
        }

        y += 1
      }
      result += finderPenaltyTerminateAndCount(runColor, runY, runHistory) * PENALTY_N3

      x += 1
    }
    // 2*2 blocks of modules having same color
    for (y <- 0 until (size - 1)) {
      for (x <- 0 until (size - 1)) {
        val color = modules(y)(x)
        if (color == modules(y)(x + 1) &&
          color == modules(y + 1)(x) &&
          color == modules(y + 1)(x + 1)) {
          result += PENALTY_N2
        }
      }
    }

    // Balance of dark and light modules
    var dark = 0
    for (row <- modules) {
      for (color <- row) {
        if (color) {
          dark += 1
        }
      }
    }
    val total = size * size // Note that size is odd, so dark/total != 1/2

    // Compute the smallest integer k >= 0 such that (45-5k)% <= dark/total <= (55+5k)%
    val k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1
    assert(0 <= k && k <= 9)
    result += k * PENALTY_N4
    assert(0 <= result && result <= 2568888) // Non-tight upper bound based on default values of PENALTY_N1, ..., N4

    result
  }

  // Returns an ascending list of positions of alignment patterns for this version number.
  // Each position is in the range [0,177), and are used on both the x and y axes.
  // This could be implemented as lookup table of 40 variable-length lists of unsigned bytes.
  private def getAlignmentPatternPositions(): Array[Int] = {
    if (version == 1)  {
      Array()
    } else {
      val numAlign = version / 7 + 2
      var step = 0
      if (version == 32) {// Special snowflake
        step = 26
      } else {
        step = (version * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2) * 2
      } // step = ceil[(size - 13) / (numAlign * 2 - 2)] * 2

      val result = new Array[Int](numAlign)
      result(0) = 6
      var i = result.length - 1
      var pos = size - 7
      while (i >= 1) {
        result(i) = pos
        i -= 1
        pos -= step
      }
      result
    }
  }


  // Can only be called immediately after a light run is added, and
  // returns either 0, 1, or 2. A helper function for getPenaltyScore().
  private def finderPenaltyCountPatterns(runHistory: Array[Int]): Int = {
    val n = runHistory(1)
    assert(n <= size * 3)
    val core = n > 0 && runHistory(2) == n && runHistory(3) == n * 3 && runHistory(4) == n && runHistory(5) == n
    (if (core && runHistory(0) >= n * 4 && runHistory(6) >= n) 1 else 0) +
      (if (core && runHistory(6) >= n * 4 && runHistory(0) >= n) 1 else 0)
  }


  // Must be called at the end of a line (row or column) of modules. A helper function for getPenaltyScore().
  private def finderPenaltyTerminateAndCount(currentRunColor: Boolean, currentRunLengthArg: Int, runHistory: Array[Int]): Int = {
    var currentRunLength = currentRunLengthArg
    if (currentRunColor) { // Terminate dark run
      finderPenaltyAddHistory(currentRunLength, runHistory)
      currentRunLength = 0
    }
    currentRunLength += size // Add light border to final run

    finderPenaltyAddHistory(currentRunLength, runHistory)
    finderPenaltyCountPatterns(runHistory)
  }


  // Pushes the given value to the front and drops the last value. A helper function for getPenaltyScore().
  private def finderPenaltyAddHistory(currentRunLengthArg: Int, runHistory: Array[Int]): Unit = {
    var currentRunLength = currentRunLengthArg
    if (runHistory(0) == 0) {
      currentRunLength += size
    } // Add light border to initial run
    System.arraycopy(runHistory, 0, runHistory, 1, runHistory.length - 1)
    runHistory(0) = currentRunLength
  }

}
