package sol.fv.qr

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import java.util.Objects
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonLocalReturns.{returning, throwReturn}

// Describes precisely all strings that are encodable in numeric mode.
val NUMERIC_REGEX = Pattern.compile("[0-9]*")

// Describes precisely all strings that are encodable in alphanumeric mode.
val ALPHANUMERIC_REGEX = Pattern.compile("[A-Z0-9 $%*+./:-]*")

// The set of all legal characters in alphanumeric mode, where
// each character value maps to the index in the string.
val ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"


enum Mode(val modeBits: Int, val numBitsCharCount: Int*):
  case NUMERIC extends Mode    (0x1, 10, 12, 14)
  case ALPHANUMERIC extends Mode (0x2,  9, 11, 13)
  case BYTE         extends Mode (0x4,  8, 16, 16)
  case KANJI        extends Mode (0x8,  8, 10, 12)
  case ECI          extends Mode (0x7,  0,  0,  0)

  def numCharCountBits(ver: Int): Int = {
    assert(QrCode.MIN_VERSION <= ver && ver <= QrCode.MAX_VERSION)
    numBitsCharCount((ver + 7) / 17)
  }

object QrSegment {

  /**
   * Returns a segment representing the specified binary data
   * encoded in byte mode. All input byte arrays are acceptable.
   * <p>Any text string can be converted to UTF-8 bytes ({@code
   * s.getBytes(StandardCharsets.UTF_8)}) and encoded as a byte mode segment.</p>
   * @param data the binary data (not {@code null})
   *
   * @return a segment (not {@code null}) containing the data
   * @throws NullPointerException if the array is {@code null}
   */
  def makeBytes(data: Array[Byte]): QrSegment = {
    val bb = new BitBuffer
    for (b <- data) {
      bb.appendBits(b & 0xFF, 8)
    }
    new QrSegment(Mode.BYTE, data.length, bb)
  }

  /**
   * Returns a segment representing the specified string of decimal digits encoded in numeric mode.
   *
   * @param digits the text (not {@code null}), with only digits from 0 to 9 allowed
   * @return a segment (not {@code null}) containing the text
   * @throws NullPointerException     if the string is {@code null}
   * @throws IllegalArgumentException if the string contains non-digit characters
   */
  def makeNumeric(digits: CharSequence): QrSegment = {
    Objects.requireNonNull(digits)
    if (!isNumeric(digits)) throw new IllegalArgumentException("String contains non-numeric characters")
    val bb = new BitBuffer
    var i = 0
    while (i < digits.length) { // Consume up to 3 digits per iteration
      val n = Math.min(digits.length - i, 3)
      bb.appendBits(digits.subSequence(i, i + n).toString.toInt, n * 3 + 1)
      i += n
    }
    new QrSegment(Mode.NUMERIC, digits.length, bb)
  }

  /**
   * Returns a segment representing the specified text string encoded in alphanumeric mode.
   * The characters allowed are: 0 to 9, A to Z (uppercase only), space,
   * dollar, percent, asterisk, plus, hyphen, period, slash, colon.
   *
   * @param text the text (not {@code null}), with only certain characters allowed
   * @return a segment (not {@code null}) containing the text
   * @throws NullPointerException     if the string is {@code null}
   * @throws IllegalArgumentException if the string contains non-encodable characters
   */
  def makeAlphanumeric(text: CharSequence): QrSegment = {
    Objects.requireNonNull(text)
    if (!isAlphanumeric(text)) throw new IllegalArgumentException("String contains unencodable characters in alphanumeric mode")
    val bb = new BitBuffer
    var i = 0
    i = 0
    while (i <= text.length - 2) { // Process groups of 2
      var temp = ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)) * 45
      temp += ALPHANUMERIC_CHARSET.indexOf(text.charAt(i + 1))
      bb.appendBits(temp, 11)

      i += 2
    }
    if (i < text.length) bb.appendBits(ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)), 6) // 1 character remaining

    new QrSegment(Mode.ALPHANUMERIC, text.length, bb)
  }

  /**
   * Returns a list of zero or more segments to represent the specified Unicode text string.
   * The result may use various segment modes and switch modes to optimize the length of the bit stream.
   *
   * @param text the text to be encoded, which can be any Unicode string
   * @return a new mutable list (not {@code null}) of segments (not {@code null}) containing the text
   * @throws NullPointerException if the text is {@code null}
   */
  def makeSegments(text: CharSequence): List[QrSegment] = {
    Objects.requireNonNull(text)
    // Select the most efficient segment encoding automatically
    val result: ArrayBuffer[QrSegment] = ArrayBuffer()
    if (text == "") {
      // Leave result empty
    } else if (isNumeric(text)) {
      result += makeNumeric(text)
    } else if (isAlphanumeric(text)) {
      result += makeAlphanumeric(text)
    } else {
      result += makeBytes(text.toString.getBytes(StandardCharsets.UTF_8))
    }
    result.toList
  }

  /**
   * Returns a segment representing an Extended Channel Interpretation
   * (ECI) designator with the specified assignment value.
   *
   * @param assignVal the ECI assignment number (see the AIM ECI specification)
   * @return a segment (not {@code null}) containing the data
   * @throws IllegalArgumentException if the value is outside the range [0, 10<sup>6</sup>)
   */
  def makeEci(assignVal: Int): QrSegment = {
    val bb = BitBuffer()
    if (assignVal < 0) throw new IllegalArgumentException("ECI assignment value out of range")
    else if (assignVal < (1 << 7)) bb.appendBits(assignVal, 8)
    else if (assignVal < (1 << 14)) {
      bb.appendBits(Integer.parseInt("10", 2), 2)
      bb.appendBits(assignVal, 14)
    }
    else if (assignVal < 1_000_000) {
      bb.appendBits(Integer.parseInt("110", 2), 3)
      bb.appendBits(assignVal, 21)
    }
    else throw new IllegalArgumentException("ECI assignment value out of range")
    new QrSegment(Mode.ECI, 0, bb)
  }


  /**
   * Tests whether the specified string can be encoded as a segment in numeric mode.
   * A string is encodable iff each character is in the range 0 to 9.
   *
   * @param text the string to test for encodability (not {@code null})
   * @return {@code true} iff each character is in the range 0 to 9.
   * @throws NullPointerException if the string is {@code null}
   * @see #makeNumeric(CharSequence)
   */
  def isNumeric(text: CharSequence): Boolean = NUMERIC_REGEX.matcher(text).matches


  /**
   * Tests whether the specified string can be encoded as a segment in alphanumeric mode.
   * A string is encodable iff each character is in the following set: 0 to 9, A to Z
   * (uppercase only), space, dollar, percent, asterisk, plus, hyphen, period, slash, colon.
   *
   * @param text the string to test for encodability (not {@code null})
   * @return {@code true} iff each character is in the alphanumeric mode character set
   * @throws NullPointerException if the string is {@code null}
   * @see #makeAlphanumeric(CharSequence)
   */
  def isAlphanumeric(text: CharSequence): Boolean = ALPHANUMERIC_REGEX.matcher(text).matches


  // Calculates the number of bits needed to encode the given segments at the given version.
  // Returns a non-negative number if successful. Otherwise returns -1 if a segment has too
  // many characters to fit its length field, or the total bits exceeds Integer.MAX_VALUE.
  def getTotalBits(segs: List[QrSegment], version: Int): Int = {
    returning {
      var result: Long = 0L
      for (seg <- segs) {
        Objects.requireNonNull(seg)
        val ccbits = seg.mode.numCharCountBits(version)
        if (seg.numChars >= (1 << ccbits)) {
          throwReturn(-1)
        } // The segment's length doesn't fit the field's bit width
        result += 4L + ccbits + seg.data.bitLength()
        if (result > Integer.MAX_VALUE) {
          throwReturn(-1)
        } // The sum will overflow an int type
      }
      result.toInt
    }
  }

  def apply(mode: Mode, numChars: Int, data: BitBuffer): QrSegment = {
    if (numChars < 0) {
      throw new IllegalArgumentException(s"Invalid value $numChars")
    }
    new QrSegment(mode, numChars, data.copy())
  }
}

class QrSegment private
  (
    /** The mode indicator of this segment. */
    val mode: Mode,

    /** The length of this segment's unencoded data. Measured in characters for
     * numeric/alphanumeric/kanji mode, bytes for byte mode, and 0 for ECI mode.
     * Always zero or positive. Not the same as the data's bit length. */
    val numChars: Int,

    // The data bits of this segment. Accessed through getData().
    val data: BitBuffer
  )