package com.app.api.common.mybatis;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;

public class CalculateForPeriodProvider {

  /**
   * 기간별 정산목록 요약
   */
  public String listCalculateSummaryForPeriod(
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("creatorName") String creatorName) {

    SQL sql = new SQL();

    // 첫 번째 SELECT 쿼리
    sql.SELECT("u.name AS creatorName",
            "SUM(p.payment_amount) AS paymentAmount",
            "SUM(CASE WHEN p.payment_amount < 0 THEN -p.payment_amount ELSE 0 END) AS refundAmount")
        .FROM("order_ o")
        .JOIN("payment p ON o.id = p.order_id")
        .JOIN("user_ u ON u.id = o.created_by")
        .WHERE("p.payment_date BETWEEN #{dateFrom} AND #{dateTo}");

    if (creatorName != null && !creatorName.isEmpty()) {
      sql.WHERE("u.name = #{creatorName}");
    }

    // GROUP BY 절 추가
    sql.GROUP_BY("u.name");

    // 최종 SQL 쿼리 반환
    return sql.toString() + " ORDER BY creatorName ASC";
  }

  /**
   * 기간별 정산목록 주문
   */
  public String listCalculateOrderForPeriod(
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("creatorName") String creatorName) {
    return new SQL() {{
      SELECT_DISTINCT("o.id AS id",
          "u1.name AS creatorName",
          "CONCAT(pd.name, '/', op.months, '개월', '/', op.quantity, pd.quantity_unit) AS orderProductName",
          "u.name AS userName",
          "o.billing_amount AS billingAmount",
          "o.payment_amount AS paymentAmount",
          "r.refund_amount AS refundAmount",
          "o.receivable_amount AS receivableAmount",
          "o.created_on AS createdOn");

      FROM("order_ o");
      JOIN("user_ u ON u.id = o.user_id");
      JOIN("user_ u1 ON u1.id = o.created_by");
      JOIN("payment p ON o.id = p.order_id AND p.payment_date BETWEEN #{dateFrom} AND #{dateTo}");
      JOIN("order_product op ON o.id = op.order_id");
      JOIN("product pd ON op.product_id = pd.id");
      LEFT_OUTER_JOIN("refund r ON r.order_product_id = o.id");

      WHERE("p.order_id IS NOT NULL");

      if (creatorName != null && !creatorName.isEmpty()) {
        WHERE("u1.name = #{creatorName}");
      }

      ORDER_BY("o.created_on");
    }}.toString();
  }

  /**
   * 기간별 정산목록 결제
   */
  public String listCalculatePaymentForPeriod(
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("creatorName") String creatorName) {
    return new SQL() {{
      SELECT("o.id AS orderId",
          "p.id AS paymentId",
          "u1.name AS creatorName",
          "p.created_on AS createdOn",
          "p.type AS paymentType",
          "p.transaction_name AS transactionName",
          "p.installment_months AS installmentMonths",
          "p.card_number AS cardNumber",
          "p.payment_amount AS paymentAmount",
          "0 as refundAmount");
      FROM("order_ o");
      JOIN("user_ u ON u.id = o.user_id");
      JOIN("user_ u1 ON u1.id = o.created_by");
      JOIN("payment p ON o.id = p.order_id AND p.payment_date BETWEEN #{dateFrom} AND #{dateTo}");

      if (creatorName != null && !creatorName.isEmpty()) {
        WHERE("u1.name = #{creatorName}");
      }

      ORDER_BY("o.created_on");
    }}.toString();
  }
}
