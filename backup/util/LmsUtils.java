package com.app.api.common.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;

public interface LmsUtils {

  String ID_PREFIX_USER = "M";
  String ID_PREFIX_ORDER = "O";
  String ID_PREFIX_ORDER_PRODUCT = "I";
  String ID_PREFIX_PAYMENT = "P";
  String ID_PREFIX_REFUND = "R";
  String ID_PREFIX_PRODUCT = "P";

  static String createUserId() {

    return createId(ID_PREFIX_USER);
  }

  static String createOrderId() {
    return createId(ID_PREFIX_ORDER);
  }

  static String createOrderProductId() {
    return createId(ID_PREFIX_ORDER_PRODUCT);
  }

  static String createPaymentId() {
    return createId(ID_PREFIX_PAYMENT);
  }

  static String createRefundId() {
    return createId(ID_PREFIX_REFUND);
  }

  static String createProductId(){return createId(ID_PREFIX_PRODUCT);}

  static String createId(String prefix) {
    return prefix + System.currentTimeMillis() + StringUtils.leftPad(
        Integer.toString((int) (Math.random() * 1000)), 3, "0");
  }

  static String encryptPassword(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }

  static boolean checkPassword(String password, String hashedPassword) {

    return BCrypt.checkpw(password, hashedPassword);
  }

  static long loginMaxAge() {
    return 60 * 60 * 24 * 365;
  }

  static long loginMaxAgeMillis() {
    return loginMaxAge() * 1000;
  }
}
