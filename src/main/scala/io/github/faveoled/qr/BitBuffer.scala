package io.github.faveoled.qr

import java.util.BitSet

class BitBuffer {

  private var data: BitSet = new BitSet()
  private var _bitLength = 0 // Non-negative

  def bitLength(): Int = {
    assert(_bitLength >= 0)
    _bitLength
  }

  def getBit(index: Int): Int = {
    if (index < 0 || index >= _bitLength)
      throw new IndexOutOfBoundsException
    if (data.get(index)) 1 else 0
  }

  def appendBits(value: Int, len: Int): Unit = {
    if (len < 0 || len > 31 || value >>> len != 0)
      throw new IllegalArgumentException("Value out of range")
    if (Integer.MAX_VALUE - _bitLength < len)
      throw new IllegalStateException("Maximum length reached")
    var i = len - 1
    while (i >= 0) {
      data.set(_bitLength, QrCode.getBit(value, i))
      _bitLength += 1 // Append bit by bit
      i -= 1
    }
  }

  def appendData(bb: BitBuffer): Unit = {
    if (Integer.MAX_VALUE - _bitLength < bb._bitLength)
      throw new IllegalStateException("Maximum length reached")
    var i = 0
    while (i < bb._bitLength) {
      data.set(_bitLength, bb.data.get(i))
      _bitLength += 1 // Append bit by bit
      i += 1
    }
  }
  def copy(): BitBuffer =
    val res = BitBuffer()
    res.data = this.data.clone.asInstanceOf[BitSet]
    res._bitLength = this._bitLength
    res

}
