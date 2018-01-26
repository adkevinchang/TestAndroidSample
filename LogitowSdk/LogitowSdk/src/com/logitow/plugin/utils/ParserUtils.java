package com.logitow.plugin.utils;

public final class ParserUtils
{
  public static long decodeHalfUuid(byte[] paramArrayOfByte, int paramInt)
  {
    return ((unsignedByteToLong(paramArrayOfByte[paramInt]) << 56) + (unsignedByteToLong(paramArrayOfByte[(paramInt + 1)]) << 48) + (unsignedByteToLong(paramArrayOfByte[(paramInt + 2)]) << 40) + (unsignedByteToLong(paramArrayOfByte[(paramInt + 3)]) << 32) + (unsignedByteToLong(paramArrayOfByte[(paramInt + 4)]) << 24) + (unsignedByteToLong(paramArrayOfByte[(paramInt + 5)]) << 16) + (unsignedByteToLong(paramArrayOfByte[(paramInt + 6)]) << 8) + unsignedByteToLong(paramArrayOfByte[(paramInt + 7)]));
  }

  public static int decodeUint16LittleEndian(byte[] paramArrayOfByte, int paramInt)
  {
    int i = 0xFF & paramArrayOfByte[paramInt];
    return (0xFF & paramArrayOfByte[(paramInt + 1)] | i << 8);
  }

  public static int unsignedByteToInt(byte paramByte)
  {
    return (paramByte & 0xFF);
  }

  public static long unsignedByteToLong(byte paramByte)
  {
    return (paramByte & 0xFF);
  }
}