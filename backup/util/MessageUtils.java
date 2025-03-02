package com.app.api.common.util;

import com.app.api.client.sms.dto.CreateSendSmsRequest.SmsType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageUtils {

  public static SmsType getMessageType(String message) {
    int byteLength = getByteLength(message);
    log.debug("## byteLength: {}", byteLength);

    if (byteLength <= 90) {
      return SmsType.SMS; // 단문 (90바이트 이하)
    } else if (byteLength <= 2000) {
      return SmsType.LMS; // 장문 (2000바이트 이하)
    } else {
      return SmsType.MMS; // 포토문자 (2000바이트 초과)
    }
  }

  private static int getByteLength(String message) {
    int byteLength = 0;
    for (char c : message.toCharArray()) {
      // 한글은 2바이트, 영문/숫자/특수문자는 1바이트로 계산
      if (Character.toString(c).getBytes().length > 1) {
        byteLength += 2;
      } else {
        byteLength += 1;
      }
    }
    return byteLength;
  }
}
